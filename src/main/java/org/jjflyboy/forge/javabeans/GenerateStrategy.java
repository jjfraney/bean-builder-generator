package org.jjflyboy.forge.javabeans;

import org.jboss.forge.roaster.model.JavaUnit;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.JavaSource;

import java.util.List;

/**
 * @author jfraney
 */
@FunctionalInterface
public interface GenerateStrategy {
	/**
	 * generate changes to the javabean, and return compilation units as string.
	 * @param javabean
	 */
	List<JavaClassSource> generate(JavaClassSource javabean);
}
