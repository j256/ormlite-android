package com.j256.ormlite.db;

import com.j256.ormlite.field.DataPersister;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.FieldConverter;

/**
 * Sqlite database type information for the Android OS that makes native calls to the Android OS database APIs.
 * 
 * @author graywatson
 */
public class SqliteAndroidDatabaseType extends BaseSqliteDatabaseType implements DatabaseType {

	public SqliteAndroidDatabaseType() {
	}

	@Override
	public void loadDriver() {
		// noop
	}

	public boolean isDatabaseUrlThisType(String url, String dbTypePart) {
		// not used by the android code
		return true;
	}

	@Override
	protected String getDriverClassName() {
		// no driver to load in android-land
		return null;
	}

	@Override
	public String getDatabaseName() {
		return "Android SQLite";
	}

	@Override
	protected void appendDateType(StringBuilder sb, int fieldWidth) {
		// default is to store the date as a string
		appendStringType(sb, fieldWidth);
	}

	@Override
	protected void appendBooleanType(StringBuilder sb, int fieldWidth) {
		// we have to convert booleans to numbers
		appendShortType(sb, fieldWidth);
	}

	@Override
	public FieldConverter getFieldConverter(DataPersister dataPersister) {
		// we are only overriding certain types
		switch (dataPersister.getSqlType()) {
			case DATE :
				return DataType.DATE_STRING.getDataPersister();
			default :
				return super.getFieldConverter(dataPersister);
		}
	}

	@Override
	public boolean isNestedSavePointsSupported() {
		return false;
	}

	@Override
	public boolean isBatchUseTransaction() {
		return true;
	}
}
