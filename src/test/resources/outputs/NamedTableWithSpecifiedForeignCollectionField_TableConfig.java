package com.j256.ormlite.android.processor.inputs;

import com.j256.ormlite.field.DatabaseFieldConfig;
import com.j256.ormlite.table.DatabaseTableConfig;
import java.util.ArrayList;
import java.util.List;

public final class NamedTableWithSpecifiedForeignCollectionField_TableConfig {
	private NamedTableWithSpecifiedForeignCollectionField_TableConfig() {
	}

	public static DatabaseTableConfig<?> createConfig() {
		List<DatabaseFieldConfig> databaseFieldConfigs = new ArrayList<DatabaseFieldConfig>();
		DatabaseFieldConfig numbersFieldConfig = new DatabaseFieldConfig(
				"numbers");
		numbersFieldConfig.setColumnName("column");
		numbersFieldConfig.setForeignCollection(true);
		numbersFieldConfig.setForeignCollectionEager(true);
		numbersFieldConfig.setForeignCollectionMaxEagerLevel(5);
		numbersFieldConfig.setForeignCollectionColumnName("column");
		numbersFieldConfig.setForeignCollectionOrderColumnName("order_column");
		numbersFieldConfig.setForeignCollectionOrderAscending(false);
		numbersFieldConfig
				.setForeignCollectionForeignFieldName("foreign_field");
		databaseFieldConfigs.add(numbersFieldConfig);
		DatabaseFieldConfig numbers_deprecatedFieldConfig = new DatabaseFieldConfig(
				"numbers_deprecated");
		numbers_deprecatedFieldConfig.setForeignCollection(true);
		numbers_deprecatedFieldConfig.setForeignCollectionMaxEagerLevel(5);
		numbers_deprecatedFieldConfig
				.setForeignCollectionForeignFieldName("foreign_field");
		databaseFieldConfigs.add(numbers_deprecatedFieldConfig);
		return new DatabaseTableConfig<NamedTableWithSpecifiedForeignCollectionField>(
				NamedTableWithSpecifiedForeignCollectionField.class, "table",
				databaseFieldConfigs);
	}
}