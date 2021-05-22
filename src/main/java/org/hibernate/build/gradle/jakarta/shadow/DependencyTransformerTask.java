package org.hibernate.build.gradle.jakarta.shadow;

import javax.inject.Inject;

import org.gradle.api.DefaultTask;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import org.hibernate.build.gradle.jakarta.internal.Helper;
import org.hibernate.build.gradle.jakarta.internal.TransformerConfig;

/**
 * @author Steve Ebersole
 */
public abstract class DependencyTransformerTask extends DefaultTask {
	private final Dependency sourceDependency;
	private final TransformerConfig transformerConfig;

	private final Configuration sourceConfiguration;

	private final Provider<RegularFile> jarFile;



	@Inject
	public DependencyTransformerTask(
			Dependency sourceDependency,
			DirectoryProperty libsDir,
			String classifier,
			TransformerConfig transformerConfig) {
		this.sourceDependency = sourceDependency;
		this.transformerConfig = transformerConfig;

		this.sourceConfiguration = getProject().getConfigurations().detachedConfiguration( sourceDependency );
		transformerConfig.applyDependencyResolutionStrategy( sourceConfiguration );

		this.jarFile = libsDir.file( determineJarFileName( classifier ) );
	}

	private String determineJarFileName(String classifier) {
		String jarFileName = getProject().getName();

		final String versionString = getProject().getVersion().toString().trim();
		if ( ! versionString.isEmpty() && ! versionString.equals( "unspecified" ) ) {
			jarFileName += ( "-" + versionString );
		}

		if ( classifier != null && ! classifier.isEmpty() && ! versionString.equals( "unspecified" ) ) {
			jarFileName += ( "-" + classifier );
		}

		return jarFileName + ".jar";
	}

	@Input
	public String getSourceDependency() {
		return sourceDependency.getGroup() + sourceDependency.getName() + sourceDependency.getVersion();
	}

	@OutputFile
	public Provider<RegularFile> getJarFile() {
		return jarFile;
	}

	@TaskAction
	public void transform() {
		transformerConfig.getTransformer().transform(
				Helper.extractResolvedArtifact( sourceConfiguration ).getFile(),
				jarFile.get().getAsFile()
		);
	}
}
