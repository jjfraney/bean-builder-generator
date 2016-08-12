package org.jjflyboy.forge.javabeans;

import org.jboss.forge.roaster.model.source.JavaClassSource;

import java.util.List;

/**
 * @author jfraney
 */
@FunctionalInterface
interface GenerateStrategy {
	/**
	 * generate changes to the javabean, and return compilation units as string.
	 * @param javabean targeted for builder generation
	 */
	List<JavaClassSource> generate(JavaClassSource javabean);
}
