package com.j256.ormlite.android.apptools;

import java.lang.reflect.Constructor;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import android.content.Context;

import com.j256.ormlite.logger.Logger;
import com.j256.ormlite.logger.LoggerFactory;

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
 * between multiple clients, which will allow multiple activities and services to run at the same time.
 * 
 * Every time you use the helper, you should call {@link #getHelper(Context)} or {@link #getHelper(Context, Class)}.
 * When you are done with the helper you should call {@link #releaseHelper()}.
 * 
 * @author graywatson, kevingalligan
 */
public class OpenHelperManager {

	private static final String HELPER_CLASS_RESOURCE_NAME = "open_helper_classname";
	private static Logger logger = LoggerFactory.getLogger(OpenHelperManager.class);

	private static Class<? extends OrmLiteSqliteOpenHelper> helperClass = null;
	private static volatile OrmLiteSqliteOpenHelper helper = null;
	private static boolean wasClosed = false;
	private static int instanceCount = 0;
	// private static String LOG_NAME = OpenHelperManager.class.getName();

	/**
	 * If you are _not_ using the {@link OrmLiteBaseActivity} type classes then you will need to call this in a static
	 * method in your code.
	 */
	public static void setOpenHelperClass(Class<? extends OrmLiteSqliteOpenHelper> openHelperClass) {
		innerSetHelperClass(openHelperClass);
	}

	/**
	 * Create a static instance of our open helper. This has a usage counter on it so make sure all calls to this method
	 * have an associated call to {@link #releaseHelper()}. This should be called during an onCreate() type of method
	 * when the application or service is starting. The caller should then keep the helper around until it is shutting
	 * down when {@link #releaseHelper()} should be called.
	 * 
	 * <p>
	 * If multiple parts of your application need the helper, they call can call this as long as they each call release
	 * when they are done.
	 * </p>
	 * 
	 * <p>
	 * To find the helper class, this does the following: <br />
	 * 1) If the factory class (albeit deprecated) was injected it will be used to get the helper. <br />
	 * 2) If the class has been set with a call to {@link #setOpenHelperClass(Class)}, it will be used to construct a
	 * helper. <br />
	 * 3) If the resource class name is configured in the strings.xml file it will be used. <br />
	 * 4) The context class hierarchy is walked looking at the generic parameters for a class extending
	 * OrmLiteSqliteOpenHelper. This is used by the {@link OrmLiteBaseActivity} and other base classes. <br />
	 * 5) An exception is thrown saying that it was not able to set the helper class.
	 * </p>
	 */
	public static synchronized OrmLiteSqliteOpenHelper getHelper(Context context) {
		if (helper == null) {
			if (context == null) {
				throw new IllegalArgumentException("context argument is null");
			}
			if (wasClosed) {
				// this can happen if you are calling get/release and then get again
				logger.info("helper has already been closed and is being re-opened.");
			}
			Context appContext = context.getApplicationContext();
			if (helperClass == null) {
				innerSetHelperClass(lookupHelperClass(appContext, context.getClass()));
			}
			helper = constructHelper(helperClass, appContext);
			// Log.d(LOG_NAME, "Zero instances.  Created helper.");
			instanceCount = 0;
		}

		instanceCount++;
		// Log.d(LOG_NAME, "helper instance count: " + instC);
		return helper;
	}
	/**
	 * Like {@link #getHelper(Context)} but sets the helper class beforehand.
	 */
	public static OrmLiteSqliteOpenHelper getHelper(Context context,
			Class<? extends OrmLiteSqliteOpenHelper> openHelperClass) {
		if (helper == null) {
			innerSetHelperClass(openHelperClass);
		}
		return getHelper(context);
	}

	/**
	 * @deprecated This has been renamed to be {@link #releaseHelper()}.
	 */
	@Deprecated
	public static void release() {
		releaseHelper();
	}

	/**
	 * Release the helper that was previously returned by a call {@link #getHelper(Context)} or
	 * {@link #getHelper(Context, Class)}. This will decrement the usage counter and close the helper if the counter is
	 * 0.
	 * 
	 * <p>
	 * <b> WARNING: </b> This should be called in an onDestroy() type of method when your application or service is
	 * terminating or if your code is no longer going to use the helper or derived DAOs in any way. _Don't_ call this
	 * method if you expect to call {@link #getHelper(Context)} again before the application terminates.
	 * </p>
	 */
	public static synchronized void releaseHelper() {
		instanceCount--;
		// Log.d(LOG_NAME, "helper instance count: " + instanceCount);
		if (instanceCount == 0) {
			if (helper != null) {
				// Log.d(LOG_NAME, "Zero instances.  Closing helper.");
				helper.close();
				helper = null;
				wasClosed = true;
			}
		} else if (instanceCount < 0) {
			throw new IllegalStateException("Too many calls to release helper.  Instance count = " + instanceCount);
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

		// try walking the context class to see if we can get the OrmLiteSqliteOpenHelper from a generic parameter
		for (; componentClass != null; componentClass = componentClass.getSuperclass()) {
			Type superType = componentClass.getGenericSuperclass();
			if (superType == null || !(superType instanceof ParameterizedType)) {
				continue;
			}
			// get the generic type arguments
			Type[] types = ((ParameterizedType) superType).getActualTypeArguments();
			// defense
			if (types == null || types.length == 0) {
				continue;
			}
			for (Type type : types) {
				// defense
				if (!(type instanceof Class)) {
					continue;
				}
				Class<?> clazz = (Class<?>) type;
				if (OrmLiteSqliteOpenHelper.class.isAssignableFrom(clazz)) {
					@SuppressWarnings("unchecked")
					Class<? extends OrmLiteSqliteOpenHelper> castOpenHelperClass =
							(Class<? extends OrmLiteSqliteOpenHelper>) clazz;
					return castOpenHelperClass;
				}
			}
		}
		throw new IllegalStateException(
				"Could not find OpenHelperClass because none of its generic parameters extends OrmLiteSqliteOpenHelper: "
						+ componentClass);
	}
}
