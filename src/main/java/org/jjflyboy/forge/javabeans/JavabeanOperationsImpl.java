package org.jjflyboy.forge.javabeans;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Generated;

import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.source.AnnotationTargetSource;
import org.jboss.forge.roaster.model.source.FieldSource;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.MemberSource;
import org.jboss.forge.roaster.model.source.MethodSource;

public class JavabeanOperationsImpl implements JavabeanOperations {

	@Override
	public JavaClassSource buildLoader(JavaClassSource javabean) {
		return rebuildLoader(javabean);
	}

	@Override
	public JavaClassSource rebuildLoader(JavaClassSource javabean) {
		JavaClassSource existingLoader = findNestedClass(javabean, "Loader");
		if (existingLoader != null) {
			if (isPreserved(existingLoader)) {
				return existingLoader;
			}
		}
		final List<FieldSource<JavaClassSource>> fields = javabean.getFields();

		final List<MethodSource<JavaClassSource>> preservedMethods = existingLoader == null ? Collections.emptyList() :
			existingLoader.getMethods().stream().filter(this::isPreserved).collect(Collectors.toList());
		final List<FieldSource<JavaClassSource>> preservedFields = existingLoader == null ? Collections.emptyList() :
			existingLoader.getFields().stream().filter(this::isPreserved).collect(Collectors.toList());

		final JavaClassSource loader = generateLoader(javabean);

		Function<FieldSource<JavaClassSource>, MemberDescriptor> fieldDescriptor = (f) -> new FieldDescriptor(f, preservedFields);
		Function<FieldSource<JavaClassSource>, MemberDescriptor> withFieldMethodDescriptor = (f) -> new WithFieldMethodDescriptor(f, preservedMethods);

		Function<FieldSource<JavaClassSource>, MemberDescriptor> fromFieldMethodDescriptor =
				(f) -> new FromFieldMethodDescriptor(f, preservedMethods);
		Function<FieldSource<JavaClassSource>, MemberDescriptor> modifyFieldMethodDescriptor =
				(f) -> new ModifyFieldMethodDescriptor(f, preservedMethods);
		Function<FieldSource<JavaClassSource>, MemberDescriptor> initializeFieldMethodDescriptor =
				(f) -> new InitializeFieldMethodDescriptor(f, preservedMethods);

		// for each ${field}, add ${field} in the loader
		fields.stream().map(fieldDescriptor).map(MemberDescriptor::asString).forEach(loader::addField);

		// for each ${field}, add with${field} method in the loader
		fields.stream().map(withFieldMethodDescriptor).map(MemberDescriptor::asString).forEach(loader::addMethod);

		// add loader.from(${javabean} example) method
		loader.addMethod(new FromMethodDescriptor(javabean, preservedMethods).asString());

		// for each ${field}, add from${field} method in the loader
		fields.stream().map(fromFieldMethodDescriptor).map(MemberDescriptor::asString).forEach(loader::addMethod);

		// add loader.modify(${javabean} target) method
		loader.addMethod(new ModifyMethodDescriptor(javabean, preservedMethods).asString());

		// for each ${field}, add modify${field} method in the loader
		fields.stream().map(modifyFieldMethodDescriptor).map(MemberDescriptor::asString).forEach(loader::addMethod);

		// add loader.initialize(${javabean} target) method
		loader.addMethod(new InitializeMethodDescriptor(javabean, preservedMethods).asString());

		// for each ${field}, add initialize${field} method in the loader
		fields.stream().map(initializeFieldMethodDescriptor).map(MemberDescriptor::asString).forEach(loader::addMethod);

		javabean.removeNestedType(existingLoader);
		return javabean.addNestedType(loader);

	}

	private interface MemberDescriptor {

		String asString();

		boolean isGenerated();
	}

	private abstract class AbstractMemberDescriptor<T extends MemberSource<JavaClassSource, ?>>
	implements MemberDescriptor {
		T existing;

		@Override
		public String asString() {
			return existing == null ? generate() : existing.toString();
		}

		@Override
		public boolean isGenerated() {
			return existing == null;
		}

		protected abstract String generate();
	}

	/**
	 * to describe the Loader's field....for each of the enclosing bean's fields
	 *
	 * @author jfraney
	 *
	 */
	private class FieldDescriptor extends AbstractMemberDescriptor<FieldSource<JavaClassSource>> {
		// the field from the enclosing bean
		private final FieldSource<JavaClassSource> field;

		FieldDescriptor(FieldSource<JavaClassSource> field, List<FieldSource<JavaClassSource>> preserved) {
			this.field = field;
			existing = preserved.stream().filter(f -> f.getName().equals(field.getName())).findFirst().orElse(null);
		}

		@Override
		protected String generate() {
			return "@Generated(" + GENERATED_ANNOTATION_VALUE + ")\n" +
					"private " + field.getType().getName() + " " + field.getName() +
					";";
		}
	}

	/**
	 * to describe methods that are not bound to fields in the enclosing javabean
	 * @author jfraney
	 *
	 */
	private abstract class MethodDescriptor extends AbstractMemberDescriptor<MethodSource<JavaClassSource>> {
		final JavaClassSource enclosure;
		public MethodDescriptor(String name, JavaClassSource enclosure, List<MethodSource<JavaClassSource>> preserved) {
			existing = preserved.stream().filter(m -> m.getName().equals(name)).findFirst().orElse(null);
			this.enclosure = enclosure;
		}
		public JavaClassSource getEnclosure() {
			return enclosure;
		}

	}
	private class FromMethodDescriptor extends MethodDescriptor {
		public FromMethodDescriptor(JavaClassSource enclosure, List<MethodSource<JavaClassSource>> preserved) {
			super("from", enclosure, preserved);
		}

		@Override
		protected String generate() {
			return "@Generated(" + GENERATED_ANNOTATION_VALUE + ")\n" +
					"public T from(" + getEnclosure().getName() +
					" example) {\n" + generateStatements() + "\n}";
		}

		private String generateStatements() {
			return getEnclosure().getFields().stream()
					.map(f -> String.format("from%1$s(example.%2$s);\n", capitalize(f.getName()), f.getName()))
					.collect(Collectors.joining());
		}
	}
	private class ModifyMethodDescriptor extends MethodDescriptor {
		public ModifyMethodDescriptor(JavaClassSource enclosure, List<MethodSource<JavaClassSource>> preserved) {
			super("modify", enclosure, preserved);
		}

		@Override
		protected String generate() {
			return "@Generated(" + GENERATED_ANNOTATION_VALUE + ")\n" +
					"public T modify(" + getEnclosure().getName() +
					" target) {\n" + generateStatements() + "\n}";
		}

		private String generateStatements() {
			return getEnclosure().getFields().stream()
					.map(f -> String.format("modify%1$s(target.%2$s);", capitalize(f.getName()), f.getName()))
					.collect(Collectors.joining());

		}
	}
	private class InitializeMethodDescriptor extends MethodDescriptor {
		public InitializeMethodDescriptor(JavaClassSource enclosure, List<MethodSource<JavaClassSource>> preserved) {
			super("initialize", enclosure, preserved);
		}

		@Override
		protected String generate() {
			return "@Generated(" + GENERATED_ANNOTATION_VALUE + ")\n" +
					"public T initialize(" + getEnclosure().getName() +
					" target) {\n" + generateStatements() + "\n}";
		}

		private String generateStatements() {
			return getEnclosure().getFields().stream()
					.map(f -> String.format("initialize%1$s(target.%2$s);", capitalize(f.getName()), f.getName()))
					.collect(Collectors.joining());

		}
	}

	/**
	 * to describe methods that are bound to the fields in the enclosing javabean
	 * @author jfraney
	 *
	 */
	private abstract class FieldMethodDescriptor extends AbstractMemberDescriptor<MethodSource<JavaClassSource>> {
		private final FieldSource<JavaClassSource> field;

		public FieldMethodDescriptor(FieldSource<JavaClassSource> field, String prefix,
				List<MethodSource<JavaClassSource>> preserved) {
			this.field = field;
			String name = prefix + capitalize(getField().getName());
			existing = preserved.stream().filter(m -> m.getName().equals(name)).findFirst().orElse(null);
		}

		public FieldSource<JavaClassSource> getField() {
			return field;
		}
	}

	private class FromFieldMethodDescriptor extends FieldMethodDescriptor {
		public FromFieldMethodDescriptor(FieldSource<JavaClassSource> field,
				List<MethodSource<JavaClassSource>> preserved) {
			super(field, "from", preserved);
		}

		@Override
		protected String generate() {
			return "@Generated(" + GENERATED_ANNOTATION_VALUE + ")\n" +
					"private void from" + capitalize(getField().getName()) + "(" +
					getField().getType().getName() + " example) { " + generateBody() + " }";
		}

		private String generateBody() {
			return String.format("%1$s = example == null ? %1$s : example;", getField().getName());
		}
	}

	private class ModifyFieldMethodDescriptor extends FieldMethodDescriptor {
		public ModifyFieldMethodDescriptor(FieldSource<JavaClassSource> field,
				List<MethodSource<JavaClassSource>> preserved) {
			super(field, "modify", preserved);
		}

		@Override
		protected String generate() {
			return "@Generated(" + GENERATED_ANNOTATION_VALUE + ")\n" +
					"private void modify" + capitalize(getField().getName()) + "(" +
					getField().getType().getName() + " target) { " +
					generateBody() +
					" }";
		}
		private String generateBody() {
			return String.format("target.%1$s = %1$s == null ? target.%1$s : %1$s;", getField().getName());
		}
	}

	private class InitializeFieldMethodDescriptor extends FieldMethodDescriptor {
		public InitializeFieldMethodDescriptor(FieldSource<JavaClassSource> field,
				List<MethodSource<JavaClassSource>> preserved) {
			super(field, "initialize", preserved);
		}

		@Override
		protected String generate() {
			return "@Generated(" + GENERATED_ANNOTATION_VALUE + ")\n" +
					"private void initialize" + capitalize(getField().getName()) + "(" +
					getField().getType().getName() + " target) { " +
					generateInitializeFieldMethodBody(getField()) + " }";
		}

		private String generateInitializeFieldMethodBody(FieldSource<JavaClassSource> field) {
			return String.format("target.%1$s = %1$s;", field.getName());
		}
	}

	private class WithFieldMethodDescriptor extends FieldMethodDescriptor {
		public WithFieldMethodDescriptor(FieldSource<JavaClassSource> field,
				List<MethodSource<JavaClassSource>> preserved) {
			super(field, "with", preserved);
		}

		@Override
		protected String generate() {
			return "@Generated(" + GENERATED_ANNOTATION_VALUE + ")\n" +
					"private T with" + capitalize(getField().getName()) + "(" +
					getField().getType().getName() + " " + getField().getName() + ") { " +
					generateBody() + " }";
		}

		private String generateBody() {
			return String.format("this.%1$s = %1$s; return (T) this;", getField().getName());
		}
	}

	private JavaClassSource generateLoader(JavaClassSource javabean) {
		return generateClass(c -> {
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
	}

	private JavaClassSource findNestedClass(JavaClassSource javabean, String name) {
		return (JavaClassSource) javabean.getNestedType(name);
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
}
