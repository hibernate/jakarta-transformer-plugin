package org.hibernate.build.gradle.jakarta;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.gradle.api.Action;
import org.gradle.api.tasks.testing.TestFrameworkOptions;
import org.gradle.api.tasks.testing.junit.JUnitOptions;
import org.gradle.api.tasks.testing.junitplatform.JUnitPlatformOptions;
import org.gradle.api.tasks.testing.testng.TestNGOptions;
import org.gradle.jvm.toolchain.JavaLauncher;

import groovy.lang.Closure;
import groovy.lang.DelegatesTo;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("unused")
public interface ShadowTestSpec {
	void useJUnit();
	void useJUnit(Closure<JUnitOptions> testFrameworkConfigure);
	void useJUnit(Action<JUnitOptions> testFrameworkConfigure);

	void useJUnitPlatform();
	void useJUnitPlatform(Closure<JUnitPlatformOptions> testFrameworkConfigure);
	void useJUnitPlatform(Action<JUnitPlatformOptions> testFrameworkConfigure);

	void useTestNG();
	void useTestNG(Closure<TestNGOptions> testFrameworkConfigure);
	void useTestNG(Action<TestNGOptions> testFrameworkConfigure);

	TestFrameworkOptions options();
	void options(Closure<TestFrameworkOptions> closure);
	void options(Action<TestFrameworkOptions> action);

	boolean getFailFast();
	void setFailFast(boolean failFast);

	boolean getIgnoreFailures();
	void setIgnoreFailures(boolean ignoreFailures);

	void beforeSuite(Closure<?> closure);
	void beforeTest(Closure<?> closure);
	void afterTest(Closure<?> closure);
	void afterSuite(Closure<?> closure);

	Map<String, Object> systemProperties();
	default void systemProperty(String prop, Object value) {
		systemProperties().put( prop, value );
	}

	@SuppressWarnings("UnstableApiUsage")
	JavaLauncher getJavaLauncher();
	@SuppressWarnings("UnstableApiUsage")
	void setJavaLauncher(JavaLauncher launcher);

	String getMinHeapSize();
	void setMinHeapSize(String size);

	String getMaxHeapSize();
	void setMaxHeapSize(String size);

	List<String> getJvmArgs();

	default void jvmArg(String arg) {
		getJvmArgs().add( arg );
	}

	Set<String> getIncludes();

	default void include(String includePattern) {
		getIncludes().add( includePattern );
	}

	Set<String> getExcludes();

	default void exclude(String excludePattern) {
		getExcludes().add( excludePattern );
	}

}
