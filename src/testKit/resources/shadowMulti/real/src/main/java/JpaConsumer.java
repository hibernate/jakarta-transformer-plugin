import java.net.URL;
import javax.persistence.*;

public class JpaConsumer {
	public void doIt() {
		EntityManager em = null;
		System.out.printf( "EntityManager class: `%s`\n", EntityManager.class.getName() );
		Helper.sayHi();

		// make sure we can find our resource...
		final URL resource = getClass().getClassLoader().getResource( "some.properties" );
		assert resource != null;
	}
}