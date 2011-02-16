package com.j256.ormlite.db;

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
	protected void appendDateType(StringBuilder sb, int fieldWidth) {
		// default is to store the date as a string
		appendDateStringType(sb, fieldWidth);
	}

	@Override
	protected void appendBooleanType(StringBuilder sb) {
		// we have to convert booleans to numbers
		appendShortType(sb);
	}

	@SuppressWarnings("deprecation")
	@Override
	public FieldConverter getFieldConverter(DataType dataType) {
		// we are only overriding certain types
		switch (dataType) {
			case JAVA_DATE :
			case DATE :
				return DataType.DATE_STRING;
			default :
				return super.getFieldConverter(dataType);
		}
	}
}
