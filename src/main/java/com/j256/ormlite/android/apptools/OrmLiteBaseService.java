package com.j256.ormlite.android.apptools;

import android.app.Service;
import android.content.Context;

import com.j256.ormlite.support.ConnectionSource;

/**
 * Base class to use for services in Android.
 * 
 * For more information, see {@link OrmLiteBaseActivity}.
 * 
 * @author graywatson, kevingalligan
 */
public abstract class OrmLiteBaseService<H extends OrmLiteSqliteOpenHelper> extends Service {

	private volatile H helper;

	/**
	 * Get a helper for this action.
	 */
	public H getHelper() {
		return helper;
	}

	/**
	 * Get a connection source for this action.
	 */
	public ConnectionSource getConnectionSource() {
		return getHelper().getConnectionSource();
	}

	@Override
	public void onCreate() {
		if (helper == null) {
			helper = getHelperInternal(this);
		}
		super.onCreate();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		releaseHelper(helper);
	}

	/**
	 * This is called internally by the class to populate the helper object instance. This should not be called directly
	 * by client code unless you know what you are doing. Use {@link #getHelper()} to get a helper instance. If you are
	 * managing your own helper creation, override this method to supply this activity with a helper instance.
	 * 
	 * <p>
	 * <b> NOTE: </b> If you override this method, you most likely will need to override the
	 * {@link #releaseHelper(OrmLiteSqliteOpenHelper)} method as well.
	 * </p>
	 */
	protected H getHelperInternal(Context context) {
		@SuppressWarnings("unchecked")
		H newHelper = (H) OpenHelperManager.getHelper(context);
		return newHelper;
	}

	/**
	 * Release the helper instance created in {@link #getHelperInternal(Context)}. You most likely will not need to call
	 * this directly since {@link #onDestroy()} does it for you.
	 * 
	 * <p>
	 * <b> NOTE: </b> If you override this method, you most likely will need to override the
	 * {@link #getHelperInternal(Context)} method as well.
	 * </p>
	 */
	protected void releaseHelper(H helper) {
		OpenHelperManager.release();
		helper = null;
	}
}
