package org.hibernate.build.gradle.jakarta.internal;

import org.gradle.api.artifacts.ResolutionStrategy;

/**
 * @author Steve Ebersole
 */
@FunctionalInterface
public interface Substitutions {
	void applySubstitutions(ResolutionStrategy resolutionStrategy);
}
