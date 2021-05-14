package org.hibernate.build.gradle.jakarta;

import javax.inject.Inject;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.util.ConfigureUtil;

import groovy.lang.Closure;
import groovy.lang.DelegatesTo;

/**
 * DSL extension for configuring state shared amongst JakartaTransformation tasks
 *
 * @author Steve Ebersole
 */
public class JakartaTransformerConfig {
	public static final String TRANSFORMATION = "Transformation";

	private final Configuration transformerToolDependencies;
	private final Project project;

	private final DirectoryProperty outputDir;

	private final RegularFileProperty renameRules;
	private final RegularFileProperty versionRules;
	private final RegularFileProperty directRules;

	private ShadowTransformationTask shadowTransformationTask;
	private ShadowTestsTransformationTask shadowTestsTransformationTask;

	// todo : for a more complete implementation we should support "selections" and "bundles" also
	//		but I have no idea if "selections" is also expected to be a properties file or ???
	//		nor how "bundles" is used (our build does not use them)
	// 		+
	// 		so for now just support the options we use

	@Inject
	@SuppressWarnings("UnstableApiUsage")
	public JakartaTransformerConfig(Configuration transformerToolDependencies, Project project) {
		this.transformerToolDependencies = transformerToolDependencies;
		this.project = project;

		this.outputDir = project.getObjects().directoryProperty();
		this.outputDir.convention( project.getLayout().getBuildDirectory().dir( "libs") );

		this.renameRules = project.getObjects().fileProperty();
		this.versionRules = project.getObjects().fileProperty();
		this.directRules = project.getObjects().fileProperty();
	}

	Configuration getTransformerToolDependencies() {
		return transformerToolDependencies;
	}

	public void shadow(
			Object shadowSource,
			Action<ShadowTransformationSpec> config) {
		if ( shadowTransformationTask == null ) {
			shadowTransformationTask = createShadowTransformationTask( shadowSource, this, project );
		}

		config.execute( shadowTransformationTask );

		linkTransformTask( shadowTransformationTask, this, project );
	}

	public void shadow(
			Object shadowSource,
			@DelegatesTo(value = ShadowTransformationSpec.class, strategy = 1) Closure config) {
		if ( shadowTransformationTask == null ) {
			shadowTransformationTask = createShadowTransformationTask( shadowSource, this, project );
		}

		ConfigureUtil.configure( config, shadowTransformationTask );

		linkTransformTask( shadowTransformationTask, this, project );
	}

	private static ShadowTransformationTask createShadowTransformationTask(
			Object shadowSource,
			JakartaTransformerConfig config,
			Project project) {

		final Dependency shadowSourceDependency = resolveSourceDependency( shadowSource, project );

		final ShadowTransformationTask task = project.getTasks().create(
				"shadowTransform",
				ShadowTransformationTask.class,
				shadowSourceDependency,
				config
		);

		// todo : link together other tasks (`compile`, ...)

		if ( config.shadowTestsTransformationTask != null ) {
			config.shadowTestsTransformationTask.dependsOn( task );
			config.shadowTestsTransformationTask.mustRunAfter( task );
		}

		return task;
	}

	private static void linkTransformTask(
			ShadowTransformationTask testsTransformTask,
			JakartaTransformerConfig config,
			Project project) {
		// todo : link together `compile`, etc

		if ( config.shadowTestsTransformationTask != null ) {
			testsTransformTask.dependsOn( config.shadowTestsTransformationTask );
		}
	}

	public void shadowTests(
			Object shadowSource,
			Action<ShadowTransformationSpec> config) {
		if ( shadowTestsTransformationTask == null ) {
			shadowTestsTransformationTask = createShadowTestsTransformationTask( shadowSource, this, project );
		}

		config.execute( shadowTestsTransformationTask );

		linkTestTransformTask( shadowTestsTransformationTask, this, project );
	}

	public void shadowTests(
			Object shadowSource,
			@DelegatesTo(value = ShadowTransformationSpec.class, strategy = 1) Closure config) {
		if ( shadowTestsTransformationTask == null ) {
			shadowTestsTransformationTask = createShadowTestsTransformationTask( shadowSource, this, project );
		}

		ConfigureUtil.configure( config, shadowTestsTransformationTask );

		linkTestTransformTask( shadowTestsTransformationTask, this, project );
	}

	private ShadowTestsTransformationTask createShadowTestsTransformationTask(
			Object shadowSource,
			JakartaTransformerConfig config,
			Project project) {
		final Dependency shadowSourceDependency = resolveSourceDependency( shadowSource, project );

		final ShadowTestsTransformationTask task = project.getTasks().create(
				"shadowTestTransform",
				ShadowTestsTransformationTask.class,
				shadowSourceDependency,
				this
		);

		return task;
	}

	private static void linkTestTransformTask(
			ShadowTestsTransformationTask testsTransformTask,
			JakartaTransformerConfig config,
			Project project) {
		// todo : link together `compile`, etc

		if ( config.shadowTransformationTask != null ) {
			testsTransformTask.dependsOn( config.shadowTransformationTask );
			testsTransformTask.mustRunAfter( config.shadowTransformationTask );
		}

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
	public DirectoryProperty getOutputDir() {
		return outputDir;
	}

	/**
	 * Setter for {@link #getOutputDir()}
	 */
	public void outputDir(Object outputDirSyntax) {
		outputDir.set( project.file( outputDirSyntax ) );
	}

	/**
	 * Rename rules to apply
	 */
	public RegularFileProperty getRenameRules() {
		return renameRules;
	}

	/**
	 * Setter for {@link #getRenameRules()}
	 */
	public void renameRules(Object renameRuleFile) {
		renameRules.set( project.file( renameRuleFile ) );
	}

	/**
	 * Version rules to apply
	 */
	public RegularFileProperty getVersionRules() {
		return versionRules;
	}

	/**
	 * Setter for {@link #getVersionRules()}
	 */
	public void versionRules(Object versionRulesFile) {
		versionRules.set( project.file( versionRulesFile ) );
	}

	/**
	 * Direct rules to apply
	 */
	public RegularFileProperty getDirectRules() {
		return directRules;
	}

	/**
	 * Setter for {@link #getDirectRules()}
	 */
	public void directRules(Object directRulesFile) {
		directRules.set( project.file( directRulesFile ) );
	}

	/**
	 * Applies transformation to a dependency
	 */
	public void dependencyTransformation(String name, Closure<DependencyTransformationSpec> configurer) {
		final String taskName = name + TRANSFORMATION;
		final TransformationTask task = project.getTasks().maybeCreate( taskName, TransformationTask.class );

		ConfigureUtil.configure( configurer, new DependencyTransformationSpec( this, task ) );
	}

	/**
	 * Applies transformation to a JAR file
	 */
	public void fileTransformation(String name, Closure<FileTransformationSpec> configurer) {
		final String taskName = name + TRANSFORMATION;
		final TransformationTask task = project.getTasks().maybeCreate(
				taskName,
				TransformationTask.class
		);

		ConfigureUtil.configure( configurer, new FileTransformationSpec( this, task ) );
	}
}
