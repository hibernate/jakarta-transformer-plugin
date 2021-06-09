import java.io.File;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.BuildTask;
import org.gradle.testkit.runner.TaskOutcome;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.github.sebersole.testkit.Project;
import com.github.sebersole.testkit.ProjectScope;
import com.github.sebersole.testkit.TestKit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Steve Ebersole
 */
@TestKit
@Project("shadowShared")
public class ShadowSharedTesting {
	@Test
	public void shadowingTest(ProjectScope scope) {
		final BuildResult buildResult = scope.createGradleRunner( "clean", "shadow" ).build();

		final BuildTask shadowTask = buildResult.task( ":real-jakarta:shadow" );
		assertThat( shadowTask ).isNotNull();
		assertThat( shadowTask.getOutcome() ).isEqualTo( TaskOutcome.SUCCESS );

		final BuildTask shadowTestingTask = buildResult.task( ":real-testing-jakarta:shadow" );
		assertThat( shadowTestingTask ).isNotNull();
		assertThat( shadowTestingTask.getOutcome() ).isEqualTo( TaskOutcome.SUCCESS );
	}

	@Test
	public void realTestingTest(ProjectScope scope) {
		final BuildResult buildResult = scope.createGradleRunner( "clean", ":real:test" ).build();

		final BuildTask testResult = buildResult.task( ":real:test" );
		assertThat( testResult ).isNotNull();
		assertThat( testResult.getOutcome() ).isEqualTo( TaskOutcome.SUCCESS );
	}

	@Test
	public void jakartaTestingTest(ProjectScope scope) {
		final BuildResult buildResult = scope.createGradleRunner( "clean", ":real-jakarta:test" ).build();

		final BuildTask testResult = buildResult.task( ":real-jakarta:test" );
		assertThat( testResult ).isNotNull();
		assertThat( testResult.getOutcome() ).isEqualTo( TaskOutcome.SUCCESS );
	}

	@Test
	public void jakartaTestingTest2(ProjectScope scope) {
		final BuildResult buildResult = scope.createGradleRunner( "clean", ":real-testing-jakarta:assemble", ":real-jakarta:test" ).build();

		final BuildTask testResult = buildResult.task( ":real-jakarta:test" );
		assertThat( testResult ).isNotNull();
		assertThat( testResult.getOutcome() ).isEqualTo( TaskOutcome.SUCCESS );
	}

	@Test
	public void showDependenciesTest(ProjectScope scope) {
		scope.createGradleRunner( ":real-jakarta:dependencies", "--configuration", "testRuntimeScope" ).build();
	}

	@Test
	public void pomGenerationTest(ProjectScope scope) {
		final BuildResult buildResult = scope.createGradleRunner( "clean", "generatePomFileForMavenShadowArtifactsPublication" ).build();

		final BuildTask shadowTask = buildResult.task( ":real-jakarta:generatePomFileForMavenShadowArtifactsPublication" );
		assertThat( shadowTask ).isNotNull();
		assertThat( shadowTask.getOutcome() ).isEqualTo( TaskOutcome.SUCCESS );

		final File pomDir = new File( scope.getProjectBaseDirectory(), "real-jakarta/build/publications/mavenShadowArtifacts" );
		assertTrue( pomDir.exists() );
		final File[] files = pomDir.listFiles( (dir, name) -> name.startsWith( "pom-" ) && name.endsWith( ".xml" ) );
		assertTrue( files.length == 1 );
	}

}
