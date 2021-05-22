package org.hibernate.build.gradle.jakarta.shadow;

import javax.inject.Inject;

import org.gradle.api.Action;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ProjectDependency;
import org.gradle.api.file.Directory;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.language.jvm.tasks.ProcessResources;
import org.gradle.util.ConfigureUtil;

import org.hibernate.build.gradle.jakarta.ShadowSpec;
import org.hibernate.build.gradle.jakarta.ShadowTestSpec;
import org.hibernate.build.gradle.jakarta.internal.Helper;
import org.hibernate.build.gradle.jakarta.internal.TransformerConfig;

import groovy.lang.Closure;

/**
 * Variation of shadowing for shadowing a sub-project within the same project
 * to which the plugin is applied
 *
 * @author Steve Ebersole
 */
public abstract class ShadowLocalProjectTask extends DefaultTask implements ShadowSpec {
	private final Project sourceProject;
	private final TransformerConfig transformerConfig;

	private ShadowLocalProjectTestTask shadowTestsTask;

	@Inject
	public ShadowLocalProjectTask(
			ProjectDependency localProject,
			TransformerConfig transformerConfig) {
		sourceProject = localProject.getDependencyProject();
		this.transformerConfig = transformerConfig;

		shadowConfiguration( "api" );
		shadowConfiguration( "implementation" );
		shadowConfiguration( "compileOnly" );
		shadowConfiguration( "compileClasspath" );
		shadowConfiguration( "runtimeClasspath" );
		shadowConfiguration( "apiElements" );
		shadowConfiguration( "runtimeElements" );

		final SourceSet sourceMainSourceSet = Helper.extractSourceSets( sourceProject ).getByName( "main" );
		final SourceSet shadowMainSourceSet = Helper.extractSourceSets( getProject() ).getByName( "main" );

		final Directory unpackBaseDir = Helper.determineUnpackBaseDir( sourceProject, getProject() );

		final Directory shadowMainCopyDir = unpackBaseDir.dir( "main" );

		// prepare the shadow-compile task
		// transform all Java sources from the shadow-source project to the shadow project
		final Directory shadowJavaTargetDirectory = shadowMainCopyDir.dir( "java" );
		final JavaCompile shadowCompile = (JavaCompile) getProject().getTasks().getByName( shadowMainSourceSet.getCompileJavaTaskName() );
		this.dependsOn( shadowCompile );

		shadowCompile.source( shadowJavaTargetDirectory );
		shadowMainSourceSet.getAllJava().srcDir( shadowJavaTargetDirectory );

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// handle transformation for each Java source directory in the source
		// project's main source-set

		final TransformLocalSourcesTask transformMainSourcesTask = getProject().getTasks().create(
				"shadowMainJava_" + sourceProject.getName(),
				TransformLocalSourcesTask.class,
				sourceMainSourceSet.getAllJava(),
				shadowJavaTargetDirectory,
				false,
				sourceMainSourceSet,
				transformerConfig,
				sourceProject
		);
		shadowCompile.dependsOn( transformMainSourcesTask );


		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// transform all resources from the shadow-source project to the shadow
		// project
		// NOTE : we use the processed-resources from the shadow-source so
		// filtering/replacing have already been applied

		final Directory shadowResourcesTargetDirectory = shadowMainCopyDir.dir( "resources" );
		//noinspection UnstableApiUsage
		final ProcessResources sourceMainProcessResourcesTask = (ProcessResources) sourceProject.getTasks().getByName( "processResources" );

		final TransformLocalSourcesTask transformMainResourcesTask = getProject().getTasks().create(
				"shadowMainResources_" + sourceProject.getName(),
				TransformLocalSourcesTask.class,
				shadowMainSourceSet.getResources(),
				shadowResourcesTargetDirectory,
				true,
				sourceMainSourceSet,
				transformerConfig,
				sourceProject
		);
		transformMainResourcesTask.dependsOn( sourceMainProcessResourcesTask );
		this.dependsOn( transformMainResourcesTask );

		//noinspection UnstableApiUsage
		final ProcessResources shadowMainProcessResourcesTask = (ProcessResources) getProject().getTasks().getByName( "processResources" );
		this.dependsOn( shadowMainProcessResourcesTask );
		shadowMainProcessResourcesTask.dependsOn( transformMainResourcesTask );

		shadowMainSourceSet.getResources().srcDir( shadowResourcesTargetDirectory );
		shadowMainProcessResourcesTask.getSource().plus( getProject().fileTree( shadowResourcesTargetDirectory ) );
		shadowMainProcessResourcesTask.getInputs().dir( shadowResourcesTargetDirectory );
	}

	@Input
	public String getSourceDependency() {
		return sourceProject.getGroup() + sourceProject.getName() + sourceProject.getVersion();
	}

	private void shadowConfiguration(String configurationName) {
		Helper.shadowConfiguration( configurationName, sourceProject, getProject(), transformerConfig );
	}

	@Override
	public void runTests(Action<ShadowTestSpec> specAction) {
		if ( shadowTestsTask == null ) {
			createShadowTask();
		}
		specAction.execute( shadowTestsTask );
	}

	private void createShadowTask() {
		shadowTestsTask = getProject().getTasks().create(
				"shadowTestTransform",
				ShadowLocalProjectTestTask.class,
				sourceProject,
				transformerConfig
		);
	}

	@Override
	public void runTests(Closure<ShadowTestSpec> closure) {
		if ( shadowTestsTask == null ) {
			createShadowTask();
		}
		ConfigureUtil.configure( closure, shadowTestsTask );
	}

	@Override
	public void runTests(Object testsDependencyNotation, Action<ShadowTestSpec> specAction) {
		// todo : assert that the incoming dependency matches the source project?
		runTests( specAction );
	}

	@Override
	public void runTests(Object testsDependencyNotation, Closure closure) {
		// todo : assert that the incoming dependency matches the source project?
		runTests( closure );
	}

	@Override
	@SuppressWarnings("UnstableApiUsage")
	public void withSources() {
		final JavaPluginExtension javaPluginExtension = (JavaPluginExtension) getProject().getExtensions().getByName( "java" );
		javaPluginExtension.withSourcesJar();
	}

	@Override
	@SuppressWarnings("UnstableApiUsage")
	public void withJavadoc() {
		final JavaPluginExtension javaPluginExtension = (JavaPluginExtension) getProject().getExtensions().getByName( "java" );
		javaPluginExtension.withJavadocJar();
	}

	@TaskAction
	public void performTransformation() {
		getProject().getLogger().lifecycle( "Coordinating `sourceSet.main` transformation" );
	}
}
