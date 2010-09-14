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
public abstract class OrmLiteBaseService<H extends OrmLiteSqliteOpenHelper> extends Service {

	private H helper;

	/**
	 * Get a helper for this service.
	 */
	public synchronized H getHelper() {
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
		releaseHelper(helper);
	}

	/**
	 * @see OrmLiteBaseActivity#getHelperInternal(Context)
	 */
	protected H getHelperInternal(Context context) {
		@SuppressWarnings("unchecked")
		H newHelper = (H) OpenHelperManager.getHelper(context);
		return newHelper;
	}

	/**
	 * @see OrmLiteBaseActivity#releaseHelper(OrmLiteSqliteOpenHelper)
	 */
	protected void releaseHelper(H helper) {
		if (helper != null) {
			OpenHelperManager.release();
			helper = null;
		}
	}
}
