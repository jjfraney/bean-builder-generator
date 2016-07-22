package org.jjflyboy.forge.javabeans;

import javax.annotation.Generated;

import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.source.FieldSource;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.MethodSource;

public class JavabeanOperationsImpl implements JavabeanOperations {

	private final static String WITH_METHOD_FORMAT = "public T with%1$s(%3$s %2$s) { this.%2$s = %2$s; return (T) this; }";
	private final static String FROM_FIELD_EXAMPLE_METHOD_FORMAT = "private void set%1$sFrom(%3$s example) { %2$s = example == null ? %2$s : example; }";
	private final static String CALL_FROM_FIELD_EXAMPLE_METHOD_FORMAT = "set%1$sFrom(example.%2$s);";

	@Override
	public JavaClassSource addLoader(JavaClassSource javabean) {

		JavaClassSource existingLoader = (JavaClassSource) javabean.getNestedType("Loader");
		if (existingLoader != null) {
			if (existingLoader.getAnnotation(Generated.class) == null) {
				return existingLoader;
			}
			javabean.removeNestedType(existingLoader);
		}

		String extendsSuperType = null;
		if (!"java.lang.Object".equals(javabean.getSuperType())) {
			extendsSuperType = "extends " + javabean.getSuperType() + ".Loader<T>";
		} else {
			extendsSuperType = "";
		}

		JavaClassSource loader = (JavaClassSource) Roaster
				.parse("protected abstract static class Loader<T extends Loader<T>> " + extendsSuperType + "{ }");
		loader.addAnnotation(Generated.class).setLiteralValue(GENERATED_ANNOTATION_VALUE);

		MethodSource<JavaClassSource> fromMethod = addFromMethod(javabean, loader);
		for (FieldSource<JavaClassSource> field : javabean.getFields()) {
			addField(loader, field);

			addWithFieldMethod(loader, field);

			addFromFieldMethod(loader, field);
			addFromMethodStatement(fromMethod, field);
		}

		return javabean.addNestedType(loader);
	}

	private MethodSource<JavaClassSource> addFromMethod(JavaClassSource javabean, JavaClassSource loader) {
		MethodSource<JavaClassSource> fromMethod = loader.addMethod().setName("from").setPublic();
		fromMethod.addParameter(javabean, "example");
		return fromMethod;
	}

	private void addFromMethodStatement(MethodSource<JavaClassSource> fromMethod, FieldSource<JavaClassSource> field) {
		String statement = createFromMethodStatement(field);
		String body = fromMethod.getBody();
		if (body == null) {
			fromMethod.setBody(statement);
		} else {
			fromMethod.setBody(fromMethod.getBody() + statement);
		}
	}

	private String createFromMethodStatement(FieldSource<JavaClassSource> field) {
		String captializedFieldName = capitalize(field.getName());
		// adds statements in from-by-example method
		String statement = String.format(CALL_FROM_FIELD_EXAMPLE_METHOD_FORMAT, captializedFieldName,
				field.getName());
		return statement;
	}

	private MethodSource<JavaClassSource> addFromFieldMethod(JavaClassSource loader,
			FieldSource<JavaClassSource> field) {
		String fromMethodDeclaration = String.format(FROM_FIELD_EXAMPLE_METHOD_FORMAT, capitalize(field.getName()),
				field.getName(), field.getType().getName());
		return loader.addMethod(fromMethodDeclaration);
	}

	private MethodSource<JavaClassSource> addWithFieldMethod(JavaClassSource loader,
			FieldSource<JavaClassSource> field) {
		String m = String.format(WITH_METHOD_FORMAT, capitalize(field.getName()), field.getName(), field.getType().getName());
		return loader.addMethod(m);
	}

	private FieldSource<JavaClassSource> addField(JavaClassSource loader, FieldSource<JavaClassSource> field) {
		return loader.addField(field.toString()).setProtected();
	}

	private String capitalize(String name) {
		return name.substring(0, 1).toUpperCase() + name.substring(1);
	}

}
