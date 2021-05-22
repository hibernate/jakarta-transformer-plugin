package org.hibernate.build.gradle.jakarta.shadow;

import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;

import org.gradle.api.Action;
import org.gradle.api.DefaultTask;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.bundling.Jar;

import org.hibernate.build.gradle.jakarta.ShadowSpec;
import org.hibernate.build.gradle.jakarta.ShadowTestSpec;
import org.hibernate.build.gradle.jakarta.internal.TransformerConfig;

import groovy.lang.Closure;

/**
 * @author Steve Ebersole
 */
public abstract class ShadowDependencyTask extends DefaultTask implements ShadowSpec {
	private final Dependency mainSourceDependency;
	private final TransformerConfig transformerConfig;

	private final DirectoryProperty libsDirectoryProperty;

	private DependencyTransformerTask sourcesTask;
	private DependencyTransformerTask javadocsTask;

	@Inject
	public ShadowDependencyTask(
			Dependency mainSourceDependency,
			TransformerConfig transformerConfig) {
		this.mainSourceDependency = mainSourceDependency;
		this.transformerConfig = transformerConfig;

		// todo : determine how to best handle manging the normal java-library tasks
		// 		like compile, jar, etc

		final Jar jarTask = (Jar) getProject().getTasks().getByName( "jar" );
		jarTask.dependsOn( this );

		libsDirectoryProperty = jarTask.getDestinationDirectory();

		final DependencyTransformerTask mainShadowTask = getProject().getTasks().create(
				"shadowMainDependency",
				DependencyTransformerTask.class,
				mainSourceDependency,
				libsDirectoryProperty,
				"",
				transformerConfig
		);

		this.dependsOn( mainShadowTask );
	}

	@Input
	public String getSourceDependency() {
		return mainSourceDependency.getGroup() + mainSourceDependency.getName() + mainSourceDependency.getVersion();
	}

	@OutputDirectory
	public Provider<Directory> getLibsDirectory() {
		return libsDirectoryProperty;
	}

	@TaskAction
	public void coordinateShadowing() {
		getLogger().lifecycle(
				"Coordinating dependency shadowing : {}:{}:{}",
				mainSourceDependency.getGroup(),
				mainSourceDependency.getName(),
				mainSourceDependency.getVersion()
		);
	}

	@Override
	public void runTests(Action<ShadowTestSpec> specAction) {
	}

	@Override
	public void runTests(Closure<ShadowTestSpec> closure) {

	}

	@Override
	public void runTests(Object testsDependencyNotation, Action<ShadowTestSpec> specAction) {

	}

	@Override
	public void runTests(Object testsDependencyNotation, Closure<ShadowTestSpec> closure) {

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

		sourcesTask = getProject().getTasks().create(
				"shadowSourcesDependency",
				DependencyTransformerTask.class,
				sourcesDependency,
				libsDirectoryProperty,
				"sources",
				transformerConfig
		);

		final JavaPluginExtension javaPluginExtension = (JavaPluginExtension) getProject().getExtensions().getByName( "java" );
		javaPluginExtension.withSourcesJar();

		this.dependsOn( sourcesTask );
	}

	@Override
	public void withJavadoc() {
		if ( javadocsTask != null ) {
			return;
		}

		final Dependency sourcesDependency = getClassifiedVariant( "javadoc" );

		javadocsTask = getProject().getTasks().create(
				"shadowJavadocsDependency",
				DependencyTransformerTask.class,
				sourcesDependency,
				libsDirectoryProperty,
				"javadoc",
				transformerConfig
		);

		final JavaPluginExtension javaPluginExtension = (JavaPluginExtension) getProject().getExtensions().getByName( "java" );
		javaPluginExtension.withJavadocJar();

		this.dependsOn( javadocsTask );
	}

	private Dependency getClassifiedVariant(String classifier) {
		final Map<String,String> testDependencyNotation = new HashMap<>();
		testDependencyNotation.put( "group", mainSourceDependency.getGroup() );
		testDependencyNotation.put( "name", mainSourceDependency.getName() );
		testDependencyNotation.put( "version", mainSourceDependency.getVersion() );
		testDependencyNotation.put( "classifier", classifier );

		return getProject().getDependencies().create( testDependencyNotation );
	}
}
