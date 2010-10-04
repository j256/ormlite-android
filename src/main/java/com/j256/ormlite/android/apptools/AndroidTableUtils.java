package com.j256.ormlite.android.apptools;

import java.sql.SQLException;

import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.DatabaseTableConfig;
import com.j256.ormlite.table.TableUtils;

/**
 * @deprecated Use {@link TableUtils}
 */
@Deprecated
public class AndroidTableUtils {

	/**
	 * @deprecated Use {@link TableUtils#createTable(ConnectionSource, Class)}
	 */
	@Deprecated
	public static <T> int createTable(ConnectionSource connectionSource, Class<T> dataClass) throws SQLException {
		return TableUtils.createTable(connectionSource, dataClass);
	}

	/**
	 * @deprecated Use {@link TableUtils#createTable(ConnectionSource, DatabaseTableConfig)}
	 */
	@Deprecated
	public static <T> int createTable(ConnectionSource connectionSource, DatabaseTableConfig<T> tableConfig)
			throws SQLException {
		return TableUtils.createTable(connectionSource, tableConfig);
	}

	/**
	 * @deprecated Use {@link TableUtils#dropTable(com.j256.ormlite.db.DatabaseType, ConnectionSource, Class, boolean)}
	 */
	@Deprecated
	public static <T> int dropTable(ConnectionSource connectionSource, Class<T> dataClass, boolean ignoreErrors)
			throws SQLException {
		return TableUtils.dropTable(connectionSource, dataClass, ignoreErrors);
	}

	/**
	 * @deprecated Use
	 *             {@link TableUtils#dropTable(com.j256.ormlite.db.DatabaseType, ConnectionSource, DatabaseTableConfig, boolean)}
	 */
	@Deprecated
	public static <T> int dropTable(ConnectionSource connectionSource, DatabaseTableConfig<T> tableConfig,
			boolean ignoreErrors) throws SQLException {
		return TableUtils.dropTable(connectionSource, tableConfig, ignoreErrors);
	}
}
