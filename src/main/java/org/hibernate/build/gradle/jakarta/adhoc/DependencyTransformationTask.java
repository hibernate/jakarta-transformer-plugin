package org.hibernate.build.gradle.jakarta.adhoc;

import java.util.Set;
import javax.inject.Inject;

import org.gradle.api.DefaultTask;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ResolvedConfiguration;
import org.gradle.api.artifacts.ResolvedDependency;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

import org.hibernate.build.gradle.jakarta.TransformationException;
import org.hibernate.build.gradle.jakarta.internal.TransformerConfig;

/**
 * @author Steve Ebersole
 */
public abstract class DependencyTransformationTask extends DefaultTask {
	private final String transformationName;
	private final TransformerConfig transformerConfig;

	private final Property<Dependency> source;
	private final DirectoryProperty output;

	@Inject
	@SuppressWarnings("UnstableApiUsage")
	public DependencyTransformationTask(String transformationName, TransformerConfig transformerConfig) {
		this.transformationName = transformationName;
		this.transformerConfig = transformerConfig;

		source = getProject().getObjects().property( Dependency.class );

		output = getProject().getObjects().directoryProperty();
		output.convention(
				(Directory) getProject().provider(
						() -> transformerConfig.outputDirectoryAccess().get().dir( transformationName )
				)
		);
	}

	@Input
	public String getTransformationName() {
		return transformationName;
	}

	@Input
	public Property<Dependency> getSource() {
		return source;
	}

	@OutputDirectory
	public DirectoryProperty getOutput() {
		return output;
	}

	@TaskAction
	public void transformDependency() {
		final Configuration configuration = getProject().getConfigurations().detachedConfiguration( source.get() );
		transformerConfig.applyDependencyResolutionStrategy( configuration );

		final ResolvedConfiguration resolvedConfiguration = configuration.getResolvedConfiguration();
		final Set<ResolvedDependency> rootDependencies = resolvedConfiguration.getFirstLevelModuleDependencies();
		if ( rootDependencies.isEmpty() ) {
			return;
		}

		if ( rootDependencies.size() > 1 ) {
			throw new TransformationException( "More than one root dependency resolved for `" + transformationName + "` transformation" );
		}

		final ResolvedDependency resolvedDependency = rootDependencies.iterator().next();
		resolvedDependency.getModuleArtifacts().forEach(
				(resolvedArtifact) -> {
					String outputFileName = transformationName;

					final String versionString = getProject().getVersion().toString().trim();
					if ( !versionString.isEmpty() && !versionString.equals( "unspecified" ) ) {
						outputFileName += ( "-" + versionString );
					}

					final String classifierString = resolvedArtifact.getClassifier();
					if ( classifierString != null && !classifierString.isEmpty() && !classifierString.equals( "unspecified" ) ) {
						outputFileName += ( "-" + classifierString );
					}

					outputFileName += resolvedArtifact.getExtension();

					transformerConfig.getTransformer().transform(
							resolvedArtifact.getFile(),
							output.get().file( outputFileName ).getAsFile()
					);
				}
		);
	}
}
