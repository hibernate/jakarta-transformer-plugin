import java.net.URL;

import org.junit.jupiter.api.Test;

public class JpaConsumerTests {
	@Test
	public void testIt() {
		TestHelper.inTransaction(
				(entityManager, transactionManager) -> {
					final JpaConsumer jpaConsumer = new JpaConsumer();
					jpaConsumer.doIt();

					final URL mainResource = getClass().getClassLoader().getResource( "some.properties" );
					assert mainResource != null;

					final URL testResource = getClass().getClassLoader().getResource( "test.properties" );
					assert testResource != null;
				}
		);
	}
}