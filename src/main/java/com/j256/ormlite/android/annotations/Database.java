package com.j256.ormlite.android.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This class is applied to a class derived from
 * {@link com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper} and lists
 * the classes representing the tables in the database. Compile-time annotation
 * processing will generate a method that can be called from your constructor to
 * cache table information to avoid slow reflection on Android. This
 * functionality replaces table configuration files which achieved the same goal
 * by manually creating a text file at build time and parsing it at runtime.
 * 
 * Add a call to YOUR_CLASS_NAME_TableConfig.cacheTableConfigurations() to your
 * constructor to make use of this functionality. You can also call
 * YOUR_CLASS_NAME_TableConfig.createTables(connectionSource) from your onCreate
 * to create all tables included in this annotation.
 * 
 * For inner/nested classes, the generated class name will use underscores to
 * separate the classes (e.g. package.Outer.Inner will result in
 * package.Outer_Inner_TableConfig begin generated).
 * 
 * @author nathancrouther
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface Database {
	Class<?>[] value();
}
