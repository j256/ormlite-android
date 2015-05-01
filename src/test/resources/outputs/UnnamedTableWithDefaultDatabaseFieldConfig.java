package com.j256.ormlite.android.apptools.tableconfigs;

import java.util.ArrayList;
import java.util.List;

import com.j256.ormlite.field.DatabaseFieldConfig;
import com.j256.ormlite.table.DatabaseTableConfig;

import javax.annotation.Generated;

@Generated("com.j256.ormlite.android.apptools.OrmLiteAnnotationProcessor")
public final class UnnamedTableWithDefaultDatabaseFieldConfig {
	private UnnamedTableWithDefaultDatabaseFieldConfig() {
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
