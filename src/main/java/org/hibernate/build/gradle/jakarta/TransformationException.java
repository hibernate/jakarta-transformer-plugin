package org.hibernate.build.gradle.jakarta;

import javax.annotation.Nullable;

import org.gradle.api.GradleException;

/**
 * @author Steve Ebersole
 */
public class TransformationException extends GradleException {
	public TransformationException(String message) {
		super( message );
	}

	public TransformationException(String message, @Nullable Throwable cause) {
		super( message, cause );
	}
}
