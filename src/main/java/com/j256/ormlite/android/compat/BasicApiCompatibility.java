package com.j256.ormlite.android.compat;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

/**
 * Basic class which provides no-op methods for all Android version.
 * 
 * @author graywatson
 */
public class BasicApiCompatibility implements ApiCompatibility {

	public Cursor rawQuery(SQLiteDatabase db, String sql, String[] selectionArgs, CancellationHook cancellationHook) {
		// NOTE: cancellationHook will always be null
		return db.rawQuery(sql, selectionArgs);
	}

	public CancellationHook createCancellationHook() {
		return null;
	}

	public void closeCursor(Cursor cursor) {
		if (cursor != null) {
			cursor.deactivate();
		}
	}

	public void closeStatement(SQLiteStatement statement) {
		if (statement != null) {
			statement.close();
		}
	}
}
