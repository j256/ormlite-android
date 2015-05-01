package inputs;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable
public class UnnamedTableWithDefaultDatabaseField {
	@DatabaseField
	public int field;
}
