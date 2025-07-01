import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.testing.TestWorkflowExtension;
import io.temporal.worker.Worker;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import transfer.*;

import java.security.SecureRandom;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TestRun {
    @RegisterExtension
    public static final TestWorkflowExtension testWorkflowExtension =
            TestWorkflowExtension.newBuilder()
                    .registerWorkflowImplementationTypes(MoneyTransferWorkflowImpl.class)
                    .setActivityImplementations(new AccountActivityImpl())
                    .build();

    public static String randomAccountIdentifier() {
        SecureRandom localRandom = new SecureRandom();
        localRandom.setSeed(System.nanoTime());
        return IntStream.range(0, 9)
                .mapToObj(i -> String.valueOf(localRandom.nextInt(10)))
                .collect(Collectors.joining());
    }

    @Test
    public void testActivityImpl(
            TestWorkflowEnvironment testEnv, Worker worker, MoneyTransferWorkflow workflow) {
        // Configure the details for this money transfer request
        String referenceId = UUID.randomUUID().toString().substring(0, 18);
        String fromAccount = randomAccountIdentifier();
        String toAccount = randomAccountIdentifier();
        int amountToTransfer = ThreadLocalRandom.current().nextInt(15, 75);
        TransactionDetails transaction =
                new CoreTransactionDetails(fromAccount, toAccount, referenceId, amountToTransfer);
        workflow.transfer(transaction);
    }
}
