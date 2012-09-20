package com.j256.ormlite.android;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import android.database.Cursor;

import com.j256.ormlite.dao.ObjectCache;
import com.j256.ormlite.db.DatabaseType;
import com.j256.ormlite.db.SqliteAndroidDatabaseType;
import com.j256.ormlite.support.DatabaseResults;

/**
 * Android implementation of our results object.
 * 
 * @author kevingalligan, graywatson
 */
public class AndroidDatabaseResults implements DatabaseResults {

	private static final int MIN_NUM_COLUMN_NAMES_MAP = 8;

	private final Cursor cursor;
	private final String[] columnNames;
	private final Map<String, Integer> columnNameMap;
	private final ObjectCache objectCache;
	private static final DatabaseType databaseType = new SqliteAndroidDatabaseType();

	public AndroidDatabaseResults(Cursor cursor, ObjectCache objectCache) {
		this.cursor = cursor;
		this.columnNames = cursor.getColumnNames();
		if (this.columnNames.length >= MIN_NUM_COLUMN_NAMES_MAP) {
			this.columnNameMap = new HashMap<String, Integer>();
			for (int i = 0; i < this.columnNames.length; i++) {
				// NOTE: this is case sensitive
				this.columnNameMap.put(this.columnNames[i], i);
			}
		} else {
			columnNameMap = null;
		}
		this.objectCache = objectCache;
	}

	/**
	 * Constructor that allows you to inject a cursor that has already been configured with first-call set to false.
	 * 
	 * @deprecated The firstCall is no longer needed since the library now calls first() and next on its own.
	 */
	@Deprecated
	public AndroidDatabaseResults(Cursor cursor, boolean firstCall, ObjectCache objectCache) {
		this(cursor, objectCache);
	}

	public int getColumnCount() {
		return cursor.getColumnCount();
	}

	public String[] getColumnNames() {
		int colN = getColumnCount();
		String[] columnNames = new String[colN];
		for (int colC = 0; colC < colN; colC++) {
			columnNames[colC] = cursor.getColumnName(colC);
		}
		return columnNames;
	}

	public boolean first() {
		return cursor.moveToFirst();
	}

	public boolean next() {
		return cursor.moveToNext();
	}

	public boolean last() {
		return cursor.moveToLast();
	}

	public boolean previous() {
		return cursor.moveToPrevious();
	}

	public boolean moveRelative(int offset) {
		return cursor.move(offset);
	}

	public boolean moveAbsolute(int position) {
		return cursor.moveToPosition(position);
	}

	/**
	 * Returns the count of results from the cursor.
	 */
	public int getCount() {
		return cursor.getCount();
	}

	/**
	 * Returns the position of the cursor in the list of results.
	 */
	public int getPosition() {
		return cursor.getPosition();
	}

	public int findColumn(String columnName) throws SQLException {
		int index = lookupColumn(columnName);
		if (index >= 0) {
			return index;
		}

		/*
		 * Hack here. It turns out that if we've asked for '*' then the field foo is in the cursor as foo. But if we ask
		 * for a particular field list with DISTINCT, which escapes the field names, they are in the cursor _with_ the
		 * escaping. Ugly!!
		 */
		StringBuilder sb = new StringBuilder(columnName.length() + 4);
		databaseType.appendEscapedEntityName(sb, columnName);
		index = lookupColumn(sb.toString());
		if (index >= 0) {
			return index;
		} else {
			String[] columnNames = cursor.getColumnNames();
			throw new SQLException("Unknown field '" + columnName + "' from the Android sqlite cursor, not in:"
					+ Arrays.toString(columnNames));
		}
	}

	public String getString(int columnIndex) {
		return cursor.getString(columnIndex);
	}

	public boolean getBoolean(int columnIndex) {
		if (cursor.isNull(columnIndex) || cursor.getShort(columnIndex) == 0) {
			return false;
		} else {
			return true;
		}
	}

	public char getChar(int columnIndex) throws SQLException {
		String string = cursor.getString(columnIndex);
		if (string == null || string.length() == 0) {
			return 0;
		} else if (string.length() == 1) {
			return string.charAt(0);
		} else {
			throw new SQLException("More than 1 character stored in database column: " + columnIndex);
		}
	}

	public byte getByte(int columnIndex) {
		return (byte) getShort(columnIndex);
	}

	public byte[] getBytes(int columnIndex) {
		return cursor.getBlob(columnIndex);
	}

	public short getShort(int columnIndex) {
		return cursor.getShort(columnIndex);
	}

	public int getInt(int columnIndex) {
		return cursor.getInt(columnIndex);
	}

	public long getLong(int columnIndex) {
		return cursor.getLong(columnIndex);
	}

	public float getFloat(int columnIndex) {
		return cursor.getFloat(columnIndex);
	}

	public double getDouble(int columnIndex) {
		return cursor.getDouble(columnIndex);
	}

	public Timestamp getTimestamp(int columnIndex) throws SQLException {
		throw new SQLException("Android does not support timestamp.  Use JAVA_DATE_LONG or JAVA_DATE_STRING types");
	}

	public InputStream getBlobStream(int columnIndex) {
		return new ByteArrayInputStream(cursor.getBlob(columnIndex));
	}

	public BigDecimal getBigDecimal(int columnIndex) throws SQLException {
		throw new SQLException("Android does not support BigDecimal type.  Use BIG_DECIMAL or BIG_DECIMAL_STRING types");
	}

	public boolean wasNull(int columnIndex) {
		return cursor.isNull(columnIndex);
	}

	public ObjectCache getObjectCache() {
		return objectCache;
	}

	public void close() {
		cursor.close();
	}

	public void closeQuietly() {
		close();
	}

	/***
	 * Returns the underlying Android cursor object. This should not be used unless you know what you are doing.
	 */
	public Cursor getRawCursor() {
		return cursor;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "@" + Integer.toHexString(super.hashCode());
	}

	private int lookupColumn(String columnName) {
		// we either use linear search or our name map
		if (columnNameMap == null) {
			for (int i = 0; i < columnNames.length; i++) {
				// NOTE: this is case sensitive
				if (columnNames[i].equals(columnName)) {
					return i;
				}
			}
			return -1;
		} else {
			// NOTE: this is case sensitive
			Integer index = columnNameMap.get(columnName);
			if (index == null) {
				return -1;
			} else {
				return index;
			}
		}
	}
}
