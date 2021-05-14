import org.junit.jupiter.api.Test;

import com.github.sebersole.testkit.Project;
import com.github.sebersole.testkit.ProjectScope;
import com.github.sebersole.testkit.TestKit;

/**
 * @author Steve Ebersole
 */
@TestKit
public class SmokeTest {
	@Test
	@Project( "orm" )
	public void test(ProjectScope scope) {
		scope.createGradleRunner( "clean", "shadowTransform" ).build();
	}
}
