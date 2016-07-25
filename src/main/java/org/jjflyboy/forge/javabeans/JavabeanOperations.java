package org.jjflyboy.forge.javabeans;

import org.jboss.forge.roaster.model.source.JavaClassSource;

public interface JavabeanOperations {
	String GENERATED_ANNOTATION_VALUE = "\"BeanGenerator\"";

	/**
	 * adds nested Loader class to the original javabean.
	 * <p>
	 * An existing nested Loader class without @Generated annotation, will be
	 * preserved. Otherwise, the class is replaced.
	 *
	 * @param javabean
	 * @return the nested Loader class
	 */
	JavaClassSource buildLoader(JavaClassSource javabean);

	JavaClassSource rebuildLoader(JavaClassSource javabean);
}
