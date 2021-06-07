package org.hibernate.build.gradle.jakarta.shadow;

import java.io.File;
import java.util.Collections;
import java.util.Date;
import java.util.Set;
import javax.annotation.Nullable;

import org.gradle.api.Task;
import org.gradle.api.artifacts.PublishArtifact;
import org.gradle.api.tasks.TaskDependency;

import org.hibernate.build.gradle.jakarta.adhoc.FileTransformationTask;
import org.hibernate.build.gradle.jakarta.internal.TransformerConfig;

/**
 * PublishArtifact wrapper around a transformed jar
 *
 * @author Steve Ebersole
 */
public class ShadowPublishArtifact implements PublishArtifact, TaskDependency {
	public static final String JAR = "jar";

	private final String name;
	private final String classifier;

	private final FileTransformationTask transformationTask;
	private final TransformerConfig transformerConfig;

	private final Set<FileTransformationTask> taskDependencies;

	public ShadowPublishArtifact(
			String name,
			String classifier,
			FileTransformationTask transformationTask,
			TransformerConfig transformerConfig) {
		this.name = name;
		this.classifier = classifier;
		this.transformationTask = transformationTask;
		this.transformerConfig = transformerConfig;

		taskDependencies = Collections.singleton( transformationTask );
	}

	@Override
	public String getName() {
		return name;
	}

	@Nullable
	@Override
	public String getClassifier() {
		return classifier;
	}

	@Override
	public String getExtension() {
		return JAR;
	}

	@Override
	public String getType() {
		return JAR;
	}

	@Override
	public File getFile() {
		return transformationTask.getOutput().get().getAsFile();
	}

	@Nullable
	@Override
	public Date getDate() {
		return Date.from( transformerConfig.getBuildStarted() );
	}

	@Override
	public TaskDependency getBuildDependencies() {
		return this;
	}

	@Override
	public Set<? extends Task> getDependencies(@Nullable Task task) {
		return Collections.singleton( transformationTask );
	}
}
