package org.jjflyboy.forge.javabeans;

import org.jboss.forge.roaster.model.source.JavaClassSource;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

/**
 * @author jfraney
 */
@SuppressWarnings("unused")
public class BuilderOnlyGenerateStrategy implements GenerateStrategy {
	@SuppressWarnings("CanBeFinal")
	@Inject
	private JavabeanOperations operations;

	@Override
	public List<JavaClassSource> generate(JavaClassSource javabean) {
		List<JavaClassSource> result = new ArrayList<>();
		operations.rebuildLoader(javabean);
		operations.rebuildBuilder(javabean);

		operations.rebuildConstructors(javabean);
		operations.rebuildBuilderMethod(javabean);

		result.add(javabean);
		return result;
	}
}
