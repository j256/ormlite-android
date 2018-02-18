package com.j256.ormlite.android.apptools;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Field;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.db.DatabaseType;
import com.j256.ormlite.db.SqliteAndroidDatabaseType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.DatabaseFieldConfig;
import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.table.DatabaseTable;
import com.j256.ormlite.table.DatabaseTableConfig;
import com.j256.ormlite.table.DatabaseTableConfigLoader;

/**
 * Database configuration file helper class that is used to write a configuration file into the raw resource
 * sub-directory to speed up DAO creation. If run from main, it takes an optional "-s" argument which turns on sorting
 * of fields by name to produce more deterministic output followed by the name of the output config file.
 * 
 * <p>
 * With help from the user list and especially Ian Dees, we discovered that calls to annotation methods in Android are
 * _very_ expensive because Method.equals() was doing a huge toString(). This was causing folks to see 2-3 seconds
 * startup time when configuring 10-15 DAOs because of 1000s of calls to @DatabaseField methods. See
 * <a href="https://code.google.com/p/android/issues/detail?id=7811" >this Android bug report</a>.
 * </p>
 * 
 * <p>
 * I added this utility class which writes a configuration file into the raw resource "res/raw" directory inside of your
 * project containing the table and field names and associated details. This file can then be loaded into the
 * {@link DaoManager} with the help of the
 * {@link OrmLiteSqliteOpenHelper#OrmLiteSqliteOpenHelper(android.content.Context, String, android.database.sqlite.SQLiteDatabase.CursorFactory, int, int)}
 * constructor. This means that you can configure your classes _without_ any runtime calls to annotations. It seems
 * significantly faster.
 * <p>
 * 
 * <p>
 * <b>WARNING:</b> Although this is fast, the big problem is that you have to remember to regenerate the config file
 * whenever you edit one of your database classes. There is no way that I know of to do this automagically.
 * </p>
 * 
 * @author graywatson
 */
public class OrmLiteConfigUtil {

	/**
	 * Resource directory name that we are looking for.
	 */
	protected static final String RESOURCE_DIR_NAME = "res";
	/**
	 * Raw directory name that we are looking for.
	 */
	protected static final String RAW_DIR_NAME = "raw";

	/**
	 * Maximum recursion level while we are looking for source files.
	 */
	protected static int maxFindSourceLevel = 20;

	private static final DatabaseType databaseType = new SqliteAndroidDatabaseType();
	private static final FieldComparator fieldComparator = new FieldComparator();

	/**
	 * A call through to {@link #writeConfigFile(File, boolean)}. It takes an optional "-s" argument which turns on
	 * sorting of fields by name to produce more deterministic output followed by the name of the output config file.
	 */
	public static void main(String[] args) throws Exception {
		int argCount = 0;
		boolean sortFields = false;
		for (; argCount < args.length; argCount++) {
			String arg = args[argCount];
			if (arg.equals("-s")) {
				sortFields = true;
			} else {
				break;
			}
		}
		// we should have one arg left
		if (argCount != args.length - 1) {
			throw new IllegalArgumentException("Usage: OrmLiteConfigUtil [-s] config-file-name");
		}
		writeConfigFile(args[argCount], sortFields);
	}

	/**
	 * Finds the annotated classes in the current directory or below and writes a configuration file to the file-name in
	 * the raw folder.
	 */
	public static void writeConfigFile(String fileName) throws SQLException, IOException {
		writeConfigFile(fileName, false);
	}

	/**
	 * Finds the annotated classes in the current directory or below and writes a configuration file to the file-name in
	 * the raw folder.
	 * 
	 * @param sortFields
	 *            Set to true to sort the fields by name before the file is generated.
	 */
	public static void writeConfigFile(String fileName, boolean sortFields) throws SQLException, IOException {
		List<Class<?>> classList = new ArrayList<Class<?>>();
		findAnnotatedClasses(classList, new File("."), 0);
		writeConfigFile(fileName, classList.toArray(new Class[classList.size()]), sortFields);
	}

	/**
	 * Writes a configuration fileName in the raw directory with the configuration for classes.
	 */
	public static void writeConfigFile(String fileName, Class<?>[] classes) throws SQLException, IOException {
		writeConfigFile(fileName, classes, false);
	}

	/**
	 * Writes a configuration fileName in the raw directory with the configuration for classes.
	 * 
	 * @param sortFields
	 *            Set to true to sort the fields by name before the file is generated.
	 */
	public static void writeConfigFile(String fileName, Class<?>[] classes, boolean sortFields)
			throws SQLException, IOException {
		File rawDir = findRawDir(new File("."));
		if (rawDir == null) {
			System.err.println("Could not find " + RAW_DIR_NAME + " directory which is typically in the "
					+ RESOURCE_DIR_NAME + " directory");
		} else {
			File configFile = new File(rawDir, fileName);
			writeConfigFile(configFile, classes, sortFields);
		}
	}

	/**
	 * Finds the annotated classes in the current directory or below and writes a configuration file.
	 */
	public static void writeConfigFile(File configFile) throws SQLException, IOException {
		writeConfigFile(configFile, false);
	}

	/**
	 * Finds the annotated classes in the current directory or below and writes a configuration file.
	 */
	public static void writeConfigFile(File configFile, boolean sortFields) throws SQLException, IOException {
		writeConfigFile(configFile, new File("."), sortFields);
	}

	/**
	 * Finds the annotated classes in the specified search directory or below and writes a configuration file.
	 */
	public static void writeConfigFile(File configFile, File searchDir) throws SQLException, IOException {
		writeConfigFile(configFile, searchDir, false);
	}

	/**
	 * Finds the annotated classes in the specified search directory or below and writes a configuration file.
	 */
	public static void writeConfigFile(File configFile, File searchDir, boolean sortFields)
			throws SQLException, IOException {
		List<Class<?>> classList = new ArrayList<Class<?>>();
		findAnnotatedClasses(classList, searchDir, 0);
		writeConfigFile(configFile, classList.toArray(new Class[classList.size()]), sortFields);
	}

	/**
	 * Write a configuration file with the configuration for classes.
	 */
	public static void writeConfigFile(File configFile, Class<?>[] classes) throws SQLException, IOException {
		writeConfigFile(configFile, classes, false);
	}

	/**
	 * Write a configuration file with the configuration for classes.
	 * 
	 * @param sortFields
	 *            Set to true to sort the fields by name before the file is generated.
	 */
	public static void writeConfigFile(File configFile, Class<?>[] classes, boolean sortFields)
			throws SQLException, IOException {
		System.out.println("Writing configurations to " + configFile.getAbsolutePath());
		writeConfigFile(new FileOutputStream(configFile), classes, sortFields);
	}

	/**
	 * Write a configuration file to an output stream with the configuration for classes.
	 */
	public static void writeConfigFile(OutputStream outputStream, File searchDir) throws SQLException, IOException {
		writeConfigFile(outputStream, searchDir, false);
	}

	/**
	 * Write a configuration file to an output stream with the configuration for classes.
	 */
	public static void writeConfigFile(OutputStream outputStream, File searchDir, boolean sortFields)
			throws SQLException, IOException {
		List<Class<?>> classList = new ArrayList<Class<?>>();
		findAnnotatedClasses(classList, searchDir, 0);
		writeConfigFile(outputStream, classList.toArray(new Class[classList.size()]), sortFields);
	}

	/**
	 * Write a configuration file to an output stream with the configuration for classes.
	 */
	public static void writeConfigFile(OutputStream outputStream, Class<?>[] classes) throws SQLException, IOException {
		writeConfigFile(outputStream, classes, false);
	}

	/**
	 * Write a configuration file to an output stream with the configuration for classes.
	 * 
	 * @param sortFields
	 *            Set to true to sort the fields by name before the file is generated.
	 */
	public static void writeConfigFile(OutputStream outputStream, Class<?>[] classes, boolean sortFields)
			throws SQLException, IOException {
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream), 4096);
		try {
			writeHeader(writer);
			for (Class<?> clazz : classes) {
				writeConfigForTable(writer, clazz, sortFields);
			}
			// NOTE: done is here because this is public
			System.out.println("Done.");
		} finally {
			writer.close();
		}
	}

	/**
	 * Look for the resource-directory in the current directory or the directories above. Then look for the
	 * raw-directory underneath the resource-directory.
	 */
	protected static File findRawDir(File dir) {
		for (int i = 0; dir != null && i < 20; i++) {
			File rawDir = findResRawDir(dir);
			if (rawDir != null) {
				return rawDir;
			}
			dir = dir.getParentFile();
		}
		return null;
	}

	private static void writeHeader(BufferedWriter writer) throws IOException {
		writer.append('#');
		writer.newLine();
		writer.append("# generated on ").append(new SimpleDateFormat("yyyy/MM/dd hh:mm:ss").format(new Date()));
		writer.newLine();
		writer.append('#');
		writer.newLine();
	}

	private static void findAnnotatedClasses(List<Class<?>> classList, File dir, int level)
			throws SQLException, IOException {
		for (File file : dir.listFiles()) {
			if (file.isDirectory()) {
				// recurse if we aren't deep enough
				if (level < maxFindSourceLevel) {
					findAnnotatedClasses(classList, file, level + 1);
				}
				continue;
			}
			// skip non .java files
			if (!file.getName().endsWith(".java")) {
				continue;
			}
			String packageName = getPackageOfClass(file);
			if (packageName == null) {
				System.err.println("Could not find package name for: " + file);
				continue;
			}
			// get the filename and cut off the .java
			String name = file.getName();
			name = name.substring(0, name.length() - ".java".length());
			String className = packageName + "." + name;
			Class<?> clazz;
			try {
				clazz = Class.forName(className);
			} catch (Throwable t) {
				// amazingly, this sometimes throws an Error
				System.err.println("Could not load class file for: " + file);
				System.err.println("     " + t);
				continue;
			}
			if (classHasAnnotations(clazz)) {
				classList.add(clazz);
			}
			// handle inner classes
			try {
				for (Class<?> innerClazz : clazz.getDeclaredClasses()) {
					if (classHasAnnotations(innerClazz)) {
						classList.add(innerClazz);
					}
				}
			} catch (Throwable t) {
				// amazingly, this sometimes throws an Error
				System.err.println("Could not load inner classes for: " + clazz);
				System.err.println("     " + t);
				continue;
			}
		}
	}

	private static void writeConfigForTable(BufferedWriter writer, Class<?> clazz, boolean sortFields)
			throws SQLException, IOException {
		String tableName = DatabaseTableConfig.extractTableName(clazz);
		List<DatabaseFieldConfig> fieldConfigs = new ArrayList<DatabaseFieldConfig>();
		// walk up the classes finding the fields
		try {
			for (Class<?> working = clazz; working != null; working = working.getSuperclass()) {
				Field[] declaredFields = working.getDeclaredFields();
				if (sortFields) {
					// sort the files by field name before we generate the field config entries
					Arrays.sort(declaredFields, fieldComparator);
				}
				for (Field field : declaredFields) {
					DatabaseFieldConfig fieldConfig = DatabaseFieldConfig.fromField(databaseType, tableName, field);
					if (fieldConfig != null) {
						fieldConfigs.add(fieldConfig);
					}
				}
			}
		} catch (Error e) {
			System.err.println(
					"Skipping " + clazz + " because we got an error finding its definition: " + e.getMessage());
			return;
		}
		if (fieldConfigs.isEmpty()) {
			System.out.println("Skipping " + clazz + " because no annotated fields found");
			return;
		}
		@SuppressWarnings({ "rawtypes", "unchecked" })
		DatabaseTableConfig<?> tableConfig = new DatabaseTableConfig(clazz, tableName, fieldConfigs);
		DatabaseTableConfigLoader.write(writer, tableConfig);
		writer.append("#################################");
		writer.newLine();
		System.out.println("Wrote config for " + clazz);
	}

	private static boolean classHasAnnotations(Class<?> clazz) {
		while (clazz != null) {
			if (clazz.getAnnotation(DatabaseTable.class) != null) {
				return true;
			}
			Field[] fields;
			try {
				fields = clazz.getDeclaredFields();
			} catch (Throwable t) {
				// amazingly, this sometimes throws an Error
				System.err.println("Could not load get delcared fields from: " + clazz);
				System.err.println("     " + t);
				return false;
			}
			for (Field field : fields) {
				if (field.getAnnotation(DatabaseField.class) != null
						|| field.getAnnotation(ForeignCollectionField.class) != null) {
					return true;
				}
			}
			try {
				clazz = clazz.getSuperclass();
			} catch (Throwable t) {
				// amazingly, this sometimes throws an Error
				System.err.println("Could not get super class for: " + clazz);
				System.err.println("     " + t);
				return false;
			}
		}

		return false;
	}

	/**
	 * Returns the package name of a file that has one of the annotations we are looking for.
	 * 
	 * @return Package prefix string or null or no annotations.
	 */
	private static String getPackageOfClass(File file) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(file));
		try {
			while (true) {
				String line = reader.readLine();
				if (line == null) {
					return null;
				}
				if (line.contains("package")) {
					String[] parts = line.split("[ \t;]");
					if (parts.length > 1 && parts[0].equals("package")) {
						return parts[1];
					}
				}
			}
		} finally {
			reader.close();
		}
	}

	/**
	 * Look for the resource directory with raw beneath it.
	 */
	private static File findResRawDir(File dir) {
		for (File file : dir.listFiles()) {
			if (file.getName().equals(RESOURCE_DIR_NAME) && file.isDirectory()) {
				File[] rawFiles = file.listFiles(new FileFilter() {
					@Override
					public boolean accept(File file) {
						return file.getName().equals(RAW_DIR_NAME) && file.isDirectory();
					}
				});
				if (rawFiles.length == 1) {
					return rawFiles[0];
				}
			}
		}
		return null;
	}

	/**
	 * Compare fields by name.
	 */
	private static class FieldComparator implements Comparator<Field> {

		@Override
		public int compare(Field arg0, Field arg1) {
			return arg0.getName().compareTo(arg1.getName());
		}
	}
}
