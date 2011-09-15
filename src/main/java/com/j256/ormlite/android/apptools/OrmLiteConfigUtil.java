package com.j256.ormlite.android.apptools;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.db.DatabaseType;
import com.j256.ormlite.db.SqliteAndroidDatabaseType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.DatabaseFieldConfig;
import com.j256.ormlite.field.DatabaseFieldSimple;
import com.j256.ormlite.table.DatabaseTable;
import com.j256.ormlite.table.DatabaseTableConfig;

/**
 * Database configuration file helper class that is used to write a configuration file into the raw resource
 * sub-directory to speed up DAO creation.
 * 
 * <p>
 * With help from the user list and especially Ian Dees, we discovered that calls to annotation methods in Android are
 * _very_ expensive because Method.equals() was doing a huge toString(). This was causing folks to see 2-3 seconds
 * startup time when configuring 10-15 DAOs because of 1000s of calls to @DatabaseField methods. See <a
 * href="https://code.google.com/p/android/issues/detail?id=7811" >this Android bug report</a>.
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
 * <p>
 * <b>NOTE:</b> If you need the speed but are worried about the regeneration, then consider using the
 * {@link DatabaseFieldSimple} and other DatabaseField* annotations instead of {@link DatabaseField} which each contain
 * fewer methods and run a lot faster.
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
	 * Calls {@link #findAnnotatedClasses(File)} for the current directory ("."), calls {@link #findRawDir(File)}, and
	 * then calls {@link #writeConfigFile(File, String, Class[])}.
	 */
	protected static void writeConfigFile(String fileName) throws Exception {
		File rootDir = new File(".");
		Class<?>[] classes = findAnnotatedClasses(rootDir);
		File rawDir = findRawDir(rootDir);
		if (rawDir == null) {
			System.err.println("Could not file " + RAW_DIR_NAME + " directory");
		} else {
			writeConfigFile(rawDir, fileName, classes);
			System.out.println("Done.");
		}
	}

	/**
	 * Run through the directories from the root-directory looking for files that end with ".java" and contain one of
	 * the {@link DatabaseTable}, {@link DatabaseField}, or {@link DatabaseFieldSimple} annotations.
	 */
	protected static Class<?>[] findAnnotatedClasses(File rootDir) throws Exception {
		List<Class<?>> classList = new ArrayList<Class<?>>();
		findAnnotatedClasses(rootDir, classList);
		if (classList.isEmpty()) {
			System.err.println("No annotated classes were found");
			System.exit(1);
		}
		return classList.toArray(new Class[classList.size()]);
	}

	/**
	 * Look for the resource-directory in the current directory or the directories above. Then look for the
	 * raw-directory underneath the resource-directory.
	 */
	protected static File findRawDir(File dir) throws Exception {
		for (int i = 0; i < 20; i++) {
			File rawDir = lookForRawDirRecurse(dir);
			if (rawDir != null) {
				return rawDir;
			}
			dir = dir.getParentFile();
		}
		return null;
	}

	/**
	 * Write a configuration file in the raw directory with the configuration from classes.
	 */
	protected static void writeConfigFile(File rawDir, String fileName, Class<?>[] classes) throws Exception {
		File configFile = new File(rawDir, fileName);
		System.out.println("Writing configurations to " + configFile.getAbsolutePath());

		DatabaseType databaseType = new SqliteAndroidDatabaseType();
		BufferedWriter writer = new BufferedWriter(new FileWriter(configFile), 4096);
		try {
			writer.append('#');
			writer.newLine();
			writer.append("# generated on ").append(new SimpleDateFormat("yyyy/MM/dd hh:mm:ss").format(new Date()));
			writer.newLine();
			writer.append('#');
			writer.newLine();
			for (Class<?> clazz : classes) {
				String tableName = DatabaseTableConfig.extractTableName(clazz);
				List<DatabaseFieldConfig> fieldConfigs = new ArrayList<DatabaseFieldConfig>();
				for (Field field : clazz.getDeclaredFields()) {
					DatabaseFieldConfig fieldConfig = DatabaseFieldConfig.fromField(databaseType, tableName, field);
					if (fieldConfig != null) {
						fieldConfigs.add(fieldConfig);
					}
				}
				if (fieldConfigs.isEmpty()) {
					System.out.println("Skipping " + clazz + " because of no annotated fields");
					continue;
				}
				@SuppressWarnings({ "rawtypes", "unchecked" })
				DatabaseTableConfig<?> tableConfig = new DatabaseTableConfig(clazz, tableName, fieldConfigs);
				tableConfig.write(writer);
				writer.append("#################################");
				writer.newLine();
				System.out.println("Wrote config for " + clazz);
			}
		} finally {
			writer.close();
		}
	}

	/**
	 * Recursive version of {@link #findAnnotatedClasses(File)}.
	 */
	private static void findAnnotatedClasses(File dir, List<Class<?>> classList) throws Exception {
		for (File file : dir.listFiles()) {
			if (file.isDirectory()) {
				findAnnotatedClasses(file, classList);
			} else if (file.getName().endsWith(".java")) {
				String prefix = getPackageOfAnnotatedFile(file);
				if (prefix == null) {
					continue;
				}
				String name = file.getName();
				// cut off the .java
				name = name.substring(0, name.length() - ".java".length());
				String className = prefix + "." + name;
				classList.add(Class.forName(className));
			}
		}
	}

	/**
	 * Returns the package name of a file that has one of the annotations we are looking for.
	 * 
	 * @return Package prefix string or null or no annotations.
	 */
	private static String getPackageOfAnnotatedFile(File file) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(file));
		String prefix = null;
		try {
			while (true) {
				String line = reader.readLine();
				if (line == null) {
					return null;
				}
				if (line.startsWith("package")) {
					String[] parts = line.split("[ \t;]");
					prefix = parts[1];
				}
				if (line.startsWith("import " + DatabaseTable.class.getName())
						|| line.startsWith("import " + DatabaseField.class.getName())
						|| line.startsWith("import " + DatabaseFieldSimple.class.getName())) {
					if (prefix == null) {
						throw new IllegalStateException("Found import in " + file + " but no package statement");
					}
					return prefix;
				}
			}
		} finally {
			reader.close();
		}
	}

	/**
	 * Recursive version of {@link #findRawDir(File)}.
	 */
	private static File lookForRawDirRecurse(File dir) throws Exception {
		for (File file : dir.listFiles()) {
			if (file.getName().equals(RESOURCE_DIR_NAME) && file.isDirectory()) {
				File[] rawFiles = file.listFiles(new FileFilter() {
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
}
