package com.j256.ormlite.android.apptools;

import android.app.Service;
import android.content.Context;

import com.j256.ormlite.support.ConnectionSource;

/**
 * Base class to use for services in Android.
 * 
 * For more information, see {@link OrmLiteBaseActivity}.
 * 
 * @author kevingalligan
 */
public abstract class OrmLiteBaseService extends Service {

	private OrmLiteSqliteOpenHelper helper;

	/**
	 * This is called internally by the service class to populate the helper object instance. This should not be called
	 * directly by client code. Use {@link #getHelper()} to get a helper instance.
	 * 
	 * If you are managing your own helper creation, override this method to supply this service with a helper instance.
	 */
	protected OrmLiteSqliteOpenHelper getHelperInternal(Context context) {
		return AndroidSqliteManager.getHelper(context);
	}

	/**
	 * Get a helper for this service.
	 */
	public synchronized OrmLiteSqliteOpenHelper getHelper() {
		if (helper == null) {
			helper = getHelperInternal(this);
		}
		return helper;
	}

	/**
	 * Get a connection source for this service.
	 */
	public ConnectionSource getConnectionSource() {
		return getHelper().getConnectionSource();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (helper != null) {
			AndroidSqliteManager.release();
			helper = null;
		}
	}
}
