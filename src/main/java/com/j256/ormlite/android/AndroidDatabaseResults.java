package com.j256.ormlite.android;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.sql.SQLException;
import java.sql.Timestamp;

import android.database.Cursor;

import com.j256.ormlite.db.DatabaseType;
import com.j256.ormlite.db.SqliteAndroidDatabaseType;
import com.j256.ormlite.support.DatabaseResults;

/**
 * Android implementation of our results object.
 * 
 * @author kevingalligan, graywatson
 */
public class AndroidDatabaseResults implements DatabaseResults {

	private final Cursor cursor;
	private boolean firstCall;
	private static final DatabaseType databaseType = new SqliteAndroidDatabaseType();

	public AndroidDatabaseResults(Cursor cursor) {
		this.cursor = cursor;
		this.firstCall = true;
	}

	public int getColumnCount() throws SQLException {
		return cursor.getColumnCount();
	}

	public String getColumnName(int column) throws SQLException {
		return cursor.getColumnName(column);
	}

	public boolean next() throws SQLException {
		boolean returnValue;
		if (firstCall) {
			returnValue = cursor.moveToFirst();
			firstCall = false;
		} else {
			returnValue = cursor.moveToNext();
		}
		return returnValue;
	}

	public int findColumn(String columnName) throws SQLException {
		int index = cursor.getColumnIndex(columnName);
		if (index < 0) {
			/*
			 * Hack here. It turns out that if we've asked for '*' then the field foo is in the cursor as foo. But if we
			 * ask for a particular field list, which escapes the field names, with DISTINCT the fiend names are in the
			 * cursor with the escaping. Ugly!!
			 */
			StringBuilder sb = new StringBuilder();
			databaseType.appendEscapedEntityName(sb, columnName);
			index = cursor.getColumnIndex(sb.toString());
			if (index < 0) {
				throw new SQLException("Unknown field '" + columnName + "' from the Android sqlite cursor");
			}
		}
		return androidColumnIndexToJdbc(index);
	}

	public String getString(int columnIndex) throws SQLException {
		return cursor.getString(jdbcColumnIndexToAndroid(columnIndex));
	}

	public boolean getBoolean(int columnIndex) throws SQLException {
		int col = jdbcColumnIndexToAndroid(columnIndex);
		if (cursor.isNull(col))
			return false;
		return cursor.getShort(col) != 0;
	}

	public byte getByte(int columnIndex) throws SQLException {
		return (byte) getShort(columnIndex);
	}

	public byte[] getBytes(int columnIndex) throws SQLException {
		return cursor.getBlob(jdbcColumnIndexToAndroid(columnIndex));
	}

	public short getShort(int columnIndex) throws SQLException {
		return cursor.getShort(jdbcColumnIndexToAndroid(columnIndex));
	}

	public int getInt(int columnIndex) throws SQLException {
		return cursor.getInt(jdbcColumnIndexToAndroid(columnIndex));
	}

	public long getLong(int columnIndex) throws SQLException {
		return cursor.getLong(jdbcColumnIndexToAndroid(columnIndex));
	}

	public float getFloat(int columnIndex) throws SQLException {
		return cursor.getFloat(jdbcColumnIndexToAndroid(columnIndex));
	}

	public double getDouble(int columnIndex) throws SQLException {
		return cursor.getDouble(jdbcColumnIndexToAndroid(columnIndex));
	}

	public Timestamp getTimestamp(int columnIndex) throws SQLException {
		throw new SQLException("Android does not support timestamp.  Use JAVA_DATE_LONG or JAVA_DATE_STRING types");
	}

	public InputStream getBlobStream(int columnIndex) throws SQLException {
		return new ByteArrayInputStream(cursor.getBlob(jdbcColumnIndexToAndroid(columnIndex)));
	}

	public boolean isNull(int columnIndex) throws SQLException {
		return cursor.isNull(jdbcColumnIndexToAndroid(columnIndex));
	}

	/**
	 * Convert the jdbc column index to one suitable for Android. Jdbc-land has the first argument being 1 and in
	 * Android it is 0.
	 */
	public static int jdbcColumnIndexToAndroid(int columnIndex) {
		return columnIndex - 1;
	}

	/**
	 * Convert the Android column index to one suitable for JDBC. Jdbc-land has the first argument being 1 and in
	 * Android it is 0.
	 */
	public static int androidColumnIndexToJdbc(int columnIndex) {
		return columnIndex + 1;
	}
}
