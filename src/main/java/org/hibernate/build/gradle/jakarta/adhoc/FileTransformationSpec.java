package org.hibernate.build.gradle.jakarta.adhoc;

import org.gradle.api.file.RegularFile;

import org.hibernate.build.gradle.jakarta.internal.TransformerSpecImpl;

/**
 * @author Steve Ebersole
 */
public class FileTransformationSpec extends TransformationSpec {
	public FileTransformationSpec(TransformerSpecImpl config, TransformationTask task) {
		super( config, task );
	}

	public void setFileToTransform(RegularFile fileToTransform) {
		task.getFileToTransform().set( fileToTransform );
		task.getOutputFile().convention(
				config.getOutputDir().file( resolveOutputFileName( fileToTransform.getAsFile() ) )
		);
	}

	public void fileToTransform(RegularFile fileToTransform) {
		setFileToTransform( fileToTransform );
	}
}
