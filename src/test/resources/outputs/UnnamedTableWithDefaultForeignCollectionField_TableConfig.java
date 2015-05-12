package com.j256.ormlite.android.processor.inputs;

import java.util.ArrayList;
import java.util.List;

import com.j256.ormlite.field.DatabaseFieldConfig;
import com.j256.ormlite.table.DatabaseTableConfig;

public final class UnnamedTableWithDefaultForeignCollectionField_TableConfig {
	private UnnamedTableWithDefaultForeignCollectionField_TableConfig() {
	}

	public static final DatabaseTableConfig<com.j256.ormlite.android.processor.inputs.UnnamedTableWithDefaultForeignCollectionField> CONFIG;

	static {
		List<DatabaseFieldConfig> databaseFieldConfigs = new ArrayList<DatabaseFieldConfig>();
		{
			DatabaseFieldConfig databaseFieldConfig = new DatabaseFieldConfig("numbers");
			databaseFieldConfig.setForeignCollection(true);
			databaseFieldConfigs.add(databaseFieldConfig);
		}
		CONFIG = new DatabaseTableConfig<com.j256.ormlite.android.processor.inputs.UnnamedTableWithDefaultForeignCollectionField>(com.j256.ormlite.android.processor.inputs.UnnamedTableWithDefaultForeignCollectionField.class, "unnamedtablewithdefaultforeigncollectionfield", databaseFieldConfigs);
	}
}
