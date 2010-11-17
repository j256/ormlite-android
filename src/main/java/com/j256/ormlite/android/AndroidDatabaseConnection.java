package com.j256.ormlite.android;

import java.sql.SQLException;
import java.sql.Savepoint;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

import com.j256.ormlite.field.SqlType;
import com.j256.ormlite.stmt.GenericRowMapper;
import com.j256.ormlite.stmt.StatementBuilder.StatementType;
import com.j256.ormlite.support.CompiledStatement;
import com.j256.ormlite.support.DatabaseConnection;
import com.j256.ormlite.support.GeneratedKeyHolder;

/**
 * Database connection for Android.
 * 
 * @author kevingalligan, graywatson
 */
public class AndroidDatabaseConnection implements DatabaseConnection {

	private final SQLiteDatabase db;
	private final boolean readWrite;

	public AndroidDatabaseConnection(SQLiteDatabase db, boolean readWrite) {
		this.db = db;
		this.readWrite = readWrite;
	}

	public boolean isAutoCommitSupported() {
		return false;
	}

	public boolean getAutoCommit() {
		// You have to explicitly commit your transactions, so this is sort of correct
		return !db.inTransaction();
	}

	public void setAutoCommit(boolean autoCommit) {
		// always in auto-commit mode
	}

	public Savepoint setSavePoint(String name) {
		db.beginTransaction();
		return null;
	}

	/**
	 * Return whether this connection is read-write or not (real-only).
	 */
	public boolean isReadWrite() {
		return readWrite;
	}

	public void commit(Savepoint savepoint) {
		db.setTransactionSuccessful();
		db.endTransaction();
	}

	public void rollback(Savepoint savepoint) {
		// no setTransactionSuccessful() means it is a rollback
		db.endTransaction();
	}

	public CompiledStatement compileStatement(String statement, StatementType type) {
		CompiledStatement stmt = new AndroidCompiledStatement(statement, db, type);
		return stmt;
	}

	/**
	 * Android doesn't return the number of rows inserted.
	 */
	public int insert(String statement, Object[] args, SqlType[] argFieldTypes) throws SQLException {
		return insert(statement, args, argFieldTypes, null);
	}

	public int insert(String statement, Object[] args, SqlType[] argFieldTypes, GeneratedKeyHolder keyHolder)
			throws SQLException {
		SQLiteStatement stmt = db.compileStatement(statement);
		try {
			bindArgs(stmt, args, argFieldTypes);
			long rowId = stmt.executeInsert();
			if (keyHolder != null) {
				keyHolder.addKey(rowId);
			}
			return 1;
		} finally {
			if (stmt != null) {
				stmt.close();
			}
		}
	}

	public int update(String statement, Object[] args, SqlType[] argFieldTypes) {
		SQLiteStatement stmt = db.compileStatement(statement);
		try {
			bindArgs(stmt, args, argFieldTypes);
			stmt.execute();
			return 1;
		} finally {
			if (stmt != null) {
				stmt.close();
			}
		}
	}

	public int delete(String statement, Object[] args, SqlType[] argFieldTypes) {
		// delete is the same as update
		return update(statement, args, argFieldTypes);
	}

	public <T> Object queryForOne(String statement, Object[] args, SqlType[] argFieldTypes,
			GenericRowMapper<T> rowMapper) throws SQLException {
		Cursor cursor = db.rawQuery(statement, toStrings(args));

		try {
			AndroidDatabaseResults results = new AndroidDatabaseResults(cursor);
			if (!results.next()) {
				return null;
			} else {
				T first = rowMapper.mapRow(results);
				if (results.next()) {
					return MORE_THAN_ONE;
				} else {
					return first;
				}
			}
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	public long queryForLong(String statement) {
		SQLiteStatement stmt = db.compileStatement(statement);
		try {
			return stmt.simpleQueryForLong();
		} finally {
			if (stmt != null) {
				stmt.close();
			}
		}
	}

	public void close() {
		db.close();
	}

	public boolean isClosed() {
		return !db.isOpen();
	}

	private void bindArgs(SQLiteStatement stmt, Object[] args, SqlType[] argFieldTypes) {
		if (args == null) {
			return;
		}
		for (int i = 0; i < args.length; i++) {
			Object arg = args[i];
			int argIndex = AndroidDatabaseResults.androidColumnIndexToJdbc(i);
			if (arg == null) {
				stmt.bindNull(argIndex);
			} else {
				switch (argFieldTypes[i]) {
					case STRING :
						stmt.bindString(argIndex, arg.toString());
						break;
					case BOOLEAN :
						stmt.bindLong(argIndex, ((Boolean) arg) ? 1 : 0);
						break;
					case BYTE :
					case SHORT :
					case INTEGER :
					case LONG :
						stmt.bindLong(argIndex, ((Number) arg).longValue());
						break;
					case FLOAT :
					case DOUBLE :
						stmt.bindDouble(argIndex, ((Number) arg).doubleValue());
						break;
					case SERIALIZABLE :
						stmt.bindBlob(argIndex, (byte[]) arg);
						break;
				}
			}
		}
	}

	private String[] toStrings(Object[] args) {
		if (args == null) {
			return null;
		}
		String[] strings = new String[args.length];
		for (int i = 0; i < args.length; i++) {
			Object arg = args[i];
			if (arg == null) {
				strings[i] = null;
			} else {
				strings[i] = arg.toString();
			}
		}

		return strings;
	}
}
