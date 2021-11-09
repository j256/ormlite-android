package com.j256.ormlite.android.processor.inputs;

import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.DatabaseTableConfig;
import com.j256.ormlite.table.TableUtils;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class UnnamedTableWithDefaultDatabaseField_OpenHelper_TableConfig {
	private UnnamedTableWithDefaultDatabaseField_OpenHelper_TableConfig() {
	}

	public static void cacheTableConfigurations() {
		List<DatabaseTableConfig<?>> tableConfigs = new ArrayList<DatabaseTableConfig<?>>();
		tableConfigs.add(UnnamedTableWithDefaultDatabaseField_TableConfig
				.createConfig());
		DaoManager.addCachedDatabaseConfigs(tableConfigs);
	}

	public static void createTables(ConnectionSource connectionSource)
			throws SQLException {
		TableUtils.createTable(connectionSource,
				UnnamedTableWithDefaultDatabaseField.class);
	}
}