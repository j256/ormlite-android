package com.j256.ormlite.db;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.FieldConverter;
import com.j256.ormlite.field.FieldType;

/**
 * Sqlite database type information for the Android OS that makes native calls to the Android OS database APIs.
 * 
 * @author graywatson
 */
public class SqliteAndroidDatabaseType extends BaseSqliteDatabaseType implements DatabaseType {

	public SqliteAndroidDatabaseType() {
	}

	@Override
	public void loadDriver() throws ClassNotFoundException {
		// Hang out. Nothing to do.
	}

	public String getDriverUrlPart() {
		return null;
	}

	public String getDriverClassName() {
		return null;
	}

	@Override
	protected void appendDateType(StringBuilder sb, int fieldWidth) {
		// default is to store the date as a string
		appendDateStringType(sb, fieldWidth);
	}

	@Override
	protected void appendBooleanType(StringBuilder sb) {
		appendShortType(sb);
	}

	@Override
	public FieldConverter getFieldConverter(FieldType fieldType) {
		// we are only overriding certain types
		switch (fieldType.getDataType()) {
			case JAVA_DATE :
				return DataType.JAVA_DATE_STRING;
			default :
				return super.getFieldConverter(fieldType);
		}
	}
}
