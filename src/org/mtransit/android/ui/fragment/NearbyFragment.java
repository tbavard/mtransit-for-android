package org.mtransit.android.ui.fragment;

import java.lang.ref.WeakReference;
import java.util.List;

import org.mtransit.android.R;
import org.mtransit.android.commons.BundleUtils;
import org.mtransit.android.commons.CollectionUtils;
import org.mtransit.android.commons.LocationUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.PreferenceUtils;
import org.mtransit.android.commons.StringUtils;
import org.mtransit.android.commons.task.MTAsyncTask;
import org.mtransit.android.data.DataSourceProvider;
import org.mtransit.android.data.DataSourceType;
import org.mtransit.android.task.StatusLoader;
import org.mtransit.android.ui.MTActivityWithLocation;
import org.mtransit.android.ui.MainActivity;
import org.mtransit.android.ui.view.SlidingTabLayout;

import android.app.Activity;
import android.content.Context;
import android.location.Address;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;

public class NearbyFragment extends ABFragment implements ViewPager.OnPageChangeListener, MTActivityWithLocation.UserLocationListener,
		SwipeRefreshLayout.OnRefreshListener {

	private static final String TAG = NearbyFragment.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}


	private static final String EXTRA_NEARBY_LOCATION = "extra_nearby_location";

	public static NearbyFragment newInstance(Location nearbyLocationOpt) {
		NearbyFragment f = new NearbyFragment();
		Bundle args = new Bundle();
		if (nearbyLocationOpt != null) {
			args.putParcelable(EXTRA_NEARBY_LOCATION, nearbyLocationOpt);
		}
		f.setArguments(args);
		return f;
	}

	private int lastPageSelected = -1;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		final View view = inflater.inflate(R.layout.fragment_nearby, container, false);
		setupView(view);
		return view;
	}

	@Override
	public void onRefresh() {
		initiateRefresh();
	}

	private void initiateRefresh() {
		if (LocationUtils.areAlmostTheSame(this.nearbyLocation, this.userLocation, 2)) {
			setSwipeRefreshLayoutRefreshing(false);
			return;
		}
		// broadcast reset nearby location to all fragments
		final List<Fragment> fragments = getChildFragmentManager().getFragments();
		if (fragments != null) {
			for (Fragment fragment : fragments) {
				if (fragment != null && fragment instanceof NearbyFragment.NearbyLocationListener) {
					((NearbyFragment.NearbyLocationListener) fragment).onNearbyLocationChanged(null);
				}
			}
		}
		// set new nearby location is current user location interesting
		setNewNearbyLocation(this.userLocation);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		restoreInstanceState(savedInstanceState);
		switchView();
	}

	private void restoreInstanceState(Bundle savedInstanceState) {
		final Location nearbyLocation = BundleUtils.getParcelable(EXTRA_NEARBY_LOCATION, savedInstanceState, getArguments());
		setNewNearbyLocation(nearbyLocation);
	}

	@Override
	public void onResume() {
		super.onResume();
		onUserLocationChanged(((MTActivityWithLocation) getActivity()).getLastLocation());
		if (this.lastPageSelected >= 0) {
			onPageSelected(this.lastPageSelected); // tell current page it's selected
		}
	}

	private AgencyTypePagerAdapter adapter;

	private Location nearbyLocation;
	protected String nearbyLocationAddress;
	private Location userLocation;
	private boolean userAwayFromNearbyLocation = true;

	private MTAsyncTask<Location, Void, String> findNearbyLocationTask;

	private void showNewNearbyLocation() {
		if (this.nearbyLocationAddress != null && this.nearbyLocation != null) {
			final FragmentActivity activity = getActivity();
			if (activity != null) {
				((MainActivity) getActivity()).notifyABChange();
			}
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		this.adapter = null;
		if (this.findNearbyLocationTask != null) {
			this.findNearbyLocationTask.cancel(true);
			this.findNearbyLocationTask = null;
		}
	}

	private void findNearbyLocation() {
		if (this.findNearbyLocationTask != null) {
			this.findNearbyLocationTask.cancel(true);
		}
		this.findNearbyLocationTask = new MTAsyncTask<Location, Void, String>() {

			@Override
			public String getLogTag() {
				return NearbyFragment.this.getLogTag();
			}

			@Override
			protected String doInBackgroundMT(Location... locations) {
				final Activity activity = getActivity();
				final Location nearbyLocation = locations[0];
				if (activity == null || nearbyLocation == null) {
					return null;
				}
				Address address = LocationUtils.getLocationAddress(activity, nearbyLocation);
				if (address == null) {
					return null;
				}
				return LocationUtils.getLocationString(activity, null, address, nearbyLocation.getAccuracy());
			}

			@Override
			protected void onPostExecute(String result) {
				boolean refreshRequired = NearbyFragment.this.nearbyLocationAddress == null;
				NearbyFragment.this.nearbyLocationAddress = result;
				if (refreshRequired) {
					showNewNearbyLocation();
				}
			}

		};
		this.findNearbyLocationTask.execute(this.nearbyLocation);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		if (this.nearbyLocation != null) {
			outState.putParcelable(EXTRA_NEARBY_LOCATION, this.nearbyLocation);
		}
		super.onSaveInstanceState(outState);
	}

	private void initTabsAndViewPager() {
		final List<DataSourceType> availableAgencyTypes = DataSourceProvider.get().getAvailableAgencyTypes(getActivity());
		if (CollectionUtils.getSize(availableAgencyTypes) == 0) {
			return;
		}
		this.adapter = new AgencyTypePagerAdapter(this, availableAgencyTypes);
		this.adapter.setNearbyLocation(this.nearbyLocation);
		setupView(getView());
		final ViewPager viewPager = (ViewPager) getView().findViewById(R.id.viewpager);
		this.lastPageSelected = 0;
		new MTAsyncTask<Void, Void, Integer>() {

			private final String TAG = NearbyFragment.class.getSimpleName() + ">LoadLastPageSelectedFromUserPreferences";

			public String getLogTag() {
				return TAG;
			}

			@Override
			protected Integer doInBackgroundMT(Void... params) {
				try {
					final int typeId = PreferenceUtils.getPrefLcl(getActivity(), PreferenceUtils.PREFS_LCL_NEARBY_TAB_TYPE,
							PreferenceUtils.PREFS_LCL_NEARBY_TAB_TYPE_DEFAULT);
					if (typeId >= 0) {
						for (int i = 0; i < availableAgencyTypes.size(); i++) {
							if (availableAgencyTypes.get(i).getId() == typeId) {
								return i;
							}
						}
					}
				} catch (Exception e) {
					MTLog.w(TAG, e, "Error while determining the select nearby tab!");
				}
				return null;
			}

			@Override
			protected void onPostExecute(Integer lastPageSelected) {
				if (NearbyFragment.this.lastPageSelected != 0) {
					return; // user has manually move to another page before, too late
				}
				if (lastPageSelected != null) {
					NearbyFragment.this.lastPageSelected = lastPageSelected.intValue();
					viewPager.setCurrentItem(NearbyFragment.this.lastPageSelected);
				}
				switchView();
				onPageSelected(NearbyFragment.this.lastPageSelected); // tell current page it's selected
			}
		}.execute();
	}

	private void setupView(View view) {
		if (view == null || this.adapter == null) {
			return;
		}
		final ViewPager viewPager = (ViewPager) view.findViewById(R.id.viewpager);
		viewPager.setAdapter(this.adapter);
		viewPager.setOffscreenPageLimit(3);
		SlidingTabLayout tabs = (SlidingTabLayout) view.findViewById(R.id.tabs);
		tabs.setViewPager(viewPager);
		tabs.setOnPageChangeListener(this);
		tabs.setSelectedIndicatorColors(0xff666666);
	}

	@Override
	public void onUserLocationChanged(Location newLocation) {
		if (newLocation != null) {
			this.userLocation = newLocation;
			final List<Fragment> fragments = getChildFragmentManager().getFragments();
			if (fragments != null) {
				for (Fragment fragment : fragments) {
					if (fragment != null && fragment instanceof MTActivityWithLocation.UserLocationListener) {
						((MTActivityWithLocation.UserLocationListener) fragment).onUserLocationChanged(this.userLocation);
					}
				}
			}
			if (this.adapter != null) {
				this.adapter.setUserLocation(newLocation);
			}
			if (this.nearbyLocation == null) {
				setNewNearbyLocation(newLocation);
			}
			boolean requireNotifyAB = false;
			if (LocationUtils.areAlmostTheSame(this.nearbyLocation, this.userLocation)) {
				if (this.userAwayFromNearbyLocation) {
					requireNotifyAB = true;
					this.userAwayFromNearbyLocation = false;
				}
			} else {
				if (!this.userAwayFromNearbyLocation) {
					requireNotifyAB = true;
					this.userAwayFromNearbyLocation = true;
				}
			}
			if (requireNotifyAB) {
				((MainActivity) getActivity()).notifyABChange();
			}
		}
	}

	private void setNewNearbyLocation(Location newNearbyLocation) {
		if (newNearbyLocation == null) {
			return;
		}
		this.nearbyLocation = newNearbyLocation;
		this.nearbyLocationAddress = null;
		((MainActivity) getActivity()).notifyABChange();
		if (this.adapter == null) {
			initTabsAndViewPager();
		}
		final List<Fragment> fragments = getChildFragmentManager().getFragments();
		if (fragments != null) {
			for (Fragment fragment : fragments) {
				if (fragment != null && fragment instanceof NearbyFragment.NearbyLocationListener) {
					((NearbyFragment.NearbyLocationListener) fragment).onNearbyLocationChanged(this.nearbyLocation);
				}
			}
		}
		findNearbyLocation();
		setSwipeRefreshLayoutRefreshing(false);
	}

	public Location getNearbyLocation() {
		return nearbyLocation;
	}

	public Location getUserLocation() {
		return userLocation;
	}

	private void switchView() {
		if (this.adapter == null) {
			showLoading();
		} else if (this.adapter.getCount() > 0) {
			showTabsAndViewPager();
		} else {
			showEmpty();
		}
	}

	private void showTabsAndViewPager() {
		// loading
		if (getView().findViewById(R.id.loading) != null) { // IF inflated/present DO
			getView().findViewById(R.id.loading).setVisibility(View.GONE); // hide
		}
		// empty
		if (getView().findViewById(R.id.empty) != null) { // IF inflated/present DO
			getView().findViewById(R.id.empty).setVisibility(View.GONE); // hide
		}
		// tabs
		if (getView().findViewById(R.id.tabs) == null) { // IF NOT present/inflated DO
			// ((ViewStub) getView().findViewById(R.id.tabs_stub)).inflate(); // inflate
		}
		getView().findViewById(R.id.tabs).setVisibility(View.VISIBLE); // show
		// view pager
		if (getView().findViewById(R.id.viewpager) == null) { // IF NOT present/inflated DO
			// ((ViewStub) getView().findViewById(R.id.viewpager_stub)).inflate(); // inflate
		}
		getView().findViewById(R.id.viewpager).setVisibility(View.VISIBLE); // show
	}

	private void showLoading() {
		// tabs
		if (getView().findViewById(R.id.tabs) != null) { // IF inflated/present DO
			getView().findViewById(R.id.tabs).setVisibility(View.GONE); // hide
		}
		// view pager
		if (getView().findViewById(R.id.viewpager) != null) { // IF inflated/present DO
			getView().findViewById(R.id.viewpager).setVisibility(View.GONE); // hide
		}
		// empty
		if (getView().findViewById(R.id.empty) != null) { // IF inflated/present DO
			getView().findViewById(R.id.empty).setVisibility(View.GONE); // hide
		}
		// loading
		if (getView().findViewById(R.id.loading) == null) { // IF NOT present/inflated DO
			((ViewStub) getView().findViewById(R.id.loading_stub)).inflate(); // inflate
		}
		getView().findViewById(R.id.loading).setVisibility(View.VISIBLE); // show
	}

	private void showEmpty() {
		// tabs
		if (getView().findViewById(R.id.tabs) != null) { // IF inflated/present DO
			getView().findViewById(R.id.tabs).setVisibility(View.GONE); // hide
		}
		// view pager
		if (getView().findViewById(R.id.viewpager) != null) { // IF inflated/present DO
			getView().findViewById(R.id.viewpager).setVisibility(View.GONE); // hide
		}
		// loading
		if (getView().findViewById(R.id.loading) != null) { // IF inflated/present DO
			getView().findViewById(R.id.loading).setVisibility(View.GONE); // hide
		}
		// empty
		if (getView().findViewById(R.id.empty) == null) { // IF NOT present/inflated DO
			((ViewStub) getView().findViewById(R.id.empty_stub)).inflate(); // inflate
		}
		getView().findViewById(R.id.empty).setVisibility(View.VISIBLE); // show
	}

	@Override
	public void onPageSelected(int position) {
		StatusLoader.get().clearAllTasks();
		PreferenceUtils.savePrefLcl(getActivity(), PreferenceUtils.PREFS_LCL_NEARBY_TAB_TYPE, this.adapter.getTypeId(position), false);
		final List<Fragment> fragments = getChildFragmentManager().getFragments();
		if (fragments != null) {
			for (Fragment fragment : fragments) {
				if (fragment instanceof NearbyAgencyTypeFragment) {
					final NearbyAgencyTypeFragment nearbyAgencyTypeFragment = (NearbyAgencyTypeFragment) fragment;
					nearbyAgencyTypeFragment.setNearbyFragment(this);
					nearbyAgencyTypeFragment.setFragmentVisisbleAtPosition(position);
				}
			}
		}
		this.lastPageSelected = position;
		if (this.adapter != null) {
			this.adapter.setLastVisisbleFragmentPosition(this.lastPageSelected);
		}
	}

	@Override
	public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
	}

	@Override
	public void onPageScrollStateChanged(int state) {
		MTLog.v(this, "onPageScrollStateChanged(%s)", state);
		switch (state) {
		case ViewPager.SCROLL_STATE_IDLE:
			List<Fragment> fragments = getChildFragmentManager().getFragments();
			if (fragments != null) {
				for (Fragment fragment : fragments) {
					if (fragment instanceof NearbyAgencyTypeFragment) {
						final NearbyAgencyTypeFragment nearbyAgencyTypeFragment = (NearbyAgencyTypeFragment) fragment;
						nearbyAgencyTypeFragment.setNearbyFragment(this);
						nearbyAgencyTypeFragment.setFragmentVisisbleAtPosition(this.lastPageSelected); // resume
					}
				}
			}
			break;
		case ViewPager.SCROLL_STATE_DRAGGING:
			List<Fragment> fragments2 = getChildFragmentManager().getFragments();
			if (fragments2 != null) {
				for (Fragment fragment : fragments2) {
					if (fragment instanceof NearbyAgencyTypeFragment) {
						final NearbyAgencyTypeFragment nearbyAgencyTypeFragment = (NearbyAgencyTypeFragment) fragment;
						nearbyAgencyTypeFragment.setNearbyFragment(this);
						nearbyAgencyTypeFragment.setFragmentVisisbleAtPosition(-1); // pause
					}
				}
			}
			break;
		}
	}


	private void setSwipeRefreshLayoutRefreshing(boolean refreshing) {
		final List<Fragment> fragments = getChildFragmentManager().getFragments();
		if (fragments != null) {
			for (Fragment fragment : fragments) {
				if (fragment instanceof NearbyAgencyTypeFragment) {
					final NearbyAgencyTypeFragment nearbyAgencyTypeFragment = (NearbyAgencyTypeFragment) fragment;
					nearbyAgencyTypeFragment.setNearbyFragment(this);
					nearbyAgencyTypeFragment.setSwipeRefreshLayoutRefreshing(refreshing);
				}
			}
		}
	}

	@Override
	public CharSequence getABTitle(Context context) {
		return context.getString(R.string.nearby);
	}

	@Override
	public CharSequence getSubtitle(Context context) {
		return this.nearbyLocationAddress; // no subtitle
	}

	@Override
	public int getABIconDrawableResId() {
		if (!this.userAwayFromNearbyLocation) {
			return R.drawable.ic_menu_nearby_active;
		} else {
			return R.drawable.ic_menu_nearby;
		}
	}

	@Override
	public Integer getBgColor() {
		return ABFragment.NO_BG_COLOR;
	}

	public static interface NearbyLocationListener extends MTActivityWithLocation.UserLocationListener {
		public void onNearbyLocationChanged(Location location);
	}

	private static class AgencyTypePagerAdapter extends FragmentStatePagerAdapter {

		private List<DataSourceType> availableAgencyTypes;
		private WeakReference<Context> contextWR;
		private Location nearbyLocation;
		private Location userLocation;
		private int lastVisisbleFragmentPosition = -1;
		private WeakReference<NearbyFragment> nearbyFragmentWR;

		public AgencyTypePagerAdapter(NearbyFragment nearbyFragment, List<DataSourceType> availableAgencyTypes) {
			super(nearbyFragment.getChildFragmentManager());
			this.contextWR = new WeakReference<Context>(nearbyFragment.getActivity());
			this.availableAgencyTypes = availableAgencyTypes;
			this.nearbyFragmentWR = new WeakReference<NearbyFragment>(nearbyFragment);
		}

		public int getTypeId(int position) {
			DataSourceType type = getType(position);
			if (type == null) {
				return 0;
			}
			return type.getId();
		}

		public DataSourceType getType(int position) {
			return this.availableAgencyTypes.size() == 0 ? null : this.availableAgencyTypes.get(position);
		}

		public void setNearbyLocation(Location nearbyLocation) {
			this.nearbyLocation = nearbyLocation;
		}

		public void setUserLocation(Location userLocation) {
			this.userLocation = userLocation;
		}

		public void setLastVisisbleFragmentPosition(int lastVisisbleFragmentPosition) {
			this.lastVisisbleFragmentPosition = lastVisisbleFragmentPosition;
		}

		@Override
		public int getCount() {
			return this.availableAgencyTypes == null ? 0 : this.availableAgencyTypes.size();
		}

		@Override
		public CharSequence getPageTitle(int position) {
			final Context context = this.contextWR == null ? null : this.contextWR.get();
			if (context == null) {
				return StringUtils.EMPTY;
			}
			if (this.availableAgencyTypes == null || position >= this.availableAgencyTypes.size()) {
				return StringUtils.EMPTY;
			}
			return context.getString(this.availableAgencyTypes.get(position).getShortNameResId());
		}

		@Override
		public Fragment getItem(int position) {
			final NearbyAgencyTypeFragment f = NearbyAgencyTypeFragment.newInstance(position, this.lastVisisbleFragmentPosition,
					this.availableAgencyTypes.get(position), this.nearbyLocation, this.userLocation);
			f.setNearbyFragment(this.nearbyFragmentWR == null ? null : this.nearbyFragmentWR.get());
			return f;
		}

	}

}
