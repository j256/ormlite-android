package com.j256.ormlite.android;

import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.j256.ormlite.db.DatabaseType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.DatabaseFieldConfig;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.DatabaseTableConfig;

/**
 * Class which extracts the table-config from a class. This use to be a reflection hack to make the job of processing
 * the {@link DatabaseField} annotation more efficient. In past versions of Android before ice-cream-sandwich (think
 * 11/2011), annotations were ghastly slow and the hack was a bit gross but yielded a significant (~20x) performance
 * improvement. Thanks to Josh Guilfoyle for them.
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
		String tableName = DatabaseTableConfig.extractTableName(databaseType, clazz);
		List<DatabaseFieldConfig> fieldConfigs = new ArrayList<DatabaseFieldConfig>();
		for (Class<?> classWalk = clazz; classWalk != null; classWalk = classWalk.getSuperclass()) {
			for (Field field : classWalk.getDeclaredFields()) {
				DatabaseFieldConfig config = DatabaseFieldConfig.fromField(databaseType, tableName, field);
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
}
