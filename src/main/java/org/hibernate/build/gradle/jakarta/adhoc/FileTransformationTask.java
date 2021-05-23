package org.hibernate.build.gradle.jakarta.adhoc;

import javax.inject.Inject;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import org.hibernate.build.gradle.jakarta.internal.TransformerConfig;

/**
 * @author Steve Ebersole
 */
public abstract class FileTransformationTask extends DefaultTask {
	private final TransformerConfig transformerConfig;

	private final RegularFileProperty source;
	private final RegularFileProperty output;

	@Inject
	@SuppressWarnings("UnstableApiUsage")
	public FileTransformationTask(String transformationName, TransformerConfig transformerConfig) {
		this.transformerConfig = transformerConfig;

		source = getProject().getObjects().fileProperty();

		output = getProject().getObjects().fileProperty();
		output.convention(
				getProject().provider(
						() -> {
							// this assumes "single dot" file extensions
							final String sourceFileName = source.get().getAsFile().getName();
							final int extensionDelimiterLocation = sourceFileName.lastIndexOf( '.' );
							final String outputFileName = transformationName + sourceFileName.substring( extensionDelimiterLocation );
							return transformerConfig.outputDirectoryAccess().get().file( outputFileName );
						}
				)
		);
	}

	@InputFile
	public RegularFileProperty getSource() {
		return source;
	}

	@OutputFile
	public RegularFileProperty getOutput() {
		return output;
	}

	@TaskAction
	public void transformFile() {
		transformerConfig.getTransformer().transform( source.get(), output.get() );
	}
}
