package android.database.sqlite;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase.CursorFactory;

/**
 * Stub implementation of the Android Sqlite database helper object to stop compilation errors.
 */
public abstract class SQLiteOpenHelper {

	public SQLiteOpenHelper(Context context, String name, CursorFactory factory, int version) {
	}

	public synchronized SQLiteDatabase getReadableDatabase() {
		return new SQLiteDatabase();
	}

	public synchronized SQLiteDatabase getWritableDatabase() {
		return new SQLiteDatabase();
	}

	public abstract void onCreate(SQLiteDatabase db);

	public abstract void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion);

	public synchronized void close() {
	}
}