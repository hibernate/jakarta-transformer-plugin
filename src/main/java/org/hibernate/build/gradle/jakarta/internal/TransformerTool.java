package org.hibernate.build.gradle.jakarta.internal;

import java.io.File;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.FileSystemLocation;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.Provider;

import org.hibernate.build.gradle.jakarta.TransformationException;

/**
 * Performs transformations via reflective execution of the JakartaTransformer tool
 *
 * @author Steve Ebersole
 */
public class TransformerTool {
	public static final String TOOL_CLI_FQN = "org.eclipse.transformer.jakarta.JakartaTransformer";

	public static final String TRANSFORMER_FQN = "org.eclipse.transformer.jakarta.JakartaTransformer";
	public static final String TOOL_FQN = "org.eclipse.transformer.Transformer";
	public static final String TOOL_OPTIONS_METHOD = "setOptionDefaults";
	public static final String TOOL_ARGS_METHOD = "setArgs";
	public static final String TOOL_RUN_METHOD = "run";

	public interface Config {
		Provider<RegularFile> renameRuleAccess();
		Provider<RegularFile> versionRuleAccess();
		Provider<RegularFile> directRuleAccess();
	}

	private final Configuration toolDependencies;
	private final Config config;
	private final Project project;

//	private final Object toolReference;
//	private final Method runMethod;

	public TransformerTool(
			Configuration toolDependencies,
			Config config,
			Project project) {
		this.toolDependencies = toolDependencies;
		this.config = config;
		this.project = project;

// todo : would be nice to call Transformer directly, but
//		- they require a Map keys by enums that are effectively Strings which, unfortunately, make it difficult to handle reflectively
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

	private Class<?> loadToolClass(String fqn, Configuration toolDependencies) {
		final ClassLoader toolClassLoader = asClassLoader( toolDependencies );
		try {
			return toolClassLoader.loadClass( fqn );
		}
		catch (ClassNotFoundException e) {
			throw new TransformationException(
					"Unable to locate JakartaTransformer class : `" + TOOL_FQN + "`",
					e
			);
		}
	}

	private ClassLoader asClassLoader(Configuration toolDependencies) {
		final ArrayList<URL> urls = new ArrayList<>();
		for ( File artifact : toolDependencies.resolve() ) {
			try {
				urls.add( artifact.toURI().toURL() );
			}
			catch (MalformedURLException e) {
				throw new TransformationException(
						"Unable to resolve artifact file as URL : " + artifact.getAbsolutePath(),
						e
				);
			}
		}

		return new URLClassLoader( urls.toArray( new URL[0] ) );
	}

	private Method resolveToolMethod(Class<?> toolClass, String methodName, Class<?>... argTypes) {
		try {
			return toolClass.getMethod( methodName, argTypes );
		}
		catch (NoSuchMethodException e) {
			throw new TransformationException(
					"Unable to locate run method : `" + TOOL_FQN + "#" + TOOL_RUN_METHOD + "`",
					e
			);
		}
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

		project.javaexec(
				javaExecSpec -> {
					javaExecSpec.classpath( toolDependencies );

					javaExecSpec.setMain( TOOL_CLI_FQN );

					javaExecSpec.setArgs( args );
				}
		);

//
//		try {
//			final int result = (int) runMethod.invoke( null, System.out, System.err, args.toArray( new String[0] ) );
//			if ( result != 0 ) {
//				throw new TransformationException(
//						String.format(
//								Locale.ROOT,
//								"%s#%s returned non-zero result",
//								TOOL_FQN,
//								TOOL_RUN_METHOD
//						)
//				);
//			}
//		}
//		catch (IllegalAccessException | InvocationTargetException e) {
//			throw new TransformationException(
//					"Unable to perform transformation on file : " + sourceFile.getAbsolutePath(),
//					e
//			);
//		}
	}
}
