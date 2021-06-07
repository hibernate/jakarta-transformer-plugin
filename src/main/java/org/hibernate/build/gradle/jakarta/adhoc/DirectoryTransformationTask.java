package org.hibernate.build.gradle.jakarta.adhoc;

import javax.inject.Inject;
import javax.inject.Provider;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFile;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

import org.hibernate.build.gradle.jakarta.internal.TransformerConfig;

/**
 * Directory based transformation
 *
 * @author Steve Ebersole
 */
public abstract class DirectoryTransformationTask extends DefaultTask implements Provider<Directory> {
	private final TransformerConfig config;

	private final DirectoryProperty source;
	private final DirectoryProperty output;

	@Inject
	@SuppressWarnings("UnstableApiUsage")
	public DirectoryTransformationTask(TransformerConfig config) {
		this.config = config;

		source = getProject().getObjects().directoryProperty();
		output = getProject().getObjects().directoryProperty();
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

	@Override
	public Directory get() {
		return output.get();
	}
}
