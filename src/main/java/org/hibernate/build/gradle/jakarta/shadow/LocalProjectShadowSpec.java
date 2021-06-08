package org.hibernate.build.gradle.jakarta.shadow;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.RegularFile;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.Provider;
import org.gradle.api.publish.Publication;
import org.gradle.api.publish.PublicationContainer;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.ivy.IvyModuleDescriptorSpec;
import org.gradle.api.publish.ivy.IvyPublication;
import org.gradle.api.publish.ivy.plugins.IvyPublishPlugin;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.util.ConfigureUtil;

import org.hibernate.build.gradle.jakarta.TransformationException;
import org.hibernate.build.gradle.jakarta.adhoc.FileTransformationTask;
import org.hibernate.build.gradle.jakarta.internal.Helper;
import org.hibernate.build.gradle.jakarta.internal.PomHelper;
import org.hibernate.build.gradle.jakarta.internal.TransformerConfig;

import groovy.lang.Closure;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("UnstableApiUsage")
public class LocalProjectShadowSpec implements ShadowSpec {
	private final Project sourceProject;
	private final Project targetProject;
	private final TransformerConfig transformerConfig;

	private final Task groupingTask;

	private final Configuration compileScopeDependencies;
	private final Configuration runtimeScopeDependencies;

	private MavenPublication shadowMavenPublication;
	private IvyPublication shadowIvyPublication;

	private LocalProjectShadowTestsSpec testsSpec;


	public LocalProjectShadowSpec(
			Project sourceProject,
			Project targetProject,
			TransformerConfig transformerConfig) {
		this.sourceProject = sourceProject;
		this.targetProject = targetProject;
		this.transformerConfig = transformerConfig;

		compileScopeDependencies = targetProject.getConfigurations().maybeCreate( "compileScope" );
		runtimeScopeDependencies = targetProject.getConfigurations().maybeCreate( "runtimeScope" );

		shadowConfiguration( "compileClasspath", compileScopeDependencies );
		shadowConfiguration( "runtimeClasspath", runtimeScopeDependencies );

		groupingTask = targetProject.getTasks().create( SHADOW_GROUPING_TASK );
		groupingTask.setGroup( TASK_GROUP );

		final Task assembleTask = targetProject.getTasks().getByName( "assemble" );
		assembleTask.dependsOn( groupingTask );

		final SourceSet sourceMainSourceSet = Helper.extractSourceSets( sourceProject ).getByName( "main" );
		final Jar sourceProjectJarTask = (Jar) sourceProject.getTasks().getByName( sourceMainSourceSet.getJarTaskName() );

		final ShadowPublishArtifact shadowPublishArtifact = createArtifactTransformationTask(
				"shadowJar",
				sourceProjectJarTask,
				null,
				transformerConfig,
				targetProject
		);

		final PublishingExtension sourcePublishingExtension = (PublishingExtension) sourceProject.getExtensions().findByName( "publishing" );
		if ( sourcePublishingExtension != null ) {
			final MavenPublishPlugin mavenPublishPlugin = sourceProject.getPlugins().findPlugin( MavenPublishPlugin.class );
			if ( mavenPublishPlugin != null ) {
				targetProject.getPluginManager().apply( MavenPublishPlugin.class );
			}

			final IvyPublishPlugin ivyPublishPlugin = sourceProject.getPlugins().findPlugin( IvyPublishPlugin.class );
			if ( ivyPublishPlugin != null ) {
				targetProject.getPluginManager().apply( IvyPublishPlugin.class );
			}

			final PublishingExtension publishingExtension = (PublishingExtension) targetProject.getExtensions().findByName( "publishing" );
			assert publishingExtension != null;

			// create the publication(s)
			if ( mavenPublishPlugin != null ) {
				final MavenPublication sourceMavenPublication = getMainSourceMavenPublication( sourcePublishingExtension.getPublications() );
				shadowMavenPublication = publishingExtension.getPublications().create( "mavenShadowArtifacts", MavenPublication.class );
				// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
				// https://github.com/gradle/gradle/issues/17273
				// shadowMavenPublication.artifact( shadowPublishArtifact );
				// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
				shadowMavenPublication.artifact(
						shadowPublishArtifact.getFile(),
						(mavenArtifact) -> {
							mavenArtifact.setClassifier( shadowPublishArtifact.getClassifier() );
							mavenArtifact.setExtension( shadowPublishArtifact.getExtension() );
						}
				);

				// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
				PomHelper.copy( sourceProject, sourceMavenPublication.getPom(), targetProject, shadowMavenPublication.getPom(), compileScopeDependencies, runtimeScopeDependencies );
			}

			if ( ivyPublishPlugin != null ) {
				final IvyPublication sourceIvyPublication = getMainSourceIvyPublication( sourcePublishingExtension.getPublications() );
				shadowIvyPublication = publishingExtension.getPublications().create( "ivyShadowArtifacts", IvyPublication.class );
				shadowIvyPublication.artifact( shadowPublishArtifact );
				copy( sourceIvyPublication.getDescriptor(), shadowIvyPublication.getDescriptor() );
			}
		}
	}

	private MavenPublication getMainSourceMavenPublication(PublicationContainer publications) {
		for ( Publication publication : publications ) {
			if ( publication instanceof MavenPublication ) {
				final MavenPublication mavenPublication = (MavenPublication) publication;
				if ( ! mavenPublication.getGroupId().equals( sourceProject.getGroup() ) ) {
					continue;
				}

				if ( ! mavenPublication.getArtifactId().equals( sourceProject.getName() ) ) {
					continue;
				}

				if ( ! mavenPublication.getVersion().equals( sourceProject.getVersion() ) ) {
					continue;
				}

				return mavenPublication;
			}
		}

		throw new TransformationException( "Could not locate main source MavenPublication" );
	}

	private IvyPublication getMainSourceIvyPublication(PublicationContainer publications) {
		for ( Publication publication : publications ) {
			if ( publication instanceof IvyPublication ) {
				final IvyPublication ivyPublication = (IvyPublication) publication;

				if ( ! ivyPublication.getOrganisation().equals( targetProject.getGroup() ) ) {
					continue;
				}

				if ( ! ivyPublication.getModule().equals( targetProject.getName() ) ) {
					continue;
				}

				if ( ! ivyPublication.getRevision().equals( targetProject.getVersion() ) ) {
					continue;
				}

				return ivyPublication;
			}
		}

		throw new TransformationException( "Could not locate main source IvyPublication" );
	}

	private void copy(IvyModuleDescriptorSpec descriptor, IvyModuleDescriptorSpec descriptor1) {

	}

	private ShadowPublishArtifact createArtifactTransformationTask(
			String taskName,
			Jar sourceProjectJarTask,
			String classifier,
			TransformerConfig transformerConfig,
			Project targetProject) {
		final FileTransformationTask transformJarTask = targetProject.getTasks().create(
				taskName,
				FileTransformationTask.class,
				transformerConfig
		);
		transformJarTask.dependsOn( sourceProjectJarTask );
		groupingTask.dependsOn( transformJarTask );

		final Provider<RegularFile> sourceProjectJarFileAccess = sourceProjectJarTask.getArchiveFile();
		transformJarTask.getSource().set( sourceProjectJarFileAccess );
		transformJarTask.getOutput().convention(
				targetProject.provider(
						() -> {
							final String jarFileName = Helper.determineJarFileName( targetProject, classifier );
							return targetProject.getLayout()
									.getBuildDirectory()
									.dir( "libs" )
									.get()
									.file( jarFileName );
						}
				)
		);

		final ShadowPublishArtifact publishArtifact = new ShadowPublishArtifact(
				targetProject.getName(),
				classifier,
				transformJarTask,
				transformerConfig
		);

		return publishArtifact;
	}

	private void shadowConfiguration(String sourceConfigurationName, Configuration targetConfiguration) {
		final Configuration source = sourceProject.getConfigurations().getByName( sourceConfigurationName );
		Helper.shadowConfiguration( source, targetConfiguration, targetProject, transformerConfig );
	}

	@Override
	public void runTests(Action<ShadowTestSpec> specAction) {
		if ( testsSpec == null ) {
			testsSpec = createTestShadowSpec();
		}
		specAction.execute( testsSpec );
	}

	@Override
	public void runTests(Closure<ShadowTestSpec> closure) {
		if ( testsSpec == null ) {
			testsSpec = createTestShadowSpec();
		}
		ConfigureUtil.configure( closure, testsSpec );
	}

	@Override
	public void runTests(Object testsDependencyNotation, Action<ShadowTestSpec> specAction) {
		// for now...
		runTests( specAction );
	}

	@Override
	public void runTests(Object testsDependencyNotation, Closure<ShadowTestSpec> closure) {
		// for now...
		runTests( closure );
	}

	private LocalProjectShadowTestsSpec createTestShadowSpec() {
		return new LocalProjectShadowTestsSpec(
				sourceProject,
				targetProject,
				compileScopeDependencies,
				runtimeScopeDependencies,
				transformerConfig
		);
	}

	@Override
	public void withSources() {
		final JavaPluginExtension sourceProjectJavaPluginExtension = (JavaPluginExtension) sourceProject.getExtensions().getByName( "java" );
		sourceProjectJavaPluginExtension.withSourcesJar();

		final Jar sourceProjectJarTask = (Jar) sourceProject.getTasks().getByName( "sourcesJar" );

		final ShadowPublishArtifact transformationArtifact = createArtifactTransformationTask(
				"shadowSourcesJar",
				sourceProjectJarTask,
				"sources",
				transformerConfig,
				targetProject
		);

		if ( shadowMavenPublication != null ) {
			shadowMavenPublication.artifact(
					transformationArtifact.getFile(),
					(mavenArtifact) -> mavenArtifact.setClassifier( transformationArtifact.getClassifier() )
			);
		}
		if ( shadowIvyPublication != null ) {
			shadowIvyPublication.artifact(
					transformationArtifact.getFile(),
					(ivyArtifact) -> ivyArtifact.setClassifier( transformationArtifact.getClassifier() )
			);
		}
	}

	@Override
	public void withJavadoc() {
		final JavaPluginExtension sourceProjectJavaPluginExtension = (JavaPluginExtension) sourceProject.getExtensions().getByName( "java" );
		sourceProjectJavaPluginExtension.withJavadocJar();

		final Jar sourceProjectJarTask = (Jar) sourceProject.getTasks().getByName( "javadocJar" );

		final ShadowPublishArtifact transformationArtifact = createArtifactTransformationTask(
				"shadowJavadocJar",
				sourceProjectJarTask,
				"javadoc",
				transformerConfig,
				targetProject
		);

		if ( shadowMavenPublication != null ) {
			shadowMavenPublication.artifact(
					transformationArtifact.getFile(),
					(mavenArtifact) -> mavenArtifact.setClassifier( transformationArtifact.getClassifier() )
			);
		}
		if ( shadowIvyPublication != null ) {
			shadowIvyPublication.artifact(
					transformationArtifact.getFile(),
					(ivyArtifact) -> ivyArtifact.setClassifier( transformationArtifact.getClassifier() )
			);
		}
	}
}
