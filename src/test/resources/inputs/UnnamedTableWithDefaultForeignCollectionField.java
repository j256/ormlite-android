package inputs;

import java.util.List;

import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable
public class UnnamedTableWithDefaultForeignCollectionField {
	@ForeignCollectionField
	public List<Integer> numbers;
}
