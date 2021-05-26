package com.j256.ormlite.android.processor.inputs;

import com.j256.ormlite.field.DatabaseFieldConfig;
import com.j256.ormlite.table.DatabaseTableConfig;
import java.util.ArrayList;
import java.util.List;

public final class UnnamedTableWithDefaultForeignCollectionField_TableConfig {
	private UnnamedTableWithDefaultForeignCollectionField_TableConfig() {
	}

	public static DatabaseTableConfig<?> createConfig() {
		List<DatabaseFieldConfig> databaseFieldConfigs = new ArrayList<DatabaseFieldConfig>();
		DatabaseFieldConfig numbersFieldConfig = new DatabaseFieldConfig(
				"numbers");
		numbersFieldConfig.setForeignCollection(true);
		databaseFieldConfigs.add(numbersFieldConfig);
		return new DatabaseTableConfig<UnnamedTableWithDefaultForeignCollectionField>(
				UnnamedTableWithDefaultForeignCollectionField.class,
				"unnamedtablewithdefaultforeigncollectionfield",
				databaseFieldConfigs);
	}
}