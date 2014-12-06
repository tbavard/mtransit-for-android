package org.mtransit.android.ui.fragment;

import java.util.ArrayList;

import org.mtransit.android.R;
import org.mtransit.android.commons.BundleUtils;
import org.mtransit.android.commons.KeyboardUtils;
import org.mtransit.android.commons.LoaderUtils;
import org.mtransit.android.commons.LocationUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.StringUtils;
import org.mtransit.android.commons.TimeUtils;
import org.mtransit.android.commons.task.MTAsyncTask;
import org.mtransit.android.commons.ui.widget.MTArrayAdapter;
import org.mtransit.android.data.DataSourceProvider;
import org.mtransit.android.data.DataSourceType;
import org.mtransit.android.data.POIArrayAdapter;
import org.mtransit.android.data.POIManager;
import org.mtransit.android.task.POISearchLoader;
import org.mtransit.android.ui.MTActivityWithLocation;
import org.mtransit.android.ui.MainActivity;
import org.mtransit.android.ui.view.MTSearchView;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.TextView;

public class SearchFragment extends ABFragment implements LoaderManager.LoaderCallbacks<ArrayList<POIManager>>, MTActivityWithLocation.UserLocationListener,
		POIArrayAdapter.TypeHeaderButtonsClickListener {

	private static final String TAG = SearchFragment.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	private static final String TRACKING_SCREEN_NAME = "Search";

	@Override
	public String getScreenName() {
		return TRACKING_SCREEN_NAME;
	}

	private static final String EXTRA_QUERY = "extra_query";
	private static final String EXTRA_TYPE_FILTER = "extra_type_filter";

	public static SearchFragment newInstance(String optQuery, Integer optTypeIdFilter, TypeFilter optTypeFilter) {
		SearchFragment f = new SearchFragment();
		Bundle args = new Bundle();
		if (!TextUtils.isEmpty(optQuery)) {
			args.putString(EXTRA_QUERY, optQuery);
			f.query = optQuery;
		}
		if (optTypeIdFilter != null) {
			args.putInt(EXTRA_TYPE_FILTER, optTypeIdFilter.intValue());
			f.typeIdFilter = optTypeIdFilter;
		}
		f.typeFilter = optTypeFilter;
		f.setArguments(args);
		return f;
	}

	private POIArrayAdapter adapter;
	private CharSequence emptyText = null;
	private Location userLocation;
	private String query = null;
	private Integer typeIdFilter = null;
	private TypeFilter typeFilter = null;

	private void resetTypeFilter() {
		this.typeFilter = null;
	}

	private boolean hasTypeFilter() {
		if (this.typeFilter == null) {
			initTypeFilterAsync();
			return false;
		}
		return true;
	}

	private void initTypeFilterAsync() {
		if (this.loadTypeFilterTask.getStatus() == MTAsyncTask.Status.RUNNING) {
			return;
		}
		if (this.typeIdFilter == null) {
			this.typeIdFilter = TypeFilter.ALL.getDataSourceTypeId(); // default
		}
		this.loadTypeFilterTask.execute();
	}

	private MTAsyncTask<Void, Void, Boolean> loadTypeFilterTask = new MTAsyncTask<Void, Void, Boolean>() {
		@Override
		public String getLogTag() {
			return TAG + ">loadTypeFilterTask";
		}

		@Override
		protected Boolean doInBackgroundMT(Void... params) {
			return initTypeFilterSync();
		}

		@Override
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);
			if (result) {
				applyNewTypeFilter();
			}
		}
	};

	private boolean initTypeFilterSync() {
		if (this.typeFilter != null) {
			return false;
		}
		if (this.typeIdFilter != null) {
			if (this.typeIdFilter.equals(TypeFilter.ALL.getDataSourceTypeId())) {
				this.typeFilter = TypeFilter.ALL;
			} else {
				this.typeFilter = TypeFilter.fromDataSourceType(DataSourceType.parseId(this.typeIdFilter));
			}
		}
		return this.typeFilter != null;
	}

	private void setTypeFilterFromType(Integer newTypeId) {
		if (newTypeId == null) {
			return;
		}
		if (this.typeIdFilter != null && this.typeIdFilter.equals(newTypeId)) {
			return;
		}
		this.typeIdFilter = newTypeId;
		resetTypeFilter();
		initTypeFilterSync();
		applyNewTypeFilter();
	}

	private void applyNewTypeFilter() {
		if (this.typeFilter == null) {
			return;
		}
		Spinner typeFiltersSpinner = (Spinner) getView().findViewById(R.id.typeFilters);
		if (this.typeFilter.getDataSourceTypeId() == TypeFilter.ALL.getDataSourceTypeId()) {
			if (this.adapter != null) {
				this.adapter.setShowTypeHeader(POIArrayAdapter.TYPE_HEADER_MORE);
			}
		} else {
			if (this.adapter != null) {
				this.adapter.setShowTypeHeader(POIArrayAdapter.TYPE_HEADER_NONE);
			}
		}
		int position = getTypeFiltersAdapter().getPosition(this.typeFilter);
		typeFiltersSpinner.setSelection(position, true);
		if (this.adapter != null) {
			this.adapter.clear();
		}
		switchView(getView());
		LoaderUtils.restartLoader(getLoaderManager(), POI_SEARCH_LOADER, null, this);
	}

	private TypeFilter getTypeFilterOrNull() {
		if (!hasTypeFilter()) {
			return null;
		}
		return this.typeFilter;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		restoreInstanceState(savedInstanceState, getArguments());
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		restoreInstanceState(savedInstanceState);
		View view = inflater.inflate(R.layout.fragment_search, container, false);
		setupView(view);
		return view;
	}

	private void initAdapter() {
		if (this.adapter != null) {
			return;
		}
		this.adapter = new POIArrayAdapter(getActivity());
		this.adapter.setTag(getLogTag());
		TypeFilter typeFilter = getTypeFilterOrNull();
		if (typeFilter == null || typeFilter.getDataSourceTypeId() == TypeFilter.ALL.getDataSourceTypeId()) {
			this.adapter.setShowTypeHeader(POIArrayAdapter.TYPE_HEADER_MORE);
		} else {
			this.adapter.setShowTypeHeader(POIArrayAdapter.TYPE_HEADER_NONE);
		}
		this.adapter.setOnTypeHeaderButtonsClickListener(this);
		View view = getView();
		setupAdapter(view);
	}

	@Override
	public boolean onTypeHeaderButtonClick(int buttonId, DataSourceType type) {
		if (buttonId == POIArrayAdapter.TypeHeaderButtonsClickListener.BUTTON_MORE) {
			KeyboardUtils.hideKeyboard(getActivity(), getView());
			setTypeFilterFromType(type.getId());
			return true; // handled
		}
		return false; // not handled
	}

	private void setupAdapter(View view) {
		if (view == null || this.adapter == null) {
			return;
		}
		inflateList(view);
		AbsListView list = (AbsListView) view.findViewById(R.id.list);
		this.adapter.setListView(list);
		Spinner typeFiltersSpinner = (Spinner) view.findViewById(R.id.typeFilters);
		typeFiltersSpinner.setAdapter(getTypeFiltersAdapter());
	}

	private void setupView(View view) {
		if (view == null) {
			return;
		}
		Spinner typeFiltersSpinner = (Spinner) view.findViewById(R.id.typeFilters);
		TypeFilter typeFilter = getTypeFilterOrNull();
		int position = getTypeFiltersAdapter().getPosition(typeFilter);
		typeFiltersSpinner.setSelection(position, false);
		typeFiltersSpinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {


			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				setTypeFilterFromType(getTypeFiltersAdapter().getItem(position).getDataSourceTypeId());
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}
		});
		setupAdapter(view);
	}

	private TypeFiltersAdapter typeFiltersAdapter = null;

	private TypeFiltersAdapter getTypeFiltersAdapter() {
		if (this.typeFiltersAdapter == null) {
			this.typeFiltersAdapter = new TypeFiltersAdapter(getActivity());
		}
		return this.typeFiltersAdapter;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		restoreInstanceState(savedInstanceState);
	}

	private void restoreInstanceState(Bundle... bundles) {
		Integer newTypeIdFilter = BundleUtils.getInt(EXTRA_TYPE_FILTER, bundles);
		if (newTypeIdFilter != null && !newTypeIdFilter.equals(this.typeIdFilter)) {
			this.typeIdFilter = newTypeIdFilter;
			resetTypeFilter();
		}
		String newQuery = BundleUtils.getString(EXTRA_QUERY, bundles);
		if (!TextUtils.isEmpty(newQuery) && !newQuery.equals(this.query)) {
			this.query = newQuery;
			applyNewQuery();
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		if (this.query != null) {
			outState.putString(EXTRA_QUERY, this.query);
		}
		if (this.typeIdFilter != null) {
			outState.putInt(EXTRA_TYPE_FILTER, this.typeIdFilter);
		}
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onResume() {
		super.onResume();
		switchView(getView());
		if (this.adapter != null) {
			this.adapter.onResume(getActivity());
		}
		onUserLocationChanged(((MTActivityWithLocation) getActivity()).getLastLocation());
	}

	@Override
	public void onPause() {
		super.onPause();
		if (this.adapter != null) {
			this.adapter.onPause();
		}
	}

	@Override
	public void onModulesUpdated() {
		getTypeFiltersAdapter().reset();
		LoaderUtils.restartLoader(getLoaderManager(), POI_SEARCH_LOADER, null, this);
	}

	private static final int POI_SEARCH_LOADER = 0;

	@Override
	public Loader<ArrayList<POIManager>> onCreateLoader(int id, Bundle args) {
		switch (id) {
		case POI_SEARCH_LOADER:
			TypeFilter typeFilter = getTypeFilterOrNull();
			if (typeFilter == null) {
				return null;
			}
			POISearchLoader poiSearchLoader = new POISearchLoader(getActivity(), this.query, typeFilter, this.userLocation);
			return poiSearchLoader;
		default:
			MTLog.w(this, "Loader id '%s' unknown!", id);
			return null;
		}
	}

	@Override
	public void onLoaderReset(Loader<ArrayList<POIManager>> loader) {
		if (this.adapter != null) {
			this.adapter.clear();
		}
	}

	@Override
	public void onLoadFinished(Loader<ArrayList<POIManager>> loader, ArrayList<POIManager> data) {
		if (this.adapter == null) {
			initAdapter();
		}
		this.adapter.setPois(data);
		this.adapter.updateDistanceNowAsync(this.userLocation);
		switchView(getView());
	}

	@Override
	public void onUserLocationChanged(Location newLocation) {
		if (newLocation != null) {
			if (this.userLocation == null || LocationUtils.isMoreRelevant(getLogTag(), this.userLocation, newLocation)) {
				this.userLocation = newLocation;
				if (this.adapter != null) {
					this.adapter.setLocation(newLocation);
				}
			}
		}
	}

	private void applyNewQuery() {
		getLoaderManager().destroyLoader(POI_SEARCH_LOADER); // cancel now
		cancelRestartSearchLater();
		if (TextUtils.isEmpty(this.query)) {
			this.emptyText = getString(R.string.search_hint);
			if (this.adapter == null) {
				initAdapter();
			}
			this.adapter.setPois(new ArrayList<POIManager>()); // empty search = no result
			switchView(getView());
			return;
		}
		this.emptyText = getString(R.string.search_no_result_for_and_query, this.query);
		if (this.adapter != null) {
			this.adapter.clear();
		}
		switchView(getView());
		this.restartSearchLater = new RestartSearchLater();
		this.handler.postDelayed(this.restartSearchLater, TimeUtils.ONE_SECOND_IN_MS);
	}

	public void setSearchQuery(String query, boolean alreadyInSearchView) {
		if (this.query == null || !StringUtils.equals(StringUtils.trim(this.query), StringUtils.trim(query))) {
			this.query = query == null ? StringUtils.EMPTY : query;
			applyNewQuery();
		}
	}

	private void cancelRestartSearchLater() {
		if (this.restartSearchLater != null) {
			this.handler.removeCallbacks(this.restartSearchLater);
			this.restartSearchLater = null;
		}
	}

	private Handler handler = new Handler();

	private RestartSearchLater restartSearchLater = null;

	private class RestartSearchLater implements Runnable {
		@Override
		public void run() {
			LoaderUtils.restartLoader(SearchFragment.this.getLoaderManager(), POI_SEARCH_LOADER, null, SearchFragment.this);
			cancelRestartSearchLater();
		}
	}


	private void switchView(View view) {
		if (view == null) {
			return;
		}
		if (this.adapter == null || !this.adapter.isInitialized()) {
			showLoading(view);
		} else if (this.adapter.getPoisCount() == 0) {
			showEmpty(view);
		} else {
			showList(view);
		}
		TypeFilter typeFilter = getTypeFilterOrNull();
		if (typeFilter == null || typeFilter.getDataSourceTypeId() == TypeFilter.ALL.getDataSourceTypeId()) {
			view.findViewById(R.id.typeFilters).setVisibility(View.GONE);
		} else {
			view.findViewById(R.id.typeFilters).setVisibility(View.VISIBLE);
		}
	}

	private void showList(View view) {
		if (view.findViewById(R.id.loading) != null) { // IF inflated/present DO
			view.findViewById(R.id.loading).setVisibility(View.GONE); // hide
		}
		if (view.findViewById(R.id.empty) != null) { // IF inflated/present DO
			view.findViewById(R.id.empty).setVisibility(View.GONE); // hide
		}
		inflateList(view);
		view.findViewById(R.id.list).setVisibility(View.VISIBLE); // show
	}

	private void inflateList(View view) {
		if (view.findViewById(R.id.list) == null) { // IF NOT present/inflated DO
			((ViewStub) view.findViewById(R.id.list_stub)).inflate(); // inflate
		}
	}

	private void showLoading(View view) {
		if (view.findViewById(R.id.list) != null) { // IF inflated/present DO
			view.findViewById(R.id.list).setVisibility(View.GONE); // hide
		}
		if (view.findViewById(R.id.empty) != null) { // IF inflated/present DO
			view.findViewById(R.id.empty).setVisibility(View.GONE); // hide
		}
		if (view.findViewById(R.id.loading) == null) { // IF NOT present/inflated DO
			((ViewStub) view.findViewById(R.id.loading_stub)).inflate(); // inflate
		}
		view.findViewById(R.id.loading).setVisibility(View.VISIBLE); // show
	}

	private void showEmpty(View view) {
		if (view.findViewById(R.id.list) != null) { // IF inflated/present DO
			view.findViewById(R.id.list).setVisibility(View.GONE); // hide
		}
		if (view.findViewById(R.id.loading) != null) { // IF inflated/present DO
			view.findViewById(R.id.loading).setVisibility(View.GONE); // hide
		}
		if (view.findViewById(R.id.empty) == null) { // IF NOT present/inflated DO
			((ViewStub) view.findViewById(R.id.empty_stub)).inflate(); // inflate
		}
		if (!TextUtils.isEmpty(this.emptyText)) {
			((TextView) view.findViewById(R.id.empty_text)).setText(this.emptyText);
		}
		view.findViewById(R.id.empty).setVisibility(View.VISIBLE); // show
	}

	@Override
	public boolean isABReady() {
		return this.searchView != null;
	}

	@Override
	public boolean isABShowSearchMenuItem() {
		return false;
	}

	@Override
	public boolean isABCustomViewRequestFocus() {
		return true;
	}

	@Override
	public View getABCustomView() {
		return getSearchView();
	}

	private MTSearchView searchView;

	private MTSearchView getSearchView() {
		if (this.searchView == null) {
			initSearchView();
		}
		return this.searchView;
	}

	private void initSearchView() {
		Activity activity = getActivity();
		if (activity == null) {
			return;
		}
		this.searchView = new MTSearchView(((MainActivity) activity), ((MainActivity) activity).getSupportActionBar().getThemedContext());
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		cancelRestartSearchLater();
		this.searchView = null;
		this.handler = null;
	}

	public static class TypeFilter implements MTLog.Loggable {

		private static final String TAG = SearchFragment.class.getSimpleName() + ">" + TypeFilter.class.getSimpleName();

		@Override
		public String getLogTag() {
			return TAG;
		}

		public static final TypeFilter ALL = new TypeFilter(-1, R.string.all, -1);

		private int dataSourceTypeId;

		private int nameResId;

		private int iconResId;

		public TypeFilter(int dataSourceTypeId, int nameResId, int iconResId) {
			this.dataSourceTypeId = dataSourceTypeId;
			this.nameResId = nameResId;
			this.iconResId = iconResId;
		}

		@Override
		public String toString() {
			return new StringBuilder(TypeFilter.class.getSimpleName()).append('[') //
					.append("dataSourceTypeId:").append(this.dataSourceTypeId).append(',') //
					.append("nameResId:").append(this.nameResId).append(',') //
					.append("iconResId:").append(this.iconResId).append(',') //
					.append(']').toString();
		}

		@Override
		public boolean equals(Object o) {
			if (o == null || !(o instanceof TypeFilter)) {
				return false;
			}
			TypeFilter other = (TypeFilter) o;
			if (this.dataSourceTypeId != other.dataSourceTypeId) {
				return false;
			}
			if (this.nameResId != other.nameResId) {
				return false;
			}
			if (this.iconResId != other.iconResId) {
				return false;
			}
			return true;
		}

		public int getNameResId() {
			return nameResId;
		}

		public int getIconResId() {
			return iconResId;
		}

		public int getDataSourceTypeId() {
			return dataSourceTypeId;
		}

		public static TypeFilter fromDataSourceType(DataSourceType dst) {
			return new TypeFilter(dst.getId(), dst.getPoiShortNameResId(), dst.getMenuResId());
		}

	}

	private static class TypeFiltersAdapter extends MTArrayAdapter<TypeFilter> {

		private static final String TAG = SearchFragment.class.getSimpleName() + ">" + TypeFiltersAdapter.class.getSimpleName();

		@Override
		public String getLogTag() {
			return TAG;
		}

		private LayoutInflater layoutInflater;

		public TypeFiltersAdapter(Context context) {
			super(context, -1);
			this.layoutInflater = LayoutInflater.from(context);
			init();
		}

		private void init() {
			ArrayList<TypeFilter> typeFilters = new ArrayList<TypeFilter>();
			typeFilters.add(TypeFilter.ALL);
			ArrayList<DataSourceType> availableTypes = DataSourceProvider.get(getContext()).getAvailableAgencyTypes();
			if (availableTypes != null) {
				for (DataSourceType dst : availableTypes) {
					if (dst.isSearchable()) {
						typeFilters.add(TypeFilter.fromDataSourceType(dst));
					}
				}
			}
			addAll(typeFilters);
		}

		public void reset() {
			clear();
			init();
			notifyDataSetChanged();
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			return getTheView(position, convertView, parent);
		}

		@Override
		public View getDropDownView(int position, View convertView, ViewGroup parent) {
			return getTheView(position, convertView, parent);
		}

		private View getTheView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView = this.layoutInflater.inflate(R.layout.layout_poi_type_item, parent, false);
				TypeViewHolder holder = new TypeViewHolder();
				holder.nameTv = (TextView) convertView.findViewById(R.id.name);
				convertView.setTag(holder);
			}
			TypeViewHolder holder = (TypeViewHolder) convertView.getTag();
			TypeFilter type = getItem(position);
			holder.nameTv.setText(type.getNameResId());
			if (type.getIconResId() != -1) {
				holder.nameTv.setCompoundDrawablesWithIntrinsicBounds(type.getIconResId(), 0, 0, 0);
			} else {
				holder.nameTv.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
			}
			return convertView;
		}

		private static class TypeViewHolder {
			TextView nameTv;
		}
	}
}
