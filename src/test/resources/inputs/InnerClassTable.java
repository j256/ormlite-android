package inputs;

import com.j256.ormlite.android.annotations.DatabaseTables;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

class InnerClassTable {
	@DatabaseTable
	static class InnerClass {
		@DatabaseField
		int field;
	}

	@DatabaseTable
	static class OtherInnerClass {
		@DatabaseField
		int field;
	}

	@DatabaseTables({ InnerClass.class, OtherInnerClass.class })
	static class Main {
	}
}
