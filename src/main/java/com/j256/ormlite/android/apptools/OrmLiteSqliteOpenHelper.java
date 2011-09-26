package com.j256.ormlite.android.apptools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.SQLException;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;

import com.j256.ormlite.android.AndroidConnectionSource;
import com.j256.ormlite.android.AndroidDatabaseConnection;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.logger.Logger;
import com.j256.ormlite.logger.LoggerFactory;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.support.DatabaseConnection;
import com.j256.ormlite.table.DatabaseTableConfigLoader;

/**
 * SQLite database open helper which can be extended by your application to help manage when the application needs to
 * create or upgrade its database.
 * 
 * @author kevingalligan, graywatson
 */
public abstract class OrmLiteSqliteOpenHelper extends SQLiteOpenHelper {

	private Logger logger = LoggerFactory.getLogger(getClass());
	private AndroidConnectionSource connectionSource = new AndroidConnectionSource(this);
	private volatile boolean isOpen = true;

	public OrmLiteSqliteOpenHelper(Context context, String databaseName, CursorFactory factory, int databaseVersion) {
		super(context, databaseName, factory, databaseVersion);
	}

	/**
	 * Same as the other constructor with the addition of a file-id of the table config-file. See
	 * {@link OrmLiteConfigUtil} for details.
	 * 
	 * @param configFileId
	 *            file-id which probably should be a R.raw.ormlite_config.txt or some static value.
	 */
	public OrmLiteSqliteOpenHelper(Context context, String databaseName, CursorFactory factory, int databaseVersion,
			int configFileId) {
		this(context, databaseName, factory, databaseVersion, openFileId(context, configFileId));
	}

	/**
	 * Same as the other constructor with the addition of a config-file. See {@link OrmLiteConfigUtil} for details.
	 * 
	 * @param configFile
	 *            Configuration file to be loaded.
	 */
	public OrmLiteSqliteOpenHelper(Context context, String databaseName, CursorFactory factory, int databaseVersion,
			File configFile) {
		this(context, databaseName, factory, databaseVersion, openFile(configFile));
	}

	/**
	 * Same as the other constructor with the addition of a input stream to the table config-file. See
	 * {@link OrmLiteConfigUtil} for details.
	 * 
	 * @param stream
	 *            Stream opened to the configuration file to be loaded.
	 */
	public OrmLiteSqliteOpenHelper(Context context, String databaseName, CursorFactory factory, int databaseVersion,
			InputStream stream) {
		super(context, databaseName, factory, databaseVersion);
		if (stream == null) {
			return;
		}

		// if a config file-id was specified then load it into the DaoManager
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(stream), 4096);
			DaoManager.addCachedDatabaseConfigs(DatabaseTableConfigLoader.loadDatabaseConfigFromReader(reader));
		} catch (SQLException e) {
			throw new IllegalStateException("Could not load object config file", e);
		} finally {
			try {
				// we close the stream here because we may not get a reader
				stream.close();
			} catch (IOException e) {
				// ignore close errors
			}
		}
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
		if (!isOpen) {
			// we don't throw this exception, but log it for debugging purposes
			logger.error(new IllegalStateException(), "Getting connectionSource called after closed");
		}
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
		DatabaseConnection conn = cs.getSpecialConnection();
		boolean clearSpecial = false;
		if (conn == null) {
			conn = new AndroidDatabaseConnection(db, true);
			try {
				cs.saveSpecialConnection(conn);
				clearSpecial = true;
			} catch (SQLException e) {
				throw new IllegalStateException("Could not save special connection", e);
			}
		}
		try {
			onCreate(db, cs);
		} finally {
			if (clearSpecial) {
				cs.clearSpecialConnection(conn);
			}
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
		DatabaseConnection conn = cs.getSpecialConnection();
		boolean clearSpecial = false;
		if (conn == null) {
			conn = new AndroidDatabaseConnection(db, true);
			try {
				cs.saveSpecialConnection(conn);
				clearSpecial = true;
			} catch (SQLException e) {
				throw new IllegalStateException("Could not save special connection", e);
			}
		}
		try {
			onUpgrade(db, cs, oldVersion, newVersion);
		} finally {
			if (clearSpecial) {
				cs.clearSpecialConnection(conn);
			}
		}
	}

	/**
	 * Close any open connections.
	 */
	@Override
	public void close() {
		super.close();
		connectionSource.close();
		/*
		 * We used to set connectionSource to null here but now we just set the closed flag and then log heavily if
		 * someone uses getConectionSource() after this point.
		 */
		isOpen = false;
	}

	/**
	 * Return true if the helper is still open. Once {@link #close()} is called then this will return false.
	 */
	public boolean isOpen() {
		return isOpen;
	}

	/**
	 * Get a DAO for our class. This stores the DAO in a cache for reuse.
	 */
	public <D extends Dao<T, ?>, T> D getDao(Class<T> clazz) throws SQLException {
		@SuppressWarnings("unchecked")
		D dao = (D) DaoManager.createDao(getConnectionSource(), clazz);
		return dao;
	}

	private static InputStream openFileId(Context context, int fileId) {
		InputStream stream = context.getResources().openRawResource(fileId);
		if (stream == null) {
			throw new IllegalStateException("Could not find object config file with id " + fileId);
		}
		return stream;
	}

	private static InputStream openFile(File configFile) {
		try {
			if (configFile == null) {
				return null;
			} else {
				return new FileInputStream(configFile);
			}
		} catch (FileNotFoundException e) {
			throw new IllegalArgumentException("Could not open config file " + configFile, e);
		}
	}
}
