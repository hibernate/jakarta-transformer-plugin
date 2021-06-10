package org.hibernate.build.gradle.jakarta.internal;

import javax.inject.Inject;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ProjectDependency;
import org.gradle.api.artifacts.ResolutionStrategy;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFile;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.util.ConfigureUtil;

import org.hibernate.build.gradle.jakarta.TransformerSpec;
import org.hibernate.build.gradle.jakarta.adhoc.DependencyTransformationTask;
import org.hibernate.build.gradle.jakarta.adhoc.DirectoryTransformationTask;
import org.hibernate.build.gradle.jakarta.adhoc.FileTransformationTask;
import org.hibernate.build.gradle.jakarta.shadow.DependencyShadowSpec;
import org.hibernate.build.gradle.jakarta.shadow.LocalProjectShadowSpec;
import org.hibernate.build.gradle.jakarta.shadow.ShadowSpec;

import groovy.lang.Closure;

import static org.hibernate.build.gradle.jakarta.TransformerPlugin.IMPLICIT_TOOL_DEPS;

/**
 * DSL extension for configuring state shared amongst JakartaTransformation tasks
 *
 * @author Steve Ebersole
 */
public class TransformerSpecImpl implements TransformerSpec {

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
		final Dependency shadowSourceDependency = resolveSourceDependency( shadowSource, project );

		if ( shadowSourceDependency instanceof ProjectDependency ) {
			return new LocalProjectShadowSpec(
					( (ProjectDependency) shadowSourceDependency ).getDependencyProject(),
					project,
					config.transformerConfig
			);
		}

		return new DependencyShadowSpec( shadowSourceDependency, project, config.transformerConfig );
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
	public void dependencyTransformation(String transformationName, Closure<DependencyTransformationTask> closure) {
		final DependencyTransformationTask transformationTask = getDependencyTransformationTask( transformationName );
		ConfigureUtil.configure( closure, transformationTask );
	}

	private DependencyTransformationTask getDependencyTransformationTask(String transformationName) {
		final String taskName = determineTransformationTaskName( transformationName );
		project.getLogger().lifecycle( "Creating dependency transformation `{}` : `{}`", transformationName, taskName );

		DependencyTransformationTask transformationTask = (DependencyTransformationTask) project.getTasks().findByName( taskName );
		if ( transformationTask == null ) {
			transformationTask = project.getTasks().create(
					taskName,
					DependencyTransformationTask.class,
					transformerConfig
			);
			transformationTask.getOutput().convention(
					project.provider(
							() -> {
								String outputFileName = transformationName;

								final String versionString = project.getVersion().toString().trim();
								if ( !versionString.isEmpty() && !versionString.equals( "unspecified" ) ) {
									outputFileName += ( "-" + versionString );

								}

								outputFileName += ".jar";

								return transformerConfig.outputDirectoryAccess().get().file( outputFileName );
							}
					)
			);
		}
		return transformationTask;
	}

	private String determineTransformationTaskName(String transformationName) {
		return "transform" + Character.toUpperCase( transformationName.charAt( 0 ) ) + transformationName.substring( 1 );
	}

	@Override
	public void dependencyTransformation(String transformationName, Action<DependencyTransformationTask> transformationSpec) {
		final DependencyTransformationTask transformationTask = getDependencyTransformationTask( transformationName );
		transformationSpec.execute( transformationTask );
	}

	@Override
	public void directoryTransformation(String transformationName, Closure<DirectoryTransformationTask> closure) {
		final DirectoryTransformationTask transformationTask = getDirectoryTransformationTask( transformationName );
		ConfigureUtil.configure( closure, transformationTask );
	}

	@Override
	public void directoryTransformation(String transformationName, Action<DirectoryTransformationTask> transformationSpec) {
		final DirectoryTransformationTask transformationTask = getDirectoryTransformationTask( transformationName );
		transformationSpec.execute( transformationTask );
	}

	private DirectoryTransformationTask getDirectoryTransformationTask(String name) {
		final String taskName = determineTransformationTaskName( name );
		project.getLogger().lifecycle( "Creating directory transformation `{}` : `{}`", name, taskName );

		DirectoryTransformationTask transformationTask = (DirectoryTransformationTask) project.getTasks().findByName( taskName );
		if ( transformationTask == null ) {
			transformationTask = project.getTasks().create(
					taskName,
					DirectoryTransformationTask.class,
					name,
					transformerConfig
			);
			transformationTask.getOutput().convention(
					project.provider(
							() -> transformerConfig.outputDirectoryAccess().get().dir( name )
					)
			);
		}
		return transformationTask;
	}


	/**
	 * Applies transformation to a JAR file
	 */
	@Override
	public void fileTransformation(String transformationName, Closure<FileTransformationTask> closure) {
		final FileTransformationTask transformationTask = getFileTransformationTask( transformationName );
		ConfigureUtil.configure( closure, transformationTask );
	}

	@Override
	public void fileTransformation(String transformationName, Action<FileTransformationTask> transformationSpec) {
		final FileTransformationTask transformationTask = getFileTransformationTask( transformationName );
		transformationSpec.execute( transformationTask );
	}

	private FileTransformationTask getFileTransformationTask(String transformationName) {
		final String taskName = determineTransformationTaskName( transformationName );
		project.getLogger().lifecycle( "Creating file transformation `{}` : `{}`", transformationName, taskName );

		FileTransformationTask transformationTask = (FileTransformationTask) project.getTasks().findByName( taskName );
		if ( transformationTask == null ) {
			final FileTransformationTask task = project.getTasks().create(
					taskName,
					FileTransformationTask.class,
					transformationName,
					transformerConfig
			);
			transformationTask = task;
			transformationTask.getOutput().convention(
					project.provider(
							() -> {
								final RegularFile source = task.getSource().get();

								// this assumes "single dot" file extensions
								final String sourceFileName = source.getAsFile().getName();
								final int extensionDelimiterLocation = sourceFileName.lastIndexOf( '.' );
								String outputFileName = transformationName;
								if ( project.getVersion() != null && ! "unspecified".equals( project.getVersion() ) ) {
									outputFileName += ( "-" + project.getVersion());
								}
								outputFileName += ( "." + sourceFileName.substring( extensionDelimiterLocation ) );
								return transformerConfig.outputDirectoryAccess().get().file( outputFileName );
							}
					)
			);
		}
		return transformationTask;
	}

}
