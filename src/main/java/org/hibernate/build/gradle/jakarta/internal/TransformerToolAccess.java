package org.hibernate.build.gradle.jakarta.internal;

/**
 * Access to the TransformerTool helper.  Allows delayed creation of the TransformerTool.
 *
 * Do not access until ready to perform a transformation!
 *
 * @author Steve Ebersole
 */
public interface TransformerToolAccess {
	/**
	 * Access the transformation helper.
	 */
	TransformerTool getTransformer();
}
