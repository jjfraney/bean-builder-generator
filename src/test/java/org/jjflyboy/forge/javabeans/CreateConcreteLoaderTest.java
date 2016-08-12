package org.jjflyboy.forge.javabeans;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.forge.arquillian.AddonDependencies;
import org.jboss.forge.arquillian.AddonDependency;
import org.jboss.forge.arquillian.archive.AddonArchive;
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;

@RunWith(Arquillian.class)
public class CreateConcreteLoaderTest {

	@Deployment
	@AddonDependencies({ @AddonDependency(name = "org.jboss.forge.furnace.container:cdi"),
		@AddonDependency(name = "org.jboss.forge.addon:parser-java") })
	public static AddonArchive getDeployment() {
		return ShrinkWrap.create(AddonArchive.class).addBeansXML().addClasses(JavabeanOperations.class,
				JavabeanOperationsImpl.class);
	}

	@SuppressWarnings({"CanBeFinal", "unused"})
	@Inject
	private JavabeanOperations classOperations;

	@Test
	public void testCreateBuilder() {
		JavaClassSource original = Roaster.create(JavaClassSource.class).setName("TestBean").setPackage("org.sample");
		classOperations.rebuildLoader(original);
		JavaClassSource builder = classOperations.rebuildBuilder(original);
		Assert.assertNotNull("builder was not created", builder);
	}

	@Test
	public void testCreateUpdater() {
		JavaClassSource original = Roaster.create(JavaClassSource.class).setName("TestBean").setPackage("org.sample");
		classOperations.rebuildLoader(original);
		JavaClassSource updater = classOperations.rebuildUpdater(original);
		Assert.assertNotNull("updater was not created", updater);
	}

}