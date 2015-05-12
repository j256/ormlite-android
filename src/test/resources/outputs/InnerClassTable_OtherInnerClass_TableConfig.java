package com.j256.ormlite.android.processor.inputs;

import java.util.ArrayList;
import java.util.List;

import com.j256.ormlite.field.DatabaseFieldConfig;
import com.j256.ormlite.table.DatabaseTableConfig;

public final class InnerClassTable_OtherInnerClass_TableConfig {
	private InnerClassTable_OtherInnerClass_TableConfig() {
	}

	public static final DatabaseTableConfig<com.j256.ormlite.android.processor.inputs.InnerClassTable.OtherInnerClass> CONFIG;

	static {
		List<DatabaseFieldConfig> databaseFieldConfigs = new ArrayList<DatabaseFieldConfig>();
		{
			DatabaseFieldConfig databaseFieldConfig = new DatabaseFieldConfig("field");
			databaseFieldConfigs.add(databaseFieldConfig);
		}
		CONFIG = new DatabaseTableConfig<com.j256.ormlite.android.processor.inputs.InnerClassTable.OtherInnerClass>(com.j256.ormlite.android.processor.inputs.InnerClassTable.OtherInnerClass.class, "otherinnerclass", databaseFieldConfigs);
	}
}
