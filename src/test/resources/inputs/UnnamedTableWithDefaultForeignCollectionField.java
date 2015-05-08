package inputs;

import java.util.List;

import com.j256.ormlite.android.annotations.DatabaseTables;
import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable
class UnnamedTableWithDefaultForeignCollectionField {
	@ForeignCollectionField
	List<Integer> numbers;

	@DatabaseTables(UnnamedTableWithDefaultForeignCollectionField.class)
	static class Main {
	}
}
