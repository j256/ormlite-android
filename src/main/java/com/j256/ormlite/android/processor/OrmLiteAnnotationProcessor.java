package com.j256.ormlite.android.processor;

import com.j256.ormlite.android.annotations.Database;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.table.DatabaseTable;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.FilerException;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.SourceVersion;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.MirroredTypesException;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic.Kind;
import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;

/**
 * Class that is automatically run when compiling client code that automatically
 * generates code to call DaoManager.addCachedDatabaseConfigs() without needing
 * a config file.
 * 
 * @author nathancrouther
 */
/*
 * Eclipse doesn't like the link to rt.jar and classes therein. This is a
 * spurious warning that can be ignored. It is intended to prevent referencing
 * com.sun packages that may not be in every JVM, but the annotation processing
 * stuff is part of JSR-269, so will always be present.
 */
@SuppressWarnings("restriction")
public final class OrmLiteAnnotationProcessor extends AbstractProcessor {
	private static final String FQCN_Object = "java.lang.Object";
	private static final String FQCN_Class = "java.lang.Class";
	private static final String FQCN_OpenHelper = "com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper";
	private static final String CN_OpenHelper = "OrmLiteSqliteOpenHelper";

	private Map<TypeElement, List<TypeElement>> foundDatabases = new HashMap<TypeElement, List<TypeElement>>();
	private Set<TypeElement> foundTables = new HashSet<TypeElement>();

	@Override
	public Set<String> getSupportedAnnotationTypes() {
		Set<String> types = new LinkedHashSet<String>();
		types.add(DatabaseTable.class.getCanonicalName());
		types.add(Database.class.getCanonicalName());
		return types;
	}

	@Override
	public SourceVersion getSupportedSourceVersion() {
		return SourceVersion.latest();
	}

	@Override
	public boolean process(Set<? extends TypeElement> elements,
			RoundEnvironment env) {
		for (Element element : env
				.getElementsAnnotatedWith(DatabaseTable.class)) {
			if (element.getAnnotation(DatabaseTable.class) == null) {
				continue;
			}

			raiseNote(String.format("Processing %s class: %s",
					DatabaseTable.class.getSimpleName(),
					((TypeElement) element).getQualifiedName()));

			TableBindings table = new TableBindings(
					new ParsedClassName(element),
					element.getAnnotation(DatabaseTable.class));

			// get all fields from this and all parents until we hit Object
			TypeElement tableClassElement = (TypeElement) element;
			foundTables.add(tableClassElement);
			do {
				for (Element child : tableClassElement.getEnclosedElements()) {
					if (child.getKind().isField()) {
						DatabaseField databaseField = child
								.getAnnotation(DatabaseField.class);
						ForeignCollectionField foreignCollectionField = child
								.getAnnotation(ForeignCollectionField.class);
						if (databaseField != null
								|| foreignCollectionField != null) {
							FieldBindings field = new FieldBindings(child
									.asType().toString(), child.getSimpleName()
									.toString(), databaseField,
									foreignCollectionField);
							table.addField(field);
						}

						if (databaseField != null
								&& foreignCollectionField != null) {
							raiseError(
									String.format(
											"Fields cannot be annotated with both %s and %s",
											DatabaseField.class.getSimpleName(),
											ForeignCollectionField.class
													.getSimpleName()), child);
						}
					}
				}

				tableClassElement = (TypeElement) processingEnv.getTypeUtils()
						.asElement(tableClassElement.getSuperclass());
			} while (!tableClassElement.getQualifiedName().toString()
					.equals(FQCN_Object));

			if (table.getFields().isEmpty()) {
				raiseWarning(
						String.format(
								"No fields annotated with %s found for class annotated with %s",
								DatabaseField.class.getSimpleName(),
								DatabaseTable.class.getSimpleName()), element);
			}

			createTableConfigSourceFile(table, element);
		}

		Set<? extends Element> tablesCollectionElements = env
				.getElementsAnnotatedWith(Database.class);
		for (Element element : tablesCollectionElements) {
			Database annotation = element.getAnnotation(Database.class);
			if (annotation == null) {
				continue;
			}

			raiseNote(String.format("Processing %s class: %s",
					Database.class.getSimpleName(),
					((TypeElement) element).getQualifiedName()));

			boolean derivedFromOpenHelper = false;
			TypeElement annotatedClassElement = (TypeElement) element;
			do {
				if (annotatedClassElement.getQualifiedName().toString()
						.equals(FQCN_OpenHelper)) {
					derivedFromOpenHelper = true;
				}
				annotatedClassElement = (TypeElement) processingEnv
						.getTypeUtils().asElement(
								annotatedClassElement.getSuperclass());
			} while (!annotatedClassElement.getQualifiedName().toString()
					.equals(FQCN_Object));
			if (!derivedFromOpenHelper) {
				raiseError(
						String.format(
								"%s annotation must be applied to a class deriving from %s",
								Database.class.getSimpleName(), CN_OpenHelper),
						element);
			}

			List<TypeElement> tableTypes = new ArrayList<TypeElement>();
			try {
				Class<?>[] classes = annotation.value();
				if (classes != null) {
					for (int i = 0; i < classes.length; ++i) {
						TypeElement typeElement;
						try {
							typeElement = processingEnv.getElementUtils()
									.getTypeElement(
											classes[i].getCanonicalName());
						} catch (MirroredTypeException mte) {
							typeElement = (TypeElement) processingEnv
									.getTypeUtils().asElement(
											mte.getTypeMirror());
						}
						tableTypes.add(typeElement);
					}
				} else {
					// eclipse populates this with null if annotation populated
					// with scalar (no {}) even though this is a legal shortcut
					raiseError(String.format(
							"%s annotation must enclose values array with {}",
							Database.class.getSimpleName()), element);
					continue;
				}
			} catch (MirroredTypesException mte) {
				for (TypeMirror m : mte.getTypeMirrors()) {
					tableTypes.add((TypeElement) processingEnv.getTypeUtils()
							.asElement(m));
				}
			}

			if (tableTypes.isEmpty()) {
				raiseError(
						String.format(
								"%s annotation must contain at least one class annotated with %s",
								Database.class.getSimpleName(),
								DatabaseTable.class.getSimpleName()), element);
			}

			foundDatabases.put((TypeElement) element, tableTypes);

			createDatabaseConfigSourceFile((TypeElement) element, tableTypes);
		}

		if (env.processingOver()) {
			raiseNote(String.format(
					"Finished processing %d %s class(es) and %d %s class(es)",
					foundTables.size(), DatabaseTable.class.getSimpleName(),
					foundDatabases.size(), Database.class.getSimpleName()));

			final StandardLocation savedDatabaseInfoLocation = StandardLocation.SOURCE_OUTPUT;
			final String savedDatabaseInfoPackageName = getClass().getPackage()
					.getName();
			final String savedDatabaseInfoFileName = "savedDatabaseInfo";

			// Try to read a file from before
			Map<String, List<String>> savedDatabaseInfo;
			try {
				FileObject file = processingEnv.getFiler()
						.getResource(savedDatabaseInfoLocation,
								savedDatabaseInfoPackageName,
								savedDatabaseInfoFileName);
				ObjectInputStream reader = new ObjectInputStream(
						file.openInputStream());
				try {
					// We created the file previously, so the cast is safe
					@SuppressWarnings("unchecked")
					Map<String, List<String>> oldDatabaseInfo = (Map<String, List<String>>) reader
							.readObject();
					raiseNote(String.format(
							"Loaded %d Database-to-DatabaseTable mappings",
							oldDatabaseInfo.size()));
					savedDatabaseInfo = oldDatabaseInfo;
				} finally {
					reader.close();
				}
			} catch (FilerException e) {
				// This file will only exist and have content during a round of
				// incremental compilation, there is no clean way to detect this
				// without trying and failing to read the file, so we catch the
				// error and initialize with empty contents.
				savedDatabaseInfo = new HashMap<String, List<String>>();
			} catch (IOException e) {
				// This file will only exist and have content during a round of
				// incremental compilation, there is no clean way to detect this
				// without trying and failing to read the file, so we catch the
				// error and initialize with empty contents.
				savedDatabaseInfo = new HashMap<String, List<String>>();
			} catch (ClassNotFoundException e) {
				// Built-in Java classes will always be available so this should
				// never happen
				throw new RuntimeException(e);
			}

			// Verify each Database annotation only contains valid tables and
			// add it to the saved list for future rounds of incremental
			// compilation
			for (Entry<TypeElement, List<TypeElement>> databaseAndTables : foundDatabases
					.entrySet()) {
				List<String> tableNames = new ArrayList<String>();

				for (TypeElement table : databaseAndTables.getValue()) {
					if (table.getAnnotation(DatabaseTable.class) == null) {
						raiseError(
								String.format(
										"%s annotation contains class %s not annotated with %s",
										Database.class.getSimpleName(),
										table.getSimpleName(),
										DatabaseTable.class.getSimpleName()),
								databaseAndTables.getKey());
					}

					tableNames.add(table.getQualifiedName().toString());
				}

				savedDatabaseInfo.put(databaseAndTables.getKey()
						.getQualifiedName().toString(), tableNames);
			}

			// Save the updated information for future rounds of incremental
			// compilation
			try {
				FileObject file = processingEnv.getFiler()
						.createResource(savedDatabaseInfoLocation,
								savedDatabaseInfoPackageName,
								savedDatabaseInfoFileName);
				ObjectOutputStream writer = new ObjectOutputStream(
						file.openOutputStream());
				writer.writeObject(savedDatabaseInfo);
				writer.close();
				raiseNote(String.format(
						"Stored %d Database-to-DatabaseTable mappings",
						savedDatabaseInfo.size()));
			} catch (FilerException e) {
				// should never happen, but if it does, it shouldn't be fatal
				// since the worst consequence would be a spurious warning
				// during future incremental compilations that would be cleared
				// with a full rebuild.
				raiseNote("FilerException while saving Database-to-DatabaseTable mappings: "
						+ e.toString());
			} catch (IOException e) {
				// should never happen, but if it does, it shouldn't be fatal
				// since the worst consequence would be a spurious warning
				// during future incremental compilations that would be cleared
				// with a full rebuild.
				raiseNote("IOException while saving Database-to-DatabaseTable mappings: "
						+ e.toString());
			}

			// Verify that every table is in a database (try to enforce using
			// the database annotation for better performance)
			Set<String> tablesIncludedInDatabases = new HashSet<String>();
			for (List<String> tableNames : savedDatabaseInfo.values()) {
				for (String tableName : tableNames) {
					tablesIncludedInDatabases.add(tableName);
				}
			}
			for (TypeElement foundTable : foundTables) {
				if (!tablesIncludedInDatabases.contains(foundTable
						.getQualifiedName().toString())) {
					raiseWarning(
							String.format(
									"Class annotated with %s is not included in any %s annotation",
									DatabaseTable.class.getSimpleName(),
									Database.class.getSimpleName()), foundTable);
				}
			}
		}

		return true;
	}

	private void createDatabaseConfigSourceFile(TypeElement openHelperClass,
			List<TypeElement> tableClasses) {
		ParsedClassName openHelperClassName = new ParsedClassName(
				openHelperClass);

		try {
			JavaFileObject javaFileObject = processingEnv
					.getFiler()
					.createSourceFile(
							openHelperClassName
									.getGeneratedFullyQualifiedClassName(),
							openHelperClass);

			Writer writer = javaFileObject.openWriter();
			try {
				writer.write("package " + openHelperClassName.getPackageName()
						+ ";\n");
				writer.write("\n");
				writer.write("import java.sql.SQLException;\n");
				writer.write("import java.util.ArrayList;\n");
				writer.write("import java.util.List;\n");
				writer.write("\n");
				writer.write("import com.j256.ormlite.dao.DaoManager;\n");
				writer.write("import com.j256.ormlite.support.ConnectionSource;\n");
				writer.write("import com.j256.ormlite.table.DatabaseTableConfig;\n");
				writer.write("import com.j256.ormlite.table.TableUtils;\n");
				writer.write("\n");
				writer.write("public final class "
						+ openHelperClassName.getGeneratedClassName() + " {\n");
				writer.write("\tprivate "
						+ openHelperClassName.getGeneratedClassName()
						+ "() {\n");
				writer.write("\t}\n");
				writer.write("\n");
				writer.write("\tpublic static void cacheTableConfigurations() {\n");
				writer.write("\t\tList<DatabaseTableConfig<?>> tableConfigs = new ArrayList<DatabaseTableConfig<?>>();\n");
				for (TypeElement tableClass : tableClasses) {
					ParsedClassName tableClassName = new ParsedClassName(
							tableClass);
					writer.write("\t\ttableConfigs.add("
							+ tableClassName
									.getGeneratedFullyQualifiedClassName()
							+ ".CONFIG);\n");
				}
				writer.write("\t\tDaoManager.addCachedDatabaseConfigs(tableConfigs);\n");
				writer.write("\t}\n");
				writer.write("\n");
				writer.write("\tpublic static void createTables(ConnectionSource connectionSource) throws SQLException {\n");
				for (TypeElement tableClass : tableClasses) {
					ParsedClassName tableClassName = new ParsedClassName(
							tableClass);
					writer.write("\t\tTableUtils.createTable(connectionSource, "
							+ tableClassName.getInputFullyQualifiedClassName()
							+ ".class);\n");
				}
				writer.write("\t}\n");
				writer.write("}\n");
			} finally {
				writer.close();
			}
		} catch (FilerException e) {
			// if multiple classes are in the same file (e.g. inner/nested
			// classes), eclipse will do an incremental compilation for all of
			// them. The unchanged ones' generated files will not be deleted, so
			// we can ignore this benign error.
			raiseNote(String
					.format("Skipping file generation for %s since file already exists",
							openHelperClassName
									.getGeneratedFullyQualifiedClassName()));
		} catch (IOException e) {
			// We should always be able to generate the source files and if we
			// can't we should bail out
			throw new RuntimeException(e);
		}
	}

	private void createTableConfigSourceFile(TableBindings table,
			Element tableClassElement) {
		try {
			JavaFileObject javaFileObject = processingEnv.getFiler()
					.createSourceFile(
							table.getParsedClassName()
									.getGeneratedFullyQualifiedClassName(),
							tableClassElement);

			Writer writer = javaFileObject.openWriter();
			try {
				writeTable(writer, table);
			} finally {
				writer.close();
			}
		} catch (FilerException e) {
			// if multiple classes are in the same file (e.g. inner/nested
			// classes), eclipse will do an incremental compilation for all of
			// them. The unchanged ones' generated files will not be deleted, so
			// we can ignore this benign error.
			raiseNote(String
					.format("Skipping file generation for %s since file already exists",
							table.getParsedClassName()
									.getGeneratedFullyQualifiedClassName()));
		} catch (IOException e) {
			// We should always be able to generate the source files and if we
			// can't we should bail out
			throw new RuntimeException(e);
		}
	}

	private static void writeTable(Writer writer, TableBindings table)
			throws IOException {
		if (!table.getParsedClassName().getPackageName().isEmpty()) {
			writer.write("package "
					+ table.getParsedClassName().getPackageName() + ";\n");
			writer.write("\n");
		}
		writer.write("import java.util.ArrayList;\n");
		writer.write("import java.util.List;\n");
		writer.write("\n");
		writer.write("import com.j256.ormlite.field.DatabaseFieldConfig;\n");
		writer.write("import com.j256.ormlite.table.DatabaseTableConfig;\n");
		writer.write("\n");
		writer.write("public final class "
				+ table.getParsedClassName().getGeneratedClassName() + " {\n");
		writer.write("\tprivate "
				+ table.getParsedClassName().getGeneratedClassName() + "() {\n");
		writer.write("\t}\n");
		writer.write("\n");
		writer.write("\tpublic static final DatabaseTableConfig<"
				+ table.getParsedClassName().getInputFullyQualifiedClassName()
				+ "> CONFIG;\n");
		writer.write("\n");
		writer.write("\tstatic {\n");
		writer.write("\t\tList<DatabaseFieldConfig> databaseFieldConfigs = new ArrayList<DatabaseFieldConfig>();\n");
		for (FieldBindings field : table.getFields()) {

			if (field.getDatabaseFieldAnnotation() != null
					&& !field.getDatabaseFieldAnnotation().persisted()) {
				continue;
			}

			writer.write("\t\t{\n");
			writer.write(String
					.format("\t\t\tDatabaseFieldConfig databaseFieldConfig = new DatabaseFieldConfig(\"%s\");\n",
							field.getFieldName()));

			if (field.getDatabaseFieldAnnotation() != null) {
				writeSetterIfNotDefault(field.getDatabaseFieldAnnotation(),
						"columnName", "setColumnName(\"%s\")", writer);

				writeSetterIfNotDefault(field.getDatabaseFieldAnnotation(),
						"dataType",
						"setDataType(com.j256.ormlite.field.DataType.%s)",
						writer);

				writeSetterIfNotDefault(field.getDatabaseFieldAnnotation(),
						"defaultValue", "setDefaultValue(\"%s\")", writer);

				writeSetterIfNotDefault(field.getDatabaseFieldAnnotation(),
						"width", "setWidth(%d)", writer);

				writeSetterIfNotDefault(field.getDatabaseFieldAnnotation(),
						"canBeNull", "setCanBeNull(%b)", writer);

				writeSetterIfNotDefault(field.getDatabaseFieldAnnotation(),
						"id", "setId(%b)", writer);

				writeSetterIfNotDefault(field.getDatabaseFieldAnnotation(),
						"generatedId", "setGeneratedId(%b)", writer);

				writeSetterIfNotDefault(field.getDatabaseFieldAnnotation(),
						"generatedIdSequence",
						"setGeneratedIdSequence(\"%s\")", writer);

				writeSetterIfNotDefault(field.getDatabaseFieldAnnotation(),
						"foreign", "setForeign(%b)", writer);

				writeSetterIfNotDefault(field.getDatabaseFieldAnnotation(),
						"useGetSet", "setUseGetSet(%b)", writer);

				writeSetterIfNotDefault(
						field.getDatabaseFieldAnnotation(),
						"unknownEnumName",
						"setUnknownEnumValue("
								+ field.getFullyQualifiedTypeName() + ".%s)",
						writer);

				writeSetterIfNotDefault(field.getDatabaseFieldAnnotation(),
						"throwIfNull", "setThrowIfNull(%b)", writer);

				writeSetterIfNotDefault(field.getDatabaseFieldAnnotation(),
						"format", "setFormat(\"%s\")", writer);

				writeSetterIfNotDefault(field.getDatabaseFieldAnnotation(),
						"unique", "setUnique(%b)", writer);

				writeSetterIfNotDefault(field.getDatabaseFieldAnnotation(),
						"uniqueCombo", "setUniqueCombo(%b)", writer);

				writeSetterIfNotDefault(field.getDatabaseFieldAnnotation(),
						"index", "setIndex(%b)", writer);

				writeSetterIfNotDefault(field.getDatabaseFieldAnnotation(),
						"uniqueIndex", "setUniqueIndex(%b)", writer);

				writeSetterIfNotDefault(field.getDatabaseFieldAnnotation(),
						"indexName", "setIndexName(\"%s\")", writer);

				writeSetterIfNotDefault(field.getDatabaseFieldAnnotation(),
						"uniqueIndexName", "setUniqueIndexName(\"%s\")", writer);

				writeSetterIfNotDefault(field.getDatabaseFieldAnnotation(),
						"foreignAutoRefresh", "setForeignAutoRefresh(%b)",
						writer);

				writeSetterIfNotDefault(field.getDatabaseFieldAnnotation(),
						"maxForeignAutoRefreshLevel",
						"setMaxForeignAutoRefreshLevel(%d)", writer);

				writeSetterIfNotDefault(field.getDatabaseFieldAnnotation(),
						"persisterClass", "setPersisterClass(%s.class)", writer);

				writeSetterIfNotDefault(field.getDatabaseFieldAnnotation(),
						"allowGeneratedIdInsert",
						"setAllowGeneratedIdInsert(%b)", writer);

				writeSetterIfNotDefault(field.getDatabaseFieldAnnotation(),
						"columnDefinition", "setColumnDefinition(\"%s\")",
						writer);

				writeSetterIfNotDefault(field.getDatabaseFieldAnnotation(),
						"foreignAutoCreate", "setForeignAutoCreate(%b)", writer);

				writeSetterIfNotDefault(field.getDatabaseFieldAnnotation(),
						"version", "setVersion(%b)", writer);

				writeSetterIfNotDefault(field.getDatabaseFieldAnnotation(),
						"foreignColumnName", "setForeignColumnName(\"%s\")",
						writer);

				writeSetterIfNotDefault(field.getDatabaseFieldAnnotation(),
						"readOnly", "setReadOnly(%b)", writer);
			}

			if (field.getForeignCollectionFieldAnnotation() != null) {
				writeSetterIfNotDefault(
						field.getForeignCollectionFieldAnnotation(),
						"columnName", "setColumnName(\"%s\")", writer);

				writeSetter(true, "setForeignCollection(%b)", writer);

				writeSetterIfNotDefault(
						field.getForeignCollectionFieldAnnotation(), "eager",
						"setForeignCollectionEager(%b)", writer);

				if (!writeSetterIfNotDefault(
						field.getForeignCollectionFieldAnnotation(),
						"maxEagerLevel",
						"setForeignCollectionMaxEagerLevel(%d)", writer)) {
					writeSetterIfNotDefault(
							field.getForeignCollectionFieldAnnotation(),
							"maxEagerForeignCollectionLevel",
							"setForeignCollectionMaxEagerLevel(%d)", writer);
				}

				writeSetterIfNotDefault(
						field.getForeignCollectionFieldAnnotation(),
						"columnName", "setForeignCollectionColumnName(\"%s\")",
						writer);

				writeSetterIfNotDefault(
						field.getForeignCollectionFieldAnnotation(),
						"orderColumnName",
						"setForeignCollectionOrderColumnName(\"%s\")", writer);

				writeSetterIfNotDefault(
						field.getForeignCollectionFieldAnnotation(),
						"orderAscending",
						"setForeignCollectionOrderAscending(%b)", writer);

				if (!writeSetterIfNotDefault(
						field.getForeignCollectionFieldAnnotation(),
						"foreignFieldName",
						"setForeignCollectionForeignFieldName(\"%s\")", writer)) {
					writeSetterIfNotDefault(
							field.getForeignCollectionFieldAnnotation(),
							"foreignColumnName",
							"setForeignCollectionForeignFieldName(\"%s\")",
							writer);
				}
			}

			writer.write("\t\t\tdatabaseFieldConfigs.add(databaseFieldConfig);\n");
			writer.write("\t\t}\n");
		}

		String tableName;
		if (table.getAnnotation().tableName() != null
				&& table.getAnnotation().tableName().length() > 0) {
			tableName = table.getAnnotation().tableName();
		} else {
			tableName = table.getParsedClassName().getInputSimpleClassName()
					.toLowerCase();
		}

		writer.write(String
				.format("\t\tCONFIG = new DatabaseTableConfig<%s>(%s.class, \"%s\", databaseFieldConfigs);\n",
						table.getParsedClassName()
								.getInputFullyQualifiedClassName(), table
								.getParsedClassName()
								.getInputFullyQualifiedClassName(), tableName));
		writer.write("\t}\n");
		writer.write("}\n");
	}

	private static boolean writeSetterIfNotDefault(Annotation annotation,
			String annotationFieldName, String setterCall, Writer writer) {
		try {
			Method method = annotation.annotationType().getMethod(
					annotationFieldName);

			Object actualValue;
			Object defaultValue;
			if (method.getReturnType().getCanonicalName().equals(FQCN_Class)) {
				try {
					actualValue = getClassNameFromClassObject(method
							.invoke(annotation));
				} catch (Exception ex) {
					actualValue = getMirroredClassNameFromException(ex);
				}
				try {
					defaultValue = getClassNameFromClassObject(method
							.getDefaultValue());
				} catch (Exception ex) {
					defaultValue = getMirroredClassNameFromException(ex);
				}
			} else {
				actualValue = method.invoke(annotation);
				defaultValue = method.getDefaultValue();
			}

			return writeSetterIfNotEqual(actualValue, defaultValue, setterCall,
					writer);
		} catch (Exception e) {
			// All possible annotation properties are unit tested, so it is not
			// possible to get an exception here
			throw new RuntimeException(e);
		}
	}

	private static String getClassNameFromClassObject(Object object) {
		return ((Class<?>) object).getCanonicalName();
	}

	private static String getMirroredClassNameFromException(Exception ex)
			throws Exception {
		Throwable t = ex;
		do {
			if (t instanceof MirroredTypeException) {
				return ((MirroredTypeException) t).getTypeMirror().toString();
			}
			t = t.getCause();
		} while (t != null);

		throw ex;
	}

	private static boolean writeSetterIfNotEqual(Object actualValue,
			Object defaultValue, String setterCall, Writer writer)
			throws IOException {
		if (!actualValue.equals(defaultValue)) {
			writeSetter(actualValue, setterCall, writer);
			return true;
		} else {
			return false;
		}
	}

	private static void writeSetter(Object value, String setterCall,
			Writer writer) throws IOException {
		writer.write(String.format("\t\t\tdatabaseFieldConfig." + setterCall
				+ ";\n", value));
	}

	private void raiseNote(String message) {
		this.processingEnv.getMessager().printMessage(Kind.NOTE, message);
	}

	/*
	 * During incremental compiles in eclipse, if the same element raises
	 * multiple warnings, the internal message printing code can throw a NPE.
	 * When this happens, all warnings are displayed properly, so we should
	 * ignore the error. We will validate our arguments ourself to ensure that
	 * this code doesn't mask a real bug.
	 */

	private void raiseWarning(String message, Element element) {
		if (message == null || element == null) {
			throw new NullPointerException();
		}
		try {
			this.processingEnv.getMessager().printMessage(Kind.WARNING,
					message, element);
		} catch (NullPointerException e) {
			// ignore to workaround issues with eclipse incremental compilation
			raiseNote("NullPointerException while raising a warning: "
					+ e.toString());
		}
	}

	private void raiseError(String message, Element element) {
		if (message == null || element == null) {
			throw new NullPointerException();
		}
		try {
			this.processingEnv.getMessager().printMessage(Kind.ERROR, message,
					element);
		} catch (NullPointerException e) {
			// ignore to workaround issues with eclipse incremental compilation
			raiseNote("NullPointerException while raising an error: "
					+ e.toString());
		}
	}
}
