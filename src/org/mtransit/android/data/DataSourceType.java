package org.mtransit.android.data;

import java.lang.ref.WeakReference;
import java.util.Comparator;

import org.mtransit.android.R;
import org.mtransit.android.commons.ComparatorUtils;
import org.mtransit.android.commons.MTLog;

import android.content.Context;

public enum DataSourceType {

	TYPE_LIGHT_RAIL(0, // GTFS - Tram, Streetcar
			R.string.agency_type_light_rail_short_name, R.string.agency_type_light_rail_all, //
			R.string.agency_type_light_rail_stations_short_name, R.string.agency_type_light_rail_nearby, //
			R.drawable.ic_directions_light_rail_black_24dp, //
			R.drawable.ic_directions_light_rail_grey600_24dp, //
			R.drawable.ic_directions_light_rail_white_24dp, //
			R.id.nav_light_rail, //
			true, true, true, true, true), //
	TYPE_SUBWAY(1, // GTFS - Metro
			R.string.agency_type_subway_short_name, R.string.agency_type_subway_all, //
			R.string.agency_type_subway_stations_short_name, R.string.agency_type_subway_nearby, //
			R.drawable.ic_directions_subway_black_24dp, //
			R.drawable.ic_directions_subway_grey600_24dp, //
			R.drawable.ic_directions_subway_white_24dp, //
			R.id.nav_subway, //
			true, true, true, true, true), //
	TYPE_RAIL(2, // GTFS - Train
			R.string.agency_type_rail_short_name, R.string.agency_type_rail_all, //
			R.string.agency_type_rail_stations_short_name, R.string.agency_type_rail_nearby, //
			R.drawable.ic_directions_railway_black_24dp, //
			R.drawable.ic_directions_railway_grey600_24dp, //
			R.drawable.ic_directions_railway_white_24dp, //
			R.id.nav_rail, //
			true, true, true, true, true), //
	TYPE_BUS(3, // GTFS - Bus
			R.string.agency_type_bus_short_name, R.string.agency_type_bus_all, //
			R.string.agency_type_bus_stops_short_name, R.string.agency_type_bus_nearby, //
			R.drawable.ic_directions_bus_black_24dp, //
			R.drawable.ic_directions_bus_grey600_24dp, //
			R.drawable.ic_directions_bus_white_24dp, //
			R.id.nav_bus, //
			true, true, true, true, true), //
	TYPE_FERRY(4, // GTFS - Boat
			R.string.agency_type_ferry_short_name, R.string.agency_type_ferry_all, //
			R.string.agency_type_ferry_stations_short_name, R.string.agency_type_ferry_nearby, //
			R.drawable.ic_directions_boat_black_24dp, //
			R.drawable.ic_directions_boat_grey600_24dp, //
			R.drawable.ic_directions_boat_white_24dp, //
			R.id.nav_ferry, //
			true, true, true, true, true), //
	TYPE_BIKE(100, // like BIXI, Velib
			R.string.agency_type_bike_short_name, R.string.agency_type_bike_all, //
			R.string.agency_type_bike_stations_short_name, R.string.agency_type_bike_nearby, //
			R.drawable.ic_directions_bike_black_24dp, //
			R.drawable.ic_directions_bike_grey600_24dp, //
			R.drawable.ic_directions_bike_white_24dp, //
			R.id.nav_bike, //
			true, true, true, true, true), //
	TYPE_PLACE(666, //
			R.string.agency_type_place_short_name, R.string.agency_type_place_all, //
			R.string.agency_type_place_app_short_name, R.string.agency_type_place_nearby, //
			-1, //
			R.drawable.ic_place_grey600_24dp, //
			-1, //
			-1, //
			false, false, false, false, true), //
	TYPE_MODULE(999, //
			R.string.agency_type_module_short_name, R.string.agency_type_module_all, //
			R.string.agency_type_module_app_short_name, R.string.agency_type_module_nearby, //
			R.drawable.ic_shop_black_24dp, //
			R.drawable.ic_shop_grey600_24dp, //
			R.drawable.ic_shop_white_24dp, //
			R.id.nav_module, //
			true, true, true, false, false), //
	;

	private static final String TAG = DataSourceType.class.getSimpleName();

	public static final int MAX_ID = 1000;

	private int id;

	private int shortNameResId;

	private int allStringResId;

	private int poiShortNameResId;

	private int nearbyNameResId;

	private int blackIconResId;

	private int grey600IconResId;

	private int whiteIconResId;

	private int navResId;

	private boolean menuList;

	private boolean homeScreen;

	private boolean nearbyScreen;

	private boolean mapScreen;

	private boolean searchable;

	DataSourceType(int id, int shortNameResId, int allStringResId, int poiShortNameResId, int nearbyNameResId, int blackIconResId, int grey600IconResId,
			int whiteIconResId, int navResId, boolean menuList, boolean homeScreen, boolean nearbyScreen, boolean mapScreen, boolean searchable) {
		if (id >= MAX_ID) {
			throw new UnsupportedOperationException(String.format("Data source type ID '%s' must be lower than '%s'!", id, MAX_ID));
		}
		this.id = id;
		this.shortNameResId = shortNameResId;
		this.allStringResId = allStringResId;
		this.poiShortNameResId = poiShortNameResId;
		this.nearbyNameResId = nearbyNameResId;
		this.blackIconResId = blackIconResId;
		this.grey600IconResId = grey600IconResId;
		this.whiteIconResId = whiteIconResId;
		this.navResId = navResId;
		this.menuList = menuList;
		this.homeScreen = homeScreen;
		this.nearbyScreen = nearbyScreen;
		this.mapScreen = mapScreen;
		this.searchable = searchable;
	}

	public int getId() {
		return id;
	}

	public int getShortNameResId() {
		return shortNameResId;
	}

	public int getAllStringResId() {
		return allStringResId;
	}

	public int getPoiShortNameResId() {
		return poiShortNameResId;
	}

	public int getNearbyNameResId() {
		return nearbyNameResId;
	}

	public int getBlackIconResId() {
		return blackIconResId;
	}

	public int getGrey600IconResId() {
		return grey600IconResId;
	}

	public int getWhiteIconResId() {
		return whiteIconResId;
	}

	public int getNavResId() {
		return navResId;
	}

	public boolean isMenuList() {
		return this.menuList;
	}

	public boolean isHomeScreen() {
		return this.homeScreen;
	}

	public boolean isNearbyScreen() {
		return this.nearbyScreen;
	}

	public boolean isMapScreen() {
		return this.mapScreen;
	}

	public boolean isSearchable() {
		return searchable;
	}

	public static DataSourceType parseId(Integer id) {
		if (id == null) {
			MTLog.w(TAG, "ID 'null' doesn't match any type!");
			return null;
		}
		switch (id) {
		case 0:
			return TYPE_LIGHT_RAIL;
		case 1:
			return TYPE_SUBWAY;
		case 2:
			return TYPE_RAIL;
		case 3:
			return TYPE_BUS;
		case 4:
			return TYPE_FERRY;
		case 100:
			return TYPE_BIKE;
		case 666:
			return TYPE_PLACE;
		case 999:
			return TYPE_MODULE;
		default:
			MTLog.w(TAG, "ID '%s' doesn't match any type!", id);
			return null;
		}
	}

	public static DataSourceType parseNavResId(Integer navResId) {
		if (navResId == null) {
			MTLog.w(TAG, "Nav res ID 'null' doesn't match any type!");
			return null;
		}
		switch (navResId) {
		case R.id.nav_light_rail:
			return TYPE_LIGHT_RAIL;
		case R.id.nav_subway:
			return TYPE_SUBWAY;
		case R.id.nav_rail:
			return TYPE_RAIL;
		case R.id.nav_bus:
			return TYPE_BUS;
		case R.id.nav_ferry:
			return TYPE_FERRY;
		case R.id.nav_bike:
			return TYPE_BIKE;
		case R.id.nav_module:
			return TYPE_MODULE;
		default:
			MTLog.w(TAG, "Nav res ID '5s' doesn't match any type!", navResId);
			return null;
		}
	}

	public static class DataSourceTypeComparator implements Comparator<DataSourceType> {

		@Override
		public int compare(DataSourceType lType, DataSourceType rType) {
			return lType.id - rType.id;
		}
	}

	public static class DataSourceTypeShortNameComparator implements Comparator<DataSourceType> {

		private WeakReference<Context> contextWR;

		public DataSourceTypeShortNameComparator(Context context) {
			this.contextWR = new WeakReference<Context>(context);
		}

		@Override
		public int compare(DataSourceType lType, DataSourceType rType) {
			Context context = this.contextWR == null ? null : this.contextWR.get();
			if (context == null) {
				return ComparatorUtils.SAME;
			}
			if (lType.equals(rType)) {
				return ComparatorUtils.SAME;
			}
			if (TYPE_MODULE.equals(lType)) {
				return ComparatorUtils.AFTER;
			} else if (TYPE_MODULE.equals(rType)) {
				return ComparatorUtils.BEFORE;
			} else if (TYPE_PLACE.equals(lType)) {
				return ComparatorUtils.AFTER;
			} else if (TYPE_PLACE.equals(rType)) {
				return ComparatorUtils.BEFORE;
			}
			String lShortName = context.getString(lType.getShortNameResId());
			String rShortName = context.getString(rType.getShortNameResId());
			return lShortName.compareTo(rShortName);
		}
	}

	public static class POIManagerTypeShortNameComparator implements Comparator<POIManager> {

		private WeakReference<Context> contextWR;

		public POIManagerTypeShortNameComparator(Context context) {
			this.contextWR = new WeakReference<Context>(context);
		}

		@Override
		public int compare(POIManager lPoim, POIManager rPoim) {
			Context context = this.contextWR == null ? null : this.contextWR.get();
			if (context == null) {
				return 0;
			}
			AgencyProperties lAgency = DataSourceProvider.get(context).getAgency(context, lPoim.poi.getAuthority());
			AgencyProperties rAgency = DataSourceProvider.get(context).getAgency(context, rPoim.poi.getAuthority());
			if (lAgency == null || rAgency == null) {
				return 0;
			}
			int lShortNameResId = lAgency.getType().getShortNameResId();
			int rShortNameResId = rAgency.getType().getShortNameResId();
			String lShortName = context.getString(lShortNameResId);
			String rShortName = context.getString(rShortNameResId);
			return lShortName.compareTo(rShortName);
		}
	}
}
