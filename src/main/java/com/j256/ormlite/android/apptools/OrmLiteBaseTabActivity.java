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
public abstract class OrmLiteBaseTabActivity<H extends OrmLiteSqliteOpenHelper> extends TabActivity {

	private H helper;
	private Object lock = new Object();

	/**
	 * Get a helper for this action.
	 */
	public H getHelper() {
		synchronized (lock) {
			if (helper == null) {
				helper = getHelperInternal(this);
			}
			return helper;
		}
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
		synchronized (lock) {
			releaseHelper(helper);
		}
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
