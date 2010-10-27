package com.j256.ormlite.android.apptools;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase.CursorFactory;

import com.j256.ormlite.android.AndroidConnectionSource;
import com.j256.ormlite.android.AndroidDatabaseConnection;
import com.j256.ormlite.support.ConnectionSource;

/**
 * SQLite database open helper which can be extended by your application to help manage when the application needs to
 * create or upgrade its database.
 * 
 * @author kevingalligan
 */
public abstract class OrmLiteSqliteOpenHelper extends SQLiteOpenHelper {

	private AndroidConnectionSource connectionSource = new AndroidConnectionSource(this);

	public OrmLiteSqliteOpenHelper(Context context, String databaseName, CursorFactory factory, int databaseVersion) {
		super(context, databaseName, factory, databaseVersion);
	}

	/**
	 * What to do when your database needs to be created. Usually this entails creating the tables and loading any
	 * initial data.
	 * 
	 * <p>
	 * <b>NOTE:</b> You should use the connectionSource argument that is passed into this method call or the one
	 * returned by getConnectionSource(). If you use your own, a recursive call or other unexpected results may result.
	 * </p>
	 * 
	 * @param database
	 *            Database being created.
	 * @param connectionSource
	 *            To use get connections to the database to be created.
	 */
	public abstract void onCreate(SQLiteDatabase database, ConnectionSource connectionSource);

	/**
	 * What to do when your database needs to be updated. This could mean careful migration of old data to new data.
	 * Maybe adding or deleting database columns, etc..
	 * 
	 * <p>
	 * <b>NOTE:</b> You should use the connectionSource argument that is passed into this method call or the one
	 * returned by getConnectionSource(). If you use your own, a recursive call or other unexpected results may result.
	 * </p>
	 * 
	 * @param database
	 *            Database being upgraded.
	 * @param connectionSource
	 *            To use get connections to the database to be updated.
	 * @param oldVersion
	 *            The version of the current database so we can know what to do to the database.
	 * @param newVersion
	 *            The version that we are upgrading the database to.
	 */
	public abstract void onUpgrade(SQLiteDatabase database, ConnectionSource connectionSource, int oldVersion,
			int newVersion);

	/**
	 * Get the connection source associated with the helper.
	 */
	public ConnectionSource getConnectionSource() {
		return connectionSource;
	}

	/**
	 * Satisfies the {@link SQLiteOpenHelper#onCreate(SQLiteDatabase)} interface method.
	 */
	@Override
	public final void onCreate(SQLiteDatabase db) {
		ConnectionSource cs = getConnectionSource();
		/*
		 * The method is called by Android database helper's get-database calls when Android detects that we need to
		 * create or update the database. So we have to use the database argument and save a connection to it on the
		 * AndroidConnectionSource, otherwise it will go recursive if the subclass calls getConnectionSource().
		 */
		AndroidDatabaseConnection conn = new AndroidDatabaseConnection(db, true);
		try {
			cs.saveSpecialConnection(conn);
			onCreate(db, cs);
		} finally {
			cs.clearSpecialConnection(conn);
		}
	}

	/**
	 * Satisfies the {@link SQLiteOpenHelper#onUpgrade(SQLiteDatabase, int, int)} interface method.
	 */
	@Override
	public final void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		ConnectionSource cs = getConnectionSource();
		/*
		 * The method is called by Android database helper's get-database calls when Android detects that we need to
		 * create or update the database. So we have to use the database argument and save a connection to it on the
		 * AndroidConnectionSource, otherwise it will go recursive if the subclass calls getConnectionSource().
		 */
		AndroidDatabaseConnection conn = new AndroidDatabaseConnection(db, true);
		try {
			cs.saveSpecialConnection(conn);
			onUpgrade(db, cs, oldVersion, newVersion);
		} finally {
			cs.clearSpecialConnection(conn);
		}
	}

	/**
	 * Close any open connections.
	 */
	@Override
	public void close() {
		super.close();
		try {
			connectionSource.close();
		} finally {
			connectionSource = null;
		}
	}
}
