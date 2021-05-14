package org.hibernate.build.gradle.jakarta;

import javax.annotation.Nullable;

import org.gradle.api.GradleException;

/**
 * @author Steve Ebersole
 */
public class JakartaTransformationException extends GradleException {
	public JakartaTransformationException(String message) {
		super( message );
	}

	public JakartaTransformationException(String message, @Nullable Throwable cause) {
		super( message, cause );
	}
}
