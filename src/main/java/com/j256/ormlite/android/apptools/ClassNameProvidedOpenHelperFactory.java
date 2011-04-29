package com.j256.ormlite.android.apptools;

import java.lang.reflect.Constructor;

import android.content.Context;

/**
 * The default helper factory. This uses the "open_helper_classname" string identifier in your context as the class-name
 * of your helper class.
 * 
 * @deprecated We are using other mechanisms now to inject the helper class. See
 *             {@link OpenHelperManager#getHelper(Context, java.lang.reflect.Type)}.
 * @author kevingalligan
 */
@Deprecated
public class ClassNameProvidedOpenHelperFactory implements OpenHelperManager.SqliteOpenHelperFactory {

	public OrmLiteSqliteOpenHelper getHelper(Context c) {
		int id = c.getResources().getIdentifier("open_helper_classname", "string", c.getPackageName());
		if (id == 0) {
			throw new IllegalStateException("string resrouce open_helper_classname required");
		}

		String className = c.getResources().getString(id);
		try {
			Class<?> helperClass = Class.forName(className);
			Constructor<?> constructor = helperClass.getConstructor(Context.class);
			return (OrmLiteSqliteOpenHelper) constructor.newInstance(c);
		} catch (Exception e) {
			throw new IllegalStateException("Count not create helper instance for class " + className, e);
		}
	}
}
