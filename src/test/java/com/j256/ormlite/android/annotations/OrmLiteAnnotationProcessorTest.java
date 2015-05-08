package com.j256.ormlite.android.annotations;

import static com.google.common.truth.Truth.assert_;
import static com.google.testing.compile.JavaSourceSubjectFactory.javaSource;

import org.junit.Test;

import com.google.testing.compile.JavaFileObjects;

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
								.forResource("outputs/UnnamedTableWithDefaultDatabaseField_TableConfig.java"));
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
								.forResource("outputs/NamedTableWithSpecifiedDatabaseField_TableConfig.java"));
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
								.forResource("outputs/UnnamedTableWithDefaultForeignCollectionField_TableConfig.java"));
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
								.forResource("outputs/NamedTableWithSpecifiedForeignCollectionField_TableConfig.java"));
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
								.forResource("outputs/InnerClassTable_OtherInnerClass_TableConfig.java"));
	}
}
