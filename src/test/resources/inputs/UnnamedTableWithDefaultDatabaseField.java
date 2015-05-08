package inputs;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable
class UnnamedTableWithDefaultDatabaseField {
	@DatabaseField
	int field;
}
