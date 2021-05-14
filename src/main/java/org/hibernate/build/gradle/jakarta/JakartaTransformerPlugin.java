package org.hibernate.build.gradle.jakarta;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;

/**
 * Plugin to support execution of the `org.eclipse.transformer.jakarta.JakartaTransformer` tool
 *
 * @author Steve Ebersole
 */
public class JakartaTransformerPlugin implements Plugin<Project> {
	public static final String JAKARTA_TRANSFORMER_TOOL = "jakartaTransformerTool";
	public static final String JAKARTA_TRANSFORMATION = "jakartaTransformation";

	@Override
	public void apply(Project project) {
		final Configuration transformerToolDependencies = project.getConfigurations().maybeCreate( JAKARTA_TRANSFORMER_TOOL );
		transformerToolDependencies.setDescription( "Dependencies for the JakartaTransformer tool" );

		final JakartaTransformerConfig config = project.getExtensions().create(
				JAKARTA_TRANSFORMATION,
				JakartaTransformerConfig.class,
				transformerToolDependencies,
				project
		);
	}
}
