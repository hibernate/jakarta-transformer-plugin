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
	public void individualShadowingTest(ProjectScope scope) {
		{
			final BuildResult buildResult = scope.createGradleRunner( "clean", ":hibernate-core-jakarta:shadow" ).build();
			final BuildTask shadowTask = buildResult.task( ":hibernate-core-jakarta:shadow" );
			assertThat( shadowTask ).isNotNull();
			assertThat( shadowTask.getOutcome() ).isEqualTo( TaskOutcome.SUCCESS );
		}

		{
			final BuildResult buildResult = scope.createGradleRunner( "clean", ":hibernate-testing-jakarta:shadow" ).build();
			final BuildTask shadowTask = buildResult.task( ":hibernate-testing-jakarta:shadow" );
			assertThat( shadowTask ).isNotNull();
			assertThat( shadowTask.getOutcome() ).isEqualTo( TaskOutcome.SUCCESS );
		}

		{
			final BuildResult buildResult = scope.createGradleRunner( "clean", ":hibernate-envers-jakarta:shadow" ).build();
			final BuildTask shadowTask = buildResult.task( ":hibernate-envers-jakarta:shadow" );
			assertThat( shadowTask ).isNotNull();
			assertThat( shadowTask.getOutcome() ).isEqualTo( TaskOutcome.SUCCESS );
		}
	}

	@Test
	public void shadowingTest(ProjectScope scope) {
		final BuildResult buildResult = scope.createGradleRunner( "clean", "shadow" ).build();

		final BuildTask shadowTask = buildResult.task( ":hibernate-core-jakarta:shadow" );
		assertThat( shadowTask ).isNotNull();
		assertThat( shadowTask.getOutcome() ).isEqualTo( TaskOutcome.SUCCESS );

		final BuildTask shadowTestingTask = buildResult.task( ":hibernate-testing-jakarta:shadow" );
		assertThat( shadowTestingTask ).isNotNull();
		assertThat( shadowTestingTask.getOutcome() ).isEqualTo( TaskOutcome.SUCCESS );

		final BuildTask shadowEnversTask = buildResult.task( ":hibernate-envers-jakarta:shadow" );
		assertThat( shadowEnversTask ).isNotNull();
		assertThat( shadowEnversTask.getOutcome() ).isEqualTo( TaskOutcome.SUCCESS );
	}

	@Test
	public void baseProjectTestingTest(ProjectScope scope) {
		final BuildResult buildResult = scope.createGradleRunner( "clean", ":hibernate-core:test" ).build();

		final BuildTask testResult = buildResult.task( ":hibernate-core:test" );
		assertThat( testResult ).isNotNull();
		assertThat( testResult.getOutcome() ).isEqualTo( TaskOutcome.SUCCESS );
	}

	@Test
	public void shadowTestingTest(ProjectScope scope) {
		final BuildResult buildResult = scope.createGradleRunner( "clean", ":hibernate-core-jakarta:test" ).build();

		final BuildTask testResult = buildResult.task( ":hibernate-core-jakarta:test" );
		assertThat( testResult ).isNotNull();
		assertThat( testResult.getOutcome() ).isEqualTo( TaskOutcome.SUCCESS );
	}

	@Test
	public void showDependenciesTest(ProjectScope scope) {
		scope.createGradleRunner( ":hibernate-core-jakarta:dependencies", "--configuration", "testRuntimeClasspath" ).build();
	}

	@Test
	public void pomGenerationTest(ProjectScope scope) {
		final BuildResult buildResult = scope.createGradleRunner( "clean", "generatePomFileForMavenShadowArtifactsPublication" ).build();

		final BuildTask shadowTask = buildResult.task( ":hibernate-core-jakarta:generatePomFileForMavenShadowArtifactsPublication" );
		assertThat( shadowTask ).isNotNull();
		assertThat( shadowTask.getOutcome() ).isEqualTo( TaskOutcome.SUCCESS );

		final File pomDir = new File( scope.getProjectBaseDirectory(), "hibernate-core-jakarta/build/publications/mavenShadowArtifacts" );
		assertTrue( pomDir.exists() );
		final File[] files = pomDir.listFiles( (dir, name) -> name.startsWith( "pom-" ) && name.endsWith( ".xml" ) );
		assertTrue( files.length == 1 );
	}

	@Test
	public void testEnversTesting(ProjectScope scope) {
		scope.createGradleRunner( ":hibernate-core:test" ).build();
	}
	@Test
	public void testEnversJakartaTesting(ProjectScope scope) {
		scope.createGradleRunner( ":hibernate-core-jakarta:test" ).build();
	}

}
