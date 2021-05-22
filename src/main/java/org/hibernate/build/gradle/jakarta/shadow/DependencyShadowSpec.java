package org.hibernate.build.gradle.jakarta.shadow;

import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.util.ConfigureUtil;

import org.hibernate.build.gradle.jakarta.TransformationException;
import org.hibernate.build.gradle.jakarta.internal.TransformerConfig;

import groovy.lang.Closure;

/**
 * @author Steve Ebersole
 */
public class DependencyShadowSpec implements ShadowSpec {
	private final Dependency mainSourceDependency;
	private final Project shadowProject;
	private final TransformerConfig transformerConfig;

	private final DirectoryProperty libsDirectoryProperty;

	private DependencyTransformerTask sourcesTask;
	private DependencyTransformerTask javadocsTask;

	private ShadowTestSpec testSpec;

	@Inject
	public DependencyShadowSpec(
			Dependency mainSourceDependency,
			Project shadowProject,
			TransformerConfig transformerConfig) {
		this.mainSourceDependency = mainSourceDependency;
		this.shadowProject = shadowProject;
		this.transformerConfig = transformerConfig;

		// todo : determine how to best handle manging the normal java-library tasks
		// 		like compile, jar, etc

		final Jar jarTask = (Jar) shadowProject.getTasks().getByName( "jar" );

		libsDirectoryProperty = jarTask.getDestinationDirectory();

		final DependencyTransformerTask mainShadowTask = shadowProject.getTasks().create(
				"shadowMainDependency",
				DependencyTransformerTask.class,
				mainSourceDependency,
				libsDirectoryProperty,
				"",
				transformerConfig
		);
		jarTask.dependsOn( mainShadowTask );


		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Create the "grouping" task
		final Task groupingTask = shadowProject.getTasks().create( SHADOW_GROUPING_TASK );
		groupingTask.setGroup( TASK_GROUP );
		groupingTask.dependsOn( mainShadowTask );
	}

	@Input
	public String getSourceDependency() {
		return mainSourceDependency.getGroup() + mainSourceDependency.getName() + mainSourceDependency.getVersion();
	}

	@OutputDirectory
	public Provider<Directory> getLibsDirectory() {
		return libsDirectoryProperty;
	}

	@Override
	public void runTests(Action<ShadowTestSpec> specAction) {
		if ( testSpec == null ) {
			testSpec = createTestSpec();
		}
		specAction.execute( testSpec );
	}

	@Override
	public void runTests(Closure<ShadowTestSpec> closure) {
		if ( testSpec == null ) {
			testSpec = createTestSpec();
		}
		ConfigureUtil.configure( closure, testSpec );
	}

	@Override
	public void runTests(Object testsDependencyNotation, Action<ShadowTestSpec> specAction) {
		runTests( specAction );
	}

	@Override
	public void runTests(Object testsDependencyNotation, Closure<ShadowTestSpec> closure) {
		throw new TransformationException( "Running tests for dependency shadowing is not yet supported" );
	}

	private ShadowTestSpec createTestSpec() {
		// to run, we really need to un-pack them...

		throw new UnsupportedOperationException(
				"Running tests for shadowed external dependencies is not yet supported"
		);
	}

	@Override
	public void withSources() {
		if ( sourcesTask != null ) {
			return;
		}

		final Dependency sourcesDependency = getClassifiedVariant( "sources" );

		final JavaPluginExtension javaPluginExtension = (JavaPluginExtension) shadowProject.getExtensions().getByName( "java" );
		javaPluginExtension.withSourcesJar();
		final Jar sourcesJarTask = (Jar) shadowProject.getTasks().getByName( "sourcesJar" );

		sourcesTask = shadowProject.getTasks().create(
				"shadowSourcesDependency",
				DependencyTransformerTask.class,
				sourcesDependency,
				libsDirectoryProperty,
				"sources",
				transformerConfig
		);
		sourcesTask.setGroup( TASK_GROUP );
		final Task groupingTask = shadowProject.getTasks().getByName( SHADOW_GROUPING_TASK );
		groupingTask.dependsOn( sourcesTask );
		sourcesJarTask.dependsOn( sourcesTask );
	}

	@Override
	public void withJavadoc() {
		if ( javadocsTask != null ) {
			return;
		}

		final JavaPluginExtension javaPluginExtension = (JavaPluginExtension) shadowProject.getExtensions().getByName( "java" );
		javaPluginExtension.withJavadocJar();
		final Jar jarTask = (Jar) shadowProject.getTasks().getByName( "javadocJar" );

		final Dependency sourcesDependency = getClassifiedVariant( "javadoc" );

		javadocsTask = shadowProject.getTasks().create(
				"shadowJavadocsDependency",
				DependencyTransformerTask.class,
				sourcesDependency,
				libsDirectoryProperty,
				"javadoc",
				transformerConfig
		);

		javadocsTask.setGroup( TASK_GROUP );
		jarTask.dependsOn( javadocsTask );

		final Task groupingTask = shadowProject.getTasks().getByName( SHADOW_GROUPING_TASK );
		groupingTask.dependsOn( sourcesTask );
	}

	private Dependency getClassifiedVariant(String classifier) {
		final Map<String,String> testDependencyNotation = new HashMap<>();
		testDependencyNotation.put( "group", mainSourceDependency.getGroup() );
		testDependencyNotation.put( "name", mainSourceDependency.getName() );
		testDependencyNotation.put( "version", mainSourceDependency.getVersion() );
		testDependencyNotation.put( "classifier", classifier );

		return shadowProject.getDependencies().create( testDependencyNotation );
	}
}
