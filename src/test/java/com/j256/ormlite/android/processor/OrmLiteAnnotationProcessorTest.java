package com.j256.ormlite.android.processor;

import static com.google.common.truth.Truth.assert_;
import static com.google.testing.compile.JavaSourceSubjectFactory.javaSource;

import org.junit.Test;

import com.google.testing.compile.JavaFileObjects;

import com.j256.ormlite.android.annotations.Database;
import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.table.DatabaseTable;

public class OrmLiteAnnotationProcessorTest {

	@Test
	public void testDatabaseFieldAllDefaults() {
		assert_()
				.about(javaSource())
				.that(JavaFileObjects
						.forResource("inputs/UnnamedTableWithDefaultDatabaseField.java"))
				.processedWith(new OrmLiteAnnotationProcessor())
				.compilesWithoutError()
				.and()
				.generatesSources(
						JavaFileObjects
								.forResource("outputs/UnnamedTableWithDefaultDatabaseField_TableConfig.java"),
						JavaFileObjects
								.forResource("outputs/UnnamedTableWithDefaultDatabaseField_OpenHelper_TableConfig.java"));
	}

	@Test
	public void testDatabaseFieldAllSpecified() {
		assert_()
				.about(javaSource())
				.that(JavaFileObjects
						.forResource("inputs/NamedTableWithSpecifiedDatabaseField.java"))
				.processedWith(new OrmLiteAnnotationProcessor())
				.compilesWithoutError()
				.and()
				.generatesSources(
						JavaFileObjects
								.forResource("outputs/NamedTableWithSpecifiedDatabaseField_TableConfig.java"),
						JavaFileObjects
								.forResource("outputs/NamedTableWithSpecifiedDatabaseField_OpenHelper_TableConfig.java"));
	}

	@Test
	public void testForeignCollectionFieldAllDefaults() {
		assert_()
				.about(javaSource())
				.that(JavaFileObjects
						.forResource("inputs/UnnamedTableWithDefaultForeignCollectionField.java"))
				.processedWith(new OrmLiteAnnotationProcessor())
				.compilesWithoutError()
				.and()
				.generatesSources(
						JavaFileObjects
								.forResource("outputs/UnnamedTableWithDefaultForeignCollectionField_TableConfig.java"),
						JavaFileObjects
								.forResource("outputs/UnnamedTableWithDefaultForeignCollectionField_OpenHelper_TableConfig.java"));
	}

	@Test
	public void testForeignCollectionFieldAllSpecified() {
		assert_()
				.about(javaSource())
				.that(JavaFileObjects
						.forResource("inputs/NamedTableWithSpecifiedForeignCollectionField.java"))
				.processedWith(new OrmLiteAnnotationProcessor())
				.compilesWithoutError()
				.and()
				.generatesSources(
						JavaFileObjects
								.forResource("outputs/NamedTableWithSpecifiedForeignCollectionField_TableConfig.java"),
						JavaFileObjects
								.forResource("outputs/NamedTableWithSpecifiedForeignCollectionField_OpenHelper_TableConfig.java"));
	}

	@Test
	public void testInnerClasses() {
		assert_()
				.about(javaSource())
				.that(JavaFileObjects
						.forResource("inputs/InnerClassTable.java"))
				.processedWith(new OrmLiteAnnotationProcessor())
				.compilesWithoutError()
				.and()
				.generatesSources(
						JavaFileObjects
								.forResource("outputs/InnerClassTable_InnerClass_TableConfig.java"),
						JavaFileObjects
								.forResource("outputs/InnerClassTable_OtherInnerClass_TableConfig.java"),
						JavaFileObjects
								.forResource("outputs/InnerClassTable_OpenHelper_TableConfig.java"));
	}

	@Test
	public void testErrorBothAnnotationsOnField() {
		assert_()
				.about(javaSource())
				.that(JavaFileObjects
						.forResource("inputs/TableWithFieldWithBothAnnotations.java"))
				.processedWith(new OrmLiteAnnotationProcessor())
				.failsToCompile()
				.withErrorContaining(
						String.format(
								"Fields cannot be annotated with both %s and %s",
								DatabaseField.class.getSimpleName(),
								ForeignCollectionField.class.getSimpleName()));
	}

	@Test
	public void testErrorDatabaseWithNoTables() {
		assert_()
				.about(javaSource())
				.that(JavaFileObjects
						.forResource("inputs/DatabaseWithNoTables.java"))
				.processedWith(new OrmLiteAnnotationProcessor())
				.failsToCompile()
				.withErrorContaining(
						String.format(
								"%s annotation must contain at least one class annotated with %s",
								Database.class.getSimpleName(),
								DatabaseTable.class.getSimpleName()));
	}

	@Test
	public void testErrorDatabaseDerivedFromWrongClass() {
		assert_()
				.about(javaSource())
				.that(JavaFileObjects
						.forResource("inputs/DatabaseDerivedFromWrongClass.java"))
				.processedWith(new OrmLiteAnnotationProcessor())
				.failsToCompile()
				.withErrorContaining(
						String.format(
								"%s annotation must be applied to a class deriving from %s",
								Database.class.getSimpleName(),
								OrmLiteSqliteOpenHelper.class.getSimpleName()));
	}

	@Test
	public void testErrorDatabaseWithNonTable() {
		assert_()
				.about(javaSource())
				.that(JavaFileObjects
						.forResource("inputs/DatabaseWithNonTable.java"))
				.processedWith(new OrmLiteAnnotationProcessor())
				.failsToCompile()
				.withErrorContaining(
						String.format(
								"%s annotation contains class %s not annotated with %s",
								Database.class.getSimpleName(),
								String.class.getSimpleName(),
								DatabaseTable.class.getSimpleName()));
	}
}
