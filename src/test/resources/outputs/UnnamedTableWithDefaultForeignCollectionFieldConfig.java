package com.j256.ormlite.android.apptools.tableconfigs;

import java.util.ArrayList;
import java.util.List;

import com.j256.ormlite.field.DatabaseFieldConfig;
import com.j256.ormlite.table.DatabaseTableConfig;

public final class UnnamedTableWithDefaultForeignCollectionFieldConfig {
	private UnnamedTableWithDefaultForeignCollectionFieldConfig() {
	}

	public static final DatabaseTableConfig<inputs.UnnamedTableWithDefaultForeignCollectionField> CONFIG;

	static {
		List<DatabaseFieldConfig> databaseFieldConfigs = new ArrayList<DatabaseFieldConfig>();
		{
			DatabaseFieldConfig databaseFieldConfig = new DatabaseFieldConfig("numbers");
			databaseFieldConfig.setForeignCollection(true);
			databaseFieldConfigs.add(databaseFieldConfig);
		}
		CONFIG = new DatabaseTableConfig<inputs.UnnamedTableWithDefaultForeignCollectionField>(inputs.UnnamedTableWithDefaultForeignCollectionField.class, "unnamedtablewithdefaultforeigncollectionfield", databaseFieldConfigs);
	}
}
