package org.jjflyboy.forge.javabeans;

import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.MethodSource;

import java.util.List;

interface JavabeanOperations {
	String GENERATED_ANNOTATION_VALUE = "\"BeanGenerator\"";

	/**
	 * adds nested Loader class to the original javabean.
	 * <p>
	 * An existing nested Loader class without @Generated annotation, will be
	 * preserved. Otherwise, the class is replaced.
	 *
	 * @param javabean the enclosing javabean
	 * @return the nested Loader class
	 */
	JavaClassSource buildLoader(JavaClassSource javabean);

	/**
	 * replaces nested Loader class in the original javabean
	 *
	 * @param javabean the enclosing javabean
	 * @return the nested Loader class
	 */
	JavaClassSource rebuildLoader(JavaClassSource javabean);

	JavaClassSource buildBuilder(JavaClassSource javabean);
	JavaClassSource rebuildBuilder(JavaClassSource javabean);
	JavaClassSource buildUpdater(JavaClassSource javabean);
	JavaClassSource rebuildUpdater(JavaClassSource javabean);

	List<MethodSource<JavaClassSource>> rebuildCtors(JavaClassSource javabean);
	MethodSource<JavaClassSource> rebuildBuilderMethod(JavaClassSource javabean);
	MethodSource<JavaClassSource> rebuildUpdaterMethod(JavaClassSource javabean);
}
