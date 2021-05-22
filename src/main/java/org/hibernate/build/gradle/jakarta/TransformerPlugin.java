package org.hibernate.build.gradle.jakarta;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;

import org.hibernate.build.gradle.jakarta.internal.TransformerSpecImpl;

/**
 * Plugin to integrate the `JakartaTransformer` tool in Gradle builds
 *
 * @author Steve Ebersole
 */
public class TransformerPlugin implements Plugin<Project> {
	public static final String JAKARTA_TRANSFORMER_TOOL = "jakartaTransformerTool";
	public static final String JAKARTA_TRANSFORMATION = "jakartaTransformation";

	public static final String[] IMPLICIT_TOOL_DEPS = new String[] {
			"org.eclipse.transformer:org.eclipse.transformer:0.2.0",
			"org.eclipse.transformer:org.eclipse.transformer.cli:0.2.0"
	};

	@Override
	public void apply(Project project) {
		final Configuration transformerToolDependencies = project.getConfigurations().maybeCreate( JAKARTA_TRANSFORMER_TOOL );
		transformerToolDependencies.setDescription( "Dependencies for the JakartaTransformer tool" );

		project.getExtensions().create(
				JAKARTA_TRANSFORMATION,
				TransformerSpecImpl.class,
				transformerToolDependencies,
				project
		);
	}
}
