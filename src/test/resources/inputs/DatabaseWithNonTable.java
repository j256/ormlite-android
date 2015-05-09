package inputs;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase.CursorFactory;

import com.j256.ormlite.android.annotations.Database;
import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable
class DatabaseWithNonTable {
	@DatabaseField
	int field;

	@Database({ DatabaseWithNonTable.class, String.class })
	static abstract class OpenHelper extends OrmLiteSqliteOpenHelper {
		OpenHelper(Context context, String databaseName, CursorFactory factory,
				int databaseVersion) {
			super(context, databaseName, factory, databaseVersion);
		}
	}
}
