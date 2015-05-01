package inputs;

import java.lang.reflect.Field;
import java.sql.SQLException;

import com.j256.ormlite.field.DataPersister;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.FieldType;
import com.j256.ormlite.field.SqlType;
import com.j256.ormlite.support.DatabaseResults;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "table")
public class NamedTableWithSpecifiedDatabaseField {

	public static enum FieldTypeEnum {
		VALUE, OTHER_VALUE;
	}

	public static class CustomPersister implements DataPersister {
		public Object parseDefaultString(FieldType fieldType, String defaultStr)
				throws SQLException {
			return null;
		}

		public Object javaToSqlArg(FieldType fieldType, Object obj)
				throws SQLException {
			return null;
		}

		public Object resultToSqlArg(FieldType fieldType,
				DatabaseResults results, int columnPos) throws SQLException {
			return null;
		}

		public Object resultToJava(FieldType fieldType,
				DatabaseResults results, int columnPos) throws SQLException {
			return null;
		}

		public Object sqlArgToJava(FieldType fieldType, Object sqlArg,
				int columnPos) throws SQLException {
			return null;
		}

		public SqlType getSqlType() {
			return null;
		}

		public boolean isStreamType() {
			return false;
		}

		public Object resultStringToJava(FieldType fieldType,
				String stringValue, int columnPos) throws SQLException {
			return null;
		}

		public Class<?>[] getAssociatedClasses() {
			return null;
		}

		public String[] getAssociatedClassNames() {
			return null;
		}

		public Object makeConfigObject(FieldType fieldType) throws SQLException {
			return null;
		}

		public Object convertIdNumber(Number number) {
			return null;
		}

		public boolean isValidGeneratedType() {
			return false;
		}

		public boolean isValidForField(Field field) {
			return false;
		}

		public Class<?> getPrimaryClass() {
			return null;
		}

		public boolean isEscapedDefaultValue() {
			return false;
		}

		public boolean isEscapedValue() {
			return false;
		}

		public boolean isPrimitive() {
			return false;
		}

		public boolean isComparable() {
			return false;
		}

		public boolean isAppropriateId() {
			return false;
		}

		public boolean isArgumentHolderRequired() {
			return false;
		}

		public boolean isSelfGeneratedId() {
			return false;
		}

		public Object generateId() {
			return null;
		}

		public int getDefaultWidth() {
			return 0;
		}

		public boolean dataIsEqual(Object obj1, Object obj2) {
			return false;
		}

		public boolean isValidForVersion() {
			return false;
		}

		public Object moveToNextValue(Object currentValue) throws SQLException {
			return null;
		}
	}

	@DatabaseField(columnName = "column", dataType = DataType.ENUM_INTEGER, defaultValue = "VALUE", width = 100, canBeNull = false, id = true, generatedId = true, generatedIdSequence = "id_sequence", foreign = true, useGetSet = true, unknownEnumName = "OTHER_VALUE", throwIfNull = true, format = "%f", unique = true, uniqueCombo = true, index = true, uniqueIndex = true, indexName = "index", uniqueIndexName = "unique_index", foreignAutoRefresh = true, maxForeignAutoRefreshLevel = 5, persisterClass = CustomPersister.class, allowGeneratedIdInsert = true, columnDefinition = "INT NOT NULL", foreignAutoCreate = true, version = true, foreignColumnName = "foreign", readOnly = true)
	public FieldTypeEnum field;

	@DatabaseField(persisted = false)
	public int ignored;
}
