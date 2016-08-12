package org.jjflyboy.forge.javabeans;

import javax.inject.Inject;

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

@RunWith(Arquillian.class)
public class FieldOperationsTest {

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
	public void testCreateField() {
		JavaClassSource original = Roaster.create(JavaClassSource.class).setName("TestBean")
				.setPackage("org.sample");
		classOperations.rebuildLoader(original);
		original.addField("Integer intField");
		JavaClassSource newLoader = classOperations.rebuildLoader(original);

		Assert.assertNotNull("test field not added", newLoader.getField("intField"));
		Assert.assertNotNull("withField was not added", newLoader.getMethod("withIntField", Integer.class));
	}

}