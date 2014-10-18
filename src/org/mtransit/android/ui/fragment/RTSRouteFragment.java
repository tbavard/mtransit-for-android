package org.mtransit.android.ui.fragment;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Locale;

import org.mtransit.android.R;
import org.mtransit.android.commons.BundleUtils;
import org.mtransit.android.commons.ColorUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.PreferenceUtils;
import org.mtransit.android.commons.SpanUtils;
import org.mtransit.android.commons.StringUtils;
import org.mtransit.android.commons.UriUtils;
import org.mtransit.android.commons.data.Route;
import org.mtransit.android.commons.data.Trip;
import org.mtransit.android.commons.task.MTAsyncTask;
import org.mtransit.android.data.DataSourceProvider;
import org.mtransit.android.task.StatusLoader;
import org.mtransit.android.ui.MTActivityWithLocation;
import org.mtransit.android.ui.MainActivity;

import android.content.Context;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;

import com.viewpagerindicator.TitlePageIndicator;

public class RTSRouteFragment extends ABFragment implements ViewPager.OnPageChangeListener, MTActivityWithLocation.UserLocationListener {

	private static final String TAG = RTSRouteFragment.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	private static final String EXTRA_AUTHORITY = "extra_authority";

	private static final String EXTRA_ROUTE_ID = "extra_route_id";

	public static RTSRouteFragment newInstance(String authority, Route route) {
		final RTSRouteFragment f = new RTSRouteFragment();
		final Bundle args = new Bundle();
		args.putString(EXTRA_AUTHORITY, authority);
		args.putInt(EXTRA_ROUTE_ID, route.id);
		f.setArguments(args);
		return f;
	}

	private int lastPageSelected = -1;
	private Location userLocation;
	private RouteTripPagerAdapter adapter;
	private String authority;
	private Route route;
	private Integer routeId;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		final View view = inflater.inflate(R.layout.fragment_rts_route, container, false);
		setupView(view);
		return view;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		restoreInstanceState(savedInstanceState);
		switchView();
		if (this.adapter == null) {
			initTabsAndViewPager();
		}
	}

	private void restoreInstanceState(Bundle savedInstanceState) {
		this.routeId = BundleUtils.getInt(EXTRA_ROUTE_ID, savedInstanceState, getArguments());
		this.authority = BundleUtils.getString(EXTRA_AUTHORITY, savedInstanceState, getArguments());
	}

	private void initTabsAndViewPager() {
		if (TextUtils.isEmpty(this.authority) || this.routeId == null) {
			return;
		}
		final Uri authorityUri = UriUtils.newContentUri(this.authority);
		this.route = DataSourceProvider.findRTSRoute(getActivity(), authorityUri, this.routeId);
		setupTabTheme(getView());
		((MainActivity) getActivity()).notifyABChange();
		final List<Trip> routeTrips = DataSourceProvider.findRTSRouteTrips(getActivity(), authorityUri, this.routeId);
		if (routeTrips == null) {
			return;
		}
		this.adapter = new RouteTripPagerAdapter(this, routeTrips, this.authority);
		setupView(getView());
		final ViewPager viewPager = (ViewPager) getView().findViewById(R.id.viewpager);
		this.lastPageSelected = 0;
		new MTAsyncTask<Void, Void, Integer>() {

			private final String TAG = AgencyTypeFragment.class.getSimpleName() + ">LoadLastPageSelectedFromUserPreferences";

			public String getLogTag() {
				return TAG;
			}

			@Override
			protected Integer doInBackgroundMT(Void... params) {
				try {
					final int tripId = PreferenceUtils.getPrefLcl(getActivity(),
							PreferenceUtils.getPREFS_LCL_RTS_ROUTE_TRIP_ID_TAB(RTSRouteFragment.this.authority, RTSRouteFragment.this.routeId),
							PreferenceUtils.PREFS_LCL_RTS_ROUTE_TRIP_ID_TAB_DEFAULT);
					for (int i = 0; i < routeTrips.size(); i++) {
						if (routeTrips.get(i).id == tripId) {
							return i;
						}
					}
				} catch (Exception e) {
					MTLog.w(TAG, e, "Error while determining the select agency tab!");
				}
				return null;
			}

			@Override
			protected void onPostExecute(Integer lastPageSelected) {
				if (RTSRouteFragment.this.lastPageSelected != 0) {
					return; // user has manually move to another page before, too late
				}
				if (lastPageSelected != null) {
					RTSRouteFragment.this.lastPageSelected = lastPageSelected.intValue();
					viewPager.setCurrentItem(RTSRouteFragment.this.lastPageSelected);
				}
				switchView();
				onPageSelected(RTSRouteFragment.this.lastPageSelected); // tell current page it's selected
			}
		}.execute();
	}

	private void setupView(View view) {
		if (view == null || this.adapter == null) {
			return;
		}
		final ViewPager viewPager = (ViewPager) getView().findViewById(R.id.viewpager);
		viewPager.setAdapter(this.adapter);
		TitlePageIndicator tabs = (TitlePageIndicator) getView().findViewById(R.id.tabs);
		tabs.setViewPager(viewPager);
		tabs.setOnPageChangeListener(this);
	}

	private void setupTabTheme(View view) {
		if (view == null || this.route == null) {
			return;
		}
		TitlePageIndicator tabs = (TitlePageIndicator) getView().findViewById(R.id.tabs);
		final int bgColor = ColorUtils.parseColor(this.route.textColor);
		final int textColor = ColorUtils.parseColor(this.route.color);
		tabs.setBackgroundColor(bgColor);
		final int notSelectedTextColor;
		if (bgColor == Color.BLACK) {
			notSelectedTextColor = Color.WHITE;
		} else if (bgColor == Color.WHITE) {
			notSelectedTextColor = Color.BLACK;
		} else {
			notSelectedTextColor = textColor;
		}
		tabs.setTextColor(notSelectedTextColor);
		tabs.setFooterColor(textColor);
		tabs.setSelectedColor(textColor);
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
		}
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
		if (getView().findViewById(R.id.loading) != null) { // IF inflated/present DO
			getView().findViewById(R.id.loading).setVisibility(View.GONE); // hide
		}
		if (getView().findViewById(R.id.empty) != null) { // IF inflated/present DO
			getView().findViewById(R.id.empty).setVisibility(View.GONE); // hide
		}
		getView().findViewById(R.id.tabs).setVisibility(View.VISIBLE); // show
		getView().findViewById(R.id.viewpager).setVisibility(View.VISIBLE); // show
	}

	private void showLoading() {
		if (getView().findViewById(R.id.tabs) != null) { // IF inflated/present DO
			getView().findViewById(R.id.tabs).setVisibility(View.GONE); // hide
		}
		if (getView().findViewById(R.id.viewpager) != null) { // IF inflated/present DO
			getView().findViewById(R.id.viewpager).setVisibility(View.GONE); // hide
		}
		if (getView().findViewById(R.id.empty) != null) { // IF inflated/present DO
			getView().findViewById(R.id.empty).setVisibility(View.GONE); // hide
		}
		if (getView().findViewById(R.id.loading) == null) { // IF NOT present/inflated DO
			((ViewStub) getView().findViewById(R.id.loading_stub)).inflate(); // inflate
		}
		getView().findViewById(R.id.loading).setVisibility(View.VISIBLE); // show
	}

	private void showEmpty() {
		if (getView().findViewById(R.id.tabs) != null) { // IF inflated/present DO
			getView().findViewById(R.id.tabs).setVisibility(View.GONE); // hide
		}
		if (getView().findViewById(R.id.viewpager) != null) { // IF inflated/present DO
			getView().findViewById(R.id.viewpager).setVisibility(View.GONE); // hide
		}
		if (getView().findViewById(R.id.loading) != null) { // IF inflated/present DO
			getView().findViewById(R.id.loading).setVisibility(View.GONE); // hide
		}
		if (getView().findViewById(R.id.empty) == null) { // IF NOT present/inflated DO
			((ViewStub) getView().findViewById(R.id.empty_stub)).inflate(); // inflate
		}
		getView().findViewById(R.id.empty).setVisibility(View.VISIBLE); // show
	}

	@Override
	public void onPageSelected(int position) {
		StatusLoader.get().clearAllTasks();
		if (this.adapter != null) {
			PreferenceUtils.savePrefLcl(getActivity(), PreferenceUtils.getPREFS_LCL_RTS_ROUTE_TRIP_ID_TAB(this.authority, this.routeId), this.adapter
					.getRouteTrip(position).getId(), false);
		}
		final List<Fragment> fragments = getChildFragmentManager().getFragments();
		if (fragments != null) {
			for (Fragment fragment : fragments) {
				if (fragment instanceof VisibilityAwareFragment) {
					final VisibilityAwareFragment visibilityAwareFragment = (VisibilityAwareFragment) fragment;
					visibilityAwareFragment.setFragmentVisisbleAtPosition(position);
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
		switch (state) {
		case ViewPager.SCROLL_STATE_IDLE:
			List<Fragment> fragments = getChildFragmentManager().getFragments();
			if (fragments != null) {
				for (Fragment fragment : fragments) {
					if (fragment instanceof VisibilityAwareFragment) {
						final VisibilityAwareFragment visibilityAwareFragment = (VisibilityAwareFragment) fragment;
						visibilityAwareFragment.setFragmentVisisbleAtPosition(this.lastPageSelected); // resume
					}
				}
			}
			break;
		case ViewPager.SCROLL_STATE_DRAGGING:
			List<Fragment> fragments2 = getChildFragmentManager().getFragments();
			if (fragments2 != null) {
				for (Fragment fragment : fragments2) {
					if (fragment instanceof VisibilityAwareFragment) {
						final VisibilityAwareFragment visibilityAwareFragment = (VisibilityAwareFragment) fragment;
						visibilityAwareFragment.setFragmentVisisbleAtPosition(-1); // pause
					}
				}
			}
			break;
		}
	}

	@Override
	public int getABIconDrawableResId() {
		return ABFragment.NO_ICON;
	}

	private CharSequence title;

	@Override
	public CharSequence getABTitle(Context context) {
		if (this.route == null) {
			return context.getString(R.string.ellipsis);
		}
		if (this.title == null) {
			initTitle();
		}
		return this.title;
	}

	private void initTitle() {
		SpannableStringBuilder titleSb = new SpannableStringBuilder();
		int startShortName = 0, endShortName = 0;
		if (!TextUtils.isEmpty(this.route.shortName)) {
			if (titleSb.length() > 0) {
				titleSb.append(StringUtils.SPACE_CAR);
			}
			startShortName = titleSb.length();
			titleSb.append(this.route.shortName.trim());
			endShortName = titleSb.length();
		}
		int startLongName = 0, endLongName = 0;
		if (!TextUtils.isEmpty(this.route.longName) && !this.route.longName.equals(this.route.shortName)) {
			if (titleSb.length() > 0) {
				titleSb.append(StringUtils.SPACE_CAR).append(StringUtils.SPACE_CAR);
			}
			startLongName = titleSb.length();
			titleSb.append(this.route.longName);
			endLongName = titleSb.length();
		}
		int startAgency = 0, endAgency = 0;
		if (startShortName != endShortName) {
			SpanUtils.set(titleSb, SpanUtils.BOLD_STYLE_SPAN, startShortName, endShortName);
			SpanUtils.set(titleSb, SpanUtils.SANS_SERIF_CONDENSED_TYPEFACE_SPAN, startShortName, endShortName);
		}
		if (startLongName != endLongName) {
			SpanUtils.set(titleSb, SpanUtils.SANS_SERIF_LIGHT_TYPEFACE_SPAN, startLongName, endLongName);
		}
		if (startAgency != endAgency) {
			titleSb.setSpan(SpanUtils.FIFTY_PERCENT_SIZE_SPAN, startAgency, endAgency, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
		}
		SpanUtils.set(titleSb, SpanUtils.getTextColor(ColorUtils.parseColor(this.route.textColor)));
		this.title = titleSb;
	}

	@Override
	public CharSequence getSubtitle(Context context) {
		return null;
	}

	@Override
	public Integer getBgColor() {
		if (this.route == null) {
			return ABFragment.NO_BG_COLOR;
		}
		return ColorUtils.parseColor(this.route.color);
	}

	private static class RouteTripPagerAdapter extends FragmentStatePagerAdapter {

		private List<Trip> routeTrips;
		private WeakReference<Context> contextWR;
		private Location userLocation;
		private int lastVisisbleFragmentPosition = -1;
		private String authority;

		public RouteTripPagerAdapter(RTSRouteFragment fragment, List<Trip> routeTrips, String authority) {
			super(fragment.getChildFragmentManager());
			this.contextWR = new WeakReference<Context>(fragment.getActivity());
			this.routeTrips = routeTrips;
			this.authority = authority;
		}

		public Trip getRouteTrip(int position) {
			return this.routeTrips.size() == 0 ? null : this.routeTrips.get(position);
		}

		public void setUserLocation(Location userLocation) {
			this.userLocation = userLocation;
		}

		public void setLastVisisbleFragmentPosition(int lastVisisbleFragmentPosition) {
			this.lastVisisbleFragmentPosition = lastVisisbleFragmentPosition;
		}

		@Override
		public int getCount() {
			return this.routeTrips == null ? 0 : this.routeTrips.size();
		}

		@Override
		public CharSequence getPageTitle(int position) {
			final Context context = this.contextWR == null ? null : this.contextWR.get();
			if (context == null) {
				return StringUtils.EMPTY;
			}
			if (this.routeTrips == null || position >= this.routeTrips.size()) {
				return StringUtils.EMPTY;
			}
			return this.routeTrips.get(position).getHeading(context).toUpperCase(Locale.ENGLISH);
		}

		@Override
		public Fragment getItem(int position) {
			final Trip trip = getRouteTrip(position);
			return RTSTripStopsFragment.newInstance(position, this.lastVisisbleFragmentPosition, this.authority, trip, this.userLocation);
		}

	}
}