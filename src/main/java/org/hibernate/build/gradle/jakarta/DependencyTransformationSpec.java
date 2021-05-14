package org.hibernate.build.gradle.jakarta;

import java.util.Locale;
import java.util.Set;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.ResolvedConfiguration;
import org.gradle.api.provider.Property;

/**
 * @author Steve Ebersole
 */
public class DependencyTransformationSpec extends TransformationSpec {
	private final Property<Configuration> sourceConfiguration;
	private final Property<Dependency> sourceDependency;

	public DependencyTransformationSpec(JakartaTransformerConfig config, TransformationTask task) {
		super( config, task );

		final Project project = task.getProject();

		sourceConfiguration = project.getObjects().property( Configuration.class );
		sourceDependency = project.getObjects().property( Dependency.class );

		task.getFileToTransform().convention(
				() -> {
					final Dependency dependency = sourceDependency.get();

					final Configuration dependencies = sourceConfiguration.get();
					final ResolvedConfiguration resolvedConfiguration = dependencies.getResolvedConfiguration();
					final Set<ResolvedArtifact> resolvedArtifacts = resolvedConfiguration.getResolvedArtifacts();

					for ( ResolvedArtifact resolvedArtifact : resolvedArtifacts ) {
						if ( matches( resolvedArtifact, dependency ) ) {
							return resolvedArtifact.getFile();
						}
					}

					throw new RuntimeException(
							String.format(
									Locale.ROOT,
									"Could not locate dependency to transform [%s] from `%s` configuration",
									dependency,
									dependencies.getName()
							)
					);
				}
		);

		task.getOutputFile().convention(
				config.getOutputDir().file(
						task.getProject().provider(
								() -> resolveOutputFileName( task.getFileToTransform().get().getAsFile() )
						)
				)
		);
	}

	public Property<Configuration> getSourceConfiguration() {
		return sourceConfiguration;
	}

	public void sourceConfiguration(Configuration configuration) {
		getSourceConfiguration().set( configuration );
	}

	public Property<Dependency> getSourceDependency() {
		return sourceDependency;
	}

	public void sourceDependency(Object dependencySyntax) {
		getSourceDependency().set( task.getProject().getDependencies().add( sourceConfiguration.get().getName(), dependencySyntax ) );
//		getSourceDependency().set( sourceDependency );
	}

	public void sourceDependency(Dependency sourceDependency) {
		getSourceDependency().set( sourceDependency );
	}

	private boolean matches(ResolvedArtifact resolvedArtifact, Dependency dependency) {
		if ( ! resolvedArtifact.getModuleVersion().getId().getGroup().equals( dependency.getGroup() ) ) {
			return false;
		}

		if ( ! resolvedArtifact.getModuleVersion().getId().getName().equals( dependency.getName() ) ) {
			return false;
		}

		return true;
	}
}
