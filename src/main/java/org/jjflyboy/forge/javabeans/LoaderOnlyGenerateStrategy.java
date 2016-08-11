package org.jjflyboy.forge.javabeans;

import org.jboss.forge.roaster.model.source.JavaClassSource;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

/**
 * @author jfraney
 */
public class LoaderOnlyGenerateStrategy implements GenerateStrategy {
	@Inject
	private JavabeanOperations operations;

	@Override
	public List<JavaClassSource> generate(JavaClassSource javabean) {
		List<JavaClassSource> result = new ArrayList<>();
		operations.rebuildLoader(javabean);
		result.add(javabean);
		return result;
	}
}
