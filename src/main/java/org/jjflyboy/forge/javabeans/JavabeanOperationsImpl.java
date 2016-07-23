package org.jjflyboy.forge.javabeans;

import java.util.function.Consumer;

import javax.annotation.Generated;

import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.source.AnnotationTargetSource;
import org.jboss.forge.roaster.model.source.FieldSource;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.MethodSource;

public class JavabeanOperationsImpl implements JavabeanOperations {

	@Override
	public JavaClassSource addLoader(JavaClassSource javabean) {

		JavaClassSource existingLoader = (JavaClassSource) javabean.getNestedType("Loader");
		if (existingLoader != null) {
			if (isGenerated(existingLoader)) {
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
		addAnnotation(loader);

		for (FieldSource<JavaClassSource> field : javabean.getFields()) {
			addField(loader, field);
			addWithFieldMethod(loader, field);
		}

		MethodSource<JavaClassSource> fromMethod = addFromMethod(javabean, loader);
		for (FieldSource<JavaClassSource> field : javabean.getFields()) {
			addFromFieldMethod(loader, field);
			addFromMethodStatement(fromMethod, field);
		}

		MethodSource<JavaClassSource> modifyMethod = addModifyMethod(javabean, loader);
		for (FieldSource<JavaClassSource> field : javabean.getFields()) {
			addModifyFieldMethod(loader, field);
			addModifyMethodStatement(modifyMethod, field);
		}

		MethodSource<JavaClassSource> initMethod = addInitializeMethod(javabean, loader);
		for (FieldSource<JavaClassSource> field : javabean.getFields()) {
			addInitializeFieldMethod(loader, field);
			addInitializeMethodStatement(initMethod, field);
		}

		return javabean.addNestedType(loader);
	}

	/**
	 * add loader.initialize(beantype) method.
	 *
	 * @param javabean
	 * @param loader
	 * @return
	 */
	private MethodSource<JavaClassSource> addInitializeMethod(JavaClassSource javabean, JavaClassSource loader) {
		return generateMethod(loader, (m) -> {
			m.setName("initialize").setProtected().addParameter(javabean, "target");
		});
	}

	/**
	 * add loader.modify(beantype) method.
	 *
	 * @param javabean
	 * @param loader
	 * @return
	 */
	private MethodSource<JavaClassSource> addModifyMethod(JavaClassSource javabean, JavaClassSource loader) {
		return generateMethod(loader, (m) -> {
			m.setName("modify").setProtected().addParameter(javabean, "target");
		});
	}

	/**
	 * add loader.from method. This method is passed an example of the same
	 * javabean.
	 *
	 * @param javabean
	 * @param loader
	 * @return
	 */
	private MethodSource<JavaClassSource> addFromMethod(JavaClassSource javabean, JavaClassSource loader) {
		return generateMethod(loader, (m) -> {
			m.setName("from").setPublic().addParameter(javabean, "example");
		});
	}

	/**
	 * adds a statement for the field to the loader's from method;
	 * @param fromMethod
	 * @param field
	 */
	private void addFromMethodStatement(MethodSource<JavaClassSource> fromMethod, FieldSource<JavaClassSource> field) {
		String statement = String.format("from%1$s(example.%2$s);", capitalize(field.getName()), field.getName());
		fromMethod.setBody(fromMethod.getBody() == null ? statement : fromMethod.getBody() + statement);
	}

	private void addInitializeMethodStatement(MethodSource<JavaClassSource> initMethod,
			FieldSource<JavaClassSource> field) {
		String statement = String.format("initialize%1$s(target.%2$s);", capitalize(field.getName()), field.getName());
		initMethod.setBody(initMethod.getBody() == null ? statement : initMethod.getBody() + statement);
	}

	private void addModifyMethodStatement(MethodSource<JavaClassSource> modifyMethod,
			FieldSource<JavaClassSource> field) {
		String statement = String.format("modify%1$s(target.%2$s);", capitalize(field.getName()), field.getName());
		modifyMethod.setBody(modifyMethod.getBody() == null ? statement : modifyMethod.getBody() + statement);
	}

	/**
	 * add loader.from${field}() method where '${field}' is the name of a field
	 * in the javabean.
	 *
	 * @param loader
	 * @param field
	 * @return
	 */
	private MethodSource<JavaClassSource> addFromFieldMethod(JavaClassSource loader, FieldSource<JavaClassSource> field) {
		return generateMethod(loader, (m) -> {
			m.setPrivate()
			.setName("from" + capitalize(field.getName()))
			.setParameters(field.getType().getName() + " example")
			.setReturnType("void")
			.setBody(generateFromFieldMethodBody(field));
		});
	}

	private String generateFromFieldMethodBody(FieldSource<JavaClassSource> field) {
		String body = String.format("%2$s = example == null ? %2$s : example;", capitalize(field.getName()),
				field.getName(), field.getType().getName());

		return body;
	}

	/**
	 * add loader.initialize${field} method
	 *
	 * @param loader
	 * @param field
	 * @return
	 */
	private MethodSource<JavaClassSource> addInitializeFieldMethod(JavaClassSource loader,
			FieldSource<JavaClassSource> field) {
		return generateMethod(loader, (m) -> {
			m.setPrivate().setName("initialize" + capitalize(field.getName()))
			.setParameters(field.getType().getName() + " target").setReturnType("void")
					.setBody(generateInitializeFieldMethodBody(field));
		});

	}

	private String generateInitializeFieldMethodBody(FieldSource<JavaClassSource> field) {
		return String.format("target.%2$s = %2$s;", capitalize(field.getName()), field.getName(),
				field.getType().getName());
	}

	/**
	 * add loader.modify${field} method
	 *
	 * @param loader
	 * @param field
	 * @return
	 */
	private MethodSource<JavaClassSource> addModifyFieldMethod(JavaClassSource loader,
			FieldSource<JavaClassSource> field) {
		return generateMethod(loader, (m) -> {
			m.setPrivate().setName("modify" + capitalize(field.getName()))
			.setParameters(field.getType().getName() + " target").setReturnType("void")
					.setBody(generateModifyFieldMethodBody(field));
		});

	}

	private String generateModifyFieldMethodBody(FieldSource<JavaClassSource> field) {
		return String.format("target.%2$s = %2$s == null ? target.%2$s : %2$s;", capitalize(field.getName()),
				field.getName(), field.getType().getName());
	}

	/**
	 * add method loader.with${field}
	 *
	 * @param loader
	 * @param field
	 * @return
	 */
	private MethodSource<JavaClassSource> addWithFieldMethod(JavaClassSource loader,
			FieldSource<JavaClassSource> field) {
		return generateMethod(loader, (m) -> {
			m.setName("with" + capitalize(field.getName()));
			m.setParameters(field.getType().getName() + " " + field.getName());
			m.setReturnType("T");
			m.setPublic();
			String body = String.format("this.%2$s = %2$s; return (T) this;", capitalize(field.getName()),
					field.getName(), field.getType().getName());
			m.setBody(body);
		});
	}

	/**
	 * add loader.${field}.
	 *
	 * @param loader
	 * @param field
	 * @return
	 */
	private FieldSource<JavaClassSource> addField(JavaClassSource loader, FieldSource<JavaClassSource> field) {
		return generateField(loader, (f) -> {
			f.setName(field.getName()).setType(field.getType().getName()).setProtected();
		});
	}

	private String capitalize(String name) {
		return name.substring(0, 1).toUpperCase() + name.substring(1);
	}

	private boolean isGenerated(AnnotationTargetSource<JavaClassSource, ?> at) {
		return at.getAnnotation(Generated.class) != null;
	}

	private void addAnnotation(AnnotationTargetSource<JavaClassSource, ?> at) {
		at.addAnnotation(Generated.class).setLiteralValue(GENERATED_ANNOTATION_VALUE);
	}

	private MethodSource<JavaClassSource> generateMethod(JavaClassSource container,
			Consumer<MethodSource<JavaClassSource>> c) {
		MethodSource<JavaClassSource> source = container.addMethod();
		addAnnotation(source);
		c.accept(source);
		return source;
	}

	private FieldSource<JavaClassSource> generateField(JavaClassSource container,
			Consumer<FieldSource<JavaClassSource>> c) {
		FieldSource<JavaClassSource> source = container.addField();
		addAnnotation(source);
		c.accept(source);
		return source;

	}

}
