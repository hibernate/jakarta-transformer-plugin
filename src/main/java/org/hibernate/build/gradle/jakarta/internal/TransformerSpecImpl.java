package org.hibernate.build.gradle.jakarta.internal;

import javax.inject.Inject;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ProjectDependency;
import org.gradle.api.artifacts.ResolutionStrategy;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.util.ConfigureUtil;

import org.hibernate.build.gradle.jakarta.ShadowSpec;
import org.hibernate.build.gradle.jakarta.TransformerSpec;
import org.hibernate.build.gradle.jakarta.adhoc.DependencyTransformationSpec;
import org.hibernate.build.gradle.jakarta.adhoc.FileTransformationSpec;
import org.hibernate.build.gradle.jakarta.adhoc.TransformationTask;
import org.hibernate.build.gradle.jakarta.shadow.ShadowDependencyTask;
import org.hibernate.build.gradle.jakarta.shadow.ShadowLocalProjectTask;

import groovy.lang.Closure;

import static org.hibernate.build.gradle.jakarta.TransformerPlugin.IMPLICIT_TOOL_DEPS;

/**
 * DSL extension for configuring state shared amongst JakartaTransformation tasks
 *
 * @author Steve Ebersole
 */
public class TransformerSpecImpl implements TransformerSpec {
	public static final String TRANSFORMATION = "Transformation";

	private final Project project;
	private final TransformerConfig transformerConfig;

	private ShadowSpec shadowSpec;

	@Inject
	@SuppressWarnings("UnstableApiUsage")
	public TransformerSpecImpl(Configuration transformerToolDependencies, Project project) {
		this.project = project;

		final DirectoryProperty outputDir = project.getObjects().directoryProperty();
		outputDir.convention( project.getLayout().getBuildDirectory().dir( "libs") );

		transformerToolDependencies.defaultDependencies(
				(dependencies) -> {
					for ( String dep : IMPLICIT_TOOL_DEPS ) {
						dependencies.add( project.getDependencies().create( dep ) );
					}
				}
		);

		transformerConfig = new TransformerConfig(
				transformerToolDependencies,
				outputDir,
				project.getObjects().fileProperty(),
				project.getObjects().fileProperty(),
				project.getObjects().fileProperty(),
				project
		);
	}

	public void shadow(Object shadowSource, Action<ShadowSpec> specAction) {
		if ( shadowSpec == null ) {
			shadowSpec = createShadowSpec( shadowSource, this, project );
		}

		specAction.execute( shadowSpec );
	}

	public void shadow(Object shadowSource, Closure<ShadowSpec> closure) {
		if ( shadowSpec == null ) {
			shadowSpec = createShadowSpec( shadowSource, this, project );
		}

		ConfigureUtil.configure( closure, shadowSpec );
	}

	private static ShadowSpec createShadowSpec(
			Object shadowSource,
			TransformerSpecImpl config,
			Project project) {
		project.getPluginManager().apply( "java-library" );

		final Dependency shadowSourceDependency = resolveSourceDependency( shadowSource, project );

		if ( shadowSourceDependency instanceof ProjectDependency ) {
			// handle local projects specially
			return project.getTasks().create(
					"shadowTransform",
					ShadowLocalProjectTask.class,
					shadowSourceDependency,
					config.transformerConfig
			);
		}

		return project.getTasks().create(
				"shadowTransform",
				ShadowDependencyTask.class,
				shadowSourceDependency,
				config.transformerConfig
		);
	}

	private static Dependency resolveSourceDependency(Object shadowSource, Project project) {
		if ( shadowSource instanceof Dependency ) {
			return  (Dependency) shadowSource;
		}
		else {
			return project.getDependencies().create( shadowSource );
		}
	}

	/**
	 * Where the transformed jars should be written
	 */
	@Override
	public DirectoryProperty getOutputDir() {
		return (DirectoryProperty) transformerConfig.outputDirectoryAccess();
	}

	/**
	 * Setter for {@link #getOutputDir()}
	 */
	@Override
	public void outputDir(Object outputDirSyntax) {
		getOutputDir().set( project.file( outputDirSyntax ) );
	}

	/**
	 * Rename rules to apply
	 */
	@Override
	public RegularFileProperty getRenameRules() {
		return (RegularFileProperty) transformerConfig.renameRuleAccess();
	}

	/**
	 * Setter for {@link #getRenameRules()}
	 */
	@Override
	public void renameRules(Object renameRuleFile) {
		getRenameRules().set( project.file( renameRuleFile ) );
	}

	/**
	 * Version rules to apply
	 */
	@Override
	public RegularFileProperty getVersionRules() {
		return (RegularFileProperty) transformerConfig.versionRuleAccess();
	}

	/**
	 * Setter for {@link #getVersionRules()}
	 */
	@Override
	public void versionRules(Object versionRulesFile) {
		getVersionRules().set( project.file( versionRulesFile ) );
	}

	/**
	 * Direct rules to apply
	 */
	@Override
	public RegularFileProperty getDirectRules() {
		return (RegularFileProperty) transformerConfig.directRuleAccess();
	}

	/**
	 * Setter for {@link #getDirectRules()}
	 */
	@Override
	public void directRules(Object directRulesFile) {
		getDirectRules().set( project.file( directRulesFile ) );
	}

	@Override
	public void dependencyResolutions(Closure<ResolutionStrategy> closure) {
		project.getConfigurations().all(
				(configuration) -> ConfigureUtil.configure( closure, configuration.getResolutionStrategy() )
		);

		transformerConfig.addSubstitutions(
				resolutionStrategy -> ConfigureUtil.configure( closure, resolutionStrategy )
		);
	}

	@Override
	public void dependencyResolutions(Action<ResolutionStrategy> strategyAction) {
		project.getConfigurations().all(
				(configuration) -> strategyAction.execute( configuration.getResolutionStrategy() )
		);

		transformerConfig.addSubstitutions( strategyAction::execute );
	}

	/**
	 * Applies transformation to a dependency
	 */
	@Override
	public void dependencyTransformation(String name, Closure<DependencyTransformationSpec> closure) {
		final String taskName = name + TRANSFORMATION;
		final TransformationTask task = project.getTasks().maybeCreate( taskName, TransformationTask.class );

		ConfigureUtil.configure( closure, new DependencyTransformationSpec( this, task ) );
	}

	/**
	 * Applies transformation to a JAR file
	 */
	@Override
	public void fileTransformation(String name, Closure<FileTransformationSpec> closure) {
		final String taskName = name + TRANSFORMATION;
		final TransformationTask task = project.getTasks().maybeCreate(
				taskName,
				TransformationTask.class
		);

		ConfigureUtil.configure( closure, new FileTransformationSpec( this, task ) );
	}

}
