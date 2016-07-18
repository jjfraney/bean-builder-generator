package org.jjflyboy.forge.javabeans;

import java.util.ArrayList;
import java.util.List;

import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.source.FieldSource;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.MethodSource;

public class JavabeanOperationsImpl implements JavabeanOperations {

	private final static String WITH_METHOD_FORMAT = "public T with%1$s(%3$s %2$s) { this.%2$s = %2$s; return (T) this; }";
	private final static String FROM_FIELD_EXAMPLE_METHOD_FORMAT = "private void set%1$sFrom(%3$s example) { %2$s = example == null ? %2$s : example; }";
	private final static String CALL_FROM_FIELD_EXAMPLE_METHOD_FORMAT = "set%1$sFrom(example.%2$s);";

	@Override
	public void addLoader(JavaClassSource javabean) {

		String extendsSuperType = null;
		if (!"java.lang.Object".equals(javabean.getSuperType())) {
			extendsSuperType = "extends " + javabean.getSuperType() + ".Loader<T>";
		} else {
			extendsSuperType = "";
		}

		JavaClassSource loader = (JavaClassSource) Roaster
				.parse("protected abstract static class Loader<T extends Loader<T>> " + extendsSuperType + "{ }");

		List<String> exampleSetters = new ArrayList<>();
		List<String> withMethods = new ArrayList<>();

		StringBuilder statementBuilder = new StringBuilder();
		for (FieldSource<JavaClassSource> field : javabean.getFields()) {
			// adds property
			loader.addField(field.toString()).setProtected();

			// adds with-methods per property
			String fieldName = field.getName();
			String captializedFieldName = capitalize(fieldName);
			String fieldType = field.getType().getName();

			String m = String.format(WITH_METHOD_FORMAT, captializedFieldName, fieldName, fieldType);
			withMethods.add(m);

			// adds field's from-by-example method
			String fromMethodDeclaration = String.format(FROM_FIELD_EXAMPLE_METHOD_FORMAT, captializedFieldName,
					fieldName, fieldType);
			exampleSetters.add(fromMethodDeclaration);

			// adds statements in from-by-example method
			statementBuilder
					.append(String.format(CALL_FROM_FIELD_EXAMPLE_METHOD_FORMAT, captializedFieldName, fieldName));
		}
		withMethods.stream().forEach(m -> loader.addMethod(m));

		MethodSource<JavaClassSource> fromMethod = loader.addMethod().setName("from").setPublic();
		fromMethod.addParameter(javabean, "example");
		fromMethod.setBody(statementBuilder.toString());

		exampleSetters.stream().forEach(m -> loader.addMethod(m));

		javabean.addNestedType(loader);
	}

	private String capitalize(String name) {
		return name.substring(0, 1).toUpperCase() + name.substring(1);
	}

}
