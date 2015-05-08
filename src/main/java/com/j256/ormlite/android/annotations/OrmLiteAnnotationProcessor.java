package com.j256.ormlite.android.annotations;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.table.DatabaseTable;

import java.io.IOException;
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
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.SourceVersion;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.MirroredTypesException;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic.Kind;
import javax.tools.JavaFileObject;

//TODO: handle javax.persistance annotations
//TODO: analyze if this should be part of core (and if config file stuff can be removed)

/**
 * Class that is automatically run when compiling client code that automatically
 * generates code to call DaoManager.addCachedDatabaseConfigs() without needing
 * a config file.
 * 
 * @author nathancrouther
 */
// TODO: understand this
@SuppressWarnings("restriction")
public final class OrmLiteAnnotationProcessor extends AbstractProcessor {
	private static final String FQCN_Object = "java.lang.Object";
	private static final String FQCN_Class = "java.lang.Class";

	// TODO: understand why reading these from Class throws exception
	private static final String FQCN_OpenHelper = "com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper";
	private static final String CN_OpenHelper = "OrmLiteSqliteOpenHelper";

	private Set<TypeElement> declaredTables = new HashSet<TypeElement>();
	private Map<TypeElement, List<TypeElement>> declaredTableToDatabaseMap = new HashMap<TypeElement, List<TypeElement>>();
	private Set<TypeElement> foundTables = new HashSet<TypeElement>();

	static final class TableModel {
		String packageName;
		List<String> nestedClasses = new ArrayList<String>();
		DatabaseTable annotation;
		List<FieldModel> fields = new ArrayList<FieldModel>();

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

	static final class FieldModel {
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

			TableModel table = new TableModel();
			extractPackageAndNestedClasses(element, table);
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

			createSourceFile(table, element);
		}

		Set<? extends Element> tablesCollectionElements = env
				.getElementsAnnotatedWith(Database.class);
		for (Element element : tablesCollectionElements) {
			Database annotation = element.getAnnotation(Database.class);
			if (annotation == null) {
				continue;
			}

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
				// TODO: understand why this is ever null
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

			for (TypeElement tableType : tableTypes) {
				List<TypeElement> databases = declaredTableToDatabaseMap
						.get(tableType);
				if (databases == null) {
					databases = new ArrayList<TypeElement>();
					declaredTableToDatabaseMap.put(tableType, databases);
				}
				databases.add((TypeElement) element);

				declaredTables.add(tableType);
			}

			// TODO: generate class for database that creates tables and loads
			// the cached field info
		}

		if (env.processingOver()) {
			for (TypeElement declared : declaredTables) {
				if (foundTables.contains(declared)) {
					foundTables.remove(declared);
				} else {
					for (TypeElement database : declaredTableToDatabaseMap
							.get(declared)) {
						raiseError(
								String.format(
										"%s annotation must contain only classes annotated with %s",
										Database.class.getSimpleName(),
										DatabaseTable.class.getSimpleName()),
								database);
					}
				}
			}

			for (TypeElement undeclared : foundTables) {
				raiseWarning(
						String.format(
								"Class annotated with %s is not included in any %s annotation",
								DatabaseTable.class.getSimpleName(),
								Database.class.getSimpleName()), undeclared);
			}
		}

		return true;
	}

	private void extractPackageAndNestedClasses(Element element,
			TableModel table) {
		Element enclosingElement = element;
		do {
			table.nestedClasses
					.add(enclosingElement.getSimpleName().toString());
			enclosingElement = enclosingElement.getEnclosingElement();
		} while (enclosingElement.getKind().isClass());
		Collections.reverse(table.nestedClasses);
		table.packageName = ((PackageElement) enclosingElement)
				.getQualifiedName().toString();
	}

	private void createSourceFile(TableModel table, Element tableClassElement) {
		try {
			JavaFileObject javaFileObject = processingEnv.getFiler()
					.createSourceFile(
							table.getGeneratedFullyQualifiedClassName(),
							tableClassElement);

			Writer writer = javaFileObject.openWriter();
			try {
				writeTable(writer, table);
			} finally {
				writer.close();
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void writeTable(Writer writer, TableModel table) throws IOException {
		if (!table.packageName.isEmpty()) {
			writer.write("package " + table.packageName + ";\n");
			writer.write("\n");
		}
		writer.write("import java.util.ArrayList;\n");
		writer.write("import java.util.List;\n");
		writer.write("\n");
		writer.write("import com.j256.ormlite.field.DatabaseFieldConfig;\n");
		writer.write("import com.j256.ormlite.table.DatabaseTableConfig;\n");
		writer.write("\n");
		writer.write("public final class " + table.getGeneratedClassName()
				+ " {\n");
		writer.write("\tprivate " + table.getGeneratedClassName() + "() {\n");
		writer.write("\t}\n");
		writer.write("\n");
		writer.write("\tpublic static final DatabaseTableConfig<"
				+ table.getInputFullyQualifiedClassName() + "> CONFIG;\n");
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
			tableName = table.getInputSimpleClassName().toLowerCase();
		}

		writer.write(String
				.format("\t\tCONFIG = new DatabaseTableConfig<%s>(%s.class, \"%s\", databaseFieldConfigs);\n",
						table.getInputFullyQualifiedClassName(),
						table.getInputFullyQualifiedClassName(), tableName));
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

	private void raiseWarning(String message, Element element) {
		this.processingEnv.getMessager().printMessage(Kind.WARNING, message,
				element);
	}

	private void raiseError(String message, Element element) {
		this.processingEnv.getMessager().printMessage(Kind.ERROR, message,
				element);
	}
}
