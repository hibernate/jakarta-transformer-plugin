import java.io.File;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.BuildTask;
import org.gradle.testkit.runner.TaskOutcome;

import org.junit.jupiter.api.Test;

import com.github.sebersole.testkit.Project;
import com.github.sebersole.testkit.ProjectScope;
import com.github.sebersole.testkit.TestKit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Steve Ebersole
 */
@TestKit
@Project("shadowMulti")
public class ShadowMultiTesting {

	@Test
	public void testShadow(ProjectScope scope) {
		final BuildResult buildResult = scope.createGradleRunner( "clean", "shadow" ).build();

		final BuildTask shadowTask = buildResult.task( ":real-jakarta:shadow" );
		assertThat( shadowTask ).isNotNull();
		assertThat( shadowTask.getOutcome() ).isEqualTo( TaskOutcome.SUCCESS );

		checkEachShadowTask( buildResult, TaskOutcome.SUCCESS );

		verifyShadowLibsDirectory( scope, buildResult );
	}

	private void verifyShadowLibsDirectory(ProjectScope scope, BuildResult buildResult) {
		// Make sure all 3 shadow tasks happened and that we have 3 transformed jars produced
		final File baseDirectory = scope.getProjectBaseDirectory();
		final File libsDir = new File( baseDirectory, "real-jakarta/build/libs" );
		assertTrue( libsDir.exists() );

		final File[] files = libsDir.listFiles();
		assertNotNull( files );
		assertEquals( 3, files.length );

		final File[] mainJarFiles = libsDir.listFiles( (dir, name) -> name.endsWith( "real-jakarta-1.0.0.jar" ) );
		assertEquals( 1, mainJarFiles.length );

		final File[] javadocJarFiles = libsDir.listFiles( (dir, name) -> name.endsWith( "real-jakarta-1.0.0-javadoc.jar" ) );
		assertEquals( 1, javadocJarFiles.length );

		final File[] sourcesJarFiles = libsDir.listFiles( (dir, name) -> name.endsWith( "real-jakarta-1.0.0-sources.jar" ) );
		assertEquals( 1, sourcesJarFiles.length );
	}

	private void checkEachShadowTask(BuildResult buildResult, TaskOutcome expectedOutcome) {
		final BuildTask shadowJarTask = buildResult.task( ":real-jakarta:shadowJar" );
		assertThat( shadowJarTask ).isNotNull();
		assertThat( shadowJarTask.getOutcome() ).isEqualTo( expectedOutcome );

		final BuildTask shadowSourcesJarTask = buildResult.task( ":real-jakarta:shadowSourcesJar" );
		assertThat( shadowSourcesJarTask ).isNotNull();
		assertThat( shadowSourcesJarTask.getOutcome() ).isEqualTo( expectedOutcome );

		final BuildTask shadowJavadocJarTask = buildResult.task( ":real-jakarta:shadowJavadocJar" );
		assertThat( shadowJavadocJarTask ).isNotNull();
		assertThat( shadowJavadocJarTask.getOutcome() ).isEqualTo( expectedOutcome );
	}

	@Test
	public void testShadowCompile(ProjectScope scope) {
		final BuildResult buildResult = scope.createGradleRunner( "clean", "compileJava", "processResources" ).build();

		final BuildTask shadowTask = buildResult.task( ":real-jakarta:shadow" );
		assertThat( shadowTask ).isNull();
	}

	@Test
	public void testShadowAssemble(ProjectScope scope) {
		final BuildResult buildResult = scope.createGradleRunner( "clean", "assemble" ).build();

		checkEachShadowTask( buildResult, TaskOutcome.SUCCESS );
		verifyShadowLibsDirectory( scope, buildResult );
	}

	@Test
	public void testShadowUpToDateChecks(ProjectScope scope) {
		final BuildResult buildResult = scope.createGradleRunner( "clean", "assemble" ).build();
		final BuildTask assembleTaskResult = buildResult.task( ":real-jakarta:assemble" );
		assertThat( assembleTaskResult ).isNotNull();
		assertThat( assembleTaskResult.getOutcome() ).isEqualTo( TaskOutcome.SUCCESS );

		checkEachShadowTask( buildResult, TaskOutcome.SUCCESS );
		verifyShadowLibsDirectory( scope, buildResult );

		// Run the build a second time to check up-to-date checks
		// This is a cumulative check - if assemble is up-to-date all
		// previous tasks should have been up-to-date as well.
		final BuildResult secondBuildResult = scope.createGradleRunner( "assemble", "--info" ).build();
		final BuildTask secondAssembleTaskResult = secondBuildResult.task( ":real-jakarta:assemble" );
		assertThat( secondAssembleTaskResult ).isNotNull();
		assertThat( secondAssembleTaskResult.getOutcome() ).isEqualTo( TaskOutcome.UP_TO_DATE );

		checkEachShadowTask( secondBuildResult, TaskOutcome.UP_TO_DATE );
		verifyShadowLibsDirectory( scope, secondBuildResult );
	}

	@Test
	public void testRealTests(ProjectScope scope) {
		// baseline for tests.  Make sure the "real" tests work before trying the shadowed tests
		final BuildResult buildResult = scope.createGradleRunner( ":real:clean", ":real:test" ).build();

		final BuildTask testTask = buildResult.task( ":real:test" );
		assertThat( testTask ).isNotNull();
		assertThat( testTask.getOutcome() ).isEqualTo( TaskOutcome.SUCCESS );

		final File testResultsDir = new File( scope.getProjectBaseDirectory(), "real/build/test-results/test" );
		assertTrue( testResultsDir.exists() );

		final File binaryTestResultsDir = new File( testResultsDir, "binary" );
		assertTrue( binaryTestResultsDir.exists() );

		assertTrue( binaryTestResultsDir.listFiles().length > 0 );
	}

	@Test
	public void testShadowTests(ProjectScope scope) {
		final BuildResult buildResult = scope.createGradleRunner( "clean", ":real-jakarta:test", "--info" ).build();

		final BuildTask shadowTask = buildResult.task( ":real-jakarta:test" );
		assertThat( shadowTask ).isNotNull();
		assertThat( shadowTask.getOutcome() ).isEqualTo( TaskOutcome.SUCCESS );

		final File testResultsDir = new File( scope.getProjectBaseDirectory(), "real-jakarta/build/test-results/test" );
		assertTrue( testResultsDir.exists() );

		final File binaryTestResultsDir = new File( testResultsDir, "binary" );
		assertTrue( binaryTestResultsDir.exists() );

		assertTrue( binaryTestResultsDir.listFiles().length > 0 );
	}

	@Test
	public void testShadowTestsUpToDateChecks(ProjectScope scope) {
		final BuildResult buildResult = scope.createGradleRunner( "clean", ":real-jakarta:test", "--info" ).build();

		final BuildTask testTask = buildResult.task( ":real-jakarta:test" );
		assertThat( testTask ).isNotNull();
		assertThat( testTask.getOutcome() ).isEqualTo( TaskOutcome.SUCCESS );

		final BuildResult secondBuildResult = scope.createGradleRunner( ":real-jakarta:test", "--info" ).build();
		final BuildTask secondTestTask = secondBuildResult.task( ":real-jakarta:test" );
		assertThat( secondTestTask ).isNotNull();
		assertThat( secondTestTask.getOutcome() ).isEqualTo( TaskOutcome.UP_TO_DATE );
	}

	@Test
	public void testShadowPom(ProjectScope scope) {
		final BuildResult buildResult = scope.createGradleRunner( "clean", "generatePomFileForMavenShadowArtifactsPublication", "--info" ).build();

		final BuildTask shadowTask = buildResult.task( ":real-jakarta:generatePomFileForMavenShadowArtifactsPublication" );
		assertThat( shadowTask ).isNotNull();
		assertThat( shadowTask.getOutcome() ).isEqualTo( TaskOutcome.SUCCESS );

		final File pomDir = new File( scope.getProjectBaseDirectory(), "real-jakarta/build/publications/mavenShadowArtifacts" );
		assertTrue( pomDir.exists() );
		final File[] files = pomDir.listFiles( (dir, name) -> name.startsWith( "pom-" ) && name.endsWith( ".xml" ) );
		assertTrue( files.length == 1 );
	}

}
