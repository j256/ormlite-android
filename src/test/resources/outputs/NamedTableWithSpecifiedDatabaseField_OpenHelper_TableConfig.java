package com.j256.ormlite.android.processor.inputs;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.DatabaseTableConfig;
import com.j256.ormlite.table.TableUtils;

public final class NamedTableWithSpecifiedDatabaseField_OpenHelper_TableConfig {
	private NamedTableWithSpecifiedDatabaseField_OpenHelper_TableConfig() {
	}

	public static void cacheTableConfigurations() {
		List<DatabaseTableConfig<?>> tableConfigs = new ArrayList<DatabaseTableConfig<?>>();
		tableConfigs.add(com.j256.ormlite.android.processor.inputs.NamedTableWithSpecifiedDatabaseField_TableConfig.CONFIG);
		DaoManager.addCachedDatabaseConfigs(tableConfigs);
	}

	public static void createTables(ConnectionSource connectionSource) throws SQLException {
		TableUtils.createTable(connectionSource, com.j256.ormlite.android.processor.inputs.NamedTableWithSpecifiedDatabaseField.class);
	}
}
