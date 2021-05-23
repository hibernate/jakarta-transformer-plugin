package org.hibernate.build.gradle.jakarta.adhoc;

import javax.inject.Inject;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

import org.hibernate.build.gradle.jakarta.internal.TransformerConfig;

/**
 * Directory based transformation
 *
 * @author Steve Ebersole
 */
public abstract class DirectoryTransformationTask extends DefaultTask {
	private final TransformerConfig config;

	private final DirectoryProperty source;
	private final DirectoryProperty output;

	@Inject
	@SuppressWarnings("UnstableApiUsage")
	public DirectoryTransformationTask(String transformationName, TransformerConfig config) {
		this.config = config;

		source = getProject().getObjects().directoryProperty();

		output = getProject().getObjects().directoryProperty();
		output.convention(
				getProject().provider(
						() -> config.outputDirectoryAccess().get().dir( transformationName )
				)
		);
	}

	@InputDirectory
	public DirectoryProperty getSource() {
		return source;
	}

	@OutputDirectory
	public DirectoryProperty getOutput() {
		return output;
	}

	@TaskAction
	public void transformDirectory() {
		config.getTransformer().transform( source.get(), output.get() );
	}
}
