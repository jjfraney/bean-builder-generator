package org.jjflyboy.forge.javabeans;

import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.MethodSource;

import java.util.List;

interface JavabeanOperations {
	String GENERATED_ANNOTATION_VALUE = "\"BeanGenerator\"";

	// by default, loader is fully generated
	enum LoaderFlags {
		OMIT_MODIFY_METHOD
	}

	/**
	 * build or replace nested Loader class in the original javabean
	 *
	 * @param javabean the enclosing javabean
	 * @param flags
	 * @return the nested Loader class
	 */
	JavaClassSource rebuildLoader(JavaClassSource javabean, LoaderFlags ... flags);
	JavaClassSource rebuildBuilder(JavaClassSource javabean);
	JavaClassSource rebuildUpdater(JavaClassSource javabean);

	List<MethodSource<JavaClassSource>> rebuildConstructors(JavaClassSource javabean);
	MethodSource<JavaClassSource> rebuildBuilderMethod(JavaClassSource javabean);
	MethodSource<JavaClassSource> rebuildUpdaterMethod(JavaClassSource javabean);
}
