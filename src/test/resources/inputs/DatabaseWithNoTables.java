package inputs;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase.CursorFactory;

import com.j256.ormlite.android.annotations.Database;
import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;

@Database({})
abstract class DatabaseWithNoTables extends OrmLiteSqliteOpenHelper {
	DatabaseWithNoTables(Context context, String databaseName,
			CursorFactory factory, int databaseVersion) {
		super(context, databaseName, factory, databaseVersion);
	}
}
