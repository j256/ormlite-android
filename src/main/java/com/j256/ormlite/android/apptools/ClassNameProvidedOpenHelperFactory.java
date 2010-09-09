package com.j256.ormlite.android.apptools;

import java.lang.reflect.Constructor;

import android.content.Context;

/**
 * The default helper factory. Provide the "open_helper_classname" in the "@string" Android resource.
 * 
 * @author kevingalligan
 */
public class ClassNameProvidedOpenHelperFactory implements AndroidSqliteManager.SqliteOpenHelperFactory {

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
