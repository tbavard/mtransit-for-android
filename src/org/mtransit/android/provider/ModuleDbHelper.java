package org.mtransit.android.provider;

import org.mtransit.android.commons.PreferenceUtils;
import org.mtransit.android.commons.SqlUtils;
import org.mtransit.android.commons.provider.MTSQLiteOpenHelper;
import org.mtransit.android.commons.provider.POIDbHelper;
import org.mtransit.android.commons.provider.StatusDbHelper;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

public class ModuleDbHelper extends MTSQLiteOpenHelper {

	private static final String TAG = ModuleDbHelper.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	protected static final String DB_NAME = "module.db";

	public static final int DB_VERSION = 1;

	protected static final String PREF_KEY_LAST_UPDATE_MS = "pModuleLastUpdate";

	public static final String T_MODULE = POIDbHelper.T_POI;
	public static final String T_MODULE_PKG = POIDbHelper.getFkColumnName("pkg");
	private static final String T_MODULE_SQL_CREATE = POIDbHelper.getSqlCreate(T_MODULE, //
			T_MODULE_PKG + SqlUtils.TXT//
	);
	private static final String T_MODULE_SQL_DROP = SqlUtils.getSQLDropIfExistsQuery(T_MODULE);

	public static final String T_MODULE_STATUS = StatusDbHelper.T_STATUS;
	private static final String T_MODULE_STATUS_SQL_CREATE = StatusDbHelper.getSqlCreate(T_MODULE_STATUS);
	private static final String T_MODULE_STATUS_SQL_DROP = SqlUtils.getSQLDropIfExistsQuery(T_MODULE_STATUS);

	public static int getDbVersion(Context context) {
		return DB_VERSION;
	}

	private Context context;

	public ModuleDbHelper(Context context) {
		super(context, DB_NAME, null, getDbVersion(context));
		this.context = context;
	}

	@Override
	public void onCreateMT(SQLiteDatabase db) {
		initAllDbTables(db);
	}

	@Override
	public void onUpgradeMT(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL(T_MODULE_SQL_DROP);
		db.execSQL(T_MODULE_STATUS_SQL_DROP);
		initAllDbTables(db);
	}

	public boolean isDbExist(Context context) {
		return SqlUtils.isDbExist(context, DB_NAME);
	}

	private void initAllDbTables(SQLiteDatabase db) {
		db.execSQL(T_MODULE_SQL_CREATE);
		db.execSQL(T_MODULE_STATUS_SQL_CREATE);
		PreferenceUtils.savePrefLcl(this.context, PREF_KEY_LAST_UPDATE_MS, 0l, true);
	}

}