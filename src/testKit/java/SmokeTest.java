import java.io.File;

import org.junit.jupiter.api.Test;

import com.github.sebersole.testkit.Project;
import com.github.sebersole.testkit.ProjectScope;
import com.github.sebersole.testkit.TestKit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Steve Ebersole
 */
@TestKit
public class SmokeTest {
	@Test
	@Project("shadow")
	public void testShadowDependency(ProjectScope scope) {
		scope.createGradleRunner( "clean", "shadowTransform" ).build();
	}

	@Test
	@Project("shadow")
	public void testShadowDependencyJar(ProjectScope scope) {
		scope.createGradleRunner( "clean", "jar" ).build();

		final File baseDirectory = scope.getProjectBaseDirectory();

		final File libsDir = new File( baseDirectory, "build/libs" );
		assertTrue( libsDir.exists() );

		final File[] files = libsDir.listFiles();
		assertNotNull( files );
		assertEquals( 3, files.length );

		final File[] mainJarFiles = libsDir.listFiles( (dir, name) -> name.equals( "shadow-1.0.0-SNAPSHOT.jar" ) );
		assertEquals( 1, mainJarFiles.length );

		final File[] javadocJarFiles = libsDir.listFiles( (dir, name) -> name.equals( "shadow-1.0.0-SNAPSHOT-javadoc.jar" ) );
		assertEquals( 1, javadocJarFiles.length );

		final File[] sourcesJarFiles = libsDir.listFiles( (dir, name) -> name.endsWith( "shadow-1.0.0-SNAPSHOT-sources.jar" ) );
		assertEquals( 1, sourcesJarFiles.length );
	}

// Need a dependency which publishes tests to verify this...
//	@Test
//	@Project("shadow")
//	public void testShadowDependencyTests(ProjectScope scope) {
//		scope.createGradleRunner( "clean", "runTransformedShadowTests" ).build();
//	}

	@Test
	@Project("shadowMulti")
	public void testShadowMulti(ProjectScope scope) {
		scope.createGradleRunner( "clean", "shadowTransform" ).build();

		final File baseDirectory = scope.getProjectBaseDirectory();

		final File copyDir = new File( baseDirectory, "shadow/build/generated/sources/jakarta/transform/real/main/java" );
		assertTrue( copyDir.exists() );

		final File copiedJpaConsumerFile = new File( copyDir, "JpaConsumer.java" );
		assertTrue( copiedJpaConsumerFile.exists() );

		final File copiedHelperFile = new File( copyDir, "Helper.java" );
		assertTrue( copiedHelperFile.exists() );
	}

	@Test
	@Project("shadowMulti")
	public void testShadowMultiCompile(ProjectScope scope) {
		scope.createGradleRunner( "clean", "compileJava", "processResources" ).build();

		final File baseDirectory = scope.getProjectBaseDirectory();

		final File compileDir = new File( baseDirectory, "shadow/build/classes/java/main" );
		assertTrue( compileDir.exists() );

		final File compiledJpaConsumerFile = new File( compileDir, "JpaConsumer.class" );
		assertTrue( compiledJpaConsumerFile.exists() );

		final File compiledHelperFile = new File( compileDir, "Helper.class" );
		assertTrue( compiledHelperFile.exists() );

		final File resourcesDir = new File( baseDirectory, "shadow/build/resources/main" );
		assertTrue( resourcesDir.exists() );

		final File[] resourceFiles = resourcesDir.listFiles();
		assertNotNull( resourceFiles, "Shadow resources dir was empty" );
		assertTrue( resourceFiles.length > 0, "Shadow resources dir was empty" );
	}

	@Test
	@Project("shadowMulti")
	public void testShadowMultiAssemble(ProjectScope scope) {
		scope.createGradleRunner( "clean", "assemble" ).build();

		final File baseDirectory = scope.getProjectBaseDirectory();

		final File libsDir = new File( baseDirectory, "shadow/build/libs" );
		assertTrue( libsDir.exists() );

		final File[] files = libsDir.listFiles();
		assertNotNull( files );
		assertEquals( 3, files.length );

		final File[] mainJarFiles = libsDir.listFiles( (dir, name) -> name.endsWith( "shadow-1.0.0-javadoc.jar" ) );
		assertEquals( 1, mainJarFiles.length );

		final File[] javadocJarFiles = libsDir.listFiles( (dir, name) -> name.endsWith( "shadow-1.0.0-javadoc.jar" ) );
		assertEquals( 1, javadocJarFiles.length );

		final File[] sourcesJarFiles = libsDir.listFiles( (dir, name) -> name.endsWith( "shadow-1.0.0-sources.jar" ) );
		assertEquals( 1, sourcesJarFiles.length );
	}

	@Test
	@Project("shadowMulti")
	public void testShadowMultiTestCompile(ProjectScope scope) {
		scope.createGradleRunner( "clean", "compileTestJava", "processTestResources" ).build();

		final File baseDirectory = scope.getProjectBaseDirectory();

		final File copyDir = new File( baseDirectory, "shadow/build/generated/sources/jakarta/transform/real/main/java" );
		assertTrue( copyDir.exists() );

		final File copiedFile = new File( copyDir, "JpaConsumer.java" );
		assertTrue( copiedFile.exists() );
	}

	@Test
	@Project("shadowMulti")
	public void testShadowMultiTests(ProjectScope scope) {
		scope.createGradleRunner( "clean", "test" ).build();

		final File baseDirectory = scope.getProjectBaseDirectory();

		final File testResultsDir = new File( baseDirectory, "shadow/build/test-results/test" );
		assertTrue( testResultsDir.exists() );

		final File binaryTestResultsDir = new File( testResultsDir, "binary" );
		assertTrue( binaryTestResultsDir.exists() );

		assertTrue( binaryTestResultsDir.listFiles().length > 0 );
	}

}
