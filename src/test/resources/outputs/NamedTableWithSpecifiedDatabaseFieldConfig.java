package com.j256.ormlite.android.apptools.tableconfigs;

import java.util.ArrayList;
import java.util.List;

import com.j256.ormlite.field.DatabaseFieldConfig;
import com.j256.ormlite.table.DatabaseTableConfig;

public final class NamedTableWithSpecifiedDatabaseFieldConfig {
	private NamedTableWithSpecifiedDatabaseFieldConfig() {
	}

	public static final DatabaseTableConfig<inputs.NamedTableWithSpecifiedDatabaseField> CONFIG;

	static {
		List<DatabaseFieldConfig> databaseFieldConfigs = new ArrayList<DatabaseFieldConfig>();
		{
			DatabaseFieldConfig databaseFieldConfig = new DatabaseFieldConfig("field");
			databaseFieldConfig.setColumnName("column");
			databaseFieldConfig.setDataType(com.j256.ormlite.field.DataType.ENUM_INTEGER);
			databaseFieldConfig.setDefaultValue("VALUE");
			databaseFieldConfig.setWidth(100);
			databaseFieldConfig.setCanBeNull(false);
			databaseFieldConfig.setId(true);
			databaseFieldConfig.setGeneratedId(true);
			databaseFieldConfig.setGeneratedIdSequence("id_sequence");
			databaseFieldConfig.setForeign(true);
			databaseFieldConfig.setUseGetSet(true);
			databaseFieldConfig.setUnknownEnumValue(inputs.NamedTableWithSpecifiedDatabaseField.FieldTypeEnum.OTHER_VALUE);
			databaseFieldConfig.setThrowIfNull(true);
			databaseFieldConfig.setFormat("%f");
			databaseFieldConfig.setUnique(true);
			databaseFieldConfig.setUniqueCombo(true);
			databaseFieldConfig.setIndex(true);
			databaseFieldConfig.setUniqueIndex(true);
			databaseFieldConfig.setIndexName("index");
			databaseFieldConfig.setUniqueIndexName("unique_index");
			databaseFieldConfig.setForeignAutoRefresh(true);
			databaseFieldConfig.setMaxForeignAutoRefreshLevel(5);
			databaseFieldConfig.setPersisterClass(inputs.NamedTableWithSpecifiedDatabaseField.CustomPersister.class);
			databaseFieldConfig.setAllowGeneratedIdInsert(true);
			databaseFieldConfig.setColumnDefinition("INT NOT NULL");
			databaseFieldConfig.setForeignAutoCreate(true);
			databaseFieldConfig.setVersion(true);
			databaseFieldConfig.setForeignColumnName("foreign");
			databaseFieldConfig.setReadOnly(true);
			databaseFieldConfigs.add(databaseFieldConfig);
		}
		CONFIG = new DatabaseTableConfig<inputs.NamedTableWithSpecifiedDatabaseField>(inputs.NamedTableWithSpecifiedDatabaseField.class, "table", databaseFieldConfigs);
	}
}
