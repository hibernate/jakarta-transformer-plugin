package org.hibernate.build.gradle.jakarta.internal;

import java.util.Set;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.DependencySet;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.ResolvedConfiguration;
import org.gradle.api.artifacts.ResolvedDependency;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.file.Directory;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSetContainer;

/**
 * @author Steve Ebersole
 */
public class Helper {
	private Helper() {
		// disallow direct instantiation
	}

	@SuppressWarnings("UnusedReturnValue")
	public static void shadowConfiguration(
			String configurationName,
			Project sourceProject,
			Project targetProject,
			TransformerConfig transformerConfig) {
		final Configuration sourceConfiguration = sourceProject.getConfigurations().getByName( configurationName );
		final Configuration shadowConfiguration = targetProject.getConfigurations().getByName( configurationName );

		// technically should already have the substitutions applied, but be sure..
		transformerConfig.applyDependencyResolutionStrategy( shadowConfiguration );

		final DependencySet sourceDependencies = sourceConfiguration.getAllDependencies();

		final DependencyHandler shadowDependenciesHandler = targetProject.getDependencies();
		sourceDependencies.forEach(
				(dependency) -> shadowDependenciesHandler.add( shadowConfiguration.getName(), dependency )
		);
	}

	public static Directory determineUnpackBaseDir(Project sourceProject, Project shadowProject) {
		return shadowProject.getLayout()
				.getBuildDirectory()
				.dir( "generated/sources/jakarta/transform" )
				.get()
				.dir( sourceProject.getName() );
	}


	public static ResolvedArtifact extractResolvedArtifact(Configuration dependencies) {
		final ResolvedConfiguration resolvedConfiguration = dependencies.getResolvedConfiguration();

		final Set<ResolvedDependency> firstLevelModuleDependencies = resolvedConfiguration.getFirstLevelModuleDependencies();
		// for now assume one
		assert firstLevelModuleDependencies.size() == 1;

		final ResolvedDependency resolvedDependency = firstLevelModuleDependencies.iterator().next();
		final Set<ResolvedArtifact> moduleArtifacts = resolvedDependency.getModuleArtifacts();
		assert moduleArtifacts.size() == 1;

		return moduleArtifacts.iterator().next();
	}

	public static SourceSetContainer extractSourceSets(Project project) {
		final JavaPluginConvention javaPluginConvention = project.getConvention().findPlugin( JavaPluginConvention.class );
		assert javaPluginConvention != null;
		return javaPluginConvention.getSourceSets();
	}

}
