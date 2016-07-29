package org.jjflyboy.forge.javabeans;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.forge.arquillian.AddonDependencies;
import org.jboss.forge.arquillian.AddonDependency;
import org.jboss.forge.arquillian.archive.AddonArchive;
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.source.FieldSource;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.MethodSource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class FieldOperationsRebuildAndPreservationTest {

	@Deployment
	@AddonDependencies({ @AddonDependency(name = "org.jboss.forge.furnace.container:cdi"),
		@AddonDependency(name = "org.jboss.forge.addon:parser-java") })
	public static AddonArchive getDeployment() {
		return ShrinkWrap.create(AddonArchive.class).addBeansXML().addClasses(JavabeanOperations.class,
				JavabeanOperationsImpl.class, ExtendedBiFunction.class);
	}

	@Inject
	private JavabeanOperations classOperations;

	private JavaClassSource original;
	private JavaClassSource loader;

	@Before
	public void before() {
		original = Roaster.create(JavaClassSource.class).setName("TestBean").setPackage("org.sample");
		original.addField("Integer intField");
		loader = classOperations.buildLoader(original);
	}

	@Test
	public void testGeneratedOnField() {

		FieldSource<JavaClassSource> field = loader.getField("intField");
		String generatedField = field.toString();
		field.setLiteralInitializer("20");
		JavaClassSource newLoader = classOperations.rebuildLoader(original);

		Assert.assertEquals("field was not overwritten", generatedField, newLoader.getField("intField").toString());
	}



	@Test
	public void testGeneratedOnWithFieldMethod() {
		testGeneratedLoaderFieldMethod("with");
	}

	@Test
	public void testGeneratedOnFromFieldMethod() {
		testGeneratedLoaderFieldMethod("from");
	}

	@Test
	public void testGeneratedOnModifyFieldMethod() {
		testGeneratedLoaderFieldMethod("modify");
	}

	@Test
	public void testGeneratedOnInitializeFieldMethod() {
		testGeneratedLoaderFieldMethod("initialize");
	}

	@Test
	public void testGeneratedOnFromMethod() {
		testGeneratedLoaderMethod("from");
	}

	@Test
	public void testGeneratedOnModifyMethod() {
		testGeneratedLoaderMethod("modify");
	}

	@Test
	public void testGeneratedOnInitializeMethod() {
		testGeneratedLoaderMethod("initialize");
	}


	@Test
	public void testCreateFieldObservesAbsentGenerated() {

		FieldSource<JavaClassSource> field = loader.getField("intField");
		field.removeAllAnnotations();
		field.setLiteralInitializer("20");
		String editedField = field.toString();

		JavaClassSource newLoader = classOperations.rebuildLoader(original);

		Assert.assertEquals("loader field was not preserved", editedField, newLoader.getField("intField").toString());
	}

	@Test
	public void testEditableWithFieldMethod() {
		testEditableLoaderFieldMethod("with");
	}

	@Test
	public void testEditableFromFieldMethod() {
		testEditableLoaderFieldMethod("from");
	}

	@Test
	public void testEditableModifyFieldMethod() {
		testEditableLoaderFieldMethod("modify");
	}

	@Test
	public void testEditableInitializeFieldMethod() {
		testEditableLoaderFieldMethod("initialize");
	}

	@Test
	public void testEditableFromMethod() {
		testEditableLoaderMethod("from");
	}

	@Test
	public void testEditableModifyMethod() {
		testEditableLoaderMethod("modify");
	}

	@Test
	public void testEditableInitializeMethod() {
		testEditableLoaderMethod("initialize");
	}

	private void testGeneratedLoaderFieldMethod(String prefix) {
		MethodSource<JavaClassSource> method = loader.getMethod(prefix + "IntField", Integer.class);
		String former = method.toString();
		// the @Generated is on this method as pre-condition.
		method.setBody("");
		JavaClassSource newLoader = classOperations.rebuildLoader(original);
		String postRebuild = newLoader.getMethod(prefix + "IntField", Integer.class).toString();
		Assert.assertEquals(prefix + " field method's body was not overwritten.", former, postRebuild);
	}

	private void testGeneratedLoaderMethod(String name) {
		MethodSource<JavaClassSource> method = loader.getMethod(name, original.getName());
		String former = method.toString();
		// the @Generated is on this method as pre-condition.
		method.setBody("");
		JavaClassSource newLoader = classOperations.rebuildLoader(original);
		String postRebuild = newLoader.getMethod(name, original.getName()).toString();
		Assert.assertEquals(name + " method's body was not overwritten.", former, postRebuild);
	}

	private void testEditableLoaderFieldMethod(String prefix) {
		MethodSource<JavaClassSource> method = loader.getMethod(prefix + "IntField", Integer.class);
		method.removeAllAnnotations();
		method.setBody("");
		String edited = method.toString();
		// the @Generated is stripped

		JavaClassSource newLoader = classOperations.rebuildLoader(original);
		String postRebuild = newLoader.getMethod(prefix + "IntField", Integer.class).toString();
		Assert.assertEquals(prefix + " field method's body was not preserved.", edited, postRebuild);
	}

	private void testEditableLoaderMethod(String name) {
		MethodSource<JavaClassSource> method = loader.getMethod(name, original.getName());
		method.removeAllAnnotations();
		method.setBody("");
		String edited = method.toString();
		// the @Generated is stripped

		JavaClassSource newLoader = classOperations.rebuildLoader(original);
		String postRebuild = newLoader.getMethod(name, original.getName()).toString();
		Assert.assertEquals(name + " method's body was not preserved.", edited, postRebuild);
	}

}