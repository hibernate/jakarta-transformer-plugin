/**
 * Integration of `JakartaTransformer` tool into Gradle build
 *
 * NOTE: A shadow transformation is the ability to consume a local project
 * or dependency, transform its artifacts and treat the transformed
 * artifacts as this project's published artifacts.  See
 * {@link org.hibernate.build.gradle.jakarta.TransformerSpec#shadow}
 *
 * @author Steve Ebersole
 */
package org.hibernate.build.gradle.jakarta;