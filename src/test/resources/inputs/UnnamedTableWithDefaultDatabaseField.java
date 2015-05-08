package inputs;

import com.j256.ormlite.android.annotations.DatabaseTables;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable
class UnnamedTableWithDefaultDatabaseField {
	@DatabaseField
	int field;
	
	@DatabaseTables(UnnamedTableWithDefaultDatabaseField.class)
	static class Main {
	}
}
