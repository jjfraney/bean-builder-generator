package org.jjflyboy.forge.javabeans;

import org.jboss.forge.addon.parser.java.beans.ProjectOperations;
import org.jboss.forge.addon.parser.java.facets.JavaSourceFacet;
import org.jboss.forge.addon.parser.java.resources.JavaResource;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.ProjectFactory;
import org.jboss.forge.addon.projects.ui.AbstractProjectCommand;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.input.UISelectMany;
import org.jboss.forge.addon.ui.input.UISelectOne;
import org.jboss.forge.addon.ui.metadata.UICommandMetadata;
import org.jboss.forge.addon.ui.metadata.WithAttributes;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.forge.addon.ui.util.Categories;
import org.jboss.forge.addon.ui.util.Metadata;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.JavaInterfaceSource;
import org.jboss.forge.roaster.model.source.JavaSource;

import javax.inject.Inject;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.stream.StreamSupport;


@SuppressWarnings("unused")
class UIJavabeansOperations extends AbstractProjectCommand {

	@SuppressWarnings("CanBeFinal")
	@Inject
	@WithAttributes(label = "targets", required = true)
	private UISelectMany<String> targets;
	// due to forge bug, cannot use: private UISelectMany<JavaResource> targets;
	// using this map to compensate for the forge bug above
	@SuppressWarnings("CanBeFinal")
	private Map<String, JavaSource<?>> targetMap = new HashMap<>();

	@SuppressWarnings("CanBeFinal")
	@Inject
	@WithAttributes(label = "strategy")
	private UISelectOne<GeneratorStrategyType> generatorStrategy;

	@SuppressWarnings("CanBeFinal")
	@Inject
	private ProjectOperations projectOperations;

	@SuppressWarnings("CanBeFinal")
	@Inject
	private BuilderOnlyGenerateStrategy builderOnlyStrategy;

	@SuppressWarnings("CanBeFinal")
	@Inject
	private BuilderAndUpdaterGenerateStrategy builderAndUpdaterStrategy;

	@SuppressWarnings("CanBeFinal")
	@Inject
	private LoaderOnlyGenerateStrategy loaderOnlyStrategy;

	@Override
	public void initializeUI(UIBuilder builder) throws Exception {
		UIContext context = builder.getUIContext();
		Project project = getSelectedProject(context);

		// maybe a filter here.
		for(JavaResource jr: projectOperations.getProjectClasses(project)) {
			try {
				targetMap.put(jr.getJavaType().getQualifiedName(), jr.getJavaType());
			} catch (FileNotFoundException e) {
				// this is ok, because we are using files found by
				// projectOperations
			}
		}
		for (JavaResource jr : projectOperations.getProjectInterfaces(project)) {
			JavaInterfaceSource targetInterface = jr.getJavaType();
			for (JavaSource<?> js : targetInterface.getNestedTypes()) {
				targetMap.put(js.getQualifiedName(), js);
			}
		}
		List<String> t = new LinkedList<>(targetMap.keySet());
		Collections.sort(t);
		targets.setValueChoices(t);

		generatorStrategy.setDefaultValue(GeneratorStrategyType.BUILDER_ONLY);
		generatorStrategy.setValueChoices(Arrays.asList(GeneratorStrategyType.values()));
		builder
		.add(targets)
		.add(generatorStrategy)
		;
	}

	@Override
	public Result execute(UIExecutionContext context) throws Exception {
		Project project = getSelectedProject(context);
		final JavaSourceFacet javaSourceFacet = project.getFacet(JavaSourceFacet.class);

		final GenerateStrategy strategy = pickStrategy();

		StreamSupport.stream(targets.getValue().spliterator(), false)
				.map(s -> targetMap.get(s))
				.filter(c -> c != null && c.isClass())
				.map(c -> strategy.generate((JavaClassSource)c))
				.flatMap(Collection::stream)
				.forEach(c -> writeJavaUnit(javaSourceFacet, c));

		return Results.success("builder generator completed");
	}

	private GenerateStrategy pickStrategy() {
		GenerateStrategy strategy = builderOnlyStrategy;
		switch(generatorStrategy.getValue()) {
			case BUILDER_ONLY:
			default:
				break;
			case BUILDER_AND_UPDATER:
				strategy = builderAndUpdaterStrategy;
				break;
			case LOADER_ONLY:
				strategy = loaderOnlyStrategy;
				break;
		}
		return strategy;
	}

	private void writeJavaUnit(JavaSourceFacet javaSourceFacet, JavaClassSource targetClass) {
		JavaSource<?> unit = targetClass;
		if(targetClass.getEnclosingType() != null) {
            unit = targetClass.getEnclosingType();
        }
		javaSourceFacet.saveJavaSourceUnformatted(unit);
	}

	@Override
	public UICommandMetadata getMetadata(UIContext context) {
		return Metadata.forCommand(UIJavabeansOperations.class).name("javabeans: generate builder")
				.category(Categories.create("javabeans"));
	}

	@Override
	protected boolean isProjectRequired() {
		return true;
	}

	@SuppressWarnings("CanBeFinal")
	@Inject
	private ProjectFactory projectFactory;

	@Override
	protected ProjectFactory getProjectFactory() {
		return projectFactory;
	}

}
