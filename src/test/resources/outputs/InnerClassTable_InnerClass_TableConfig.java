package inputs;

import java.util.ArrayList;
import java.util.List;

import com.j256.ormlite.field.DatabaseFieldConfig;
import com.j256.ormlite.table.DatabaseTableConfig;

public final class InnerClassTable_InnerClass_TableConfig {
	private InnerClassTable_InnerClass_TableConfig() {
	}

	public static final DatabaseTableConfig<inputs.InnerClassTable.InnerClass> CONFIG;

	static {
		List<DatabaseFieldConfig> databaseFieldConfigs = new ArrayList<DatabaseFieldConfig>();
		{
			DatabaseFieldConfig databaseFieldConfig = new DatabaseFieldConfig("field");
			databaseFieldConfigs.add(databaseFieldConfig);
		}
		CONFIG = new DatabaseTableConfig<inputs.InnerClassTable.InnerClass>(inputs.InnerClassTable.InnerClass.class, "innerclass", databaseFieldConfigs);
	}
}
