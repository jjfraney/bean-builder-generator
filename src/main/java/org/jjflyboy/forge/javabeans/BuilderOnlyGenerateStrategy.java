package org.jjflyboy.forge.javabeans;

import org.jboss.forge.roaster.model.JavaUnit;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.JavaSource;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

/**
 * @author jfraney
 */
public class BuilderOnlyGenerateStrategy implements GenerateStrategy {
	@Inject
	private JavabeanOperations operations;

	@Override
	public List<JavaClassSource> generate(JavaClassSource javabean) {
		List<JavaClassSource> result = new ArrayList<>();
		operations.rebuildLoader(javabean);
		operations.rebuildBuilder(javabean);

		operations.rebuildCtors(javabean);
		operations.rebuildBuilderMethod(javabean);

		result.add(javabean);
		return result;
	}
}
