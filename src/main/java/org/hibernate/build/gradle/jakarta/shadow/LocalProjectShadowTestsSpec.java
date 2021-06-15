package org.hibernate.build.gradle.jakarta.shadow;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ProjectDependency;
import org.gradle.api.file.Directory;
import org.gradle.api.internal.tasks.testing.TestFramework;
import org.gradle.api.internal.tasks.testing.junit.JUnitTestFramework;
import org.gradle.api.internal.tasks.testing.junitplatform.JUnitPlatformTestFramework;
import org.gradle.api.internal.tasks.testing.testng.TestNGTestFramework;
import org.gradle.api.plugins.JavaLibraryPlugin;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.api.tasks.testing.Test;
import org.gradle.api.tasks.testing.TestFrameworkOptions;
import org.gradle.api.tasks.testing.junit.JUnitOptions;
import org.gradle.api.tasks.testing.junitplatform.JUnitPlatformOptions;
import org.gradle.api.tasks.testing.testng.TestNGOptions;
import org.gradle.jvm.toolchain.JavaLauncher;
import org.gradle.language.jvm.tasks.ProcessResources;
import org.gradle.util.ConfigureUtil;

import org.hibernate.build.gradle.jakarta.adhoc.DirectoryTransformationTask;
import org.hibernate.build.gradle.jakarta.adhoc.FileTransformationTask;
import org.hibernate.build.gradle.jakarta.internal.Helper;
import org.hibernate.build.gradle.jakarta.internal.TransformerConfig;

import groovy.lang.Closure;

import static org.hibernate.build.gradle.jakarta.shadow.ShadowSpec.SHADOW_GROUPING_TASK;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("UnstableApiUsage")
public class LocalProjectShadowTestsSpec implements ShadowTestSpec {
	public static final String SHADOW_TEST_JAVA_TASK = "shadowTestJava";
	public static final String SHADOW_TEST_RESOURCES_TASK = "shadowTestResources";

	private final Project sourceProject;
	private final Project targetProject;
	private final TransformerConfig transformerConfig;

	/**
	 * We hold the `Test` task reference so we can configure it through the ShadowTestSpec contract
	 */
	private final Test runnerTask;

	public LocalProjectShadowTestsSpec(
			Project sourceProject,
			Project targetProject,
			TransformerConfig transformerConfig) {
		this.sourceProject = sourceProject;
		this.targetProject = targetProject;
		this.transformerConfig = transformerConfig;

		runnerTask = createTestTask();

		final Test sourceTestTask = (Test) sourceProject.getTasks().getByName( "test" );
		final TestFramework sourceTestFramework = sourceTestTask.getTestFramework();
		if ( sourceTestFramework instanceof JUnitTestFramework ) {
			useJUnit();
		}
		else if ( sourceTestFramework instanceof JUnitPlatformTestFramework ) {
			useJUnitPlatform();
		}
		else if ( sourceTestFramework instanceof TestNGTestFramework ) {
			useTestNG();
		}
	}

	private Test createTestTask() {
		final Task groupingTask = targetProject.getTasks().getByName( SHADOW_GROUPING_TASK );

		final Test runnerTask = (Test) targetProject.getTasks().getByName("test" );
		final Test sourceProjectRunnerTask = (Test) sourceProject.getTasks().getByName("test" );

		final JavaLibraryPlugin javaLibraryPlugin = sourceProject.getPlugins().findPlugin( JavaLibraryPlugin.class );
		if ( javaLibraryPlugin != null ) {
			shadowConfiguration( "testImplementation" );
			shadowConfiguration( "testCompileOnly" );
			shadowConfiguration( "testRuntimeOnly" );
		}
		else {
			shadowConfiguration( "testCompile" );
			shadowConfiguration( "testRuntime" );
		}

		final SourceSet sourceTestSourceSet = Helper.extractSourceSets( sourceProject ).getByName( "test" );

		final DirectoryTransformationTask javaTransformationTask = createJavaTransformationTask(
				sourceProject,
				targetProject,
				sourceTestSourceSet,
				groupingTask
		);
		final DirectoryTransformationTask resourcesTransformationTask = createResourcesTransformationTask(
				sourceProject,
				targetProject,
				sourceTestSourceSet,
				groupingTask
		);

		runnerTask.dependsOn( javaTransformationTask );
		runnerTask.dependsOn( resourcesTransformationTask );

		// prepare the Test task's classpath.  add:
		// 	* shadowJar output
		//	* test java shadow output
		//	* test resources shadow output
		final FileTransformationTask shadowJarTask = (FileTransformationTask) targetProject.getTasks().getByName( "shadowJar" );
		runnerTask.getInputs().file( shadowJarTask.getOutput() );
		runnerTask.getInputs().dir( javaTransformationTask.getOutput() );
		runnerTask.getInputs().dir( resourcesTransformationTask.getOutput() );

		runnerTask.setClasspath(
				targetProject.files( shadowJarTask.getOutput() ).plus( runnerTask.getClasspath() )
		);

		// prepare the Test task's classes dir (for test discovery)
		runnerTask.setTestClassesDirs(
				targetProject.files( javaTransformationTask.getOutput(), resourcesTransformationTask.getOutput() )
		);

		// todo : maybe allow hook to transform these
		runnerTask.setAllJvmArgs( sourceProjectRunnerTask.getAllJvmArgs() );
		runnerTask.setSystemProperties( sourceProjectRunnerTask.getSystemProperties() );
		runnerTask.setEnvironment( sourceProjectRunnerTask.getEnvironment() );

		runnerTask.setFailFast( sourceProjectRunnerTask.getFailFast() );
		runnerTask.setIgnoreFailures( sourceProjectRunnerTask.getIgnoreFailures() );
		runnerTask.setDebug( sourceProjectRunnerTask.getDebug() );
		runnerTask.setEnableAssertions( sourceProjectRunnerTask.getEnableAssertions() );

		if ( sourceProjectRunnerTask.getDefaultCharacterEncoding() != null ) {
			runnerTask.setDefaultCharacterEncoding( sourceProjectRunnerTask.getDefaultCharacterEncoding() );
		}

		if ( sourceProjectRunnerTask.getMaxHeapSize() != null ) {
			runnerTask.setMaxHeapSize( sourceProjectRunnerTask.getMaxHeapSize() );
		}
		if ( sourceProjectRunnerTask.getMinHeapSize() != null ) {
			runnerTask.setMinHeapSize( sourceProjectRunnerTask.getMinHeapSize() );
		}


		// todo : allow complete access to the Test task for access?

		return runnerTask;
	}

	private DirectoryTransformationTask createJavaTransformationTask(
			Project sourceProject,
			Project targetProject,
			SourceSet sourceTestSourceSet,
			Task groupingTask) {
		final DirectoryTransformationTask javaTransformationTask = targetProject.getTasks().create(
				SHADOW_TEST_JAVA_TASK,
				DirectoryTransformationTask.class,
				transformerConfig
		);
		groupingTask.dependsOn( javaTransformationTask );
		javaTransformationTask.doFirst(
				new Action<Task>() {
					@Override
					public void execute(Task task) {
						javaTransformationTask.getOutput().getAsFile().get().delete();
					}
				}
		);

		final JavaCompile sourceCompileTestJavaTask = (JavaCompile) sourceProject.getTasks().getByName( sourceTestSourceSet.getCompileJavaTaskName() );
		javaTransformationTask.dependsOn( sourceCompileTestJavaTask );
		javaTransformationTask.getSource().convention( sourceCompileTestJavaTask.getDestinationDirectory() );
		final Provider<Directory> javaTransformationOutputDirectory = targetProject.getLayout().getBuildDirectory().dir( "classes/java/test" );
		javaTransformationTask.getOutput().convention( javaTransformationOutputDirectory );
		return javaTransformationTask;
	}

	private DirectoryTransformationTask createResourcesTransformationTask(
			Project sourceProject,
			Project targetProject,
			SourceSet sourceTestSourceSet,
			Task groupingTask) {
		final DirectoryTransformationTask resourcesTransformationTask = targetProject.getTasks().create(
				SHADOW_TEST_RESOURCES_TASK,
				DirectoryTransformationTask.class,
				transformerConfig
		);
		groupingTask.dependsOn( resourcesTransformationTask );

		resourcesTransformationTask.doFirst(
				new Action<Task>() {
					@Override
					public void execute(Task task) {
						resourcesTransformationTask.getOutput().getAsFile().get().delete();
					}
				}
		);

		final ProcessResources sourceProcessTestResourcesTask = (ProcessResources) sourceProject
				.getTasks()
				.getByName( sourceTestSourceSet.getProcessResourcesTaskName() );

		resourcesTransformationTask.dependsOn( sourceProcessTestResourcesTask );
		resourcesTransformationTask.getSource().convention(
				sourceProject.getLayout().dir(
						sourceProject.provider( sourceProcessTestResourcesTask::getDestinationDir )
				)
		);

		final Provider<Directory> transformResourcesOutputDirectory = targetProject.getLayout().getBuildDirectory().dir( "resources/test" );
		resourcesTransformationTask.getOutput().convention( transformResourcesOutputDirectory );
		return resourcesTransformationTask;
	}

	private void shadowConfiguration(String name) {
		final Configuration source = sourceProject.getConfigurations().getByName( name );
		final Configuration target = targetProject.getConfigurations().getByName( name );

		transformerConfig.applyDependencyResolutionStrategy( target );

		final List<Project> dependencyProjects = new ArrayList<>();
		Helper.shadowConfiguration(
				source,
				target,
				targetProject,
				(dependency) -> {
					if ( dependency instanceof ProjectDependency ) {
						// any project dependency found here might refer to a project that has a shadowed
						// variant which we need to resolve manually in terms of task dependencies
						//
						// see `CrossProjectTransformationController`

						final ProjectDependency projectDependency = (ProjectDependency) dependency;
						final Project dependencyProject = projectDependency.getDependencyProject();

						dependencyProjects.add( dependencyProject );
					}
				}
		);

		if ( ! dependencyProjects.isEmpty() ) {
			transformerConfig.registerShadowTestProjectDependencies( targetProject, dependencyProjects );
		}
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// ShadowTestSpec

	@Override
	public void useJUnit() {
		runnerTask.useJUnit();
	}

	@Override
	public void useJUnit(Closure<JUnitOptions> testFrameworkConfigure) {
		runnerTask.useJUnit( testFrameworkConfigure );
	}

	@Override
	public void useJUnit(Action<JUnitOptions> testFrameworkConfigure) {
		runnerTask.useJUnit( testFrameworkConfigure );
	}

	@Override
	public void useJUnitPlatform() {
		runnerTask.useJUnitPlatform();
	}

	@Override
	public void useJUnitPlatform(Closure<JUnitPlatformOptions> testFrameworkConfigure) {
		ConfigureUtil.configure( testFrameworkConfigure, runnerTask.getOptions() );
	}

	@Override
	public void useJUnitPlatform(Action<JUnitPlatformOptions> testFrameworkConfigure) {
		runnerTask.useJUnitPlatform( testFrameworkConfigure );
	}

	@Override
	public void useTestNG() {
		runnerTask.useTestNG();
	}

	@Override
	public void useTestNG(Closure<TestNGOptions> testFrameworkConfigure) {
		runnerTask.useTestNG( testFrameworkConfigure );
	}

	@Override
	public void useTestNG(Action<TestNGOptions> testFrameworkConfigure) {
		runnerTask.useTestNG( testFrameworkConfigure );
	}

	@Override
	public TestFrameworkOptions options() {
		return runnerTask.getOptions();
	}

	@Override
	public void options(Closure<TestFrameworkOptions> closure) {
		runnerTask.options( closure );
	}

	@Override
	public void options(Action<TestFrameworkOptions> action) {
		runnerTask.options( action );
	}

	@Override
	public boolean getFailFast() {
		return runnerTask.getFailFast();
	}

	@Override
	public void setFailFast(boolean failFast) {
		runnerTask.setFailFast( failFast );
	}

	@Override
	public boolean getIgnoreFailures() {
		return runnerTask.getIgnoreFailures();
	}

	@Override
	public void setIgnoreFailures(boolean ignoreFailures) {
		runnerTask.setIgnoreFailures( ignoreFailures );
	}

	@Override
	public void beforeSuite(Closure<?> closure) {
		runnerTask.beforeSuite( closure );
	}

	@Override
	public void beforeTest(Closure<?> closure) {
		runnerTask.beforeTest( closure );
	}

	@Override
	public void afterTest(Closure<?> closure) {
		runnerTask.afterTest( closure );
	}

	@Override
	public void afterSuite(Closure<?> closure) {
		runnerTask.afterSuite( closure );
	}

	@Override
	public Map<String, Object> systemProperties() {
		return runnerTask.getSystemProperties();
	}

	@Override
	@SuppressWarnings("UnstableApiUsage")
	public JavaLauncher getJavaLauncher() {
		return runnerTask.getJavaLauncher().get();
	}

	@Override
	@SuppressWarnings("UnstableApiUsage")
	public void setJavaLauncher(JavaLauncher launcher) {
		runnerTask.getJavaLauncher().set( launcher );
	}

	@Override
	public String getMinHeapSize() {
		return runnerTask.getMinHeapSize();
	}

	@Override
	public void setMinHeapSize(String size) {
		runnerTask.setMinHeapSize( size );
	}

	@Override
	public String getMaxHeapSize() {
		return runnerTask.getMaxHeapSize();
	}

	@Override
	public void setMaxHeapSize(String size) {
		runnerTask.setMaxHeapSize( size );
	}

	@Override
	public List<String> getJvmArgs() {
		return runnerTask.getJvmArgs();
	}

	@Override
	public Set<String> getIncludes() {
		return runnerTask.getIncludes();
	}

	@Override
	public Set<String> getExcludes() {
		return runnerTask.getExcludes();
	}
}
