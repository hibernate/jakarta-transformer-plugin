package org.hibernate.build.gradle.jakarta.adhoc;

import org.gradle.api.DefaultTask;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputFile;

import static org.hibernate.build.gradle.jakarta.TransformerPlugin.JAKARTA_TRANSFORMER_TOOL;

/**
 * Transforms {@link #getFileToTransform()} using the JakartaTransformer tool producing
 * {@link #getOutputFile()}
 *
 * @author Steve Ebersole
 */
public class TransformationTask extends DefaultTask {
	private final RegularFileProperty fileToTransform;
	private final RegularFileProperty outputFile;
	private final Property<Configuration> targetConfiguration;

	private final RegularFileProperty renameRules;
	private final RegularFileProperty versionRules;
	private final RegularFileProperty directRules;

	private final Configuration transformerDependencies;

	public TransformationTask() {
		// input
		this.fileToTransform = getProject().getObjects().fileProperty();

		// output
		this.outputFile = getProject().getObjects().fileProperty();
		this.targetConfiguration = getProject().getObjects().property( Configuration.class );

		// transformation rules
		this.renameRules = getProject().getObjects().fileProperty();
		this.versionRules = getProject().getObjects().fileProperty();
		this.directRules = getProject().getObjects().fileProperty();

		this.transformerDependencies = getProject().getConfigurations().maybeCreate( JAKARTA_TRANSFORMER_TOOL );
	}

	/**
	 * The JAR file to transform
	 */
	@InputFile
	public RegularFileProperty getFileToTransform() {
		return fileToTransform;
	}

	/**
	 * Where the transformed JAR file will be written
	 */
	@OutputFile
	public RegularFileProperty getOutputFile() {
		return outputFile;
	}

	/**
	 * (Optional) A Configuration to which the transformed JAR
	 * should be added after transformation
	 */
	@Input
	public Property<Configuration> getTargetConfiguration() {
		return targetConfiguration;
	}

	@InputFile
	public RegularFileProperty getRenameRules() {
		return renameRules;
	}

	@InputFile
	public RegularFileProperty getVersionRules() {
		return versionRules;
	}

	@InputFile
	public RegularFileProperty getDirectRules() {
		return directRules;
	}

	/**
	 * Dependencies of the JakartaTransformer tool itself
	 */
	public Configuration getTransformerDependencies() {
		return transformerDependencies;
	}
}
