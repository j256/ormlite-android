package com.j256.ormlite.android.apptools;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.SourceVersion;
import javax.tools.JavaFileObject;

//TODO: handle ForeignCollectionField annotations
//TODO: handle javax.persistance annotations
//TODO: add note that this must be updated to Annotation classes
//TODO: analyze if this should be part of core (and if config file stuff can be removed)
//TODO: make sure no generated code is added to ormlite.android jar
//TODO: add error messages
//TODO: use cleaner resource loading code

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

	static final class AnnotationFileWriter {
		private AnnotationFileWriter() {
		}

		private static final List<String> templateLinesBefore = new ArrayList<String>();
		private static final List<String> templateLinesAfter = new ArrayList<String>();

		static {
			InputStreamReader reader;
			try {
				reader = new InputStreamReader(new FileInputStream(
						AnnotationFileWriter.class.getResource(
								"template/OrmLiteSqliteOpenHelper.java")
								.getFile()), StandardCharsets.UTF_8);
			} catch (FileNotFoundException e) {
				throw new RuntimeException(e);
			}
			BufferedReader br = new BufferedReader(reader);
			try {
				boolean pastInsertionPoint = false;
				String line;
				while ((line = br.readLine()) != null) {
					if (line.trim()
							.equals("//********** GENERATED CODE INSERTED HERE **********//")) {
						pastInsertionPoint = true;
					} else if (pastInsertionPoint) {
						templateLinesAfter.add(line);
					} else {
						templateLinesBefore.add(line);
					}
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			} finally {
				try {
					br.close();
				} catch (IOException e) {
				}
			}
		}

		static void writeHeader(Writer writer) throws IOException {
			for (String line : templateLinesBefore) {
				writer.write(line);
				writer.write("\n");
			}
			writer.write("\t\t\tList<DatabaseTableConfig<?>> databaseTableConfigs = new ArrayList<DatabaseTableConfig<?>>();\n");
		}

		static void writeTable(Writer writer, TableModel table)
				throws IOException {
			writer.write("\t\t\t{\n");
			writer.write("\t\t\t\tList<DatabaseFieldConfig> databaseFieldConfigs = new ArrayList<DatabaseFieldConfig>();\n");
			for (FieldModel field : table.fields) {

				if (!field.annotation.persisted()) {
					continue;
				}

				writer.write("\t\t\t\t{\n");
				writer.write(String
						.format("\t\t\t\t\tDatabaseFieldConfig databaseFieldConfig = new DatabaseFieldConfig(\"%s\")",
								field.fieldName));

				if (!field.annotation.columnName().equals(
						getDefaultAnnotationValue(field, "columnName"))) {
					writer.write(String
							.format("\t\t\t\t\tdatabaseFieldConfig.setColumnName(\"%s\");",
									field.annotation.columnName()));
				}

				if (!field.annotation.dataType().equals(
						getDefaultAnnotationValue(field, "dataType"))) {
					writer.write(String
							.format("\t\t\t\t\tdatabaseFieldConfig.setDataType(DataType.%s);",
									field.annotation.dataType().toString()));
				}

				if (!field.annotation.defaultValue().equals(
						getDefaultAnnotationValue(field, "defaultValue"))) {
					writer.write(String
							.format("\t\t\t\t\tdatabaseFieldConfig.setDefaultValue(\"%s\");",
									field.annotation.defaultValue()));
				}

				if (Integer.valueOf(field.annotation.width()).equals(
						getDefaultAnnotationValue(field, "width"))) {
					writer.write(String.format(
							"\t\t\t\t\tdatabaseFieldConfig.setWidth(%d);",
							field.annotation.width()));
				}

				if (Boolean.valueOf(field.annotation.canBeNull()).equals(
						getDefaultAnnotationValue(field, "canBeNull"))) {
					writer.write(String.format(
							"\t\t\t\t\tdatabaseFieldConfig.setCanBeNull(%b);",
							field.annotation.canBeNull()));
				}

				if (Boolean.valueOf(field.annotation.id()).equals(
						getDefaultAnnotationValue(field, "id"))) {
					writer.write(String.format(
							"\t\t\t\t\tdatabaseFieldConfig.setId(%b);",
							field.annotation.id()));
				}

				if (Boolean.valueOf(field.annotation.generatedId()).equals(
						getDefaultAnnotationValue(field, "generatedId"))) {
					writer.write(String
							.format("\t\t\t\t\tdatabaseFieldConfig.setGeneratedId(%b);",
									field.annotation.generatedId()));
				}

				if (!field.annotation.generatedIdSequence()
						.equals(getDefaultAnnotationValue(field,
								"generatedIdSequence"))) {
					writer.write(String
							.format("\t\t\t\t\tdatabaseFieldConfig.setGeneratedIdSequence(\"%s\");",
									field.annotation.generatedIdSequence()));
				}

				if (Boolean.valueOf(field.annotation.foreign()).equals(
						getDefaultAnnotationValue(field, "foreign"))) {
					writer.write(String.format(
							"\t\t\t\t\tdatabaseFieldConfig.setForeign(%b);",
							field.annotation.foreign()));
				}

				if (Boolean.valueOf(field.annotation.useGetSet()).equals(
						getDefaultAnnotationValue(field, "useGetSet"))) {
					writer.write(String.format(
							"\t\t\t\t\tdatabaseFieldConfig.setUseGetSet(%b);",
							field.annotation.useGetSet()));
				}

				if (!field.annotation.unknownEnumName().equals(
						getDefaultAnnotationValue(field, "unknownEnumName"))) {
					writer.write(String
							.format("\t\t\t\t\tdatabaseFieldConfig.setUnknownEnumName(%s.%s);",
									field.fullyQualifiedTypeName,
									field.annotation.unknownEnumName()));
				}

				if (Boolean.valueOf(field.annotation.throwIfNull()).equals(
						getDefaultAnnotationValue(field, "throwIfNull"))) {
					writer.write(String
							.format("\t\t\t\t\tdatabaseFieldConfig.setThrowIfNull(%b);",
									field.annotation.throwIfNull()));
				}

				if (!field.annotation.format().equals(
						getDefaultAnnotationValue(field, "format"))) {
					writer.write(String.format(
							"\t\t\t\t\tdatabaseFieldConfig.setFormat(\"%s\");",
							field.annotation.format()));
				}

				if (Boolean.valueOf(field.annotation.unique()).equals(
						getDefaultAnnotationValue(field, "unique"))) {
					writer.write(String.format(
							"\t\t\t\t\tdatabaseFieldConfig.setUnique(%b);",
							field.annotation.unique()));
				}

				if (Boolean.valueOf(field.annotation.uniqueCombo()).equals(
						getDefaultAnnotationValue(field, "uniqueCombo"))) {
					writer.write(String
							.format("\t\t\t\t\tdatabaseFieldConfig.setUniqueCombo(%b);",
									field.annotation.uniqueCombo()));
				}

				if (Boolean.valueOf(field.annotation.index()).equals(
						getDefaultAnnotationValue(field, "index"))) {
					writer.write(String.format(
							"\t\t\t\t\tdatabaseFieldConfig.setIndex(%b);",
							field.annotation.index()));
				}

				if (Boolean.valueOf(field.annotation.uniqueIndex()).equals(
						getDefaultAnnotationValue(field, "uniqueIndex"))) {
					writer.write(String
							.format("\t\t\t\t\tdatabaseFieldConfig.setUniqueIndex(%b);",
									field.annotation.uniqueIndex()));
				}

				if (!field.annotation.indexName().equals(
						getDefaultAnnotationValue(field, "indexName"))) {
					writer.write(String
							.format("\t\t\t\t\tdatabaseFieldConfig.setIndexName(\"%s\");",
									field.annotation.indexName()));
				}

				if (!field.annotation.uniqueIndexName().equals(
						getDefaultAnnotationValue(field, "uniqueIndexName"))) {
					writer.write(String
							.format("\t\t\t\t\tdatabaseFieldConfig.setUniqueIndexName(\"%s\");",
									field.annotation.uniqueIndexName()));
				}

				if (Boolean.valueOf(field.annotation.foreignAutoRefresh())
						.equals(getDefaultAnnotationValue(field,
								"foreignAutoRefresh"))) {
					writer.write(String
							.format("\t\t\t\t\tdatabaseFieldConfig.setForeignAutoRefresh(%b);",
									field.annotation.foreignAutoRefresh()));
				}

				if (Integer.valueOf(
						field.annotation.maxForeignAutoRefreshLevel()).equals(
						getDefaultAnnotationValue(field,
								"maxForeignAutoRefreshLevel"))) {
					writer.write(String
							.format("\t\t\t\t\tdatabaseFieldConfig.setMaxForeignAutoRefreshLevel(%d);",
									field.annotation
											.maxForeignAutoRefreshLevel()));
				}

				if (!field.annotation.persisterClass().equals(
						getDefaultAnnotationValue(field, "persisterClass"))) {
					writer.write(String
							.format("\t\t\t\t\t\ttdatabaseFieldConfig.setPersisterClass(%s.class);\n",
									field.annotation.persisterClass()
											.getCanonicalName()));
				}

				if (Boolean.valueOf(field.annotation.allowGeneratedIdInsert())
						.equals(getDefaultAnnotationValue(field,
								"allowGeneratedIdInsert"))) {
					writer.write(String
							.format("\t\t\t\t\tdatabaseFieldConfig.setAllowGeneratedIdInsert(%b);",
									field.annotation.allowGeneratedIdInsert()));
				}

				if (!field.annotation.columnDefinition().equals(
						getDefaultAnnotationValue(field, "columnDefinition"))) {
					writer.write(String
							.format("\t\t\t\t\tdatabaseFieldConfig.setColumnDefinition(\"%s\");",
									field.annotation.columnDefinition()));
				}

				if (Boolean.valueOf(field.annotation.foreignAutoCreate())
						.equals(getDefaultAnnotationValue(field,
								"foreignAutoCreate"))) {
					writer.write(String
							.format("\t\t\t\t\tdatabaseFieldConfig.setForeignAutoCreate(%b);",
									field.annotation.foreignAutoCreate()));
				}

				if (Boolean.valueOf(field.annotation.version()).equals(
						getDefaultAnnotationValue(field, "version"))) {
					writer.write(String.format(
							"\t\t\t\t\tdatabaseFieldConfig.setVersion(%b);",
							field.annotation.version()));
				}

				if (!field.annotation.foreignColumnName().equals(
						getDefaultAnnotationValue(field, "foreignColumnName"))) {
					writer.write(String
							.format("\t\t\t\t\tdatabaseFieldConfig.setForeignColumnName(\"%s\");",
									field.annotation.foreignColumnName()));
				}

				if (Boolean.valueOf(field.annotation.readOnly()).equals(
						getDefaultAnnotationValue(field, "readOnly"))) {
					writer.write(String.format(
							"\t\t\t\t\tdatabaseFieldConfig.setReadOnly(%b);",
							field.annotation.readOnly()));
				}

				writer.write("\t\t\t\t\tdatabaseFieldConfigs.add(databaseFieldConfig);\n");
				writer.write("\t\t\t\t}\n");
			}

			String tableName;
			if (table.annotation.tableName() != null
					&& table.annotation.tableName().length() > 0) {
				tableName = table.annotation.tableName();
			} else {
				tableName = table.simpleClassName.toLowerCase();
			}

			writer.write(String
					.format("\t\t\t\tdatabaseTableConfigs.add(new DatabaseTableConfig<?>(%s.class, \"%s\", databaseFieldConfigs));\n",
							table.fullyQualifiedClassName, tableName));
			writer.write("\t\t\t}\n");
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

		static void writeFooter(Writer writer) throws IOException {
			writer.write("\t\t\tDaoManager.addCachedDatabaseConfigs(databaseTableConfigs);\n");
			for (String line : templateLinesAfter) {
				writer.write(line);
				writer.write("\n");
			}
		}
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
		List<TableModel> tables = new ArrayList<TableModel>();

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
							TypeElement fieldClassElement = (TypeElement) processingEnv
									.getTypeUtils().asElement(child.asType());

							FieldModel field = new FieldModel();
							field.fullyQualifiedTypeName = fieldClassElement
									.getQualifiedName().toString();
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
		}

		createSourceFile(tables);
		return true;
	}

	private void createSourceFile(List<TableModel> tables) {
		try {
			JavaFileObject javaFileObject = processingEnv
					.getFiler()
					.createSourceFile(
							"com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelperAutomatic"); //TODO
			try {
				Writer writer = javaFileObject.openWriter();
				try {
					AnnotationFileWriter.writeHeader(writer);
					for (TableModel table : tables) {
						AnnotationFileWriter.writeTable(writer, table);
					}
					AnnotationFileWriter.writeFooter(writer);
				} finally {
					writer.close();
				}
			} catch (IOException ex) {
			}
		} catch (IOException e) {
		}
	}
}
