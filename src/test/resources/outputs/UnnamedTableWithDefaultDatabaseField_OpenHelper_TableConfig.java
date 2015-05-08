package inputs;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.DatabaseTableConfig;
import com.j256.ormlite.table.TableUtils;

public final class UnnamedTableWithDefaultDatabaseField_OpenHelper_TableConfig {
	private UnnamedTableWithDefaultDatabaseField_OpenHelper_TableConfig() {
	}

	public static void cacheTableConfigurations() {
		List<DatabaseTableConfig<?>> tableConfigs = new ArrayList<DatabaseTableConfig<?>>();
		tableConfigs.add(inputs.UnnamedTableWithDefaultDatabaseField_TableConfig.CONFIG);
		DaoManager.addCachedDatabaseConfigs(tableConfigs);
	}

	public static void createTables(ConnectionSource connectionSource) throws SQLException {
		TableUtils.createTable(connectionSource, inputs.UnnamedTableWithDefaultDatabaseField.class);
	}
}
