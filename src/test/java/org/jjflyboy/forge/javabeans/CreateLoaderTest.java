package org.jjflyboy.forge.javabeans;

import javax.annotation.Generated;
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
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class CreateLoaderTest {

	@Deployment
	@AddonDependencies({ @AddonDependency(name = "org.jboss.forge.furnace.container:cdi"),
		@AddonDependency(name = "org.jboss.forge.addon:parser-java") })
	public static AddonArchive getDeployment() {
		return ShrinkWrap.create(AddonArchive.class).addBeansXML().addClasses(JavabeanOperations.class,
				JavabeanOperationsImpl.class);
	}

	@Inject
	private JavabeanOperations classOperations;

	@Test
	public void testCreateLoader() {
		JavaClassSource original = Roaster.create(JavaClassSource.class).setName("TestBean").setPackage("org.sample");
		classOperations.addLoader(original);
		JavaClassSource loader = (JavaClassSource) original.getNestedType("Loader");
		Assert.assertNotNull("loader is not created", loader);
		String supertype = loader.getSuperType();
		Assert.assertNotNull("loader has supertype", !"java.lang.Object".equals(supertype));

		MethodSource<JavaClassSource> fromMethod = loader.getMethod("from", original.getName());
		Assert.assertNotNull("from method was not created", fromMethod);

		MethodSource<JavaClassSource> modifyMethod = loader.getMethod("modify", original.getName());
		Assert.assertNotNull("modify method was not created", modifyMethod);

		MethodSource<JavaClassSource> initMethod = loader.getMethod("initialize", original.getName());
		Assert.assertNotNull("initialize method was not created", initMethod);
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
	public void annotatesWithGenerated() {
		JavaClassSource original = Roaster.create(JavaClassSource.class).setName("TestBean").setPackage("org.sample");
		classOperations.addLoader(original);
		JavaClassSource loader = classOperations.addLoader(original);
		Assert.assertNotNull("didn't mark 'Generated' annotation", loader.getAnnotation(Generated.class));
	}

	@Test
	public void observesPresentGeneratedAnnotation() {
		JavaClassSource original = Roaster.create(JavaClassSource.class).setName("TestBean").setPackage("org.sample");
		FieldSource<JavaClassSource> testField = original.addField("int testField");
		JavaClassSource firstLoader = classOperations.addLoader(original);
		Assert.assertNotNull("test field not in loader", firstLoader.getField("testField"));

		original.removeField(testField);
		JavaClassSource secondLoader = classOperations.addLoader(original);
		Assert.assertNull("original loader not overridden", secondLoader.getField("testField"));

	}

	@Test
	public void observesAbsentGeneratedAnnotation() {
		JavaClassSource original = Roaster.create(JavaClassSource.class).setName("TestBean").setPackage("org.sample");
		FieldSource<JavaClassSource> testField = original.addField("int testField");
		JavaClassSource firstLoader = classOperations.addLoader(original);
		Assert.assertNotNull("test field not in loader", firstLoader.getField("testField"));

		firstLoader.removeAnnotation(firstLoader.getAnnotation(Generated.class));
		original.removeField(testField);
		JavaClassSource secondLoader = classOperations.addLoader(original);
		Assert.assertNotNull("original loader has been overridden", secondLoader.getField("testField"));

	}

}