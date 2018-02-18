package com.j256.ormlite.android.apptools;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Collection;

import org.junit.Test;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.ForeignCollectionField;

public class OrmLiteConfigUtilTest {

	private static final String lineSeparator = System.getProperty("line.separator");

	@Test
	public void testBasic() throws Exception {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		OrmLiteConfigUtil.writeConfigFile(output, new Class[] { Foo.class });
		String result = output.toString();
		assertTrue(result,
				result.contains(lineSeparator //
						+ "fieldName=id" //
						+ lineSeparator + "id=true" + lineSeparator));
	}

	@Test
	public void testBasicSorted() throws Exception {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		OrmLiteConfigUtil.writeConfigFile(output, new Class[] { Foo.class, Bar.class }, false);
		String result1 = output.toString();
		assertTrue(result1,
				result1.contains(lineSeparator //
						+ "fieldName=id" //
						+ lineSeparator + "id=true" + lineSeparator));
		output.reset();
		OrmLiteConfigUtil.writeConfigFile(output, new Class[] { Foo.class, Bar.class }, true);
		String result2 = output.toString();
		assertFalse(result2.equals(result1));
	}

	@Test
	public void testCurrentDir() throws Exception {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		OrmLiteConfigUtil.writeConfigFile(output, new File("src/test/java/com/j256/ormlite/android/apptools/"));
		String result = output.toString();
		assertTrue(result,
				result.contains(lineSeparator //
						+ "fieldName=id" + lineSeparator //
						+ "id=true" + lineSeparator));
	}

	@Test
	public void testCurrentDirSorted() throws Exception {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		OrmLiteConfigUtil.writeConfigFile(output, new File("src/test/java/com/j256/ormlite/android/apptools/"), true);
		String result = output.toString();
		assertTrue(result,
				result.contains(lineSeparator //
						+ "fieldName=id" + lineSeparator //
						+ "id=true" + lineSeparator));
	}

	@Test
	public void testForeignCollection() throws Exception {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		OrmLiteConfigUtil.writeConfigFile(output, new Class[] { ForeignCollectionTest.class });
		String result = output.toString();
		assertTrue(result,
				result.contains(lineSeparator //
						+ "fieldName=collection" + lineSeparator //
						+ "foreignCollection=true" + lineSeparator));
	}

	@Test
	public void testForeign() throws Exception {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		OrmLiteConfigUtil.writeConfigFile(output, new Class[] { Foo.class });
		String result = output.toString();
		assertTrue(result,
				result.contains(lineSeparator //
						+ "fieldName=foreign" + lineSeparator //
						+ "foreign=true" + lineSeparator));
	}

	protected static class Foo {
		@DatabaseField(id = true)
		int id;
		@DatabaseField(foreign = true)
		Foreign foreign;
	}

	protected static class Bar {
		@DatabaseField(id = true)
		int id;
		@DatabaseField(foreign = true)
		String zipper;
	}

	protected static class Foreign {
		@DatabaseField(id = true)
		int id;
	}

	protected static class ForeignCollectionTest {
		@ForeignCollectionField
		Collection<Foo> collection;
	}
}
