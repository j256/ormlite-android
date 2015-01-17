package com.j256.ormlite.android;

import com.j256.ormlite.db.DatabaseType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.DatabaseFieldConfig;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.DatabaseTableConfig;

import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Class which uses reflection to make the job of processing the {@link DatabaseField} annotation more efficient. In
 * current (as of 11/2011) versions of Android, Annotations are ghastly slow. This uses reflection on the Android
 * classes to work around this issue. Gross and a hack but a significant (~20x) performance improvement.
 *
 * <p>
 * Thanks much go to Josh Guilfoyle for the idea and the code framework to make this happen.
 * </p>
 *
 * @author joshguilfoyle, graywatson
 */
public class DatabaseTableConfigUtil {
	/**
	 * Build our list table config from a class using some annotation fu around.
	 */
	public static <T> DatabaseTableConfig<T> fromClass(ConnectionSource connectionSource, Class<T> clazz)
			throws SQLException {
		DatabaseType databaseType = connectionSource.getDatabaseType();
		String tableName = DatabaseTableConfig.extractTableName(clazz);
		List<DatabaseFieldConfig> fieldConfigs = new ArrayList<DatabaseFieldConfig>();
		for (Class<?> classWalk = clazz; classWalk != null; classWalk = classWalk.getSuperclass()) {
			for (Field field : classWalk.getDeclaredFields()) {
				DatabaseFieldConfig config = configFromField(databaseType, tableName, field);
				if (config != null && config.isPersisted()) {
					fieldConfigs.add(config);
				}
			}
		}
		if (fieldConfigs.size() == 0) {
			return null;
		} else {
			return new DatabaseTableConfig<T>(clazz, tableName, fieldConfigs);
		}
	}
	/**
	 * Extract our configuration information from the field by looking for a {@link DatabaseField} annotation.
	 */
	private static DatabaseFieldConfig configFromField(DatabaseType databaseType, String tableName, Field field)
			throws SQLException {
		return DatabaseFieldConfig.fromField(databaseType, tableName, field);

	}

}
