package org.hibernate.build.gradle.jakarta;

import org.gradle.api.DefaultTask;

/**
 * Specialized transformation task for handling the case of
 * a project (B) being simply the transformed version of another (A)
 *
 * @author Steve Ebersole
 */
public abstract class TransformationSourceTask extends DefaultTask {
}
