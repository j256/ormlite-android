package com.j256.ormlite.android.apptools;

import java.lang.reflect.Constructor;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.concurrent.atomic.AtomicInteger;

import android.content.Context;

/**
 * This helps organize and access database connections to optimize connection sharing. There are several schemes to
 * manage the database connections in an Android app, but as an app gets more complicated, there are many potential
 * places where database locks can occur. This class allows database connection sharing between multiple threads in a
 * single app.
 * 
 * This gets injected or called with the {@link OrmLiteSqliteOpenHelper} class that is used to manage the database
 * connection. The helper instance will be kept in a static field and only released once its internal usage count goes
 * to 0.
 * 
 * The SQLiteOpenHelper and database classes maintain one connection under the hood, and prevent locks in the java code.
 * Creating multiple connections can potentially be a source of trouble. This class shares the same connection instance
 * between multiple clients, which will allow multiple activities and services to run at the same time. Every time you
 * use the helper, you should call {@link #getHelper(Context, Class)} on this class. When you are done with the helper
 * you should call {@link #release()}.
 * 
 * @author graywatson, kevingalligan
 */
public class OpenHelperManager {

	private static final String HELPER_CLASS_RESOURCE_NAME = "open_helper_classname";

	private static SqliteOpenHelperFactory factory = null;
	private static Class<? extends OrmLiteSqliteOpenHelper> helperClass = null;
	private static volatile OrmLiteSqliteOpenHelper helper = null;
	private static AtomicInteger instanceCount = new AtomicInteger();
	// private static String LOG_NAME = OpenHelperManager.class.getName();
	private static Object helperLock = new Object();

	/**
	 * Set the manager with your own helper factory. This is now a noop.
	 * 
	 * @deprecated You should either use {@link #setOpenHelperClass(Class)} or call {@link #getHelper(Context)}.
	 */
	@Deprecated
	public static void setOpenHelperFactory(SqliteOpenHelperFactory factory) {
		OpenHelperManager.factory = factory;
	}

	/**
	 * If you are _not_ using the {@link OrmLiteBaseActivity} type classes then you will need to call this in a static
	 * method in your code.
	 */
	public static void setOpenHelperClass(Class<? extends OrmLiteSqliteOpenHelper> openHelperClass) {
		innerSetHelperClass(openHelperClass);
	}

	/**
	 * Get the static instance of our open helper. This has a usage counter on it so make sure all calls to this method
	 * have an associated call to {@link #release()}.
	 */
	public static OrmLiteSqliteOpenHelper getHelper(Context context) {
		if (helper == null) {
			synchronized (helperLock) {
				if (helper == null) {
					Context appContext = context.getApplicationContext();
					if (factory == null) {
						if (helperClass == null) {
							innerSetHelperClass(lookupHelperClass(appContext, context.getClass()));
						}
						helper = constructHelper(helperClass, appContext);
					} else {
						helper = factory.getHelper(appContext);
					}
					// Log.d(LOG_NAME, "Zero instances.  Created helper.");
					instanceCount.set(0);
				}
			}
		}

		instanceCount.incrementAndGet();
		// Log.d(LOG_NAME, "helper instance count: " + instC);
		return helper;
	}

	/**
	 * Release the helper that was previous returned by a call to one of the getHelper methods. This will decrement the
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
	 * Set the helper class and make sure we aren't changing it to another class.
	 */
	private static void innerSetHelperClass(Class<? extends OrmLiteSqliteOpenHelper> openHelperClass) {
		// make sure if that there are not 2 helper classes in an application
		if (helperClass == null) {
			helperClass = openHelperClass;
		} else if (helperClass != openHelperClass) {
			throw new IllegalStateException("Helper class was " + helperClass + " but is trying to be reset to "
					+ openHelperClass);
		}
	}

	/**
	 * Call the constructor on our helper class.
	 */
	private static OrmLiteSqliteOpenHelper constructHelper(Class<? extends OrmLiteSqliteOpenHelper> openHelperClass,
			Context context) {
		Constructor<?> constructor;
		try {
			constructor = openHelperClass.getConstructor(Context.class);
		} catch (Exception e) {
			throw new IllegalStateException("Could not find constructor that takes a Context argument for "
					+ openHelperClass, e);
		}
		try {
			return (OrmLiteSqliteOpenHelper) constructor.newInstance(context);
		} catch (Exception e) {
			throw new IllegalStateException("Could not construct instance of helper class " + openHelperClass, e);
		}
	}

	/**
	 * Lookup the helper class either from the resource string or by looking for a generic parameter.
	 */
	private static Class<? extends OrmLiteSqliteOpenHelper> lookupHelperClass(Context context, Class<?> componentClass) {

		// see if we have the magic resource class name set
		int resourceId =
				context.getResources().getIdentifier(HELPER_CLASS_RESOURCE_NAME, "string", context.getPackageName());
		if (resourceId != 0) {
			String className = context.getResources().getString(resourceId);
			try {
				@SuppressWarnings("unchecked")
				Class<? extends OrmLiteSqliteOpenHelper> castClass =
						(Class<? extends OrmLiteSqliteOpenHelper>) Class.forName(className);
				return castClass;
			} catch (Exception e) {
				throw new IllegalStateException("Could not create helper instance for class " + className, e);
			}
		}

		// try walking the context to see if we can get the class from a parameter
		for (; componentClass != null; componentClass = componentClass.getSuperclass()) {
			Type superType = componentClass.getGenericSuperclass();
			if (superType == null || !(superType instanceof ParameterizedType)) {
				continue;
			}
			Type[] types = ((ParameterizedType) superType).getActualTypeArguments();
			if (types == null || types.length == 0) {
				continue;
			}
			if (!(types[0] instanceof Class)) {
				continue;
			}
			Class<?> clazz = (Class<?>) types[0];
			if (OrmLiteSqliteOpenHelper.class.isAssignableFrom(clazz)) {
				@SuppressWarnings("unchecked")
				Class<? extends OrmLiteSqliteOpenHelper> castOpenHelperClass =
						(Class<? extends OrmLiteSqliteOpenHelper>) clazz;
				return castOpenHelperClass;
			}
		}
		throw new IllegalStateException(
				"Could not find OpenHelperClass because none of its generic parameters extends OrmLiteSqliteOpenHelper: "
						+ componentClass);
	}

	/**
	 * Factory for providing open helpers.
	 * 
	 * @deprecated We are using other mechanisms now to inject the helper class. See
	 *             {@link OpenHelperManager#getHelper(Context, java.lang.reflect.Type)}.
	 */
	@Deprecated
	public interface SqliteOpenHelperFactory {

		/**
		 * Create and return an open helper associated with the context.
		 */
		public OrmLiteSqliteOpenHelper getHelper(Context context);
	}
}
