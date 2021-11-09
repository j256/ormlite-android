package com.j256.ormlite.android.processor.inputs;

import com.j256.ormlite.field.DatabaseFieldConfig;
import com.j256.ormlite.table.DatabaseTableConfig;
import java.util.ArrayList;
import java.util.List;

public final class NamedTableWithSpecifiedDatabaseField_TableConfig {
	private NamedTableWithSpecifiedDatabaseField_TableConfig() {
	}

	public static DatabaseTableConfig<?> createConfig() {
		List<DatabaseFieldConfig> databaseFieldConfigs = new ArrayList<DatabaseFieldConfig>();
		DatabaseFieldConfig fieldFieldConfig = new DatabaseFieldConfig("field");
		fieldFieldConfig.setColumnName("column");
		fieldFieldConfig
				.setDataType(com.j256.ormlite.field.DataType.ENUM_INTEGER);
		fieldFieldConfig.setDefaultValue("VALUE");
		fieldFieldConfig.setWidth(100);
		fieldFieldConfig.setCanBeNull(false);
		fieldFieldConfig.setId(true);
		fieldFieldConfig.setGeneratedId(true);
		fieldFieldConfig.setGeneratedIdSequence("id_sequence");
		fieldFieldConfig.setForeign(true);
		fieldFieldConfig.setUseGetSet(true);
		fieldFieldConfig
				.setUnknownEnumValue(com.j256.ormlite.android.processor.inputs.NamedTableWithSpecifiedDatabaseField.FieldTypeEnum.OTHER_VALUE);
		fieldFieldConfig.setThrowIfNull(true);
		fieldFieldConfig.setFormat("%f");
		fieldFieldConfig.setUnique(true);
		fieldFieldConfig.setUniqueCombo(true);
		fieldFieldConfig.setIndex(true);
		fieldFieldConfig.setUniqueIndex(true);
		fieldFieldConfig.setIndexName("index");
		fieldFieldConfig.setUniqueIndexName("unique_index");
		fieldFieldConfig.setForeignAutoRefresh(true);
		fieldFieldConfig.setMaxForeignAutoRefreshLevel(5);
		fieldFieldConfig
				.setPersisterClass(NamedTableWithSpecifiedDatabaseField.CustomPersister.class);
		fieldFieldConfig.setAllowGeneratedIdInsert(true);
		fieldFieldConfig.setColumnDefinition("INT NOT NULL");
		fieldFieldConfig.setForeignAutoCreate(true);
		fieldFieldConfig.setVersion(true);
		fieldFieldConfig.setForeignColumnName("foreign");
		fieldFieldConfig.setReadOnly(true);
		databaseFieldConfigs.add(fieldFieldConfig);
		return new DatabaseTableConfig<NamedTableWithSpecifiedDatabaseField>(
				NamedTableWithSpecifiedDatabaseField.class, "table",
				databaseFieldConfigs);
	}
}