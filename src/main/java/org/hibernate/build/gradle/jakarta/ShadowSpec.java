package org.hibernate.build.gradle.jakarta;

import org.gradle.api.Action;

import groovy.lang.Closure;

/**
 * Specification for configuring a shadow transformation
 *
 * @implNote Regardless of the original artifact name of any additional
 * artifacts to add to the publication, the shadow form will always
 * use the artifact name from the main shadow dependency.
 *
 * @author Steve Ebersole
 */
@SuppressWarnings("unused")
public interface ShadowSpec {
	/**
	 * Transform the test dependency and run the transformed tests
	 * against the transformed main JAR.
	 *
	 * This form attempts to implicitly determine the dependency notation
	 * for the tests
	 */
	void runTests(Action<ShadowTestSpec> specAction);
	void runTests(Closure<ShadowTestSpec> closure);

	/**
	 * Transform the specified test dependency and run the transformed tests
	 * against the transformed main JAR.
	 */
	void runTests(Object testsDependencyNotation, Action<ShadowTestSpec> specAction);
	void runTests(Object testsDependencyNotation, Closure<ShadowTestSpec> closure);

	/**
	 * Include sources in the transformation and as an artifact in the shadow publication
	 *
	 * This form attempts to implicitly determine the dependency notation for the tests
	 */
	void withSources();

	/**
	 * Include javadoc in the transformation and as an artifact in the shadow publication
	 *
	 * This form attempts to implicitly determine the dependency notation for the tests
	 */
	void withJavadoc();
}
