package org.jjflyboy.forge.javabeans;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.annotation.Generated;

import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.source.AnnotationTargetSource;
import org.jboss.forge.roaster.model.source.FieldSource;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.MethodSource;

public class JavabeanOperationsImpl implements JavabeanOperations {

	@Override
	public JavaClassSource buildLoader(JavaClassSource javabean) {

		JavaClassSource existingLoader = findNestedClass(javabean, "Loader");
		if (existingLoader != null) {
			return rebuildLoader(javabean);
		}

		JavaClassSource loader = generateLoader(javabean);
		javabean.getFields().stream().forEach(f -> defineField(loader, f));
		javabean.getFields().stream().forEach(f -> defineWithMethod(loader, f));


		MethodSource<JavaClassSource> fromMethod = addFromMethod(javabean, loader);
		javabean.getFields().stream().forEach(f -> defineFromFieldMethod(loader, f));
		javabean.getFields().stream().forEach(f -> addFromMethodStatement(fromMethod, f));


		MethodSource<JavaClassSource> modifyMethod = addModifyMethod(javabean, loader);
		javabean.getFields().stream().forEach(f -> defineModifyFieldMethod(loader, f));
		javabean.getFields().stream().forEach(f -> addModifyMethodStatement(modifyMethod, f));

		MethodSource<JavaClassSource> initMethod = addInitializeMethod(javabean, loader);
		javabean.getFields().stream().forEach(f -> defineInitializeFieldMethod(loader, f));
		javabean.getFields().stream().forEach(f -> addInitializeMethodStatement(initMethod, f));

		return javabean.addNestedType(loader);
	}

	@Override
	public JavaClassSource rebuildLoader(JavaClassSource javabean) {
		JavaClassSource existingLoader = findNestedClass(javabean, "Loader");
		if (existingLoader == null) {
			return buildLoader(javabean);
		} else if (isPreserved(existingLoader)) {
			return existingLoader;
		}
		List<FieldSource<JavaClassSource>> fields = javabean.getFields();

		JavaClassSource loader = generateLoader(javabean);
		fields.stream().forEach(field -> preserveField(existingLoader, loader, field));
		fields.stream().forEach(field -> preserveWithMethod(existingLoader, loader, field));

		MethodSource<JavaClassSource> fromMethod = preserveMethod(existingLoader,
				loader, () -> addFromMethod(javabean, loader), "from", javabean.getName());
		fields.stream().forEach(field -> preserveFromFieldMethod(existingLoader, loader, field));
		if (isGenerated(fromMethod)) {
			fields.stream().forEach(f -> addFromMethodStatement(fromMethod, f));
		}

		MethodSource<JavaClassSource> modifyMethod = preserveMethod(existingLoader,
				loader, () -> addModifyMethod(javabean, loader), "modify", javabean.getName());
		fields.stream().forEach(f -> preserveModifyFieldMethod(existingLoader, loader, f));
		if (isGenerated(modifyMethod)) {
			fields.stream().forEach(f -> addModifyMethodStatement(modifyMethod, f));
		}

		MethodSource<JavaClassSource> initMethod = preserveMethod(existingLoader,
				loader, () -> addInitializeMethod(javabean, loader), "initialize", javabean.getName());
		fields.stream().forEach(f -> preserveInitializeFieldMethod(existingLoader, loader, f));
		if (isGenerated(initMethod)) {
			fields.stream().forEach(f -> addInitializeMethodStatement(initMethod, f));
		}

		javabean.removeNestedType(existingLoader);
		return javabean.addNestedType(loader);

	}

	private MethodSource<JavaClassSource> preserveMethod(JavaClassSource oldLoader, JavaClassSource newLoader, Supplier<MethodSource<JavaClassSource>> maker, String name, String ... parameterTypes) {
		MethodSource<JavaClassSource> old = oldLoader.getMethod(name, parameterTypes);
		if(old == null || isGenerated(old)) {
			return maker.get();
		} else {
			return newLoader.addMethod(old.toString());
		}
	}

	private MethodSource<JavaClassSource> preserveMethodField(JavaClassSource oldLoader,
			JavaClassSource newLoader, Supplier<MethodSource<JavaClassSource>> maker, String prefix, FieldSource<JavaClassSource> field) {
		String name = prefix + capitalize(field.getName());
		return preserveMethod(oldLoader, newLoader, maker, name, field.getType().getName());
	}

	private MethodSource<JavaClassSource> preserveInitializeFieldMethod(JavaClassSource existingLoader,
			JavaClassSource loader,
			FieldSource<JavaClassSource> field) {
		return preserveMethodField(existingLoader, loader, () -> defineInitializeFieldMethod(loader, field),
				"initialize", field);

	}

	private MethodSource<JavaClassSource> preserveModifyFieldMethod(JavaClassSource existingLoader,
			JavaClassSource loader,
			FieldSource<JavaClassSource> field) {
		return preserveMethodField(existingLoader, loader, () -> defineModifyFieldMethod(loader, field), "modify",
				field);

	}

	private MethodSource<JavaClassSource> preserveFromFieldMethod(JavaClassSource existingLoader,
			JavaClassSource loader,
			FieldSource<JavaClassSource> field) {
		return preserveMethodField(existingLoader, loader, () -> defineFromFieldMethod(loader, field), "from", field);
	}

	private MethodSource<JavaClassSource> preserveWithMethod(JavaClassSource existingLoader, JavaClassSource newLoader,
			FieldSource<JavaClassSource> field) {
		return preserveMethodField(existingLoader, newLoader, () -> defineWithMethod(newLoader, field), "with", field);
	}

	private void preserveField(JavaClassSource oldLoader, JavaClassSource newLoader,
			FieldSource<JavaClassSource> field) {
		FieldSource<JavaClassSource> oldField = oldLoader.getField(field.getName());
		if (oldField == null || isGenerated(oldField)) {
			defineField(newLoader, field);
		} else {
			newLoader.addField(oldField.toString());
		}
	}

	private JavaClassSource generateLoader(JavaClassSource javabean) {
		JavaClassSource loader = generateClass(c -> {
			String extendsSuperType = null;
			if (!"java.lang.Object".equals(javabean.getSuperType())) {
				extendsSuperType = javabean.getSuperType() + ".Loader<T>";
			}

			c.setAbstract(true).setProtected().setStatic(true).setName("Loader");
			if (extendsSuperType != null) {
				c.setSuperType(extendsSuperType);
			}
			c.addTypeVariable().setName("T").setBounds("Loader<T>");
		});
		return loader;
	}


	private JavaClassSource findNestedClass(JavaClassSource javabean, String name) {
		return (JavaClassSource) javabean.getNestedType(name);
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
			m.setName("initialize").setProtected().setBody("").addParameter(javabean, "target");
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
			m.setName("modify").setProtected().setBody("").addParameter(javabean, "target");
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
			m.setName("from")
			.setPublic()
			.setReturnType("T")
			.addParameter(javabean, "example");
		});
	}

	/**
	 * adds a statement for the field to the loader's from method;
	 * @param fromMethod
	 * @param field
	 */
	private void addFromMethodStatement(MethodSource<JavaClassSource> fromMethod, FieldSource<JavaClassSource> field) {
		String statement = defineFromMethodStatement(field);
		fromMethod.setBody(fromMethod.getBody() == null ? statement : fromMethod.getBody() + statement);
	}

	private String defineFromMethodStatement(FieldSource<JavaClassSource> field) {
		return String.format("from%1$s(example.%2$s);", capitalize(field.getName()), field.getName());
	}

	private void addInitializeMethodStatement(MethodSource<JavaClassSource> initMethod,
			FieldSource<JavaClassSource> field) {
		String statement = defineIntializeMethodStatement(field);
		initMethod.setBody(initMethod.getBody() == null ? statement : initMethod.getBody() + statement);
	}

	private String defineIntializeMethodStatement(FieldSource<JavaClassSource> field) {
		return String.format("initialize%1$s(target.%2$s);", capitalize(field.getName()), field.getName());
	}

	private void addModifyMethodStatement(MethodSource<JavaClassSource> modifyMethod,
			FieldSource<JavaClassSource> field) {
		String statement = defineModifyMethodStatement(field);
		modifyMethod.setBody(modifyMethod.getBody() == null ? statement : modifyMethod.getBody() + statement);
	}

	private String defineModifyMethodStatement(FieldSource<JavaClassSource> field) {
		return String.format("modify%1$s(target.%2$s);", capitalize(field.getName()), field.getName());
	}

	/**
	 * add loader.from${field}() method where '${field}' is the name of a field
	 * in the javabean.
	 *
	 * @param loader
	 * @param field
	 * @return
	 */
	private MethodSource<JavaClassSource> defineFromFieldMethod(JavaClassSource loader,
			FieldSource<JavaClassSource> field) {
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
	private MethodSource<JavaClassSource> defineInitializeFieldMethod(JavaClassSource loader,
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
	private MethodSource<JavaClassSource> defineModifyFieldMethod(JavaClassSource loader,
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
	private MethodSource<JavaClassSource> defineWithMethod(JavaClassSource loader,
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
	private FieldSource<JavaClassSource> defineField(JavaClassSource loader, FieldSource<JavaClassSource> field) {
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

	private boolean isPreserved(AnnotationTargetSource<JavaClassSource, ?> at) {
		return !isGenerated(at);
	}

	private void addAnnotation(AnnotationTargetSource<JavaClassSource, ?> at) {
		at.addAnnotation(Generated.class).setLiteralValue(GENERATED_ANNOTATION_VALUE);
	}

	private JavaClassSource generateClass(Consumer<JavaClassSource> c) {
		JavaClassSource source = Roaster.create(JavaClassSource.class);
		c.accept(source);
		addAnnotation(source);
		return source;
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
