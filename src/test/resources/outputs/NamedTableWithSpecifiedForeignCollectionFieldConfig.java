package com.j256.ormlite.android.apptools.tableconfigs;

import java.util.ArrayList;
import java.util.List;

import com.j256.ormlite.field.DatabaseFieldConfig;
import com.j256.ormlite.table.DatabaseTableConfig;

public final class NamedTableWithSpecifiedForeignCollectionFieldConfig {
	private NamedTableWithSpecifiedForeignCollectionFieldConfig() {
	}

	public static final DatabaseTableConfig<inputs.NamedTableWithSpecifiedForeignCollectionField> CONFIG;

	static {
		List<DatabaseFieldConfig> databaseFieldConfigs = new ArrayList<DatabaseFieldConfig>();
		{
			DatabaseFieldConfig databaseFieldConfig = new DatabaseFieldConfig("numbers");
			databaseFieldConfig.setColumnName("column");
			databaseFieldConfig.setForeignCollection(true);
			databaseFieldConfig.setForeignCollectionEager(true);
			databaseFieldConfig.setForeignCollectionMaxEagerLevel(5);
			databaseFieldConfig.setForeignCollectionColumnName("column");
			databaseFieldConfig.setForeignCollectionOrderColumnName("order_column");
			databaseFieldConfig.setForeignCollectionOrderAscending(false);
			databaseFieldConfig.setForeignCollectionForeignFieldName("foreign_field");
			databaseFieldConfigs.add(databaseFieldConfig);
		}
		{
			DatabaseFieldConfig databaseFieldConfig = new DatabaseFieldConfig("numbers_deprecated");
			databaseFieldConfig.setForeignCollection(true);
			databaseFieldConfig.setForeignCollectionMaxEagerLevel(5);
			databaseFieldConfig.setForeignCollectionForeignFieldName("foreign_field");
			databaseFieldConfigs.add(databaseFieldConfig);
		}
		CONFIG = new DatabaseTableConfig<inputs.NamedTableWithSpecifiedForeignCollectionField>(inputs.NamedTableWithSpecifiedForeignCollectionField.class, "table", databaseFieldConfigs);
	}
}
