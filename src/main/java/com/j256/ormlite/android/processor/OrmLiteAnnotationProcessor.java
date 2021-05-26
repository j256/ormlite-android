package com.j256.ormlite.android.processor;

import com.j256.ormlite.android.annotations.Database;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.DatabaseFieldConfig;
import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.DatabaseTable;
import com.j256.ormlite.table.DatabaseTableConfig;
import com.j256.ormlite.table.TableUtils;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.WildcardTypeName;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.sql.SQLException;
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
import javax.lang.model.element.Modifier;
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
			DatabaseTable annotation = element
					.getAnnotation(DatabaseTable.class);
			if (annotation == null) {
				continue;
			}

			raiseNote(String.format("Processing %s class: %s",
					DatabaseTable.class.getSimpleName(),
					((TypeElement) element).getQualifiedName()));

			ParsedClassName parsedClassName = new ParsedClassName(element);

			String tableName;
			if (annotation.tableName() != null
					&& annotation.tableName().length() > 0) {
				tableName = annotation.tableName();
			} else {
				tableName = element.getSimpleName().toString().toLowerCase();
			}

			TableBindings table = new TableBindings(parsedClassName, tableName);

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

						if (databaseField != null) {
							if (databaseField.persisted()) {
								table.addField(new FieldBindings(child
										.getSimpleName().toString(),
										getSettersForDatabaseField(
												databaseField, child.asType()
														.toString())));
							}
							if (foreignCollectionField != null) {
								raiseError(
										String.format(
												"Fields cannot be annotated with both %s and %s",
												DatabaseField.class
														.getSimpleName(),
												ForeignCollectionField.class
														.getSimpleName()),
										child);
							}
						} else if (foreignCollectionField != null) {
							table.addField(new FieldBindings(
									child.getSimpleName().toString(),
									getSettersForForeignCollectionField(foreignCollectionField)));
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

	private void createDatabaseConfigSourceFile(
			TypeElement openHelperClassElement, List<TypeElement> tableClasses) {
		ParsedClassName openHelperClassName = new ParsedClassName(
				openHelperClassElement);

		try {
			JavaFileObject javaFileObject = processingEnv
					.getFiler()
					.createSourceFile(
							openHelperClassName
									.getGeneratedFullyQualifiedClassName(),
							openHelperClassElement);

			Writer writer = javaFileObject.openWriter();

			try {
				MethodSpec.Builder constructor = MethodSpec
						.constructorBuilder().addModifiers(Modifier.PRIVATE);

				MethodSpec.Builder cacheTableConfigurations = MethodSpec
						.methodBuilder("cacheTableConfigurations")
						.addModifiers(Modifier.PUBLIC, Modifier.STATIC);
				cacheTableConfigurations.addStatement(
						"$T tableConfigs = new $T()",
						ParameterizedTypeName.get(ClassName.get(List.class),
								ParameterizedTypeName.get(ClassName
										.get(DatabaseTableConfig.class),
										WildcardTypeName
												.subtypeOf(Object.class))),
						ParameterizedTypeName.get(ClassName
								.get(ArrayList.class), ParameterizedTypeName
								.get(ClassName.get(DatabaseTableConfig.class),
										WildcardTypeName
												.subtypeOf(Object.class))));
				for (TypeElement tableClass : tableClasses) {
					ParsedClassName tableClassName = new ParsedClassName(
							tableClass);
					cacheTableConfigurations.addStatement(
							"tableConfigs.add($T.createConfig())", ClassName
									.get(tableClassName.getPackageName(),
											tableClassName
													.getGeneratedClassName()));
				}
				cacheTableConfigurations.addStatement(
						"$T.addCachedDatabaseConfigs(tableConfigs)",
						DaoManager.class);

				MethodSpec.Builder createTables = MethodSpec
						.methodBuilder("createTables")
						.addParameter(ConnectionSource.class,
								"connectionSource")
						.addModifiers(Modifier.PUBLIC, Modifier.STATIC)
						.addException(SQLException.class);
				for (TypeElement tableClass : tableClasses) {
					createTables.addStatement(
							"$T.createTable(connectionSource, $T.class)",
							TableUtils.class, tableClass.asType());
				}

				TypeSpec.Builder openHelperClass = TypeSpec
						.classBuilder(
								openHelperClassName.getGeneratedClassName())
						.addModifiers(Modifier.PUBLIC, Modifier.FINAL)
						.addMethod(constructor.build())
						.addMethod(cacheTableConfigurations.build())
						.addMethod(createTables.build());

				JavaFile openHelperFile = JavaFile.builder(
						openHelperClassName.getPackageName(),
						openHelperClass.build()).build();
				openHelperFile.writeTo(writer);
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

	private List<SetterBindings> getSettersForDatabaseField(
			DatabaseField annotation, String fullyQualifiedFieldType) {
		List<SetterBindings> output = new ArrayList<SetterBindings>();
		addSetterIfNotDefault(annotation, "columnName", "setColumnName($S)",
				output);
		addSetterIfNotDefault(annotation, "dataType",
				"setDataType(com.j256.ormlite.field.DataType.$L)", output);
		addSetterIfNotDefault(annotation, "defaultValue",
				"setDefaultValue($S)", output);
		addSetterIfNotDefault(annotation, "width", "setWidth($L)", output);
		addSetterIfNotDefault(annotation, "canBeNull", "setCanBeNull($L)",
				output);
		addSetterIfNotDefault(annotation, "id", "setId($L)", output);
		addSetterIfNotDefault(annotation, "generatedId", "setGeneratedId($L)",
				output);
		addSetterIfNotDefault(annotation, "generatedIdSequence",
				"setGeneratedIdSequence($S)", output);
		addSetterIfNotDefault(annotation, "foreign", "setForeign($L)", output);
		addSetterIfNotDefault(annotation, "useGetSet", "setUseGetSet($L)",
				output);
		addSetterIfNotDefault(annotation, "unknownEnumName",
				"setUnknownEnumValue(" + fullyQualifiedFieldType + ".$L)",
				output);
		addSetterIfNotDefault(annotation, "throwIfNull", "setThrowIfNull($L)",
				output);
		addSetterIfNotDefault(annotation, "format", "setFormat($S)", output);
		addSetterIfNotDefault(annotation, "unique", "setUnique($L)", output);
		addSetterIfNotDefault(annotation, "uniqueCombo", "setUniqueCombo($L)",
				output);
		addSetterIfNotDefault(annotation, "index", "setIndex($L)", output);
		addSetterIfNotDefault(annotation, "uniqueIndex", "setUniqueIndex($L)",
				output);
		addSetterIfNotDefault(annotation, "indexName", "setIndexName($S)",
				output);
		addSetterIfNotDefault(annotation, "uniqueIndexName",
				"setUniqueIndexName($S)", output);
		addSetterIfNotDefault(annotation, "foreignAutoRefresh",
				"setForeignAutoRefresh($L)", output);
		addSetterIfNotDefault(annotation, "maxForeignAutoRefreshLevel",
				"setMaxForeignAutoRefreshLevel($L)", output);
		addSetterIfNotDefault(annotation, "persisterClass",
				"setPersisterClass($T.class)", output);
		addSetterIfNotDefault(annotation, "allowGeneratedIdInsert",
				"setAllowGeneratedIdInsert($L)", output);
		addSetterIfNotDefault(annotation, "columnDefinition",
				"setColumnDefinition($S)", output);
		addSetterIfNotDefault(annotation, "foreignAutoCreate",
				"setForeignAutoCreate($L)", output);
		addSetterIfNotDefault(annotation, "version", "setVersion($L)", output);
		addSetterIfNotDefault(annotation, "foreignColumnName",
				"setForeignColumnName($S)", output);
		addSetterIfNotDefault(annotation, "readOnly", "setReadOnly($L)", output);
		return output;
	}

	private List<SetterBindings> getSettersForForeignCollectionField(
			ForeignCollectionField annotation) {
		List<SetterBindings> output = new ArrayList<SetterBindings>();
		addSetterIfNotDefault(annotation, "columnName", "setColumnName($S)",
				output);
		addSetter("setForeignCollection($L)", true, output);
		addSetterIfNotDefault(annotation, "eager",
				"setForeignCollectionEager($L)", output);
		addSetterIfNotDefault(annotation, "maxEagerLevel",
				"maxEagerForeignCollectionLevel",
				"setForeignCollectionMaxEagerLevel($L)", output);
		addSetterIfNotDefault(annotation, "columnName",
				"setForeignCollectionColumnName($S)", output);
		addSetterIfNotDefault(annotation, "orderColumnName",
				"setForeignCollectionOrderColumnName($S)", output);
		addSetterIfNotDefault(annotation, "orderAscending",
				"setForeignCollectionOrderAscending($L)", output);
		addSetterIfNotDefault(annotation, "foreignFieldName",
				"foreignColumnName",
				"setForeignCollectionForeignFieldName($S)", output);
		return output;
	}

	private void addSetterIfNotDefault(Annotation annotation, String name,
			String format, List<SetterBindings> output) {
		addSetterIfNotDefault(annotation, name, null, format, output);
	}

	private void addSetterIfNotDefault(Annotation annotation, String name,
			String fallbackName, String format, List<SetterBindings> output) {
		Object value = getValueIfNotDefault(annotation, name);
		if (value != null) {
			addSetter(format, value, output);
		} else if (fallbackName != null) {
			value = getValueIfNotDefault(annotation, fallbackName);
			if (value != null) {
				addSetter(format, value, output);
			}
		}
	}

	private void addSetter(String format, Object value,
			List<SetterBindings> output) {
		output.add(new SetterBindings(format, value));
	}

	/**
	 * This function examines a single field in an annotation and if the current
	 * value doesn't match the default returns the current value. If the current
	 * value matches the default, it returns null.
	 * 
	 * @param annotation
	 *            the annotation containing the field of interest
	 * @param name
	 *            the name of the field in the annotation to read
	 * @return the current value if it is not the default, null otherwise
	 */
	private Object getValueIfNotDefault(Annotation annotation, String name) {
		try {
			Method method = annotation.annotationType().getMethod(name);

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

			if (defaultValue.equals(actualValue)) {
				return null;
			} else {
				return actualValue;
			}
		} catch (Exception e) {
			// All possible annotation properties are unit tested, so it is not
			// possible to get an exception here
			throw new RuntimeException(e);
		}
	}

	private TypeName getClassNameFromClassObject(Object object) {
		return TypeName.get((Class<?>) object);
	}

	private TypeName getMirroredClassNameFromException(Exception ex)
			throws Exception {
		Throwable t = ex;
		do {
			if (t instanceof MirroredTypeException) {
				return TypeName
						.get(((MirroredTypeException) t).getTypeMirror());
			}
			t = t.getCause();
		} while (t != null);

		throw ex;
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

	private void writeTable(Writer writer, TableBindings table)
			throws IOException {
		MethodSpec.Builder constructor = MethodSpec.constructorBuilder()
				.addModifiers(Modifier.PRIVATE);

		MethodSpec.Builder createConfig = MethodSpec
				.methodBuilder("createConfig")
				.addModifiers(Modifier.PUBLIC, Modifier.STATIC)
				.returns(
						ParameterizedTypeName.get(
								ClassName.get(DatabaseTableConfig.class),
								WildcardTypeName.subtypeOf(Object.class)));
		createConfig.addStatement("$T databaseFieldConfigs = new $T()",
				ParameterizedTypeName.get(ClassName.get(List.class),
						TypeName.get(DatabaseFieldConfig.class)),
				ParameterizedTypeName.get(ClassName.get(ArrayList.class),
						TypeName.get(DatabaseFieldConfig.class)));

		for (FieldBindings field : table.getFields()) {
			createConfig.addStatement("$T $LFieldConfig = new $T($S)",
					DatabaseFieldConfig.class, field.getFieldName(),
					DatabaseFieldConfig.class, field.getFieldName());

			for (SetterBindings setter : field.getSetters()) {
				createConfig.addStatement(
						"$LFieldConfig." + setter.getFormat(),
						field.getFieldName(), setter.getParameter());
			}

			createConfig.addStatement(
					"databaseFieldConfigs.add($LFieldConfig)",
					field.getFieldName());
		}

		createConfig.addStatement(
				"return new $T<$T>($T.class, $S, databaseFieldConfigs)",
				DatabaseTableConfig.class, ClassName.get(table
						.getParsedClassName().getPackageName(), table
						.getParsedClassName().getInputClassName()), ClassName
						.get(table.getParsedClassName().getPackageName(), table
								.getParsedClassName().getInputClassName()),
				table.getTableName());

		TypeSpec.Builder tableConfigClass = TypeSpec
				.classBuilder(
						table.getParsedClassName().getGeneratedClassName())
				.addModifiers(Modifier.PUBLIC, Modifier.FINAL)
				.addMethod(constructor.build()).addMethod(createConfig.build());

		JavaFile tableConfigFile = JavaFile.builder(
				table.getParsedClassName().getPackageName(),
				tableConfigClass.build()).build();
		tableConfigFile.writeTo(writer);
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
