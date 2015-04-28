package com.j256.ormlite.android.apptools;

import com.j256.ormlite.field.DataPersister;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.SourceVersion;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
import javax.tools.JavaFileObject;

//TODO: handle ForeignCollectionField annotations
//TODO: handle javax.persistance annotations
//TODO: add note that this must be updated to Annotation classes
//TODO: analyze if this should be part of core (and if config file stuff can be removed)
//TODO: make sure no generated code is added to ormlite.android jar (and optimize pom)
//TODO: add error messages

/**
 * Class that is automatically run when compiling client code that automatically
 * generates an OrmLiteSqliteOpenHelper class that has inline code to generate
 * the arguments for DaoManager.addCachedDatabaseConfigs() without needing a
 * config file.
 * 
 * @author nathancrouther
 */
// TODO: understand this
@SuppressWarnings("restriction")
public final class OrmLiteAnnotationProcessor extends AbstractProcessor {
	private static final String PACKAGE = "com.j256.ormlite.android.apptools.tableconfigs";
	private static final String SUFFIX = "Config";

	static final class TableModel {
		String fullyQualifiedClassName;
		String simpleClassName;
		DatabaseTable annotation;
		List<FieldModel> fields = new ArrayList<FieldModel>();
	}

	static final class FieldModel {
		String fullyQualifiedTypeName;
		String fieldName;
		DatabaseField annotation;
	}

	@Override
	public Set<String> getSupportedAnnotationTypes() {
		Set<String> types = new LinkedHashSet<String>();
		types.add(DatabaseTable.class.getCanonicalName());
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
			TypeElement tableClassElement = (TypeElement) element;

			TableModel table = new TableModel();
			table.fullyQualifiedClassName = tableClassElement
					.getQualifiedName().toString();
			table.simpleClassName = tableClassElement.getSimpleName()
					.toString();
			table.annotation = tableClassElement
					.getAnnotation(DatabaseTable.class);

			// get all fields from this and all parents until we hit Object
			do {
				for (Element child : tableClassElement.getEnclosedElements()) {
					if (child.getKind().isField()) {
						DatabaseField databaseField = child
								.getAnnotation(DatabaseField.class);
						if (databaseField != null) {
							FieldModel field = new FieldModel();
							field.fullyQualifiedTypeName = child.asType()
									.toString();
							field.fieldName = child.getSimpleName().toString();
							field.annotation = databaseField;
							table.fields.add(field);
						}
					}
				}

				tableClassElement = (TypeElement) processingEnv.getTypeUtils()
						.asElement(tableClassElement.getSuperclass());
			} while (!tableClassElement.getQualifiedName().toString()
					.equals("java.lang.Object"));

			createSourceFile(table);
		}

		return true;
	}

	private void createSourceFile(TableModel table) {
		try {
			JavaFileObject javaFileObject = processingEnv.getFiler()
					.createSourceFile(
							PACKAGE + "." + table.simpleClassName + SUFFIX);

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
		writer.write("package " + PACKAGE + ";\n");
		writer.write("\n");
		writer.write("import java.util.ArrayList;\n");
		writer.write("import java.util.List;\n");
		writer.write("\n");
		writer.write("import com.j256.ormlite.field.DatabaseFieldConfig;\n");
		writer.write("import com.j256.ormlite.table.DatabaseTableConfig;\n");
		writer.write("\n");
		writer.write("public final class " + table.simpleClassName + SUFFIX + " {\n");
		writer.write("\tprivate " + table.simpleClassName + SUFFIX + "() {\n");
		writer.write("\t}\n");
		writer.write("\n");
		writer.write("\tpublic static final DatabaseTableConfig<"
				+ table.fullyQualifiedClassName + "> CONFIG;\n");
		writer.write("\n");
		writer.write("\tstatic {\n");
		writer.write("\t\tList<DatabaseFieldConfig> databaseFieldConfigs = new ArrayList<DatabaseFieldConfig>();\n");
		for (FieldModel field : table.fields) {

			if (!field.annotation.persisted()) {
				continue;
			}

			writer.write("\t\t{\n");
			writer.write(String
					.format("\t\t\tDatabaseFieldConfig databaseFieldConfig = new DatabaseFieldConfig(\"%s\");\n",
							field.fieldName));

			if (!field.annotation.columnName().equals(
					getDefaultAnnotationValue(field, "columnName"))) {
				writer.write(String.format(
						"\t\t\tdatabaseFieldConfig.setColumnName(\"%s\");\n",
						field.annotation.columnName()));
			}

			if (!field.annotation.dataType().equals(
					getDefaultAnnotationValue(field, "dataType"))) {
				writer.write(String
						.format("\t\t\tdatabaseFieldConfig.setDataType(com.j256.ormlite.field.DataType.%s);\n",
								field.annotation.dataType().toString()));
			}

			if (!field.annotation.defaultValue().equals(
					getDefaultAnnotationValue(field, "defaultValue"))) {
				writer.write(String.format(
						"\t\t\tdatabaseFieldConfig.setDefaultValue(\"%s\");\n",
						field.annotation.defaultValue()));
			}

			if (!Integer.valueOf(field.annotation.width()).equals(
					getDefaultAnnotationValue(field, "width"))) {
				writer.write(String.format(
						"\t\t\tdatabaseFieldConfig.setWidth(%d);\n",
						field.annotation.width()));
			}

			if (!Boolean.valueOf(field.annotation.canBeNull()).equals(
					getDefaultAnnotationValue(field, "canBeNull"))) {
				writer.write(String.format(
						"\t\t\tdatabaseFieldConfig.setCanBeNull(%b);\n",
						field.annotation.canBeNull()));
			}

			if (!Boolean.valueOf(field.annotation.id()).equals(
					getDefaultAnnotationValue(field, "id"))) {
				writer.write(String.format(
						"\t\t\tdatabaseFieldConfig.setId(%b);\n",
						field.annotation.id()));
			}

			if (!Boolean.valueOf(field.annotation.generatedId()).equals(
					getDefaultAnnotationValue(field, "generatedId"))) {
				writer.write(String.format(
						"\t\t\tdatabaseFieldConfig.setGeneratedId(%b);\n",
						field.annotation.generatedId()));
			}

			if (!field.annotation.generatedIdSequence().equals(
					getDefaultAnnotationValue(field, "generatedIdSequence"))) {
				writer.write(String
						.format("\t\t\tdatabaseFieldConfig.setGeneratedIdSequence(\"%s\");\n",
								field.annotation.generatedIdSequence()));
			}

			if (!Boolean.valueOf(field.annotation.foreign()).equals(
					getDefaultAnnotationValue(field, "foreign"))) {
				writer.write(String.format(
						"\t\t\tdatabaseFieldConfig.setForeign(%b);\n",
						field.annotation.foreign()));
			}

			if (!Boolean.valueOf(field.annotation.useGetSet()).equals(
					getDefaultAnnotationValue(field, "useGetSet"))) {
				writer.write(String.format(
						"\t\t\tdatabaseFieldConfig.setUseGetSet(%b);\n",
						field.annotation.useGetSet()));
			}

			if (!field.annotation.unknownEnumName().equals(
					getDefaultAnnotationValue(field, "unknownEnumName"))) {
				writer.write(String
						.format("\t\t\tdatabaseFieldConfig.setUnknownEnumName(%s.%s);\n",
								field.fullyQualifiedTypeName,
								field.annotation.unknownEnumName()));
			}

			if (!Boolean.valueOf(field.annotation.throwIfNull()).equals(
					getDefaultAnnotationValue(field, "throwIfNull"))) {
				writer.write(String.format(
						"\t\t\tdatabaseFieldConfig.setThrowIfNull(%b);\n",
						field.annotation.throwIfNull()));
			}

			if (!field.annotation.format().equals(
					getDefaultAnnotationValue(field, "format"))) {
				writer.write(String.format(
						"\t\t\tdatabaseFieldConfig.setFormat(\"%s\");\n",
						field.annotation.format()));
			}

			if (!Boolean.valueOf(field.annotation.unique()).equals(
					getDefaultAnnotationValue(field, "unique"))) {
				writer.write(String.format(
						"\t\t\tdatabaseFieldConfig.setUnique(%b);\n",
						field.annotation.unique()));
			}

			if (!Boolean.valueOf(field.annotation.uniqueCombo()).equals(
					getDefaultAnnotationValue(field, "uniqueCombo"))) {
				writer.write(String.format(
						"\t\t\tdatabaseFieldConfig.setUniqueCombo(%b);\n",
						field.annotation.uniqueCombo()));
			}

			if (!Boolean.valueOf(field.annotation.index()).equals(
					getDefaultAnnotationValue(field, "index"))) {
				writer.write(String.format(
						"\t\t\tdatabaseFieldConfig.setIndex(%b);\n",
						field.annotation.index()));
			}

			if (!Boolean.valueOf(field.annotation.uniqueIndex()).equals(
					getDefaultAnnotationValue(field, "uniqueIndex"))) {
				writer.write(String.format(
						"\t\t\tdatabaseFieldConfig.setUniqueIndex(%b);\n",
						field.annotation.uniqueIndex()));
			}

			if (!field.annotation.indexName().equals(
					getDefaultAnnotationValue(field, "indexName"))) {
				writer.write(String.format(
						"\t\t\tdatabaseFieldConfig.setIndexName(\"%s\");\n",
						field.annotation.indexName()));
			}

			if (!field.annotation.uniqueIndexName().equals(
					getDefaultAnnotationValue(field, "uniqueIndexName"))) {
				writer.write(String
						.format("\t\t\tdatabaseFieldConfig.setUniqueIndexName(\"%s\");\n",
								field.annotation.uniqueIndexName()));
			}

			if (!Boolean.valueOf(field.annotation.foreignAutoRefresh()).equals(
					getDefaultAnnotationValue(field, "foreignAutoRefresh"))) {
				writer.write(String
						.format("\t\t\tdatabaseFieldConfig.setForeignAutoRefresh(%b);\n",
								field.annotation.foreignAutoRefresh()));
			}

			if (!Integer.valueOf(field.annotation.maxForeignAutoRefreshLevel())
					.equals(getDefaultAnnotationValue(field,
							"maxForeignAutoRefreshLevel"))) {
				writer.write(String
						.format("\t\t\tdatabaseFieldConfig.setMaxForeignAutoRefreshLevel(%d);\n",
								field.annotation.maxForeignAutoRefreshLevel()));
			}

			TypeMirror persisterClassActual;
			try {
				field.annotation.persisterClass(); // this will throw
				throw new RuntimeException("Failed to get TypeMirror");
			} catch (MirroredTypeException mte) {
				persisterClassActual = mte.getTypeMirror();
			}
			@SuppressWarnings("unchecked")
			Class<? extends DataPersister> persisterClassDefault = (Class<? extends DataPersister>) getDefaultAnnotationValue(
					field, "persisterClass");
			if (!persisterClassActual.toString().equals(
					persisterClassDefault.getCanonicalName())) {
				writer.write(String
						.format("\t\t\t\tdatabaseFieldConfig.setPersisterClass(%s.class);\n",
								persisterClassActual.toString()));
			}

			if (!Boolean.valueOf(field.annotation.allowGeneratedIdInsert())
					.equals(getDefaultAnnotationValue(field,
							"allowGeneratedIdInsert"))) {
				writer.write(String
						.format("\t\t\tdatabaseFieldConfig.setAllowGeneratedIdInsert(%b);\n",
								field.annotation.allowGeneratedIdInsert()));
			}

			if (!field.annotation.columnDefinition().equals(
					getDefaultAnnotationValue(field, "columnDefinition"))) {
				writer.write(String
						.format("\t\t\tdatabaseFieldConfig.setColumnDefinition(\"%s\");\n",
								field.annotation.columnDefinition()));
			}

			if (!Boolean.valueOf(field.annotation.foreignAutoCreate()).equals(
					getDefaultAnnotationValue(field, "foreignAutoCreate"))) {
				writer.write(String
						.format("\t\t\tdatabaseFieldConfig.setForeignAutoCreate(%b);\n",
								field.annotation.foreignAutoCreate()));
			}

			if (!Boolean.valueOf(field.annotation.version()).equals(
					getDefaultAnnotationValue(field, "version"))) {
				writer.write(String.format(
						"\t\t\tdatabaseFieldConfig.setVersion(%b);\n",
						field.annotation.version()));
			}

			if (!field.annotation.foreignColumnName().equals(
					getDefaultAnnotationValue(field, "foreignColumnName"))) {
				writer.write(String
						.format("\t\t\tdatabaseFieldConfig.setForeignColumnName(\"%s\");\n",
								field.annotation.foreignColumnName()));
			}

			if (!Boolean.valueOf(field.annotation.readOnly()).equals(
					getDefaultAnnotationValue(field, "readOnly"))) {
				writer.write(String.format(
						"\t\t\tdatabaseFieldConfig.setReadOnly(%b);\n",
						field.annotation.readOnly()));
			}

			writer.write("\t\t\tdatabaseFieldConfigs.add(databaseFieldConfig);\n");
			writer.write("\t\t}\n");
		}

		String tableName;
		if (table.annotation.tableName() != null
				&& table.annotation.tableName().length() > 0) {
			tableName = table.annotation.tableName();
		} else {
			tableName = table.simpleClassName.toLowerCase();
		}

		writer.write(String
				.format("\t\tCONFIG = new DatabaseTableConfig<%s>(%s.class, \"%s\", databaseFieldConfigs);\n",
						table.fullyQualifiedClassName,
						table.fullyQualifiedClassName, tableName));
		writer.write("\t}\n");
		writer.write("}\n");
	}

	private static Object getDefaultAnnotationValue(FieldModel field,
			String name) {
		try {
			return field.annotation.annotationType().getMethod(name)
					.getDefaultValue();
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		} catch (SecurityException e) {
			throw new RuntimeException(e);
		}
	}
}
