import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.carl.infrastructure.persistence.core.Dsl;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class TestPersistenceContext {
    @Inject Dsl dsl;

    @Test
    public void testPersistenceContext() {
        System.out.println(dsl);
    }
}
