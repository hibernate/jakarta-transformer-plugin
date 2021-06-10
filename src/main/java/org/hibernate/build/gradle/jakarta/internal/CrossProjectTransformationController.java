package org.hibernate.build.gradle.jakarta.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.gradle.BuildAdapter;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.invocation.Gradle;

import static org.hibernate.build.gradle.jakarta.shadow.ShadowTestSpec.SHADOW_TEST_JAVA_TASK;
import static org.hibernate.build.gradle.jakarta.shadow.ShadowTestSpec.SHADOW_TEST_RESOURCES_TASK;

/**
 * Handles details of transformation which need to operate across all projects
 *
 * @author Steve Ebersole
 */
public class CrossProjectTransformationController {
	public static final String EXT_NAME = "jakarta-transformer-controller";

	public static CrossProjectTransformationController apply(Project project) {
		final Project rootProject = project.getRootProject();
		final Object byName = rootProject.getExtensions().findByName( EXT_NAME );
		if ( byName != null ) {
			return (CrossProjectTransformationController) byName;
		}

		final CrossProjectTransformationController controller = new CrossProjectTransformationController( rootProject );
		rootProject.getExtensions().add( EXT_NAME, controller );
		return controller;
	}

	private final Map<Project, Project> shadowedProjectMap = new HashMap<>();
	private final ProjectDependencyCallbackManager dependencyCallbackManager;

	public CrossProjectTransformationController(Project rootProject) {
		dependencyCallbackManager = new ProjectDependencyCallbackManager( rootProject );
	}

	public void registerShadowedProject(Project sourceProject, Project shadowProject) {
		shadowedProjectMap.put( sourceProject, shadowProject );
	}

	public Project getShadowedProject(Project sourceProject) {
		return shadowedProjectMap.get( sourceProject );
	}

	public void registerProjectDependencies(Project targetProject, List<Project> dependencyProjects) {
		dependencyCallbackManager.registerCallback( targetProject, dependencyProjects );
	}

	private class ProjectDependencyCallbackManager {
		private final List<ProjectDependencyCallbackHandler> handlers = new ArrayList<>();

		public ProjectDependencyCallbackManager(Project rootProject) {
			rootProject.getGradle().addBuildListener(
					new BuildAdapter() {
						@Override
						public void projectsEvaluated(Gradle gradle) {
							handleProjectDependencies();
						}
					}
			);
		}

		private void registerCallback(Project targetProject, List<Project> dependedOnProjects) {
			handlers.add( new ProjectDependencyCallbackHandler( targetProject, dependedOnProjects ) );
		}

		private void handleProjectDependencies() {
			handlers.forEach( ProjectDependencyCallbackHandler::handle );
		}

	}

	private class ProjectDependencyCallbackHandler {
		private final Project project;
		private final List<Project> dependedOnProjects;

		public ProjectDependencyCallbackHandler(Project project, List<Project> dependedOnProjects) {
			this.project = project;
			this.dependedOnProjects = dependedOnProjects;
		}

		private void handle() {
			for ( Project dependedOnProject : dependedOnProjects ) {
				// We have:
				// 		1) `project` is shadowed
				//		2) `dependedOnProject` is a test dependency

				final Project shadowedDependencyVariant = shadowedProjectMap.get( dependedOnProject );
				if ( shadowedDependencyVariant == null ) {
					// `dependedOnProject` does not have a shadowed variant
					break;
				}

				// `dependedOnProject` -> `shadowedDependencyVariant` resolution is already
				// handled as far as classpath resolution (through substitution rules).
				//
				// however, Gradle does not understand this relationship in terms of task-dependencies.
				// we need to configure that  manually

				final Task dependencyAssembleTask = shadowedDependencyVariant.getTasks().getByName( "assemble" );

				final Task shadowTestJavaTask = project.getTasks().getByName( SHADOW_TEST_JAVA_TASK );
				final Task shadowTestResourcesTask = project.getTasks().getByName( SHADOW_TEST_RESOURCES_TASK );

				shadowTestJavaTask.dependsOn( dependencyAssembleTask );
				shadowTestResourcesTask.dependsOn( dependencyAssembleTask );
			}
		}
	}
}
