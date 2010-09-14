package com.j256.ormlite.android.apptools;

import java.util.concurrent.atomic.AtomicInteger;

import android.content.Context;

/**
 * There are several schemes to manage the database connections in an Android app, but as an app gets more complicated,
 * there are many potential places where database locks can occur. This class helps organize and access in a manner that
 * will allow database connection sharing between multiple threads in a single app.
 * 
 * By default, this class uses the {@link ClassNameProvidedOpenHelperFactory} to generate the open helper object -- see
 * its Javadocs for more information about how it works. You can also call {@link #setOpenHelperFactory} with an
 * instance your own {@link SqliteOpenHelperFactory} or call {@link #setOpenHelper} the name of your helper class
 * directly. The helper instance will be kept in a static field and only released once its internal usage count goes to
 * 0.
 * 
 * The SQLiteOpenHelper and database classes maintain one connection under the hood, and prevent locks in the java code.
 * Creating multiple connections can potentially be a source of trouble. This class shares the same connection instance
 * between multiple clients, which will allow multiple activities and services to run at the same time.
 * 
 * @author kevingalligan, graywatson
 */
public class OpenHelperManager {

	private static SqliteOpenHelperFactory factory;
	private static volatile OrmLiteSqliteOpenHelper helper = null;
	private static AtomicInteger instanceCount = new AtomicInteger();
	@SuppressWarnings("unused")
	private static String LOG_NAME = OpenHelperManager.class.getName();
	private static Object helperLock = new Object();

	/**
	 * Set the manager with your own helper factory. Default is to use the {@link ClassNameProvidedOpenHelperFactory}.
	 */
	public static void setOpenHelperFactory(SqliteOpenHelperFactory factory) {
		OpenHelperManager.factory = factory;
	}

	/**
	 * Set the manager with your own helper instance. Default is to use the {@link ClassNameProvidedOpenHelperFactory}
	 * to generate an instance.
	 */
	public static void setOpenHelper(OrmLiteSqliteOpenHelper helper) {
		OpenHelperManager.helper = helper;
	}

	/**
	 * Get the static instance of our open helper. This has a usage counter on it so make sure all calls to this method
	 * have an associated call to {@link #release()}.
	 */
	public static OrmLiteSqliteOpenHelper getHelper(Context context) {
		if (helper == null) {
			synchronized (helperLock) {
				if (helper == null) {
					if (factory == null) {
						setOpenHelperFactory(new ClassNameProvidedOpenHelperFactory());
					}
					// Log.d(LOG_NAME, "Zero instances.  Creating helper.");
					helper = factory.getHelper(context);
					instanceCount.set(0);
				}
			}
		}

		@SuppressWarnings("unused")
		int instC = instanceCount.incrementAndGet();
		// Log.d(LOG_NAME, "helper instance count: " + instC);
		return helper;
	}

	/**
	 * Release the helper that was previous returned by a call to {@link #getHelper(Context)}. This will decrement the
	 * usage counter and close the helper if the counter is 0.
	 */
	public static void release() {
		int instC = instanceCount.decrementAndGet();
		// Log.d(LOG_NAME, "helper instance count: " + instC);
		if (instC == 0) {
			synchronized (helperLock) {
				if (helper != null) {
					// Log.d(LOG_NAME, "Zero instances.  Closing helper.");
					helper.close();
					helper = null;
				}
			}
		} else if (instC < 0) {
			throw new IllegalStateException("Too many calls to release helper.  Instance count = " + instC);
		}
	}

	/**
	 * Factory for providing open helpers.
	 */
	public interface SqliteOpenHelperFactory {

		/**
		 * Create and return an open helper associated with the context.
		 */
		public OrmLiteSqliteOpenHelper getHelper(Context c);
	}
}
