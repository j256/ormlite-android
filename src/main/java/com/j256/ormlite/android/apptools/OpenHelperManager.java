package com.j256.ormlite.android.apptools;

import java.util.concurrent.atomic.AtomicInteger;

import android.content.Context;
import android.util.Log;

/**
 * There are several schemes to manage the database connections in an Android app, but as an app gets more complicated,
 * there are many potential places where database locks can occur. This class helps organize database creation and
 * access in a manner that will allow database connection sharing between multiple threads in a single app.
 * 
 * To use this class, you must either call init with an instance of SQLiteOpenHelperFactory, or (more commonly) provide
 * the name of your helper class in the Android resource "@string" under "open_helper_classname". The factory simply
 * creates your SQLiteOpenHelper instance. This will only be called once per app VM instance and kept in a static field.
 * 
 * The SQLiteOpenHelper and database classes maintain one connection under the hood, and prevent locks in the java code.
 * Creating multiple connections can potentially be a source of trouble. This class shares the same connection instance
 * between multiple clients, which will allow multiple activities and services to run at the same time.
 * 
 * @author kevingalligan
 */
public class OpenHelperManager {

	private static SqliteOpenHelperFactory factory;
	private static volatile OrmLiteSqliteOpenHelper instance = null;
	private static AtomicInteger instanceCount = new AtomicInteger();
	private static String LOG_NAME = OpenHelperManager.class.getName();

	/**
	 * Initialize the manager with your own helper factory. Default is to use the
	 * {@link ClassNameProvidedOpenHelperFactory}.
	 */
	public static void init(SqliteOpenHelperFactory factory) {
		OpenHelperManager.factory = factory;
	}

	/**
	 * Get the static instance of our open helper. This has a usage counter on it so make sure all calls to this method
	 * have an associated call to {@link #release()}.
	 */
	public static synchronized OrmLiteSqliteOpenHelper getHelper(Context context) {
		if (factory == null) {
			ClassNameProvidedOpenHelperFactory fact = new ClassNameProvidedOpenHelperFactory();
			init(fact);
		}

		if (instance == null) {
			Log.d(LOG_NAME, "Zero instances.  Creating helper.");
			instance = factory.getHelper(context);
			// let's be careful out there
			instanceCount.set(0);
		}

		int instC = instanceCount.incrementAndGet();
		Log.d(LOG_NAME, "helper instance count: " + instC);
		return instance;
	}

	/**
	 * Release the helper that was previous returned by a call to {@link #getHelper(Context)}. This will decrement the
	 * usage counter and close the helper if the counter is 0.
	 */
	public static synchronized void release() {
		int instC = instanceCount.decrementAndGet();
		Log.d(LOG_NAME, "helper instance count: " + instC);
		if (instC == 0) {
			if (instance != null) {
				Log.d(LOG_NAME, "Zero instances.  Closing helper.");
				instance.close();
				instance = null;
			}
		} else if (instC < 0) {
			throw new IllegalStateException("Too many calls to release helper.");
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
