package com.j256.ormlite.android.apptools;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Collection;

import org.junit.Test;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.ForeignCollectionField;

@SuppressWarnings("deprecation")
public class OrmLiteConfigUtilTest {

	private static final String lineSeparator = System.getProperty("line.separator");

	@Test
	public void testBasic() throws Exception {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		OrmLiteConfigUtil.writeConfigFile(output, new Class[] { Foo.class });
		String result = output.toString();
		assertTrue(result, result.contains(lineSeparator +
			"fieldName=id" + lineSeparator +
			"id=true" + lineSeparator));
	}

	@Test
	public void testCurrentDir() throws Exception {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		OrmLiteConfigUtil.writeConfigFile(output, new File("src/test/java/com/j256/ormlite/android/apptools/"));
		String result = output.toString();
		assertTrue(result, result.contains(lineSeparator +
			"fieldName=id" + lineSeparator +
			"id=true" + lineSeparator));
	}

	@Test
	public void testForeignCollection() throws Exception {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		OrmLiteConfigUtil.writeConfigFile(output, new Class[] { ForeignCollectionTest.class });
		String result = output.toString();
		assertTrue(result, result.contains(lineSeparator +
			"fieldName=collection" + lineSeparator +
			"foreignCollection=true" + lineSeparator));
	}

	@Test
	public void testForeign() throws Exception {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		OrmLiteConfigUtil.writeConfigFile(output, new Class[] { Foo.class });
		String result = output.toString();
		assertTrue(result, result.contains(lineSeparator +
			"fieldName=foreign" + lineSeparator +
			"foreign=true" + lineSeparator));
	}

	protected static class Foo {
		@DatabaseField(id = true)
		int id;
		@DatabaseField(foreign = true)
		Foreign foreign;
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
