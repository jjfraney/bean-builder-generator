package org.jjflyboy.forge.javabeans;

import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.source.FieldSource;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.MethodSource;

public class JavabeanOperationsImpl implements JavabeanOperations {

	private final static String WITH_METHOD_FORMAT = "public T with%1$s(%3$s %2$s) { this.%2$s = %2$s; return (T) this; }";

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

		for (FieldSource<JavaClassSource> field : javabean.getFields()) {
			loader.addField(field.toString()).setProtected();
			String fieldName = field.getName();
			String fieldType = field.getType().getName();
			String m = String.format(WITH_METHOD_FORMAT, capitalize(fieldName), fieldName, fieldType);
			loader.addMethod(m);
		}
		MethodSource<JavaClassSource> fromMethod = loader.addMethod().setName("from");
		fromMethod.addParameter(javabean, "example");
		javabean.addNestedType(loader);
	}

	private Object capitalize(String name) {
		return name.substring(0, 1).toUpperCase() + name.substring(1);
	}

}
