package org.jjflyboy.forge.javabeans;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.forge.arquillian.AddonDependencies;
import org.jboss.forge.arquillian.AddonDependency;
import org.jboss.forge.arquillian.archive.AddonArchive;
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.MethodSource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class CreateBeansTest {

	@Deployment
	@AddonDependencies({ @AddonDependency(name = "org.jboss.forge.furnace.container:cdi"),
			@AddonDependency(name = "org.jboss.forge.addon:parser-java") })
	public static AddonArchive getDeployment() {
		return ShrinkWrap.create(AddonArchive.class).addBeansXML().addClasses(JavabeanOperations.class,
				JavabeanOperationsImpl.class);
	}

	@Inject
	private JavabeanOperations classOperations;
	private static final Pattern FROM_STATEMENT_PATTERN = Pattern.compile("\\s*(\\w+)\\s*=\\s*example\\.(\\w+)\\s*==\\s*null\\s*\\?\\s*(\\w+)\\s*:\\s*example\\.(\\w+);");

	@Test
	public void testCreateLoader() {
		JavaClassSource original = Roaster.create(JavaClassSource.class).setName("TestBean").setPackage("org.sample");
		classOperations.addLoader(original);
		JavaClassSource loader = (JavaClassSource) original.getNestedType("Loader");
		Assert.assertNotNull("loader is not created", loader);
		String supertype = loader.getSuperType();
		Assert.assertNotNull("loader has supertype", !"java.lang.Object".equals(supertype));
	}

	@Test
	public void testCreateLoaderField() {
		JavaClassSource original = Roaster.create(JavaClassSource.class).setName("TestBean").setPackage("org.sample");
		original.addField("String stringProp").setPrivate();
		original.addField("UUID uuidProp").setPrivate();

		classOperations.addLoader(original);
		JavaClassSource loader = (JavaClassSource) original.getNestedType("Loader");
		Assert.assertNotNull("loader is not created", original.getNestedType("Loader"));
		Assert.assertNotNull("did not create stringProp field", loader.getField("stringProp"));
		Assert.assertNotNull("did not create uuidProp field", loader.getField("uuidProp"));

		MethodSource<JavaClassSource> withStringProp = loader.getMethod("withStringProp", String.class);
		Assert.assertNotNull("loader does not have withStringProp method", withStringProp);
		Assert.assertTrue("with method is not public", withStringProp.isPublic());

		MethodSource<JavaClassSource> withFrom = loader.getMethod("from", original.getName());
		Assert.assertNotNull(withFrom);

		String[] fields = new String[] {"stringProp", "uuidProp"};
		String[] lines = withFrom.getBody().toString().split("\n");
		Assert.assertEquals("not only two lines", 2, lines.length);
		for(int i = 0; i < lines.length; i++) {
			Matcher m = FROM_STATEMENT_PATTERN.matcher(lines[i]);
			Assert.assertTrue("line does not match", m.matches());
			Assert.assertEquals("wrong group count", 4, m.groupCount());
			for(int j = 1; j < m.groupCount() + 1; j++) {
				Assert.assertEquals("group " + Integer.toString(j) + " is not the right field", fields[i], m.group(j));
			}
		}
	}

	@Test
	public void testCreateLoaderWithSuperClass() {
		JavaClassSource original = (JavaClassSource) Roaster.parse("public class TestBean extends SuperTestBean {}");
		original.setPackage("org.sample");
		classOperations.addLoader(original);
		JavaClassSource loader = (JavaClassSource) original.getNestedType("Loader");
		Assert.assertNotNull("loader is not created", original.getNestedType("Loader"));
		String supertype = loader.getSuperType();
		Assert.assertNotNull("loader does not have correct supertype", supertype.contains("SuperTestBean"));

	}

	@Test
	public void testFromStatementPattern() {
		Matcher m = FROM_STATEMENT_PATTERN.matcher("number = example.number == null ? number :     example.number;");
		Assert.assertTrue("from statement pattern doesn't work anymore", m.matches());
		Assert.assertEquals("not 'number'", "number", m.group(1));
		Assert.assertEquals("not 'number'", "number", m.group(2));
		Assert.assertEquals("not 'number'", "number", m.group(3));
		Assert.assertEquals("not 'number'", "number", m.group(4));

	}
}