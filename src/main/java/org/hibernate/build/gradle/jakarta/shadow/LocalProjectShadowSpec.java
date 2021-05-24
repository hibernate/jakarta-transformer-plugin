package org.hibernate.build.gradle.jakarta.shadow;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.Directory;
import org.gradle.api.plugins.JavaLibraryPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.language.jvm.tasks.ProcessResources;
import org.gradle.util.ConfigureUtil;

import org.hibernate.build.gradle.jakarta.internal.Helper;
import org.hibernate.build.gradle.jakarta.internal.TransformerConfig;

import groovy.lang.Closure;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("UnstableApiUsage")
public class LocalProjectShadowSpec implements ShadowSpec {
	public static final String SHADOW_JAVA_TASK = "shadowJava";
	public static final String SHADOW_RESOURCES_TASK = "shadowResources";

	private final Project sourceProject;
	private final Project targetProject;
	private final TransformerConfig transformerConfig;

	private LocalProjectShadowTestsSpec testsSpec;

	public LocalProjectShadowSpec(
			Project sourceProject,
			Project targetProject,
			TransformerConfig transformerConfig) {
		this.sourceProject = sourceProject;
		this.targetProject = targetProject;
		this.transformerConfig = transformerConfig;

		final boolean isJavaLibrary = sourceProject.getPlugins().findPlugin( JavaLibraryPlugin.class ) != null;

		if ( isJavaLibrary ) {
			targetProject.getPluginManager().apply( "java-library" );

			shadowConfiguration( "api" );
			shadowConfiguration( "implementation" );
			shadowConfiguration( "compileOnly" );
			shadowConfiguration( "apiElements" );
			shadowConfiguration( "runtimeElements" );
		}
		else {
			targetProject.getPluginManager().apply( "java" );
		}

		shadowConfiguration( "compileClasspath" );
		shadowConfiguration( "runtimeClasspath" );

		final SourceSet sourceMainSourceSet = Helper.extractSourceSets( sourceProject ).getByName( "main" );
		final SourceSet shadowMainSourceSet = Helper.extractSourceSets( targetProject ).getByName( "main" );

		final Directory unpackBaseDir = Helper.determineUnpackBaseDir( sourceProject, targetProject );
		final Directory shadowMainCopyDir = unpackBaseDir.dir( "main" );

		final Directory shadowJavaTargetDirectory = shadowMainCopyDir.dir( "java" );
		shadowMainSourceSet.getAllJava().srcDir( shadowJavaTargetDirectory );

		final Directory shadowResourcesTargetDirectory = shadowMainCopyDir.dir( "resources" );
		shadowMainSourceSet.getResources().srcDir( shadowResourcesTargetDirectory );

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// prepare tasks
		//		1. create `:shadow:{SHADOW_JAVA_TASK}` to transform the source project's main java
		//		2. `:shadow:compileJava` depends-on `:shadow:{SHADOW_JAVA_TASK}`
		//		3. create `:shadow:{SHADOW_RESOURCES_TASK}` to transform the source project's main resources
		//		4. `:shadow:{SHADOW_RESOURCES_TASK}` depends-on `:source:processResources`
		//		5. `:shadow:processResources` depends-on `:shadow:{SHADOW_RESOURCES_TASK}`
		//		6. create a `:shadow:shadow` "grouping" task
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~


		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// 1. create `:shadow:{SHADOW_JAVA_TASK}`
		final TransformLocalSourcesTask transformMainSourcesTask = targetProject.getTasks().create(
				SHADOW_JAVA_TASK,
				TransformLocalSourcesTask.class,
				sourceMainSourceSet.getAllJava(),
				shadowJavaTargetDirectory,
				false,
				sourceMainSourceSet,
				transformerConfig,
				sourceProject
		);
		transformMainSourcesTask.setGroup( TASK_GROUP );


		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// 2. prepare `:shadow:compile`
		final JavaCompile shadowCompile = (JavaCompile) targetProject.getTasks().getByName( shadowMainSourceSet.getCompileJavaTaskName() );
		shadowCompile.dependsOn( transformMainSourcesTask );
		shadowCompile.source( shadowJavaTargetDirectory );


		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// 3. create `:shadow:{SHADOW_RESOURCES_TASK}`
		final TransformLocalSourcesTask transformMainResourcesTask = targetProject.getTasks().create(
				SHADOW_RESOURCES_TASK,
				TransformLocalSourcesTask.class,
				shadowMainSourceSet.getResources(),
				shadowResourcesTargetDirectory,
				true,
				sourceMainSourceSet,
				transformerConfig,
				sourceProject
		);
		transformMainResourcesTask.setGroup( TASK_GROUP );


		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// 4. `:shadow:{SHADOW_RESOURCES_TASK}` depends-on `:source:processResources`
		final ProcessResources sourceMainProcessResourcesTask = (ProcessResources) sourceProject.getTasks().getByName( "processResources" );
		transformMainResourcesTask.dependsOn( sourceMainProcessResourcesTask );


		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// 5. prepare `:shadow:processResources`
		final ProcessResources shadowMainProcessResourcesTask = (ProcessResources) targetProject.getTasks().getByName( "processResources" );
		shadowMainProcessResourcesTask.dependsOn( transformMainResourcesTask );
		shadowMainProcessResourcesTask.getSource().plus( targetProject.fileTree( shadowResourcesTargetDirectory ) );
		shadowMainProcessResourcesTask.getInputs().dir( shadowResourcesTargetDirectory );


		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// 6. Create the "grouping" task
		final Task groupingTask = targetProject.getTasks().create( SHADOW_GROUPING_TASK );
		groupingTask.setGroup( TASK_GROUP );
		groupingTask.dependsOn( transformMainSourcesTask );
		groupingTask.dependsOn( shadowMainProcessResourcesTask );
	}

	private void shadowConfiguration(String configurationName) {
		Helper.shadowConfiguration( configurationName, sourceProject, targetProject, transformerConfig );
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
		return new LocalProjectShadowTestsSpec( sourceProject, targetProject, transformerConfig );
	}

	@Override
	public void withSources() {
		final JavaPluginExtension javaPluginExtension = (JavaPluginExtension) targetProject.getExtensions().getByName( "java" );
		javaPluginExtension.withSourcesJar();
	}

	@Override
	public void withJavadoc() {
		final JavaPluginExtension javaPluginExtension = (JavaPluginExtension) targetProject.getExtensions().getByName( "java" );
		javaPluginExtension.withJavadocJar();
	}
}
