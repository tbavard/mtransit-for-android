package org.mtransit.android.task;

import java.util.ArrayList;
import java.util.List;

import org.mtransit.android.commons.CollectionUtils;
import org.mtransit.android.commons.UriUtils;
import org.mtransit.android.commons.data.Route;
import org.mtransit.android.commons.task.MTAsyncTaskLoaderV4;
import org.mtransit.android.data.AgencyProperties;
import org.mtransit.android.data.DataSourceProvider;

import android.content.Context;
import android.net.Uri;

public class RTSAgencyRoutesLoader extends MTAsyncTaskLoaderV4<List<Route>> {

	private static final String TAG = RTSAgencyRoutesLoader.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	private AgencyProperties agency;
	private List<Route> routes;

	public RTSAgencyRoutesLoader(Context context, AgencyProperties agency) {
		super(context);
		this.agency = agency;
	}

	@Override
	public List<Route> loadInBackgroundMT() {
		if (this.routes != null) {
			return this.routes;
		}
		this.routes = new ArrayList<Route>();
		final Uri contentUri = UriUtils.newContentUri(this.agency.getAuthority());
		this.routes = DataSourceProvider.findAllRTSAgencyRoutes(getContext(), contentUri);
		CollectionUtils.sort(this.routes, Route.SHORT_NAME_COMPATOR);
		return routes;
	}

	@Override
	protected void onStartLoading() {
		super.onStartLoading();
		if (this.routes != null) {
			deliverResult(this.routes);
		}
		if (this.routes == null) {
			forceLoad();
		}
	}

	@Override
	protected void onStopLoading() {
		super.onStopLoading();
		cancelLoad();
	}

	@Override
	public void deliverResult(List<Route> data) {
		this.routes = data;
		if (isStarted()) {
			super.deliverResult(data);
		}
	}

}