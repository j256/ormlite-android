package com.j256.ormlite.android.apptools;

import android.app.TabActivity;
import android.content.Context;

import com.j256.ormlite.support.ConnectionSource;

/**
 * Base class to use for Tab activities in Android.
 * 
 * For more information, see {@link OrmLiteBaseActivity}.
 * 
 * @author kevingalligan
 */
public abstract class OrmLiteBaseTabActivity extends TabActivity {

	private OrmLiteSqliteOpenHelper helper;

	/**
	 * This is called internally by the activity class to populate the helper object instance. This should not be called
	 * directly by client code. Use {@link #getHelper()} to get a helper instance.
	 * 
	 * If you are managing your own helper creation, override this method to supply this activity with a helper
	 * instance.
	 */
	protected OrmLiteSqliteOpenHelper getHelperInternal(Context context) {
		return AndroidSqliteManager.getHelper(context);
	}

	/**
	 * Get a helper for this action.
	 */
	public synchronized OrmLiteSqliteOpenHelper getHelper() {
		if (helper == null) {
			helper = getHelperInternal(this);
		}
		return helper;
	}

	/**
	 * Get a connection source for this action.
	 */
	public ConnectionSource getConnectionSource() {
		return getHelper().getConnectionSource();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (helper != null) {
			AndroidSqliteManager.release();
			helper = null;
		}
	}
}
