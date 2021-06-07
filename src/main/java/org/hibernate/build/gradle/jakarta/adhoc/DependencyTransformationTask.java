package org.hibernate.build.gradle.jakarta.adhoc;

import javax.inject.Inject;
import javax.inject.Provider;

import org.gradle.api.DefaultTask;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.file.RegularFile;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import org.hibernate.build.gradle.jakarta.internal.Helper;
import org.hibernate.build.gradle.jakarta.internal.TransformerConfig;

/**
 * @author Steve Ebersole
 */
public abstract class DependencyTransformationTask extends DefaultTask implements Provider<RegularFile> {
	private final TransformerConfig transformerConfig;

	private final Property<Dependency> source;
	private final RegularFileProperty output;

	@Inject
	@SuppressWarnings("UnstableApiUsage")
	public DependencyTransformationTask(TransformerConfig transformerConfig) {
		this.transformerConfig = transformerConfig;

		source = getProject().getObjects().property( Dependency.class );
		output = getProject().getObjects().fileProperty();
	}

	@Input
	public Property<Dependency> getSource() {
		return source;
	}

	@OutputFile
	public RegularFileProperty getOutput() {
		return output;
	}

	@TaskAction
	public void transformDependency() {
		final Configuration configuration = getProject().getConfigurations().detachedConfiguration( source.get() );
		transformerConfig.applyDependencyResolutionStrategy( configuration );

		final ResolvedArtifact resolvedArtifact = Helper.extractResolvedArtifact( configuration );

		transformerConfig.getTransformer().transform(
				resolvedArtifact.getFile(),
				output.get().getAsFile()
		);
	}

	@Override
	public RegularFile get() {
		return output.get();
	}
}
