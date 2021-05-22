package org.hibernate.build.gradle.jakarta;

import org.gradle.api.Action;
import org.gradle.api.artifacts.DependencySubstitutions;
import org.gradle.api.artifacts.ResolutionStrategy;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;

import org.hibernate.build.gradle.jakarta.adhoc.DependencyTransformationSpec;
import org.hibernate.build.gradle.jakarta.adhoc.FileTransformationSpec;

import groovy.lang.Closure;

/**
 * Gradle DSL extension for configuring the transformations
 *
 * @author Steve Ebersole
 */
@SuppressWarnings("unused")
public interface TransformerSpec {

	/**
	 * Where the transformed jars should be written
	 */
	DirectoryProperty getOutputDir();

	/**
	 * Setter for {@link #getOutputDir()}
	 */
	void outputDir(Object outputDirSyntax);

	/**
	 * Rename rules to apply
	 */
	RegularFileProperty getRenameRules();

	/**
	 * Setter for {@link #getRenameRules()}
	 */
	void renameRules(Object renameRuleFile);

	/**
	 * Version rules to apply
	 */
	RegularFileProperty getVersionRules();

	/**
	 * Setter for {@link #getVersionRules()}
	 */
	void versionRules(Object versioRulesFile);

	/**
	 * Direct rules to apply
	 */
	RegularFileProperty getDirectRules();

	/**
	 * Setter for {@link #getDirectRules()}
	 */
	void directRules(Object directRulesFile);

	/**
	 * Configure resolution rules for the resolution of the dependencies, generally to
	 * apply substitutions
	 *
	 * @see ResolutionStrategy#getDependencySubstitution()
	 * @see DependencySubstitutions#substitute
	 */
	void dependencyResolutions(Closure<ResolutionStrategy> closure);

	/**
	 * Configure resolution rules for the resolution of the dependencies, generally to
	 * apply substitutions
	 *
	 * @see ResolutionStrategy#getDependencySubstitution()
	 * @see DependencySubstitutions#substitute
	 */
	void dependencyResolutions(Action<ResolutionStrategy> strategyAction);

	/**
	 * Perform a "shadow" transformation.  See {@link org.hibernate.build.gradle.jakarta}.
	 */
	void shadow(Object shadowSource, Closure<ShadowSpec> closure);

	/**
	 * Perform a "shadow" transformation.  See {@link org.hibernate.build.gradle.jakarta}.
	 */
	void shadow(Object shadowSource, Action<ShadowSpec> specAction);

	/**
	 * Applies transformation to a dependency
	 */
	void dependencyTransformation(String name, Closure<DependencyTransformationSpec> closure);

	/**
	 * Applies transformation to a JAR file
	 */
	void fileTransformation(String name, Closure<FileTransformationSpec> closure);
}
