package com.j256.ormlite.android;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;

/**
 * Main connection source. Uses the standrd android SQLiteOpenHelper. For best results, use our helper,
 * 
 * @see OrmLiteSqliteOpenHelper
 * 
 * @author kevingalligan, graywatson
 */
public class AndroidConnectionSource extends BaseAndroidConnectionSource {

	private SQLiteOpenHelper helper;

	public AndroidConnectionSource(SQLiteOpenHelper helper) {
		this.helper = helper;
	}

	@Override
	protected SQLiteDatabase getReadOnlyDatabase() {
		return helper.getReadableDatabase();
	}

	@Override
	protected SQLiteDatabase getReadWriteDatabase() {
		return helper.getWritableDatabase();
	}
}
