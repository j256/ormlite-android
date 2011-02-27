package com.j256.ormlite.android;

import java.sql.SQLException;
import java.sql.Savepoint;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

import com.j256.ormlite.field.FieldType;
import com.j256.ormlite.misc.SqlExceptionUtil;
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

	public boolean getAutoCommit() throws SQLException {
		try {
			// You have to explicitly commit your transactions, so this is sort of correct
			return !db.inTransaction();
		} catch (android.database.SQLException e) {
			throw SqlExceptionUtil.create("problems getting auto-commit from database", e);
		}
	}

	public void setAutoCommit(boolean autoCommit) {
		// always in auto-commit mode
	}

	public Savepoint setSavePoint(String name) throws SQLException {
		try {
			db.beginTransaction();
			return null;
		} catch (android.database.SQLException e) {
			throw SqlExceptionUtil.create("problems beginning transaction", e);
		}
	}

	/**
	 * Return whether this connection is read-write or not (real-only).
	 */
	public boolean isReadWrite() {
		return readWrite;
	}

	public void commit(Savepoint savepoint) throws SQLException {
		try {
			db.setTransactionSuccessful();
			db.endTransaction();
		} catch (android.database.SQLException e) {
			throw SqlExceptionUtil.create("problems commiting transaction", e);
		}
	}

	public void rollback(Savepoint savepoint) throws SQLException {
		try {
			// no setTransactionSuccessful() means it is a rollback
			db.endTransaction();
		} catch (android.database.SQLException e) {
			throw SqlExceptionUtil.create("problems rolling back transaction", e);
		}
	}

	public CompiledStatement compileStatement(String statement, StatementType type, FieldType[] argFieldTypes,
			FieldType[] resultFieldTypes) {
		CompiledStatement stmt = new AndroidCompiledStatement(statement, db, type);
		return stmt;
	}

	/**
	 * Android doesn't return the number of rows inserted.
	 */
	public int insert(String statement, Object[] args, FieldType[] argFieldTypes) throws SQLException {
		return insert(statement, args, argFieldTypes, null);
	}

	public int insert(String statement, Object[] args, FieldType[] argFieldTypes, GeneratedKeyHolder keyHolder)
			throws SQLException {
		SQLiteStatement stmt = null;
		try {
			stmt = db.compileStatement(statement);
			bindArgs(stmt, args, argFieldTypes);
			long rowId = stmt.executeInsert();
			if (keyHolder != null) {
				keyHolder.addKey(rowId);
			}
			return 1;
		} catch (android.database.SQLException e) {
			throw SqlExceptionUtil.create("inserting to database failed: " + statement, e);
		} finally {
			if (stmt != null) {
				stmt.close();
			}
		}
	}

	public int update(String statement, Object[] args, FieldType[] argFieldTypes) throws SQLException {
		SQLiteStatement stmt = null;
		try {
			stmt = db.compileStatement(statement);
			bindArgs(stmt, args, argFieldTypes);
			stmt.execute();
			return 1;
		} catch (android.database.SQLException e) {
			throw SqlExceptionUtil.create("updating database failed: " + statement, e);
		} finally {
			if (stmt != null) {
				stmt.close();
			}
		}
	}

	public int delete(String statement, Object[] args, FieldType[] argFieldTypes) throws SQLException {
		// delete is the same as update
		return update(statement, args, argFieldTypes);
	}

	public <T> Object queryForOne(String statement, Object[] args, FieldType[] argFieldTypes,
			GenericRowMapper<T> rowMapper) throws SQLException {
		Cursor cursor = null;

		try {
			cursor = db.rawQuery(statement, toStrings(args));
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
		} catch (android.database.SQLException e) {
			throw SqlExceptionUtil.create("queryForOne from database failed: " + statement, e);
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	public long queryForLong(String statement) throws SQLException {
		SQLiteStatement stmt = null;
		try {
			stmt = db.compileStatement(statement);
			return stmt.simpleQueryForLong();
		} catch (android.database.SQLException e) {
			throw SqlExceptionUtil.create("queryForLong from database failed: " + statement, e);
		} finally {
			if (stmt != null) {
				stmt.close();
			}
		}
	}

	public void close() throws SQLException {
		try {
			db.close();
		} catch (android.database.SQLException e) {
			throw SqlExceptionUtil.create("problems closing the database connection", e);
		}
	}

	public boolean isClosed() throws SQLException {
		try {
			return !db.isOpen();
		} catch (android.database.SQLException e) {
			throw SqlExceptionUtil.create("problems detecting if the database is closed", e);
		}
	}

	public boolean isTableExists(String tableName) throws SQLException {
		// NOTE: it is non trivial to do this check since the helper will auto-create if it doesn't exist
		return true;
	}

	private void bindArgs(SQLiteStatement stmt, Object[] args, FieldType[] argFieldTypes) throws SQLException {
		if (args == null) {
			return;
		}
		for (int i = 0; i < args.length; i++) {
			Object arg = args[i];
			int argIndex = AndroidDatabaseResults.androidColumnIndexToJdbc(i);
			if (arg == null) {
				stmt.bindNull(argIndex);
			} else {
				switch (argFieldTypes[i].getSqlType()) {
					case STRING :
					case LONG_STRING :
						stmt.bindString(argIndex, arg.toString());
						break;
					case BOOLEAN :
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
					case BYTE_ARRAY :
					case SERIALIZABLE :
						stmt.bindBlob(argIndex, (byte[]) arg);
						break;
					default :
						throw new SQLException("Unknown sql argument type " + argFieldTypes[i].getSqlType());
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
