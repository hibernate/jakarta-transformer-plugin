package org.hibernate.build.gradle.jakarta.shadow;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.Directory;
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

import org.hibernate.build.gradle.jakarta.internal.Helper;
import org.hibernate.build.gradle.jakarta.internal.TransformerConfig;

import groovy.lang.Closure;

import static org.hibernate.build.gradle.jakarta.shadow.ShadowSpec.SHADOW_GROUPING_TASK;
import static org.hibernate.build.gradle.jakarta.shadow.ShadowSpec.TASK_GROUP;

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

	public LocalProjectShadowTestsSpec(Project sourceProject, Project targetProject, TransformerConfig transformerConfig) {
		this.sourceProject = sourceProject;
		this.targetProject = targetProject;
		this.transformerConfig = transformerConfig;

		shadowConfiguration( "testCompileOnly" );
		shadowConfiguration( "testImplementation" );
		shadowConfiguration( "testRuntimeOnly" );

		shadowConfiguration( "testCompileClasspath" );
		shadowConfiguration( "testRuntimeClasspath" );

		final SourceSet sourceTestSourceSet = Helper.extractSourceSets( sourceProject ).getByName( "test" );
		final SourceSet shadowTestSourceSet = Helper.extractSourceSets( targetProject ).getByName( "test" );

		final Directory unpackBaseDirectory = Helper.determineUnpackBaseDir( sourceProject, targetProject );
		final Directory shadowBaseCopyDirectory = unpackBaseDirectory.dir( "test" );

		final Directory shadowJavaDirectory = shadowBaseCopyDirectory.dir( "java" );
		shadowTestSourceSet.getAllJava().srcDir( shadowJavaDirectory );

		final Directory shadowResourcesDirectory = shadowBaseCopyDirectory.dir( "resources" );
		shadowTestSourceSet.getResources().srcDir( shadowResourcesDirectory );


		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// prepare tasks
		//		1. create `:shadow:{SHADOW_TEST_JAVA_TASK}` to transform the source project's test java
		//		2. `:shadow:compileTestJava` depends-on `:shadow:{SHADOW_TEST_JAVA_TASK}`
		//		3. create `:shadow:{SHADOW_TEST_RESOURCES_TASK}` to transform the source project's test resources
		//		4. `:shadow:{SHADOW_TEST_RESOURCES_TASK}` depends-on `:source:processTestResources`
		//		5. `:shadow:processTestResources` depends-on `:shadow:{SHADOW_TEST_RESOURCES_TASK}`
		//		6. `:shadow:shadow` depends on both `:shadow:{SHADOW_TEST_JAVA_TASK}` and `:shadow:{SHADOW_TEST_RESOURCES_TASK}`
		//		7. prepare `:shadow:test`
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~


		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// 1. create `:shadow:{SHADOW_TEST_JAVA_TASK}`
		final TransformLocalSourcesTask transformJavaSourcesTask = targetProject.getTasks().create(
				SHADOW_TEST_JAVA_TASK,
				TransformLocalSourcesTask.class,
				sourceTestSourceSet.getAllJava(),
				shadowJavaDirectory,
				false,
				sourceTestSourceSet,
				transformerConfig,
				sourceProject
		);
		transformJavaSourcesTask.setGroup( TASK_GROUP );


		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// 2. prepare `:shadow:compileTestJava`
		final JavaCompile shadowCompile = (JavaCompile) targetProject.getTasks().getByName( shadowTestSourceSet.getCompileJavaTaskName() );
		shadowCompile.dependsOn( transformJavaSourcesTask );
		shadowCompile.source( shadowJavaDirectory );


		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// 3. create `:shadow:{SHADOW_TEST_RESOURCES_TASK}`
		final TransformLocalSourcesTask transformResourcesTask = targetProject.getTasks().create(
				SHADOW_TEST_RESOURCES_TASK,
				TransformLocalSourcesTask.class,
				sourceTestSourceSet.getResources(),
				shadowResourcesDirectory,
				true,
				sourceTestSourceSet,
				transformerConfig,
				sourceProject
		);
		transformResourcesTask.setGroup( TASK_GROUP );


		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// 4. `:shadow:{SHADOW_TEST_RESOURCES_TASK}` depends-on `:source:processTestResources`
		final ProcessResources sourceProcessResourcesTask = (ProcessResources) sourceProject.getTasks().getByName(
				sourceTestSourceSet.getProcessResourcesTaskName()
		);
		transformResourcesTask.dependsOn( sourceProcessResourcesTask );


		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// 5. prepare `:shadow:processTestResources`
		final ProcessResources shadowProcessResourcesTask = (ProcessResources) targetProject.getTasks().getByName(
				shadowTestSourceSet.getProcessResourcesTaskName()
		);
		shadowProcessResourcesTask.dependsOn( transformResourcesTask );
		shadowProcessResourcesTask.getSource().plus( targetProject.fileTree( shadowResourcesDirectory ) );
		shadowProcessResourcesTask.getInputs().dir( shadowResourcesDirectory );


		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// 6. prepare `:shadow:shadow`
		final Task shadowGroupingTask = targetProject.getTasks().getByName( SHADOW_GROUPING_TASK );
		shadowGroupingTask.dependsOn( transformJavaSourcesTask );
		shadowGroupingTask.dependsOn( transformResourcesTask );


		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// 7. prepare `:shadow:test`
		runnerTask = (Test) targetProject.getTasks().getByName( "test" );
		runnerTask.dependsOn( shadowGroupingTask );
	}

	private void shadowConfiguration(String configurationName) {
		Helper.shadowConfiguration( configurationName, sourceProject, targetProject, transformerConfig );
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
