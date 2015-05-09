package com.j256.ormlite.android.annotations;

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
import java.util.Collections;
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
import javax.lang.model.element.PackageElement;
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

	private static final class ParsedClassName {
		private String packageName;
		private List<String> nestedClasses = new ArrayList<String>();

		ParsedClassName(Element element) {
			Element elementIterator = element;
			do {
				nestedClasses.add(elementIterator.getSimpleName().toString());
				elementIterator = elementIterator.getEnclosingElement();
			} while (elementIterator.getKind().isClass());
			Collections.reverse(nestedClasses);
			packageName = ((PackageElement) elementIterator).getQualifiedName()
					.toString();
		}

		String getInputFullyQualifiedClassName() {
			StringBuilder sb = new StringBuilder();
			if (!packageName.isEmpty()) {
				sb.append(packageName);
				sb.append('.');
			}
			for (int i = 0; i < nestedClasses.size(); ++i) {
				if (i != 0) {
					sb.append('.');
				}
				sb.append(nestedClasses.get(i));
			}
			return sb.toString();
		}

		String getInputSimpleClassName() {
			return nestedClasses.get(nestedClasses.size() - 1);
		}

		String getGeneratedClassName() {
			final String SUFFIX = "_TableConfig";

			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < nestedClasses.size(); ++i) {
				if (i != 0) {
					sb.append('_');
				}
				sb.append(nestedClasses.get(i));
			}
			sb.append(SUFFIX);
			return sb.toString();
		}

		String getGeneratedFullyQualifiedClassName() {
			StringBuilder sb = new StringBuilder();
			if (!packageName.isEmpty()) {
				sb.append(packageName);
				sb.append('.');
			}
			sb.append(getGeneratedClassName());
			return sb.toString();
		}
	}

	private static final class TableModel {
		ParsedClassName parsedClassName;
		DatabaseTable annotation;
		List<FieldModel> fields = new ArrayList<FieldModel>();
	}

	private static final class FieldModel {
		String fullyQualifiedTypeName;
		String fieldName;
		DatabaseField databaseFieldAnnotation;
		ForeignCollectionField foreignCollectionFieldAnnotation;
	}

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

			TableModel table = new TableModel();
			table.parsedClassName = new ParsedClassName(element);
			table.annotation = element.getAnnotation(DatabaseTable.class);

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
							FieldModel field = new FieldModel();
							field.fullyQualifiedTypeName = child.asType()
									.toString();
							field.fieldName = child.getSimpleName().toString();
							field.databaseFieldAnnotation = databaseField;
							field.foreignCollectionFieldAnnotation = foreignCollectionField;
							table.fields.add(field);
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

			if (table.fields.isEmpty()) {
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
				} catch (IOException e) {
					// Inability to read is not an error, just initialize with
					// empty contents
					savedDatabaseInfo = new HashMap<String, List<String>>();
				} finally {
					reader.close();
				}
			} catch (FilerException e) {
				// Inability to read is not an error, just initialize with empty
				// contents
				savedDatabaseInfo = new HashMap<String, List<String>>();
			} catch (IOException e) {
				// Inability to read is not an error, just initialize with empty
				// contents
				savedDatabaseInfo = new HashMap<String, List<String>>();
			} catch (ClassNotFoundException e) {
				// Built-in Java classes will always be available
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
				// intentionally ignore the error
			} catch (IOException e) {
				// intentionally ignore the error
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
				writer.write("package " + openHelperClassName.packageName
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
			// We should always be able to generate the source files
			throw new RuntimeException(e);
		}
	}

	private void createTableConfigSourceFile(TableModel table,
			Element tableClassElement) {
		try {
			JavaFileObject javaFileObject = processingEnv
					.getFiler()
					.createSourceFile(
							table.parsedClassName
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
							table.parsedClassName
									.getGeneratedFullyQualifiedClassName()));
		} catch (IOException e) {
			// We should always be able to generate the source files
			throw new RuntimeException(e);
		}
	}

	private static void writeTable(Writer writer, TableModel table)
			throws IOException {
		if (!table.parsedClassName.packageName.isEmpty()) {
			writer.write("package " + table.parsedClassName.packageName + ";\n");
			writer.write("\n");
		}
		writer.write("import java.util.ArrayList;\n");
		writer.write("import java.util.List;\n");
		writer.write("\n");
		writer.write("import com.j256.ormlite.field.DatabaseFieldConfig;\n");
		writer.write("import com.j256.ormlite.table.DatabaseTableConfig;\n");
		writer.write("\n");
		writer.write("public final class "
				+ table.parsedClassName.getGeneratedClassName() + " {\n");
		writer.write("\tprivate "
				+ table.parsedClassName.getGeneratedClassName() + "() {\n");
		writer.write("\t}\n");
		writer.write("\n");
		writer.write("\tpublic static final DatabaseTableConfig<"
				+ table.parsedClassName.getInputFullyQualifiedClassName()
				+ "> CONFIG;\n");
		writer.write("\n");
		writer.write("\tstatic {\n");
		writer.write("\t\tList<DatabaseFieldConfig> databaseFieldConfigs = new ArrayList<DatabaseFieldConfig>();\n");
		for (FieldModel field : table.fields) {

			if (field.databaseFieldAnnotation != null
					&& !field.databaseFieldAnnotation.persisted()) {
				continue;
			}

			writer.write("\t\t{\n");
			writer.write(String
					.format("\t\t\tDatabaseFieldConfig databaseFieldConfig = new DatabaseFieldConfig(\"%s\");\n",
							field.fieldName));

			if (field.databaseFieldAnnotation != null) {
				writeSetterIfNotDefault(field.databaseFieldAnnotation,
						"columnName", "setColumnName(\"%s\")", writer);

				writeSetterIfNotDefault(field.databaseFieldAnnotation,
						"dataType",
						"setDataType(com.j256.ormlite.field.DataType.%s)",
						writer);

				writeSetterIfNotDefault(field.databaseFieldAnnotation,
						"defaultValue", "setDefaultValue(\"%s\")", writer);

				writeSetterIfNotDefault(field.databaseFieldAnnotation, "width",
						"setWidth(%d)", writer);

				writeSetterIfNotDefault(field.databaseFieldAnnotation,
						"canBeNull", "setCanBeNull(%b)", writer);

				writeSetterIfNotDefault(field.databaseFieldAnnotation, "id",
						"setId(%b)", writer);

				writeSetterIfNotDefault(field.databaseFieldAnnotation,
						"generatedId", "setGeneratedId(%b)", writer);

				writeSetterIfNotDefault(field.databaseFieldAnnotation,
						"generatedIdSequence",
						"setGeneratedIdSequence(\"%s\")", writer);

				writeSetterIfNotDefault(field.databaseFieldAnnotation,
						"foreign", "setForeign(%b)", writer);

				writeSetterIfNotDefault(field.databaseFieldAnnotation,
						"useGetSet", "setUseGetSet(%b)", writer);

				writeSetterIfNotDefault(field.databaseFieldAnnotation,
						"unknownEnumName", "setUnknownEnumValue("
								+ field.fullyQualifiedTypeName + ".%s)", writer);

				writeSetterIfNotDefault(field.databaseFieldAnnotation,
						"throwIfNull", "setThrowIfNull(%b)", writer);

				writeSetterIfNotDefault(field.databaseFieldAnnotation,
						"format", "setFormat(\"%s\")", writer);

				writeSetterIfNotDefault(field.databaseFieldAnnotation,
						"unique", "setUnique(%b)", writer);

				writeSetterIfNotDefault(field.databaseFieldAnnotation,
						"uniqueCombo", "setUniqueCombo(%b)", writer);

				writeSetterIfNotDefault(field.databaseFieldAnnotation, "index",
						"setIndex(%b)", writer);

				writeSetterIfNotDefault(field.databaseFieldAnnotation,
						"uniqueIndex", "setUniqueIndex(%b)", writer);

				writeSetterIfNotDefault(field.databaseFieldAnnotation,
						"indexName", "setIndexName(\"%s\")", writer);

				writeSetterIfNotDefault(field.databaseFieldAnnotation,
						"uniqueIndexName", "setUniqueIndexName(\"%s\")", writer);

				writeSetterIfNotDefault(field.databaseFieldAnnotation,
						"foreignAutoRefresh", "setForeignAutoRefresh(%b)",
						writer);

				writeSetterIfNotDefault(field.databaseFieldAnnotation,
						"maxForeignAutoRefreshLevel",
						"setMaxForeignAutoRefreshLevel(%d)", writer);

				writeSetterIfNotDefault(field.databaseFieldAnnotation,
						"persisterClass", "setPersisterClass(%s.class)", writer);

				writeSetterIfNotDefault(field.databaseFieldAnnotation,
						"allowGeneratedIdInsert",
						"setAllowGeneratedIdInsert(%b)", writer);

				writeSetterIfNotDefault(field.databaseFieldAnnotation,
						"columnDefinition", "setColumnDefinition(\"%s\")",
						writer);

				writeSetterIfNotDefault(field.databaseFieldAnnotation,
						"foreignAutoCreate", "setForeignAutoCreate(%b)", writer);

				writeSetterIfNotDefault(field.databaseFieldAnnotation,
						"version", "setVersion(%b)", writer);

				writeSetterIfNotDefault(field.databaseFieldAnnotation,
						"foreignColumnName", "setForeignColumnName(\"%s\")",
						writer);

				writeSetterIfNotDefault(field.databaseFieldAnnotation,
						"readOnly", "setReadOnly(%b)", writer);
			}

			if (field.foreignCollectionFieldAnnotation != null) {
				writeSetterIfNotDefault(field.foreignCollectionFieldAnnotation,
						"columnName", "setColumnName(\"%s\")", writer);

				writeSetter(true, "setForeignCollection(%b)", writer);

				writeSetterIfNotDefault(field.foreignCollectionFieldAnnotation,
						"eager", "setForeignCollectionEager(%b)", writer);

				if (!writeSetterIfNotDefault(
						field.foreignCollectionFieldAnnotation,
						"maxEagerLevel",
						"setForeignCollectionMaxEagerLevel(%d)", writer)) {
					writeSetterIfNotDefault(
							field.foreignCollectionFieldAnnotation,
							"maxEagerForeignCollectionLevel",
							"setForeignCollectionMaxEagerLevel(%d)", writer);
				}

				writeSetterIfNotDefault(field.foreignCollectionFieldAnnotation,
						"columnName", "setForeignCollectionColumnName(\"%s\")",
						writer);

				writeSetterIfNotDefault(field.foreignCollectionFieldAnnotation,
						"orderColumnName",
						"setForeignCollectionOrderColumnName(\"%s\")", writer);

				writeSetterIfNotDefault(field.foreignCollectionFieldAnnotation,
						"orderAscending",
						"setForeignCollectionOrderAscending(%b)", writer);

				if (!writeSetterIfNotDefault(
						field.foreignCollectionFieldAnnotation,
						"foreignFieldName",
						"setForeignCollectionForeignFieldName(\"%s\")", writer)) {
					writeSetterIfNotDefault(
							field.foreignCollectionFieldAnnotation,
							"foreignColumnName",
							"setForeignCollectionForeignFieldName(\"%s\")",
							writer);
				}
			}

			writer.write("\t\t\tdatabaseFieldConfigs.add(databaseFieldConfig);\n");
			writer.write("\t\t}\n");
		}

		String tableName;
		if (table.annotation.tableName() != null
				&& table.annotation.tableName().length() > 0) {
			tableName = table.annotation.tableName();
		} else {
			tableName = table.parsedClassName.getInputSimpleClassName()
					.toLowerCase();
		}

		writer.write(String
				.format("\t\tCONFIG = new DatabaseTableConfig<%s>(%s.class, \"%s\", databaseFieldConfigs);\n",
						table.parsedClassName.getInputFullyQualifiedClassName(),
						table.parsedClassName.getInputFullyQualifiedClassName(),
						tableName));
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
		}
	}
}
