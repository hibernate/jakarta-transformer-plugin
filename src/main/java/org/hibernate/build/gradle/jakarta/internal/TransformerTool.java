package org.hibernate.build.gradle.jakarta.internal;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.FileSystemLocation;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.Provider;

import static java.time.format.DateTimeFormatter.ofPattern;

/**
 * Performs transformations via reflective execution of the JakartaTransformer tool
 *
 * @author Steve Ebersole
 */
public class TransformerTool {
	public static final String TOOL_CLI_FQN = "org.eclipse.transformer.jakarta.JakartaTransformer";

	public interface Config {
		Provider<RegularFile> renameRuleAccess();
		Provider<RegularFile> versionRuleAccess();
		Provider<RegularFile> directRuleAccess();
	}

	private final Configuration toolDependencies;
	private final Config config;
	private final Project project;

	private final RegularFile transformerLoggingOutput;

	public TransformerTool(
			Configuration toolDependencies,
			Config config,
			Project project) {
		this.toolDependencies = toolDependencies;
		this.config = config;
		this.project = project;

		final DateTimeFormatter formatter = ofPattern( "yyyy-MM-dd_HH-mm-ss" );

		transformerLoggingOutput = project.getLayout()
				.getBuildDirectory()
				.dir( "tmp/jakarta-transformer-output" )
				.get()
				.dir( project.getName() )
				.file( formatter.format( LocalDateTime.now() ) + ".txt" );

// todo : would be nice to call Transformer directly, but
//		- they require a Map keys by enums that are effectively Strings.  But they still require use of the enums which,
//			unfortunately, make it difficult to handle reflectively
//		- they have odd calls, like you "set" the files and then call a no-arg method
//  	:(
//
//
//		final Class<?> transformerClass = loadToolClass( TRANSFORMER_FQN, toolDependencies );
//
//		final Class<?> toolClass = loadToolClass( TOOL_FQN, toolDependencies );
//
//		try {
//			toolReference = toolClass.getDeclaredConstructor().newInstance();
//		}
//		catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
//			throw new TransformationException( "Unable to create `" + TOOL_FQN + "` reference", e );
//		}
//
//		final Map<Str>
//		final Method optionsMethod = resolveToolMethod( toolClass, TOOL_OPTIONS_METHOD, Class.class, Map.class );
//		try {
//			optionsMethod.invoke( toolReference, transformerClass );
//		}
//		catch (IllegalAccessException | InvocationTargetException e) {
//			throw new TransformationException( "Unable to call `" + TOOL_FQN + "#" + TOOL_OPTIONS_METHOD + "`", e );
//		}
//
//		final Method argsMethod = resolveToolMethod( toolClass, TOOL_ARGS_METHOD, String[].class );
//
//		runMethod = resolveToolMethod( toolClass, TOOL_RUN_METHOD );
	}

	public void transform(RegularFile sourceFile, RegularFile targetFile) {
		transform( sourceFile.getAsFile(), targetFile.getAsFile() );
	}

	public void transform(FileSystemLocation source, FileSystemLocation target) {
		transform( source.getAsFile(), target.getAsFile() );
	}

	public void transform(File source, File target) {

		final List<String> args = new ArrayList<>(
				Arrays.asList(
						source.getAbsolutePath(),
						target.getAbsolutePath(),
						"-q"
				)
		);

		if ( config.renameRuleAccess().isPresent() ) {
			args.add( "-tr" );
			args.add( config.renameRuleAccess().get().getAsFile().getAbsolutePath() );
		}

		if ( config.versionRuleAccess().isPresent() ) {
			args.add( "-tv" );
			args.add( config.versionRuleAccess().get().getAsFile().getAbsolutePath() );
		}

		if ( config.directRuleAccess().isPresent() ) {
			args.add( "-td" );
			args.add( config.directRuleAccess().get().getAsFile().getAbsolutePath() );
		}


		try ( OutputStream outputStream = createOutputStream() ) {
			project.javaexec(
					javaExecSpec -> {
						javaExecSpec.classpath( toolDependencies );

						javaExecSpec.setMain( TOOL_CLI_FQN );

						javaExecSpec.setArgs( args );

						javaExecSpec.setStandardOutput( outputStream );
						javaExecSpec.setErrorOutput( outputStream );
					}
			);
		}
		catch (IOException e) {
			project.getLogger().debug( "Unable to close JakartaTransformer logging output stream" );
		}
	}

	private OutputStream createOutputStream() {
		final File outputAsFile = transformerLoggingOutput.getAsFile();
		if ( ! outputAsFile.exists() ) {
			outputAsFile.getParentFile().mkdirs();
			try {
				outputAsFile.createNewFile();
			}
			catch (IOException e) {
				project.getLogger().info( "Unable to generate JakartaTransformer output file {}", outputAsFile.getAbsolutePath() );
			}
		}

		try {
			return new BufferedOutputStream( new FileOutputStream( outputAsFile, true ) );
		}
		catch (FileNotFoundException ignore) {
			return null;
		}
	}
}
