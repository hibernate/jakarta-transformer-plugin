package org.hibernate.build.gradle.jakarta.internal;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ResolutionStrategy;
import org.gradle.api.file.Directory;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Nested;

import org.hibernate.build.gradle.jakarta.TransformationException;

/**
 * Information shared across transformations
 *
 * @author Steve Ebersole
 */
public class TransformerConfig implements TransformerTool.Config, TransformerToolAccess {
	private final Instant buildStarted = Instant.now();

	private final Provider<Directory> outputDirectory;

	private final Provider<RegularFile> renameRules;
	private final Provider<RegularFile> versionRules;
	private final Provider<RegularFile> directRules;

	private final List<Substitutions> substitutions = new ArrayList<>();

	private TransformerTool transformerTool;

	public TransformerConfig(
			Configuration jakartaToolDependencies,
			Provider<Directory> outputDirectory,
			Provider<RegularFile> renameRules,
			Provider<RegularFile> versionRules,
			Provider<RegularFile> directRules,
			Project project) {
		this.outputDirectory = outputDirectory;

		this.renameRules = renameRules;
		this.versionRules = versionRules;
		this.directRules = directRules;

		project.afterEvaluate(
				(p) -> {
					transformerTool = new TransformerTool( jakartaToolDependencies, this, p );
				}
		);
	}

	public Provider<Directory> outputDirectoryAccess() {
		return outputDirectory;
	}

	public Instant getBuildStarted() {
		return buildStarted;
	}

	@Override
	@InputFile
	public Provider<RegularFile> renameRuleAccess() {
		return renameRules;
	}

	@Override
	@InputFile
	public Provider<RegularFile> versionRuleAccess() {
		return versionRules;
	}

	@Override
	@InputFile
	public Provider<RegularFile> directRuleAccess() {
		return directRules;
	}

	public void addSubstitutions(Substitutions substitutions) {
		this.substitutions.add( substitutions );
	}

	public void applyDependencyResolutionStrategy(Configuration configuration) {
		for ( Substitutions substitution : substitutions ) {
			substitution.applySubstitutions( configuration.getResolutionStrategy() );
		}
	}

	@Override
	public TransformerTool getTransformer() {
		if ( transformerTool == null ) {
			throw new TransformationException( "TransformerTool not yet initialized" );
		}
		return transformerTool;
	}

	@FunctionalInterface
	public interface Substitutions {
		void applySubstitutions(ResolutionStrategy resolutionStrategy);
	}
}
