import javax.persistence.*;

public class JpaConsumer {
	public void doIt() {
		EntityManager em = null;
		System.out.printf( "EntityManager class: `%s`\n", EntityManager.class.getName() );
		Helper.sayHi();
	}
}