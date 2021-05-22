package org.hibernate.build.gradle.jakarta.shadow;

import java.io.File;
import javax.inject.Inject;

import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.file.Directory;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskAction;
import org.gradle.language.jvm.tasks.ProcessResources;

import org.hibernate.build.gradle.jakarta.internal.TransformerConfig;
import org.hibernate.build.gradle.jakarta.internal.TransformerTool;

/**
 * Transformation task for processing local sources (Java and resources).  Local
 * transformations always work on a directory
 *
 * @author Steve Ebersole
 */
public abstract class TransformLocalSourcesTask extends DefaultTask {
	private final SourceDirectorySet transformationSources;
	private final Directory transformationTarget;
	private final boolean processResources;
	private final SourceSet sourceSet;
	private final TransformerConfig transformerConfig;
	private final Project sourceProject;

	@Inject
	public TransformLocalSourcesTask(
			SourceDirectorySet transformationSources,
			Directory transformationTarget,
			boolean processResources,
			SourceSet sourceSet,
			TransformerConfig transformerConfig,
			Project sourceProject) {
		this.transformationSources = transformationSources;
		this.transformationTarget = transformationTarget;
		this.processResources = processResources;
		this.sourceSet = sourceSet;
		this.transformerConfig = transformerConfig;
		this.sourceProject = sourceProject;
	}

	@InputFiles
	public FileCollection getTransformationSource() {
		return transformationSources.getSourceDirectories();
	}

	@OutputDirectory
	public Directory getTransformationTarget() {
		return transformationTarget;
	}

	@Nested
	@SuppressWarnings("unused")
	public TransformerTool.Config getTransformerConfig() {
		return transformerConfig;
	}

	@TaskAction
	public void transform() {
		final File transformationTargetAsFile = transformationTarget.getAsFile();
		// for some reason sometimes this directory exists and the JakartaTransformer
		// seems to not be able to deal with that and simply does not transform anything.
		//
		// so avoid that whole mess by deleting this direct
		transformationTargetAsFile.delete();

		if ( processResources ) {
			// we handle resources differently to be able to transform just the already
			// processed resources from the source project

			//noinspection UnstableApiUsage
			final ProcessResources sourceProcessResourcesTask = (ProcessResources) sourceProject.getTasks().getByName(
					sourceSet.getProcessResourcesTaskName()
			);
			final File sourceProcessResourcesOutputDir = sourceProcessResourcesTask.getDestinationDir();
			final Directory sourceMainProcessResourcesOutputDirectory = sourceProject.getLayout()
					.dir( sourceProject.provider( () -> sourceProcessResourcesOutputDir ) )
					.get();

			transformerConfig.getTransformer().transform(
					sourceMainProcessResourcesOutputDirectory.getAsFile(),
					transformationTargetAsFile
			);
		}
		else {
			transformationSources.getSourceDirectories().forEach(
					(dir) -> transformerConfig.getTransformer().transform(
							dir,
							transformationTargetAsFile
					)
			);
		}
	}

}
