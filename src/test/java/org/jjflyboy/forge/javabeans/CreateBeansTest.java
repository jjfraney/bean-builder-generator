package org.jjflyboy.forge.javabeans;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.forge.arquillian.AddonDependencies;
import org.jboss.forge.arquillian.AddonDependency;
import org.jboss.forge.arquillian.archive.AddonArchive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class CreateBeansTest {

	@Deployment
	@AddonDependencies({
			@AddonDependency(name = "org.jboss.forge.furnace.container:cdi") })
	public static AddonArchive getDeployment() {
		return ShrinkWrap.create(AddonArchive.class)
				.addBeansXML()
				.addClasses(JavabeansClassOperations.class, JavabeansClassOperationsImpl.class)
				;
	}

	@Inject
	private JavabeansClassOperations classOperations;

	@Test
	public void testAddon() {
		classOperations.createClass("hello");
	}
}