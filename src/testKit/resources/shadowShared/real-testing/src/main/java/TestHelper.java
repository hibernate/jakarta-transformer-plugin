import java.util.function.BiConsumer;
import javax.transaction.TransactionManager;
import javax.persistence.EntityManager;

public class TestHelper {
	public static void inTransaction(BiConsumer<EntityManager, TransactionManager> action) {
		System.out.printf( "(TestHelper) EntityManager class : `%s`\n", EntityManager.class.getName() );
		System.out.printf( "(TestHelper) TransactionManager class : `%s`\n", TransactionManager.class.getName() );
	}
}