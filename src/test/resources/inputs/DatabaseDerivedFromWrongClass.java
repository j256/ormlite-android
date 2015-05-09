package inputs;

import com.j256.ormlite.android.annotations.Database;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable
class DatabaseDerivedFromWrongClass {
	@DatabaseField
	int field;

	@Database({ DatabaseDerivedFromWrongClass.class })
	static abstract class OpenHelper {
	}
}
