package com.j256.ormlite.android.processor.inputs;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.DatabaseTableConfig;
import com.j256.ormlite.table.TableUtils;

public final class InnerClassTable_OpenHelper_TableConfig {
	private InnerClassTable_OpenHelper_TableConfig() {
	}

	public static void cacheTableConfigurations() {
		List<DatabaseTableConfig<?>> tableConfigs = new ArrayList<DatabaseTableConfig<?>>();
		tableConfigs.add(com.j256.ormlite.android.processor.inputs.InnerClassTable_InnerClass_TableConfig.CONFIG);
		tableConfigs.add(com.j256.ormlite.android.processor.inputs.InnerClassTable_OtherInnerClass_TableConfig.CONFIG);
		DaoManager.addCachedDatabaseConfigs(tableConfigs);
	}

	public static void createTables(ConnectionSource connectionSource) throws SQLException {
		TableUtils.createTable(connectionSource, com.j256.ormlite.android.processor.inputs.InnerClassTable.InnerClass.class);
		TableUtils.createTable(connectionSource, com.j256.ormlite.android.processor.inputs.InnerClassTable.OtherInnerClass.class);
	}
}
