package org.jjflyboy.forge.javabeans;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.annotation.Generated;

import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.source.*;

public class JavabeanOperationsImpl implements JavabeanOperations {


	@Override
	public List<MethodSource<JavaClassSource>> rebuildConstructors(JavaClassSource javabean) {

		// Builder needs a default constructor...only.

		List<MethodSource<JavaClassSource>> result = new ArrayList<>();

		List<MethodSource<JavaClassSource>> allConstructors = javabean.getMethods()
				.stream()
				.filter(f -> f.getName().equals(javabean.getName()))
				.collect(Collectors.toList());

		// if any constructors exist, we may need to generate one.
		if(allConstructors.size() > 0) {
			MethodSource<JavaClassSource> defaultConstructor = allConstructors
					.stream()
					.filter(f -> f.getParameters().size() == 0)
					.findFirst()
					.orElse(null);

			// if there is not a default constructor, or it had been pre-generated.
			if (defaultConstructor == null || !isPreserved(defaultConstructor)) {
				if (defaultConstructor != null) {
					javabean.removeMethod(defaultConstructor);
				}

				// generate a default ctor...private to be polite.
				String declarationFormat = "@Generated(" + GENERATED_ANNOTATION_VALUE + ")\n" +
						"private ${javabean.name}() {}";
				String d = declarationFormat.replace("${javabean.name}", javabean.getName());

				MethodSource<JavaClassSource> method = javabean.addMethod(d);
				result.add(method);
			}
		}

		return result;
	}

	@Override
	public MethodSource<JavaClassSource> rebuildBuilderMethod(JavaClassSource javabean) {
		return rebuildMaker(javabean, "builder");
	}

	@Override
	public MethodSource<JavaClassSource> rebuildUpdaterMethod(JavaClassSource javabean) {
		return rebuildMaker(javabean, "updater");
	}
	private MethodSource<JavaClassSource> rebuildMaker(JavaClassSource javabean, String name) {
		MethodSource<JavaClassSource> result;
		String typeName = capitalize(name);
		MethodSource<JavaClassSource> method = javabean.getMethod(name);
		if(method == null || !isPreserved(method)) {
			if(method != null) {
				javabean.removeMethod(method);
			}
			String declarationFormat = "@Generated(" + GENERATED_ANNOTATION_VALUE + ")\n" +
					"public static ${type.name} ${name}() { return new ${type.name}();}";
			String d = declarationFormat.replace("${type.name}", typeName).replace("${name}", name);

			result = javabean.addMethod(d);
		} else {
			result = method;
		}
		return result;
	}

	@Override
	public JavaClassSource rebuildLoader(JavaClassSource javabean) {
		return generateNestedClass(javabean, "Loader", this::generateLoader);
	}

	@Override
	public JavaClassSource rebuildBuilder(JavaClassSource javabean) {
		return generateNestedClass(javabean, "Builder", this::generateBuilder);
	}

	@Override
	public JavaClassSource rebuildUpdater(JavaClassSource javabean) {
		return generateNestedClass(javabean, "Updater", this::generateUpdater);
	}


	private JavaClassSource generateNestedClass(JavaClassSource javabean, String name, BiFunction<JavaClassSource, JavaClassSource, JavaClassSource> generator) {
		JavaClassSource existingNestedClass = findNestedClass(javabean, name);
		if (existingNestedClass != null) {
			if (isPreserved(existingNestedClass)) {
				return existingNestedClass;
			}
		}

		JavaClassSource newNestedClass =  generator.apply(javabean, existingNestedClass);
		javabean.removeNestedType(existingNestedClass);
		javabean.addNestedType(newNestedClass);
		javabean.addImport(Generated.class);
		return newNestedClass;
	}

	private interface MemberDescriptor {

		String asString();
	}

	private abstract class AbstractMemberDescriptor<T extends MemberSource<JavaClassSource, ?>>
	implements MemberDescriptor {
		T existing;

		@Override
		public String asString() {
			return existing == null ? generate() : existing.toString();
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
		final List<MethodSource<JavaClassSource>> preserved;
		final String name;
		MethodDescriptor(String name, JavaClassSource enclosure, List<MethodSource<JavaClassSource>> preserved) {
			existing = preserved.stream().filter(m -> m.getName().equals(name)).findFirst().orElse(null);
			this.enclosure = enclosure;
			this.preserved = preserved;
			this.name = name;
		}
		JavaClassSource getEnclosure() {
			return enclosure;
		}
		List<MethodSource<JavaClassSource>> getPreserved() {
			return preserved;
		}

		/**
		 * get the field method from the preserved list, or null.  The field method is named ${name}${field.name} and
		 * has a single parameter of the target bean's type.
		 * @param field the field related to the desired method
		 * @return method for the field
		 */
		MethodSource<JavaClassSource> getPreservedFieldMethod(FieldSource<JavaClassSource> field) {
			return getPreserved().stream()
					.filter(m -> isFieldMethod(m, field))
					.findAny().orElse(null);
		}
		private boolean isFieldMethod(MethodSource<JavaClassSource> m, FieldSource<JavaClassSource> field) {
			String methodName = name + capitalize(field.getName());
			return 	m.getName().equals(methodName)
					&& m.getParameters().size() == 1
					&& m.getParameters().get(0).getType().getName().equals(enclosure.getName());
		}

		/**
		 * return a generated statement, using the call descriptor to call a preserved method (if any) or
		 * the inline descriptor
		 * @param field related field for statement generation
		 * @param inlineDesc inline statement descriptor
		 * @param callDesc method call descriptor
		 * @return a string with java statement
		 */

		String generateStatement(FieldSource<JavaClassSource> field, String inlineDesc, String callDesc) {
			MethodSource<JavaClassSource> method = getPreservedFieldMethod(field);
			return method == null ? inlineDesc.replace("${field.name}", field.getName())
					: callDesc.replace("${method.name}", method.getName());
		}
	}
	private class FromMethodDescriptor extends MethodDescriptor {
		FromMethodDescriptor(JavaClassSource enclosure, List<MethodSource<JavaClassSource>> preserved) {
			super("from", enclosure, preserved);
		}

		@Override
		protected String generate() {
			String superCall = hasSuperType(getEnclosure()) ? "super.from(example);\n" : "";
			return "@Generated(" + GENERATED_ANNOTATION_VALUE + ")\n" +
					"public T from(" + getEnclosure().getName() +
					" example) {\n" +
					superCall +
					generateStatements() +
					"return (T) this;\n}";
		}

		private String generateStatements() {
			return getEnclosure().getFields().stream()
					.filter(isProperty)
					.map(this::generateStatement)
					.collect(Collectors.joining());
		}

		private final String STATEMENT_CALL_METHOD_DESC = "${method.name}(example);";
		private final String STATEMENT_INLINE_DESC = "this.${field.name} = example.${field.name} == null ? " +
				"this.${field.name} : example.${field.name};";

		private String generateStatement(FieldSource<JavaClassSource> field) {
			return generateStatement(field, STATEMENT_INLINE_DESC, STATEMENT_CALL_METHOD_DESC);
		}

	}

	private class ModifyMethodDescriptor extends MethodDescriptor {
		ModifyMethodDescriptor(JavaClassSource enclosure, List<MethodSource<JavaClassSource>> preserved) {
			super("modify", enclosure, preserved);
		}

		private final String DESC = "@Generated(" + GENERATED_ANNOTATION_VALUE + ")" +
				"public ${javabean.name} modify(${javabean.name} target) {" +
				"${statements}" +
				"return target;}";

		@Override
		protected String generate() {
			String superCall = hasSuperType(getEnclosure()) ? "super.modify(target);" : "";
			String statements = superCall + generateStatements();
			return DESC.replace("${javabean.name}", getEnclosure().getName()).replace("${statements}", statements);
		}

		private String generateStatements() {
			return getEnclosure().getFields().stream()
					.filter(isProperty)
					.map(this::generateStatement)
					.collect(Collectors.joining());
		}

		private final String STATEMENT_CALL_METHOD_DESC = "${method.name}(target);";
		private final String STATEMENT_INLINE_DESC = "target.${field.name} = " +
				"this.${field.name} == null ? target.${field.name} : this.${field.name};";


		private String generateStatement(FieldSource<JavaClassSource> field) {
			return generateStatement(field, STATEMENT_INLINE_DESC, STATEMENT_CALL_METHOD_DESC);
		}

	}
	private class InitializeMethodDescriptor extends MethodDescriptor {
		InitializeMethodDescriptor(JavaClassSource enclosure, List<MethodSource<JavaClassSource>> preserved) {
			super("initialize", enclosure, preserved);
		}

		private final String DESC = "@Generated(" + GENERATED_ANNOTATION_VALUE + ")" +
				"public ${javabean.name} initialize(${javabean.name} target) {" +
				"${statements}" +
				"return target;}";

		@Override
		protected String generate() {
			String superCall = hasSuperType(getEnclosure()) ? "super.initialize(target);" : "";
			String statements = superCall + generateStatements();
			return DESC.replace("${javabean.name}", getEnclosure().getName()).replace("${statements}", statements);
		}

		private String generateStatements() {
			return getEnclosure().getFields().stream()
					.filter(isProperty)
					.map(this::generateStatement)
					.collect(Collectors.joining());

		}
		private final String STATEMENT_CALL_METHOD_DESC = "${method.name}(target);";
		private final String STATEMENT_INLINE_DESC = "target.${field.name} = this.${field.name};";


		private String generateStatement(FieldSource<JavaClassSource> field) {
			return generateStatement(field, STATEMENT_INLINE_DESC, STATEMENT_CALL_METHOD_DESC);
		}
	}

	/**
	 * to describe methods that are bound to the fields in the enclosing javabean
	 * @author jfraney
	 *
	 */
	private abstract class FieldMethodDescriptor extends AbstractMemberDescriptor<MethodSource<JavaClassSource>> {
		private final FieldSource<JavaClassSource> field;

		FieldMethodDescriptor(FieldSource<JavaClassSource> field, String prefix,
							  List<MethodSource<JavaClassSource>> preserved) {
			this.field = field;
			String name = prefix + capitalize(getField().getName());
			existing = preserved.stream().filter(m -> m.getName().equals(name)).findFirst().orElse(null);
		}

		FieldSource<JavaClassSource> getField() {
			return field;
		}
	}

	@SuppressWarnings("unused")
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

	@SuppressWarnings("unused")
	private class ModifyFieldMethodDescriptor extends FieldMethodDescriptor {
		public ModifyFieldMethodDescriptor(FieldSource<JavaClassSource> field,
				List<MethodSource<JavaClassSource>> preserved) {
			super(field, "modify", preserved);
		}

		private final String DESC = "@Generated(" + GENERATED_ANNOTATION_VALUE + ")" +
				"private void modify${field.name?cap_first}(${javabean.name} target)" +
				"target.${field.name} = this.${field.name} == null ? target.${field.name} : this.${field.name};" +
				"}";
		@Override
		protected String generate() {
			// poor man's templates  :)
			return DESC.replace("${field.name?cap_first}", capitalize(getField().getName()))
					.replace("${field.name}", getField().getName())
					.replace("${javabean.name}", getField().getOrigin().getName());
		}
	}

	@SuppressWarnings("unused")
	private class InitializeFieldMethodDescriptor extends FieldMethodDescriptor {
		public InitializeFieldMethodDescriptor(FieldSource<JavaClassSource> field,
				List<MethodSource<JavaClassSource>> preserved) {
			super(field, "initialize", preserved);
		}

		@Override
		protected String generate() {
			return "@Generated(" + GENERATED_ANNOTATION_VALUE + ")\n" +
					"private void initialize" + capitalize(getField().getName()) + "(" +
					getField().getOrigin().getName() + " target) { " +
					generateInitializeFieldMethodBody(getField()) + " }";
		}

		private String generateInitializeFieldMethodBody(FieldSource<JavaClassSource> field) {
			return String.format("target.%1$s = %1$s;", field.getName());
		}
	}

	private class WithFieldMethodDescriptor extends FieldMethodDescriptor {
		WithFieldMethodDescriptor(FieldSource<JavaClassSource> field,
								  List<MethodSource<JavaClassSource>> preserved) {
			super(field, "with", preserved);
		}

		@Override
		protected String generate() {
			return "@Generated(" + GENERATED_ANNOTATION_VALUE + ")\n" +
					"public T with" + capitalize(getField().getName()) + "(" +
					getField().getType().getName() + " " + getField().getName() + ") { " +
					generateBody() + " }";
		}

		private String generateBody() {
			return String.format("this.%1$s = %1$s; return (T) this;", getField().getName());
		}
	}

	// we have to compensate for current Roaster behavior: setSuperType results in simple name
	private static final String SUPERTYPE_HOLDER = "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX";

	private JavaClassSource generateLoader(JavaClassSource javabean, JavaClassSource existingLoader) {

		JavaClassSource loader = generateClass(c -> {
			c.setAbstract(true).setProtected().setStatic(true).setName("Loader");
			c.addTypeVariable().setName("T").setBounds("Loader<T>");

			if (hasSuperType(javabean)) {
				// replaced before finalizing the Loader
				c.setSuperType(SUPERTYPE_HOLDER);
			}
		});
		final List<MethodSource<JavaClassSource>> preservedMethods = existingLoader == null ? Collections.emptyList() :
				existingLoader.getMethods().stream().filter(this::isPreserved).collect(Collectors.toList());
		final List<FieldSource<JavaClassSource>> preservedFields = existingLoader == null ? Collections.emptyList() :
				existingLoader.getFields().stream().filter(this::isPreserved).collect(Collectors.toList());

		final List<FieldSource<JavaClassSource>> genFields = javabean.getFields()
				.stream()
				.filter(isProperty)
				.filter(f -> ! preservedFields.contains(f))
				.collect(Collectors.toList());

		Function<FieldSource<JavaClassSource>, MemberDescriptor> fieldDescriptor = (f) -> new FieldDescriptor(f, preservedFields);
		Function<FieldSource<JavaClassSource>, MemberDescriptor> withFieldMethodDescriptor = (f) -> new WithFieldMethodDescriptor(f, preservedMethods);

		// copy all preserved fields to new loader
		preservedFields.stream().map(Object::toString).forEach(loader::addField);

		// for each non-preserved ${field}, generate and add ${field} to the loader
		genFields.stream().map(fieldDescriptor).map(MemberDescriptor::asString).forEach(loader::addField);

		// copy all preserved methods to new loader
		preservedMethods.stream().map(Object::toString).forEach(loader::addMethod);

		// add loader.from(${javabean} example) method
		loader.addMethod(new FromMethodDescriptor(javabean, preservedMethods).asString());

		// add loader.modify(${javabean} target) method
		loader.addMethod(new ModifyMethodDescriptor(javabean, preservedMethods).asString());

		// add loader.initialize(${javabean} target) method
		loader.addMethod(new InitializeMethodDescriptor(javabean, preservedMethods).asString());


		// for each ${field}, generate and add with${field} method to the new loader
		genFields.stream().map(withFieldMethodDescriptor).map(MemberDescriptor::asString).forEach(loader::addMethod);

		// we want supertype: ${javabean.name}.Loader<T>
		// roaster's setSuperType(${javabean.name}.Loader<T>) gives supertype of "Loader<T>", WRONG.
		// roaster's parse from string gives what we want.
		// a better workaround then this?  A redesign?
		String superType = loader.getSuperType();
		if(superType.contains(SUPERTYPE_HOLDER)) {
			// roaster's parse from string CAN handle a nested supertype
			String p = javabean.getPackage() + ".";
			String desiredSupertype = javabean.getSuperType().replace(p, "") + ".Loader<T>";

			String asString = loader.toString();
			asString = asString.replace(SUPERTYPE_HOLDER, desiredSupertype);
			loader = Roaster.parse(JavaClassSource.class, asString);
		}
		return loader;
	}


	/**
	 * true if the the field can be identified as input for generation.
	 */
	private final Predicate<FieldSource<JavaClassSource>> isProperty = f -> ! f.isStatic();

	private JavaClassSource findNestedClass(JavaClassSource javabean, String name) {
		return (JavaClassSource) javabean.getNestedType(name);
	}

	private JavaClassSource generateBuilder(JavaClassSource javabean, @SuppressWarnings("UnusedParameters") JavaClassSource existingBuild) {
		JavaClassSource classSource = generateConcreteLoader("Builder");
		String asString = new BuildMethodDescriptor(javabean, Collections.emptyList()).asString();
		classSource.addMethod(asString);
		return classSource;
	}

	private class BuildMethodDescriptor extends MethodDescriptor {
		BuildMethodDescriptor(JavaClassSource enclosure, List<MethodSource<JavaClassSource>> preserved) {
			super("build", enclosure, preserved);
		}
		private final static String DECLARATION_TEMPLATE = "@Generated(" + GENERATED_ANNOTATION_VALUE + ")\n" +
				"public ${javabean.name} build() {" +
				"return initialize(new ${javabean.name}());" +
				"}";
		@Override
		protected String generate() {
			return DECLARATION_TEMPLATE.replace("${javabean.name}", enclosure.getName());
		}
	}

	private JavaClassSource generateUpdater(JavaClassSource javabean, @SuppressWarnings("UnusedParameters") JavaClassSource existingUpdater) {
		JavaClassSource classSource = generateConcreteLoader("Updater");
		classSource.addMethod(new UpdateMethodDescriptor(javabean, Collections.emptyList()).asString());
		return classSource;
	}

	private class UpdateMethodDescriptor extends MethodDescriptor {

		UpdateMethodDescriptor(JavaClassSource enclosure, List<MethodSource<JavaClassSource>> preserved) {
			super("update", enclosure, preserved);
		}

		private final static String DECLARATION_TEMPLATE = "public ${javabean.name} update(${javabean.name} target) {" +
				"return modify(target);" +
				"}";
		@Override
		protected String generate() {
			return DECLARATION_TEMPLATE.replace("${javabean.name}", enclosure.getName());
		}
	}

	private JavaClassSource generateConcreteLoader(String name) {
		JavaClassSource classSource = generateClass(c -> {
			c.setPublic().setStatic(true).setName(name);
			c.setSuperType("Loader<" + name + ">");
		});

		String declarationTemplate = "@Generated(" + GENERATED_ANNOTATION_VALUE + ")\n" +
				"private  " + name + "()\n {}";
		classSource.addMethod(declarationTemplate).setConstructor(true);
		return classSource;
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

	private boolean hasSuperType(JavaClassSource javabean) {
		return !"java.lang.Object".equals(javabean.getSuperType());
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
