package org.hibernate.build.gradle.jakarta;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import javax.inject.Inject;

import org.gradle.api.Action;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ResolutionStrategy;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.ResolvedConfiguration;
import org.gradle.api.artifacts.ResolvedDependency;
import org.gradle.api.file.Directory;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.gradle.util.ConfigureUtil;

import groovy.lang.Closure;
import groovy.lang.DelegatesTo;

/**
 * @author Steve Ebersole
 */
public abstract class ShadowTestsTransformationTask
		extends DefaultTask
		implements ShadowTransformationSpec, Transformer.Options {
	private final JakartaTransformerConfig transformerConfig;
	private final Configuration sourceDependencyConfiguration;

	//private final SoftwareComponentFactory softwareComponentFactory;

	private final Dependency sourceDependency;
	private final Set<Dependency> additionalClassifiedSourceDependencies;

	private final Property<String> artifactNamePattern;

	@Inject
	public ShadowTestsTransformationTask(
			Dependency sourceDependency,
			JakartaTransformerConfig transformerConfig) {
//			SoftwareComponentFactory softwareComponentFactory) {
		this.transformerConfig = transformerConfig;
//		this.softwareComponentFactory = softwareComponentFactory;

		this.sourceDependency = sourceDependency;

		sourceDependencyConfiguration = getProject().getConfigurations().detachedConfiguration( sourceDependency );

		artifactNamePattern = getProject().getObjects().property( String.class );
		artifactNamePattern.convention( "${name}-jakarta" );

		additionalClassifiedSourceDependencies = new HashSet<>();
	}

	@OutputDirectory
	public Directory getOutputDirectory() {
		transformerConfig.getOutputDir().finalizeValue();
		return transformerConfig.getOutputDir().get();
	}

	@Input
	public String getShadowSource() {
		return sourceDependency.getGroup() + ":" + sourceDependency.getName() + ":" + sourceDependency.getVersion();
	}

	@Input
	public Configuration ___sourceDependencyConfiguration() {
		return sourceDependencyConfiguration;
	}

	@Input
	@Override
	public Property<String> shadowArtifactNamePattern() {
		artifactNamePattern.finalizeValue();
		return artifactNamePattern;
	}

	@Input
	public Set<Dependency> ___additionalClassifiedSourceDependencies() {
		return additionalClassifiedSourceDependencies;
	}

	@Override
	public void shadowArtifactNamePattern(String name) {
		artifactNamePattern.set( name );
	}

	@Override
	public void additionalSourceClassifier(String classifiers) {

	}

	@Override
	public void additionalSourceClassifiers(String... classifiers) {

	}

	public void dependencyResolutions(Action<ResolutionStrategy> config) {
		config.execute( sourceDependencyConfiguration.getResolutionStrategy() );
	}

	public void dependencyResolutions(@DelegatesTo(value = ResolutionStrategy.class, strategy = 1) Closure config) {
		ConfigureUtil.configure( config, sourceDependencyConfiguration.getResolutionStrategy() );
	}

	@TaskAction
	public void performTransformation() {
		// todo : most (all?) of this is/will-be shared between the 2 shadow transform tasks; consolidate the impls

		final Project transformedProject = getProject();

		final ResolvedConfiguration resolvedConfiguration = sourceDependencyConfiguration.getResolvedConfiguration();

		// `resolvedSourceDependency` will be the main artifact one we want to shadow
		final Set<ResolvedDependency> firstLevelModuleDependencies = resolvedConfiguration.getFirstLevelModuleDependencies();
		assert firstLevelModuleDependencies.size() == 1;
		final RegularFile transformed = transform( firstLevelModuleDependencies.iterator().next() );

		getLogger().lifecycle( "Transformed file : {}", transformed.getAsFile().getAbsolutePath() );


		final ResolvedDependency resolvedSourceDependency = firstLevelModuleDependencies.iterator().next();

		// `dependencies` are the (substituted) dependencies
		final Set<ResolvedDependency> dependencies = resolvedSourceDependency.getChildren();
		getProject().getLogger().lifecycle( "    Dependencies:" );
		for ( ResolvedDependency dependency : dependencies ) {
			getProject().getLogger().lifecycle( "        - {}", dependency.getName() );
		}



		// for each artifact in the resolved source dependency, perform the transformation
		// and add the produced artifact to this projects published artifacts


//		// set up the `apiElements` and `runtimeElements` Configurations and have them
//		// extend from the source project's same-named Configurations.  the substitutions
//		// we expose via `dependencySubstitutions` operate on these
//		final Configuration apiElements = transformedProject.getConfigurations().maybeCreate( "apiElements" );
//		apiElements.extendsFrom( sourceDependencyConfiguration( "apiElements" ) );
//
//		final Configuration runtimeElements = transformedProject.getConfigurations().maybeCreate( "runtimeElements" );
//		runtimeElements.extendsFrom( this.sourceDependency.getConfigurations().getByName( "runtimeElements" ) );
//		runtimeElements.extendsFrom( apiElements );
//
//		final AdhocComponentWithVariants component = softwareComponentFactory.adhoc("javaPlatform" );
//		// do we have-to/want-to register this?
//		transformedProject.getComponents().add( component );
//
//		component.addVariantsFromConfiguration(
//				apiElements,
//				new JavaConfigurationVariantMapping( "compile", false )
//		);
//		component.addVariantsFromConfiguration(
//				runtimeElements,
//				new JavaConfigurationVariantMapping( "runtime", false )
//		);
//
//		final PublishingExtension publishing = transformedProject.getExtensions().findByType( PublishingExtension.class );
//		final MavenPublication shadowPublication = publishing.getPublications().create(
//				"shadow",
//				MavenPublication.class
//		);
//
//		shadowPublication.from( component );
	}

	@Override
	public RegularFile getRenameRules() {
		return transformerConfig.getRenameRules().get();
	}

	@Override
	public RegularFile getVersionRules() {
		return transformerConfig.getVersionRules().get();
	}

	@Override
	public RegularFile getDirectRules() {
		return transformerConfig.getDirectRules().get();
	}

	private RegularFile transform(ResolvedDependency dependency) {
		final String renamePattern = artifactNamePattern.get();
		final Directory outputDirectory = getOutputDirectory();

		final Set<ResolvedArtifact> sourceArtifacts = dependency.getModuleArtifacts();
		if ( sourceArtifacts.size() != 1 ) {
			throw new JakartaTransformationException(
					String.format(
							Locale.ROOT,
							"Resolved Dependency `%s` defined multiple artifacts; expecting one",
							dependency.getModule().getId()
					)
			);
		}

		final ResolvedArtifact sourceArtifact = sourceArtifacts.iterator().next();

		final String transformedArtifactName = renamePattern.replace( "${name}", sourceArtifact.getName() );
		getProject().getLogger().lifecycle( "Transforming `{}` (tests) -> `{}` (tests)", sourceArtifact.getName(), transformedArtifactName );

		String transformedFileName = transformedArtifactName;
		if ( sourceArtifact.getClassifier() != null && ! sourceArtifact.getClassifier().isEmpty() ) {
			transformedFileName += ( "-" + sourceArtifact.getClassifier() );
		}
		transformedFileName += "." + sourceArtifact.getExtension();

		final RegularFile outputFile = outputDirectory.file( transformedFileName );

		Transformer.transform(
				sourceArtifact.getFile(),
				outputFile,
				transformerConfig.getTransformerToolDependencies(),
				this,
				getProject()
		);

		return outputDirectory.file( transformedFileName );
	}
}
