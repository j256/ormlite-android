package inputs;

import java.sql.SQLException;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;

import com.j256.ormlite.android.annotations.Database;
import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable
class UnnamedTableWithDefaultDatabaseField {
	@DatabaseField
	int field;

	@Database({ UnnamedTableWithDefaultDatabaseField.class })
	static abstract class OpenHelper extends OrmLiteSqliteOpenHelper {
		OpenHelper(Context context, String databaseName, CursorFactory factory,
				int databaseVersion) {
			super(context, databaseName, factory, databaseVersion);
			UnnamedTableWithDefaultDatabaseField_OpenHelper_TableConfig
					.cacheTableConfigurations();
		}

		@Override
		public void onCreate(SQLiteDatabase database,
				ConnectionSource connectionSource) {
			try {
				UnnamedTableWithDefaultDatabaseField_OpenHelper_TableConfig
						.createTables(connectionSource);
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
		}
	}
}
