package com.j256.ormlite.android;

import java.sql.SQLException;

import android.database.sqlite.SQLiteDatabase;

import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.support.DatabaseConnection;

/**
 * Abstract connection source. Supports concrete implementations.
 * 
 * @author kevingalligan, graywatson
 */
public abstract class BaseAndroidConnectionSource implements ConnectionSource {

	private DatabaseConnection readOnlyConnection = null;
	private DatabaseConnection readWriteConnection = null;

	/**
	 * Get a read-only version of our database.
	 */
	protected abstract SQLiteDatabase getReadOnlyDatabase();

	/**
	 * Get a read-write version of our database.
	 */
	protected abstract SQLiteDatabase getReadWriteDatabase();

	public DatabaseConnection getReadOnlyConnection() throws SQLException {
		if (readOnlyConnection == null) {
			readOnlyConnection = new AndroidDatabaseConnection(getReadOnlyDatabase(), false);
		}
		return readOnlyConnection;
	}

	public DatabaseConnection getReadWriteConnection() throws SQLException {
		if (readWriteConnection == null) {
			readWriteConnection = new AndroidDatabaseConnection(getReadWriteDatabase(), true);
		}
		return readWriteConnection;
	}

	public void releaseConnection(DatabaseConnection connection) throws SQLException {
	}

	public void close() throws SQLException {
	}
}
