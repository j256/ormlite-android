package com.j256.ormlite.android;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.j256.ormlite.db.DatabaseType;
import com.j256.ormlite.field.DataPersister;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.DatabaseFieldConfig;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.DatabaseTableConfig;

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

	private static Class<?> annotationFactoryClazz;
	private static Field elementsField;
	private static Class<?> annotationMemberClazz;
	private static Field nameField;
	private static Field valueField;

	private static final ConfigField[] configFields = lookupClasses();

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

		if (configFields == null) {
			return DatabaseFieldConfig.fromField(databaseType, tableName, field);
		}

		/*
		 * This, unfortunately, we can't get around. This creates a AnnotationFactory, an array of AnnotationMember
		 * fields, and possibly another array of AnnotationMember values. This creates a large number of GC'd objects.
		 */
		DatabaseField databaseField = field.getAnnotation(DatabaseField.class);
		if (databaseField == null) {
			return null;
		}

		DatabaseFieldConfig config = null;
		try {
			config = buildConfig(databaseField, tableName, field);
		} catch (Exception e) {
			// ignored so we will configure normally below
		}

		if (config == null) {
			return DatabaseFieldConfig.fromField(databaseType, tableName, field);
		} else {
			return config;
		}
	}

	/**
	 * Instead of calling the annotation methods directly, we peer inside the proxy and investigate the array of
	 * AnnotationMember objects stored by the AnnotationFactory.
	 */
	private static DatabaseFieldConfig buildConfig(DatabaseField databaseField, String tableName, Field field)
			throws Exception {
		InvocationHandler proxy = Proxy.getInvocationHandler(databaseField);
		if (proxy.getClass() != annotationFactoryClazz) {
			return null;
		}
		// this should be an array of AnnotationMember objects
		Object elementsObject = elementsField.get(proxy);
		if (elementsObject == null) {
			return null;
		}
		DatabaseFieldConfig config = new DatabaseFieldConfig(field.getName());
		Object[] objs = (Object[]) elementsObject;
		for (int i = 0; i < configFields.length; i++) {
			Object value = valueField.get(objs[i]);
			if (value != null) {
				configFields[i].assignConfigField(config, field, value);
			}
		}
		return config;
	}

	/**
	 * This does all of the class reflection fu to find our classes, find the order of field names, and construct our
	 * array of ConfigField entries the correspond to the AnnotationMember array.
	 */
	private static ConfigField[] lookupClasses() {
		try {
			annotationFactoryClazz = Class.forName("org.apache.harmony.lang.annotation.AnnotationFactory");
			annotationMemberClazz = Class.forName("org.apache.harmony.lang.annotation.AnnotationMember");
			Class<?> annotationMemberArrayClazz =
					Class.forName("[Lorg.apache.harmony.lang.annotation.AnnotationMember;");

			elementsField = annotationFactoryClazz.getDeclaredField("elements");
			elementsField.setAccessible(true);

			annotationMemberClazz = Class.forName("org.apache.harmony.lang.annotation.AnnotationMember");
			nameField = annotationMemberClazz.getDeclaredField("name");
			nameField.setAccessible(true);
			valueField = annotationMemberClazz.getDeclaredField("value");
			valueField.setAccessible(true);

			Field field = DatabaseFieldSample.class.getDeclaredField("field");
			DatabaseField databaseField = field.getAnnotation(DatabaseField.class);
			InvocationHandler proxy = Proxy.getInvocationHandler(databaseField);
			if (proxy.getClass() != annotationFactoryClazz) {
				return null;
			}

			// this should be an array of AnnotationMember objects
			Object elements = elementsField.get(proxy);
			if (elements == null || elements.getClass() != annotationMemberArrayClazz) {
				return null;
			}

			Object[] elementArray = (Object[]) elements;
			ConfigField[] configFields = new ConfigField[elementArray.length];

			// build our array of ConfigField enum entries that match the AnnotationMember array
			for (int i = 0; i < elementArray.length; i++) {
				String name = (String) nameField.get(elementArray[i]);
				configFields[i] = ConfigField.valueOf(name);
			}
			return configFields;
		} catch (Exception e) {
			// if any reflection fu fails then we bail and use the default _slow_ mechanisms
			return null;
		}
	}

	/**
	 * Class used to investigate the @DatabaseField annotation.
	 */
	private static class DatabaseFieldSample {
		@SuppressWarnings("unused")
		@DatabaseField
		String field;
	}

	/**
	 * Enumeration that converts from field/value to {@link DatabaseFieldConfig}.
	 */
	private enum ConfigField {
		columnName() {
			@Override
			public void assignConfigField(DatabaseFieldConfig config, Field field, Object value) {
				config.setColumnName(valueIfNotBlank((String) value));
			}
		},
		dataType() {
			@Override
			public void assignConfigField(DatabaseFieldConfig config, Field field, Object value) {
				config.setDataType((DataType) value);
			}
		},
		defaultValue() {
			@Override
			public void assignConfigField(DatabaseFieldConfig config, Field field, Object value) {
				String defaultValue = (String) value;
				if (!defaultValue.equals(DatabaseField.DEFAULT_STRING)) {
					config.setDefaultValue(defaultValue);
				}
			}
		},
		width() {
			@Override
			public void assignConfigField(DatabaseFieldConfig config, Field field, Object value) {
				config.setWidth((Integer) value);
			}
		},
		canBeNull() {
			@Override
			public void assignConfigField(DatabaseFieldConfig config, Field field, Object value) {
				config.setCanBeNull((Boolean) value);
			}
		},
		id() {
			@Override
			public void assignConfigField(DatabaseFieldConfig config, Field field, Object value) {
				config.setId((Boolean) value);
			}
		},
		generatedId() {
			@Override
			public void assignConfigField(DatabaseFieldConfig config, Field field, Object value) {
				config.setGeneratedId((Boolean) value);
			}
		},
		generatedIdSequence() {
			@Override
			public void assignConfigField(DatabaseFieldConfig config, Field field, Object value) {
				config.setGeneratedIdSequence(valueIfNotBlank((String) value));
			}
		},
		foreign() {
			@Override
			public void assignConfigField(DatabaseFieldConfig config, Field field, Object value) {
				config.setForeign((Boolean) value);
			}
		},
		useGetSet() {
			@Override
			public void assignConfigField(DatabaseFieldConfig config, Field field, Object value) {
				config.setUseGetSet((Boolean) value);
			}
		},
		unknownEnumName() {
			@Override
			public void assignConfigField(DatabaseFieldConfig config, Field field, Object value) {
				config.setUnknownEnumValue(DatabaseFieldConfig.findMatchingEnumVal(field, (String) value));
			}
		},
		throwIfNull() {
			@Override
			public void assignConfigField(DatabaseFieldConfig config, Field field, Object value) {
				config.setThrowIfNull((Boolean) value);
			}
		},
		persisted() {
			@Override
			public void assignConfigField(DatabaseFieldConfig config, Field field, Object value) {
				config.setPersisted((Boolean) value);
			}
		},
		format() {
			@Override
			public void assignConfigField(DatabaseFieldConfig config, Field field, Object value) {
				config.setFormat(valueIfNotBlank((String) value));
			}
		},
		unique() {
			@Override
			public void assignConfigField(DatabaseFieldConfig config, Field field, Object value) {
				config.setUnique((Boolean) value);
			}
		},
		uniqueCombo() {
			@Override
			public void assignConfigField(DatabaseFieldConfig config, Field field, Object value) {
				config.setUniqueCombo((Boolean) value);
			}
		},
		index() {
			@Override
			public void assignConfigField(DatabaseFieldConfig config, Field field, Object value) {
				config.setIndex((Boolean) value);
			}
		},
		uniqueIndex() {
			@Override
			public void assignConfigField(DatabaseFieldConfig config, Field field, Object value) {
				config.setUniqueIndex((Boolean) value);
			}
		},
		indexName() {
			@Override
			public void assignConfigField(DatabaseFieldConfig config, Field field, Object value) {
				config.setIndexName(valueIfNotBlank((String) value));
			}
		},
		uniqueIndexName() {
			@Override
			public void assignConfigField(DatabaseFieldConfig config, Field field, Object value) {
				config.setUniqueIndexName(valueIfNotBlank((String) value));
			}
		},
		foreignAutoRefresh() {
			@Override
			public void assignConfigField(DatabaseFieldConfig config, Field field, Object value) {
				config.setForeignAutoRefresh((Boolean) value);
			}
		},
		maxForeignAutoRefreshLevel() {
			@Override
			public void assignConfigField(DatabaseFieldConfig config, Field field, Object value) {
				config.setMaxForeignAutoRefreshLevel((Integer) value);
			}
		},
		persisterClass() {
			@Override
			public void assignConfigField(DatabaseFieldConfig config, Field field, Object value) {
				@SuppressWarnings("unchecked")
				Class<? extends DataPersister> clazz = (Class<? extends DataPersister>) value;
				config.setPersisterClass(clazz);
			}
		},
		allowGeneratedIdInsert() {
			@Override
			public void assignConfigField(DatabaseFieldConfig config, Field field, Object value) {
				config.setAllowGeneratedIdInsert((Boolean) value);
			}
		},
		columnDefinition() {
			@Override
			public void assignConfigField(DatabaseFieldConfig config, Field field, Object value) {
				config.setColumnDefinition(valueIfNotBlank((String) value));
			}
		},
		foreignAutoCreate() {
			@Override
			public void assignConfigField(DatabaseFieldConfig config, Field field, Object value) {
				config.setForeignAutoCreate((Boolean) value);
			}
		},
		version() {
			@Override
			public void assignConfigField(DatabaseFieldConfig config, Field field, Object value) {
				config.setVersion((Boolean) value);
			}
		},
		// end
		;

		/**
		 * Take the value from the annotation and assign it into the config.
		 */
		public abstract void assignConfigField(DatabaseFieldConfig config, Field field, Object value);

		protected String valueIfNotBlank(String value) {
			if (value == null || value.length() == 0) {
				return null;
			} else {
				return value;
			}
		}
	}
}
