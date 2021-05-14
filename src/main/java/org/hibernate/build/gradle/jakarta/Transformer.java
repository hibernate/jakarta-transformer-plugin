package org.hibernate.build.gradle.jakarta;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.RegularFile;

/**
 * Centralize logic for the actual transformation.
 *
 * @author Steve Ebersole
 */
public class Transformer {
	public interface Options {
		RegularFile getRenameRules();
		RegularFile getVersionRules();
		RegularFile getDirectRules();
	}

	public static void transform(
			RegularFile sourceFile,
			RegularFile targetFile,
			Configuration toolDependencies,
			Options options,
			Project project) {
		transform( sourceFile.getAsFile(), targetFile, toolDependencies, options, project );
	}

	public static void transform(
			File sourceFile,
			RegularFile targetFile,
			Configuration toolDependencies,
			Options options,
			Project project) {
		project.javaexec(
				javaExecSpec -> {
					javaExecSpec.classpath( toolDependencies );
					javaExecSpec.setMain( "org.eclipse.transformer.jakarta.JakartaTransformer" );

					final List<String> args = new ArrayList<>(
							Arrays.asList(
									sourceFile.getAbsolutePath(),
									targetFile.getAsFile().getAbsolutePath(),
									"-q"
							)
					);

					if ( options.getRenameRules() != null ) {
						args.add( "-tr" );
						args.add( options.getRenameRules().getAsFile().getAbsolutePath() );
					}

					if ( options.getVersionRules() != null ) {
						args.add( "-tv" );
						args.add( options.getVersionRules().getAsFile().getAbsolutePath() );
					}

					if ( options.getDirectRules() != null ) {
						args.add( "-td" );
						args.add( options.getDirectRules().getAsFile().getAbsolutePath() );
					}

					javaExecSpec.setArgs( args );

					project.getLogger().lifecycle( "javaexec cml: {}", javaExecSpec.getCommandLine() );
				}
		);
	}
}
