package com.j256.ormlite.android.processor.inputs;

import java.util.ArrayList;
import java.util.List;

import com.j256.ormlite.field.DatabaseFieldConfig;
import com.j256.ormlite.table.DatabaseTableConfig;

public final class UnnamedTableWithDefaultDatabaseField_TableConfig {
	private UnnamedTableWithDefaultDatabaseField_TableConfig() {
	}

	public static final DatabaseTableConfig<inputs.UnnamedTableWithDefaultDatabaseField> CONFIG;

	static {
		List<DatabaseFieldConfig> databaseFieldConfigs = new ArrayList<DatabaseFieldConfig>();
		{
			DatabaseFieldConfig databaseFieldConfig = new DatabaseFieldConfig("field");
			databaseFieldConfigs.add(databaseFieldConfig);
		}
		CONFIG = new DatabaseTableConfig<inputs.UnnamedTableWithDefaultDatabaseField>(inputs.UnnamedTableWithDefaultDatabaseField.class, "unnamedtablewithdefaultdatabasefield", databaseFieldConfigs);
	}
}
