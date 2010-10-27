package com.j256.ormlite.android;

import java.sql.SQLException;

import android.database.sqlite.SQLiteOpenHelper;

import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.db.DatabaseType;
import com.j256.ormlite.db.SqliteAndroidDatabaseType;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.support.DatabaseConnection;

/**
 * Main connection source. Uses the standard Android SQLiteOpenHelper. For best results, use our helper,
 * 
 * @see OrmLiteSqliteOpenHelper
 * 
 * @author kevingalligan, graywatson
 */
public class AndroidConnectionSource implements ConnectionSource {

	private SQLiteOpenHelper helper;
	private DatabaseConnection readOnlyConnection = null;
	private DatabaseConnection readWriteConnection = null;
	private DatabaseType databaseType = new SqliteAndroidDatabaseType();
	private boolean useThreadLocal = false;
	private final ThreadLocal<DatabaseConnection> savedConnection = new ThreadLocal<DatabaseConnection>();

	public AndroidConnectionSource(SQLiteOpenHelper helper) {
		this.helper = helper;
	}

	public DatabaseConnection getReadOnlyConnection() throws SQLException {
		if (useThreadLocal) {
			DatabaseConnection conn = savedConnection.get();
			if (conn != null) {
				return conn;
			}
		}
		if (readOnlyConnection == null) {
			readOnlyConnection = new AndroidDatabaseConnection(helper.getReadableDatabase(), false);
		}
		return readOnlyConnection;
	}

	public DatabaseConnection getReadWriteConnection() throws SQLException {
		if (useThreadLocal) {
			DatabaseConnection conn = savedConnection.get();
			if (conn != null) {
				return conn;
			}
		}
		if (readWriteConnection == null) {
			readWriteConnection = new AndroidDatabaseConnection(helper.getWritableDatabase(), true);
		}
		return readWriteConnection;
	}

	public void releaseConnection(DatabaseConnection connection) throws SQLException {
		// noop since connection management is handled by AndroidOS
	}

	public void saveSpecialConnection(DatabaseConnection connection) {
		/*
		 * We can't just set the read-only and read-write connections to be the connection since it is not synchronized
		 * and other threads may not get the reset in the clear method.
		 */
		useThreadLocal = true;
		savedConnection.set(connection);
	}

	public void clearSpecialConnection(DatabaseConnection connection) {
		useThreadLocal = false;
		savedConnection.set(null);
	}

	public void close() {
		// the helper is closed so it calls close here, so this CANNOT be a call back to helper.close()
	}

	public DatabaseType getDatabaseType() {
		return databaseType;
	}
}
