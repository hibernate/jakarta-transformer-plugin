package org.hibernate.build.gradle.jakarta.internal;

import java.util.function.Supplier;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ResolvedConfiguration;
import org.gradle.api.provider.Property;
import org.gradle.api.publish.maven.MavenPom;

import groovy.util.Node;

/**
 * @author Steve Ebersole
 */
public class PomHelper {
	private PomHelper() {
		// disallow direct instantiation
	}

	public static void copy(
			Project sourceProject,
			MavenPom sourcePom,
			Project targetProject,
			MavenPom targetPom,
			Configuration compileDependencies,
			Configuration runtimeDependencies) {
		injectPomString( sourcePom.getName(), targetPom.getName(), () -> "(shadowed) " + sourceProject.getPath() );
		injectPomString( sourcePom.getDescription(), targetPom.getDescription(), () -> "(shadowed) " + sourceProject.getName() );
		injectPomString( sourcePom.getUrl(), targetPom.getUrl(), () -> null );

		sourcePom.ciManagement(
				(sourceCiManagement) -> {
					targetPom.ciManagement(
							(targetCiManagement) -> {
								injectPomString( sourceCiManagement.getUrl(), targetCiManagement.getUrl(), () -> null );
								injectPomString( sourceCiManagement.getSystem(), targetCiManagement.getSystem(), () -> null );
							}
					);
				}
		);

		sourcePom.organization(
				(sourceOrganization) -> {
					targetPom.organization(
							(targetOrganization) -> {
								injectPomString( sourceOrganization.getName(), targetOrganization.getName(), () -> null );
								injectPomString( sourceOrganization.getUrl(), targetOrganization.getUrl(), () -> null );
							}
					);
				}
		);

		sourcePom.licenses(
				(sourceLicenses) -> {
					targetPom.licenses(
							(targetLicenses) -> {
								sourceLicenses.license(
										(sourceLicense) -> {
											targetLicenses.license(
													(targetLicense) -> {
														injectPomString( sourceLicense.getName(), targetLicense.getName(), () -> null );
														injectPomString( sourceLicense.getComments(), targetLicense.getComments(), () -> null );
														injectPomString( sourceLicense.getUrl(), targetLicense.getUrl(), () -> null );
														injectPomString( sourceLicense.getDistribution(), targetLicense.getDistribution(), () -> null );
													}
											);

										}
								);
							}
					);
				}
		);

		sourcePom.scm(
				(sourceScm) -> {
					targetPom.scm(
							(targetScm) -> {
								injectPomString( sourceScm.getConnection(), targetScm.getConnection(), () -> null );
								injectPomString( sourceScm.getDeveloperConnection(), targetScm.getDeveloperConnection(), () -> null );
								injectPomString( sourceScm.getUrl(), targetScm.getUrl(), () -> null );
								injectPomString( sourceScm.getTag(), targetScm.getTag(), () -> null );
							}
					);
				}
		);

		sourcePom.issueManagement(
				(sourceIssueTracker) -> {
					targetPom.issueManagement(
							(targetIssueTracker) -> {
								injectPomString( sourceIssueTracker.getSystem(), targetIssueTracker.getSystem(), () -> null );
								injectPomString( sourceIssueTracker.getUrl(), targetIssueTracker.getUrl(), () -> null );
							}
					);
				}
		);

		sourcePom.developers(
				(sourceDevelopers) -> {
					targetPom.developers(
							(targetDevelopers) -> {
								sourceDevelopers.developer(
										(sourceDeveloper) -> {
											targetDevelopers.developer(
													(targetDeveloper) -> {
														injectPomString( sourceDeveloper.getId(), targetDeveloper.getId(), () -> null );
														injectPomString( sourceDeveloper.getName(), targetDeveloper.getName(), () -> null );
														injectPomString( sourceDeveloper.getUrl(), targetDeveloper.getUrl(), () -> null );
														injectPomString( sourceDeveloper.getEmail(), targetDeveloper.getEmail(), () -> null );
														injectPomString( sourceDeveloper.getTimezone(), targetDeveloper.getTimezone(), () -> null );
														injectPomString( sourceDeveloper.getOrganization(), targetDeveloper.getOrganization(), () -> null );
														injectPomString( sourceDeveloper.getOrganizationUrl(), targetDeveloper.getOrganizationUrl(), () -> null );

														targetDeveloper.getRoles().addAll( sourceDeveloper.getRoles() );
														targetDeveloper.getProperties().putAll( sourceDeveloper.getProperties() );
													}
											);
										}
								);

							}
					);
				}
		);


//		// Ugh...
//		targetPom.withXml(
//				(xml) -> {
//					final Node rootNode = xml.asNode();
//					final Node dependenciesNode = rootNode.appendNode( "dependencies" );
//					applyDependencies( dependenciesNode, compileDependencies, "compile" );
//					applyDependencies( dependenciesNode, runtimeDependencies, "runtime" );
//				}
//		);
	}

	private static void injectPomString(
			Property<String> sourceProperty,
			Property<String> targetProperty,
			Supplier<String> defaultValueAccess) {
		if ( targetProperty.isPresent() ) {
			return;
		}

		if ( sourceProperty.isPresent() ) {
			targetProperty.set( "(shadowed) " + sourceProperty.get() );
		}
		else {
			targetProperty.set( defaultValueAccess.get() );
		}
	}

	private static void applyDependencies(Node dependenciesNode, Configuration dependencies, String scope) {
		final ResolvedConfiguration resolvedCompileDependencies = dependencies.getResolvedConfiguration();
		resolvedCompileDependencies.getFirstLevelModuleDependencies().forEach(
				(resolvedDependency) -> {
					final Node dependencyNode = dependenciesNode.appendNode( "dependency" );
					dependencyNode.appendNode( "groupId" ).setValue( resolvedDependency.getModuleGroup() );
					dependencyNode.appendNode( "artifactId" ).setValue( resolvedDependency.getModuleName() );
					dependencyNode.appendNode( "version" ).setValue( resolvedDependency.getModuleVersion() );
					dependencyNode.appendNode( "scope" ).setValue( scope );
				}
		);

	}
}
