package android.database.sqlite;

import java.sql.SQLException;

/**
 * Stub implementation of the Android Sqlite statement object to stop compilation errors.
 */
public class SQLiteStatement {

	public long simpleQueryForLong() {
		return 0L;
	}

	public void execute() throws SQLException {
	}

	public long executeInsert() throws SQLException {
		return 0L;
	}

	public void close() {
	}

	public void bindNull(int bindIndex) {
	}

	public void bindLong(int bindIndex, long value) {
	}

	public void bindDouble(int bindIndex, double value) {
	}

	public void bindString(int bindIndex, String value) {
	}

	public void bindBlob(int bindIndex, byte[] value) {
	}
}
