package org.jjflyboy.forge.javabeans;

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

import javax.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

@RunWith(Arquillian.class)
public class CreateJavabeanMethodsTest {

	@Deployment
	@AddonDependencies({ @AddonDependency(name = "org.jboss.forge.furnace.container:cdi"),
		@AddonDependency(name = "org.jboss.forge.addon:parser-java") })
	public static AddonArchive getDeployment() {
		return ShrinkWrap.create(AddonArchive.class).addBeansXML().addClasses(JavabeanOperations.class,
				JavabeanOperationsImpl.class);
	}

	@SuppressWarnings("CanBeFinal")
	@Inject
	private JavabeanOperations classOperations;

	@Test
	public void testCreateCtors() {
		JavaClassSource original = Roaster.create(JavaClassSource.class).setName("TestBean").setPackage("org.sample");
		List<MethodSource<JavaClassSource>> ctors = classOperations.rebuildCtors(original);
		Assert.assertEquals("wrong number of method definitions returned.", 2, ctors.size());
		MethodSource<JavaClassSource> privateCtor = ctors.stream().filter(s -> s.isPrivate()).filter(s -> "TestBean".equals(s.getName())).findAny().orElse(null);
		Assert.assertNotNull("private ctor not found.", privateCtor);

		MethodSource<JavaClassSource> publicCtor = ctors.stream().filter(s -> s.isPublic()).filter(s -> "TestBean".equals(s.getName())).findAny().orElse(null);
		Assert.assertNotNull("public ctor not found.", publicCtor);
	}
	@Test
	public void testPreserveCtors() {
		// test setup: build original with ctors for preservation
		JavaClassSource original = Roaster.create(JavaClassSource.class).setName("TestBean").setPackage("org.sample");
		List<MethodSource<JavaClassSource>> ctors = classOperations.rebuildCtors(original);

		// add methods without @Generated to the original javabean.
		List<MethodSource<JavaClassSource>> pm = ctors.stream()
				.map(m -> {m.removeAllAnnotations(); return m;})
				.collect(Collectors.toList());
		pm.stream().forEach(m -> Assert.assertEquals("method still has annotation " + m.getName(), 0, m.getAnnotations().size()));


		// now...run the test to see if the methods were preserved
		List<MethodSource<JavaClassSource>> newCtors = classOperations.rebuildCtors(original);
		pm.stream().forEach(m -> Assert.assertEquals("method was not preserved " + m.getName(), 0, m.getAnnotations().size()));
	}

	@Test
	public void testCreateBuilderMethod() {
		JavaClassSource original = Roaster.create(JavaClassSource.class).setName("TestBean").setPackage("org.sample");
		MethodSource<JavaClassSource> method = classOperations.rebuildBuilderMethod(original);
		Assert.assertTrue("didn't create builder method", method.toString().contains("public static Builder builder()"));
	}
	@Test
	public void testCreateUpdaterMethod() {
		JavaClassSource original = Roaster.create(JavaClassSource.class).setName("TestBean").setPackage("org.sample");
		MethodSource<JavaClassSource> method = classOperations.rebuildUpdaterMethod(original);
		Assert.assertTrue("didn't create updater method", method.toString().contains("public static Updater updater()"));
	}

}