package org.hibernate.build.gradle.jakarta;

import java.io.File;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.provider.Property;

/**
 * @author Steve Ebersole
 */
public abstract class TransformationSpec {
	protected final JakartaTransformerConfig config;
	protected final TransformationTask task;

	protected final Property<String> replacementMatch;
	protected final Property<String> replacement;

	public TransformationSpec(JakartaTransformerConfig config, TransformationTask task) {
		this.config = config;
		this.task = task;

		final Project project = task.getProject();

		replacementMatch = project.getObjects().property( String.class );
		replacement = project.getObjects().property( String.class );

		task.getRenameRules().convention( config.getRenameRules() );
		task.getVersionRules().convention( config.getVersionRules() );
		task.getDirectRules().convention( config.getDirectRules() );
	}

	public Property<String> getReplacementMatch() {
		return replacementMatch;
	}

	public Property<String> getReplacement() {
		return replacement;
	}

	public Property<Configuration> getTargetConfiguration() {
		return task.getTargetConfiguration();
	}

	protected String resolveOutputFileName(File sourceFile) {
		final String sourceFileName = sourceFile.getName();
		return sourceFileName.replace( replacementMatch.get(), replacement.get() );
	}
}
