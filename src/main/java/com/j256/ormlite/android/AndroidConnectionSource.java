package com.j256.ormlite.android;

import java.sql.SQLException;

import android.database.sqlite.SQLiteOpenHelper;

import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.db.DatabaseType;
import com.j256.ormlite.db.SqliteAndroidDatabaseType;
import com.j256.ormlite.logger.Logger;
import com.j256.ormlite.logger.LoggerFactory;
import com.j256.ormlite.support.BaseConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.support.DatabaseConnection;

/**
 * Android version of the connection source. Uses the standard Android SQLiteOpenHelper. For best results, use our
 * helper,
 * 
 * @see OrmLiteSqliteOpenHelper
 * 
 * @author kevingalligan, graywatson
 */
public class AndroidConnectionSource extends BaseConnectionSource implements ConnectionSource {

	private static final Logger logger = LoggerFactory.getLogger(AndroidConnectionSource.class);

	private final SQLiteOpenHelper helper;
	private DatabaseConnection connection = null;
	private volatile boolean isOpen = true;
	private final DatabaseType databaseType = new SqliteAndroidDatabaseType();

	public AndroidConnectionSource(SQLiteOpenHelper helper) {
		this.helper = helper;
	}

	public DatabaseConnection getReadOnlyConnection() throws SQLException {
		/*
		 * We have to use the read-write connection because getWritableDatabase() can call close on
		 * getReadableDatabase() in the future. This has something to do with Android's SQLite connection management.
		 * 
		 * See android docs: http://developer.android.com/reference/android/database/sqlite/SQLiteOpenHelper.html
		 */
		return getReadWriteConnection();
	}

	public DatabaseConnection getReadWriteConnection() throws SQLException {
		DatabaseConnection conn = getSavedConnection();
		if (conn != null) {
			return conn;
		}
		if (connection == null) {
			connection = new AndroidDatabaseConnection(helper.getWritableDatabase(), true);
		}
		return connection;
	}

	public void releaseConnection(DatabaseConnection connection) throws SQLException {
		// noop since connection management is handled by AndroidOS
	}

	public boolean saveSpecialConnection(DatabaseConnection connection) throws SQLException {
		return saveSpecial(connection);
	}

	public void clearSpecialConnection(DatabaseConnection connection) {
		clearSpecial(connection, logger);
	}

	public void close() {
		// the helper is closed so it calls close here, so this CANNOT be a call back to helper.close()
		isOpen = false;
	}

	public DatabaseType getDatabaseType() {
		return databaseType;
	}

	public boolean isOpen() {
		return isOpen;
	}
}
