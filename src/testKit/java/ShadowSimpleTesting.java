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
@Project("shadow")
public class ShadowSimpleTesting {
	@Test
	public void testShadowDependency(ProjectScope scope) {
		scope.createGradleRunner( "clean", "shadow" ).build();

		verifyLibs( scope );
	}

	@Test
	public void testShadowDependencyAssemble(ProjectScope scope) {
		scope.createGradleRunner( "clean", "assemble" ).build();

		verifyLibs( scope );
	}

	private void verifyLibs(ProjectScope scope) {
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
}
