package org.hibernate.build.gradle.jakarta.shadow;

import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;

import org.gradle.api.Action;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.file.Directory;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.api.tasks.testing.Test;
import org.gradle.api.tasks.testing.TestFrameworkOptions;
import org.gradle.api.tasks.testing.junit.JUnitOptions;
import org.gradle.api.tasks.testing.junitplatform.JUnitPlatformOptions;
import org.gradle.api.tasks.testing.testng.TestNGOptions;
import org.gradle.jvm.toolchain.JavaLauncher;
import org.gradle.language.jvm.tasks.ProcessResources;
import org.gradle.util.ConfigureUtil;

import org.hibernate.build.gradle.jakarta.ShadowTestSpec;
import org.hibernate.build.gradle.jakarta.internal.Helper;
import org.hibernate.build.gradle.jakarta.internal.TransformerConfig;

import groovy.lang.Closure;

/**
 * @author Steve Ebersole
 */
public abstract class ShadowLocalProjectTestTask extends DefaultTask implements ShadowTestSpec {
	private final Project sourceProject;
	private final TransformerConfig transformerConfig;

	private final Test runnerTask;

	@Inject
	public ShadowLocalProjectTestTask(Project sourceProject, TransformerConfig transformerConfig) {
		this.sourceProject = sourceProject;
		this.transformerConfig = transformerConfig;

		shadowConfiguration( "testCompileOnly" );
		shadowConfiguration( "testImplementation" );
		shadowConfiguration( "testRuntimeOnly" );

		shadowConfiguration( "testCompileClasspath" );
		shadowConfiguration( "testRuntimeClasspath" );

		final SourceSet sourceTestSourceSet = Helper.extractSourceSets( sourceProject ).getByName( "test" );
		final SourceSet shadowTestSourceSet = Helper.extractSourceSets( getProject() ).getByName( "test" );

		final Directory unpackBaseDirectory = Helper.determineUnpackBaseDir( sourceProject, getProject() );
		final Directory shadowBaseCopyDirectory = unpackBaseDirectory.dir( "test" );
		final Directory shadowJavaDirectory = shadowBaseCopyDirectory.dir( "java" );
		final Directory shadowResourcesDirectory = shadowBaseCopyDirectory.dir( "resources" );

		runnerTask = (Test) getProject().getTasks().getByName( "test" );
		runnerTask.dependsOn( this );

		final JavaCompile shadowCompile = (JavaCompile) getProject().getTasks().getByName( shadowTestSourceSet.getCompileJavaTaskName() );
		this.dependsOn( shadowCompile );

		final TransformLocalSourcesTask transformJavaSourcesTask = getProject().getTasks().create(
				"shadowTestJava_" + sourceProject.getName(),
				TransformLocalSourcesTask.class,
				sourceTestSourceSet.getAllJava(),
				shadowJavaDirectory,
				false,
				sourceTestSourceSet,
				transformerConfig,
				sourceProject
		);
		shadowCompile.dependsOn( transformJavaSourcesTask );

		shadowCompile.source( shadowJavaDirectory );
		shadowTestSourceSet.getAllJava().srcDir( shadowJavaDirectory );

		//noinspection UnstableApiUsage
		final ProcessResources sourceProcessResourcesTask = (ProcessResources) sourceProject.getTasks().getByName(
				sourceTestSourceSet.getProcessResourcesTaskName()
		);

		final TransformLocalSourcesTask transformMainResourcesTask = getProject().getTasks().create(
				"shadowTestResources_" + sourceProject.getName(),
				TransformLocalSourcesTask.class,
				sourceTestSourceSet.getResources(),
				shadowResourcesDirectory,
				true,
				sourceTestSourceSet,
				transformerConfig,
				sourceProject
		);
		transformMainResourcesTask.dependsOn( sourceProcessResourcesTask );
		this.dependsOn( transformMainResourcesTask );

		//noinspection UnstableApiUsage
		final ProcessResources shadowMainProcessResourcesTask = (ProcessResources) getProject().getTasks().getByName(
				shadowTestSourceSet.getProcessResourcesTaskName()
		);
		this.dependsOn( shadowMainProcessResourcesTask );
		shadowMainProcessResourcesTask.dependsOn( transformMainResourcesTask );

		shadowTestSourceSet.getResources().srcDir( shadowResourcesDirectory );
		shadowMainProcessResourcesTask.getSource().plus( getProject().fileTree( shadowResourcesDirectory ) );
		shadowMainProcessResourcesTask.getInputs().dir( shadowResourcesDirectory );
	}


	@SuppressWarnings("UnusedReturnValue")
	private void shadowConfiguration(String configurationName) {
		Helper.shadowConfiguration( configurationName, sourceProject, getProject(), transformerConfig );
	}

	@TaskAction
	public void performTransformation() {
		getProject().getLogger().lifecycle( "Coordinating `sourceSet.test` transformation" );
	}






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
