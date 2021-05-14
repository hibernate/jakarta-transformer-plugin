package org.hibernate.build.gradle.jakarta;

import java.util.Set;

import org.gradle.api.Action;
import org.gradle.api.artifacts.ResolutionStrategy;
import org.gradle.api.provider.Property;

import groovy.lang.Closure;
import groovy.lang.DelegatesTo;

/**
 * @author Steve Ebersole
 */
public interface ShadowTransformationSpec {
	void additionalSourceClassifier(String classifiers);
	void additionalSourceClassifiers(String... classifiers);

	/**
	 * Pattern to rename the artifacts for the source dependency.
	 * The original name is referencable using `${name}`.  E.g.
	 * `${name}-jakarta` would rename a source artifact `my-artifact`
	 * as `my-artifact-jakarta`.
	 *
	 * `${name}-jakarta` is actually the default
	 */
	Property<String> shadowArtifactNamePattern();
	void shadowArtifactNamePattern(String name);

	void dependencyResolutions(@DelegatesTo(value = ResolutionStrategy.class, strategy = 1) Closure config);
	void dependencyResolutions(Action<ResolutionStrategy> config);
}
