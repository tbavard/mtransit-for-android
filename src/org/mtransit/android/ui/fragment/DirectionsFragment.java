package org.mtransit.android.ui.fragment;

import org.mtransit.android.R;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

public class DirectionsFragment extends ABFragment {

	private static final String TAG = DirectionsFragment.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	public static DirectionsFragment newInstance() {
		return new DirectionsFragment();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return new FrameLayout(getActivity());
	}

	@Override
	public int getIconDrawableResId() {
		return R.drawable.ic_menu_directions;
	}

	@Override
	public CharSequence getTitle(Context context) {
		return context.getString(R.string.directions);
	}

	@Override
	public CharSequence getSubtitle(Context context) {
		return null;
	}
}