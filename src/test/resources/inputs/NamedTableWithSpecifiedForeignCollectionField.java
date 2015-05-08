package inputs;

import java.util.List;

import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "table")
class NamedTableWithSpecifiedForeignCollectionField {
	@ForeignCollectionField(eager = true, maxEagerLevel = 5, columnName = "column", orderColumnName = "order_column", orderAscending = false, foreignFieldName = "foreign_field")
	List<Integer> numbers;

	@ForeignCollectionField(maxEagerForeignCollectionLevel = 5, foreignColumnName = "foreign_field")
	List<Integer> numbers_deprecated;
}
