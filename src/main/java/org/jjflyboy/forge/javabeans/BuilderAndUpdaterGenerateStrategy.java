package org.jjflyboy.forge.javabeans;

import org.jboss.forge.roaster.model.source.JavaClassSource;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

/**
 * @author jfraney
 */
public class BuilderAndUpdaterGenerateStrategy implements GenerateStrategy {
	@Inject
	private JavabeanOperations operations;

	@Override
	public List<JavaClassSource> generate(JavaClassSource javabean) {
		List<JavaClassSource> result = new ArrayList<>();
		operations.rebuildLoader(javabean);
		operations.rebuildBuilder(javabean);
		operations.rebuildUpdater(javabean);

		operations.rebuildCtors(javabean);
		operations.rebuildBuilderMethod(javabean);
		operations.rebuildUpdaterMethod(javabean);

		result.add(javabean);
		return result;
	}
}
