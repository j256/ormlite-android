package com.j256.ormlite.android.apptools;

import android.app.Activity;
import android.content.Context;

import com.j256.ormlite.support.ConnectionSource;

/**
 * Base class to use for activities in Android.
 * 
 * If you are using the default helper factory, you can simply call {@link #getHelper()} to get your helper class, or
 * {@link #getConnectionSource()} to get a {@link ConnectionSource}.
 * 
 * The method {@link #getHelper()} assumes you are using the default helper factory -- see {@link AndroidSqliteManager}.
 * If not, you'll need to provide your own helper instances which will need to implement a reference counting scheme.
 * This method will only be called if you use the database, and only called once for this activity's life-cycle. 'close'
 * will also be called once for each call to createInstance.
 * 
 * @author kevingalligan
 */
public abstract class OrmLiteBaseActivity extends Activity {

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
