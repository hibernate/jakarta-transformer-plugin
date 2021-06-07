package org.hibernate.build.gradle.jakarta.shadow;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.Directory;
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
		runnerTask.doFirst(
				new Action<Task>() {
					@Override
					public void execute(Task testTask) {
						targetProject.getLogger().debug( "############################################################" );
						targetProject.getLogger().debug( " `:{shadow}:test` task classpath..." );
						targetProject.getLogger().debug( "############################################################" );
						runnerTask.getClasspath().forEach(
								(cpEntry) -> targetProject.getLogger().debug( "   > {}", cpEntry.getAbsolutePath() )
						);
						targetProject.getLogger().debug( "############################################################" );
						targetProject.getLogger().debug( "############################################################" );
					}
				}
		);
	}

	private Test createTestTask() {
		final Configuration testRuntimeClasspath = targetProject.getConfigurations().maybeCreate( "testRuntimeClasspath" );
		shadowConfiguration( "testRuntimeClasspath" );

		final SourceSet sourceTestSourceSet = Helper.extractSourceSets( sourceProject ).getByName( "test" );

		final Task groupingTask = targetProject.getTasks().getByName( SHADOW_GROUPING_TASK );

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

		final Test runnerTask = targetProject.getTasks().maybeCreate( "test", Test.class );
		runnerTask.dependsOn( groupingTask );

		// prepare the Test task's classpath.  add:
		// 	* shadowJar output
		//	* test java shadow output
		//	* test resources shadow output
		final FileTransformationTask shadowJarTask = (FileTransformationTask) targetProject.getTasks().getByName( "shadowJar" );
		runnerTask.setClasspath(
				testRuntimeClasspath.plus( targetProject.files( shadowJarTask.getOutput() ) )
						.plus( targetProject.files( javaTransformationTask.getOutput() ) )
						.plus( targetProject.files( resourcesTransformationTask.getOutput() ) )
		);

		// prepare the Test task's classes dir (for test discovery)
		runnerTask.setTestClassesDirs(
				targetProject.files( javaTransformationTask.getOutput(), resourcesTransformationTask.getOutput() )
		);

		// prepare the Test task's outputs
		runnerTask.getBinaryResultsDirectory().convention( targetProject.getLayout().getBuildDirectory().dir( "test-results/test/binary" ) );
		runnerTask.getReports().getJunitXml().getOutputLocation().convention( targetProject.getLayout().getBuildDirectory().dir( "reports/tests/test" ) );
		runnerTask.getReports().getHtml().getOutputLocation().convention( targetProject.getLayout().getBuildDirectory().dir( "test-results/test" ) );

		// todo : realistically we probably need to copy over stuff like jvm-args, etc...
		//		although we could simply let user do that in the target-project build script
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
						targetProject.getLogger().lifecycle( "###########################################" );
						targetProject.getLogger().lifecycle( "Deleting shadow test-Java dir because... well... jakarta" );
						targetProject.getLogger().lifecycle( "###########################################" );
						javaTransformationTask.getOutput().getAsFile().get().delete();
						targetProject.getLogger().lifecycle( "###########################################" );
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
						targetProject.getLogger().lifecycle( "###########################################" );
						targetProject.getLogger().lifecycle( "Deleting shadow test-resources dir because... well... jakarta" );
						targetProject.getLogger().lifecycle( "###########################################" );
						resourcesTransformationTask.getOutput().getAsFile().get().delete();
						targetProject.getLogger().lifecycle( "###########################################" );
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

	private void shadowConfiguration(String configurationName) {
		Helper.shadowConfiguration( configurationName, sourceProject, targetProject, transformerConfig );

		final Configuration targetConfiguration = targetProject.getConfigurations().getByName( configurationName );
		targetConfiguration.getResolutionStrategy().dependencySubstitution(
				(dependencySubstitutions) -> {
					dependencySubstitutions.substitute( dependencySubstitutions.project( sourceProject.getPath() ) )
							.with( dependencySubstitutions.project( targetProject.getPath() ) );
				}
		);
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
