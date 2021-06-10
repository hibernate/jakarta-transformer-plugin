package org.hibernate.build.gradle.jakarta.internal;

import java.util.Set;
import java.util.function.Consumer;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.DependencySet;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.ResolvedConfiguration;
import org.gradle.api.artifacts.ResolvedDependency;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSetContainer;

/**
 * @author Steve Ebersole
 */
public class Helper {
	private Helper() {
		// disallow direct instantiation
	}

	public static void shadowConfiguration(
			Configuration source,
			Configuration target,
			Project targetProject,
			Consumer<Dependency> dependencyConsumer) {
		final DependencySet sourceDependencies = source.getAllDependencies();

		final DependencyHandler shadowDependenciesHandler = targetProject.getDependencies();
		sourceDependencies.forEach(
				(dependency) -> {
					final Dependency added = shadowDependenciesHandler.add( target.getName(), dependency );
					dependencyConsumer.accept( added );
				}
		);
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
