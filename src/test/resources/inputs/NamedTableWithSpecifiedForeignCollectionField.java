package inputs;

import java.util.List;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase.CursorFactory;

import com.j256.ormlite.android.annotations.Database;
import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "table")
class NamedTableWithSpecifiedForeignCollectionField {
	@ForeignCollectionField(eager = true, maxEagerLevel = 5, columnName = "column", orderColumnName = "order_column", orderAscending = false, foreignFieldName = "foreign_field")
	List<Integer> numbers;

	@ForeignCollectionField(maxEagerForeignCollectionLevel = 5, foreignColumnName = "foreign_field")
	List<Integer> numbers_deprecated;
	
	@Database(NamedTableWithSpecifiedForeignCollectionField.class)
	static abstract class OpenHelper extends OrmLiteSqliteOpenHelper {
		OpenHelper(Context context, String databaseName, CursorFactory factory,
				int databaseVersion) {
			super(context, databaseName, factory, databaseVersion);
		}
	}
}
