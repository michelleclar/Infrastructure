package workflow;

import io.smallrye.mutiny.Uni;
import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.carl.infrastructure.component.web.annotations.ControllerLogged;
import org.jboss.logging.Logger;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Path("/workflow")
@ControllerLogged
public class WorkflowController {
    @Inject WorkflowClient client;
    @Inject Logger logger;

    public static String randomAccountIdentifier() {
        return IntStream.range(0, 9)
                .mapToObj(i -> String.valueOf(random.nextInt(10)))
                .collect(Collectors.joining());
    }

    private static final SecureRandom random;

    static {
        // Seed the random number generator with nano date
        random = new SecureRandom();
        random.setSeed(Instant.now().getNano());
    }

    @GET
    @Path("/transfer")
    public Uni<String> transfer() {
        WorkflowOptions options =
                WorkflowOptions.newBuilder()
                        .setTaskQueue(Shared.MONEY_TRANSFER_TASK_QUEUE)
                        .setWorkflowId("money-transfer-workflow")
                        .build();

        // WorkflowStubs enable calls to methods as if the Workflow object is local
        // but actually perform a gRPC call to the Temporal Service.
        MoneyTransferWorkflow workflow =
                client.newWorkflowStub(MoneyTransferWorkflow.class, options);
        String referenceId = UUID.randomUUID().toString().substring(0, 18);
        String fromAccount = randomAccountIdentifier();
        String toAccount = randomAccountIdentifier();
        int amountToTransfer = ThreadLocalRandom.current().nextInt(15, 75);
        TransactionDetails transaction =
                new CoreTransactionDetails(fromAccount, toAccount, referenceId, amountToTransfer);

        // Perform asynchronous execution.
        // This process exits after making this call and printing details.
        WorkflowExecution we = WorkflowClient.start(workflow::transfer, transaction);
        logger.info("\nMONEY TRANSFER PROJECT\n\n");
        logger.infof(
                "Initiating transfer of $%d from [Account %s] to [Account %s].\n\n",
                amountToTransfer, fromAccount, toAccount);
        logger.infof(
                "[WorkflowID: %s]\n[RunID: %s]\n[Transaction Reference: %s]\n\n",
                we.getWorkflowId(), we.getRunId(), referenceId);
        return Uni.createFrom().item(we.getRunId());
    }
}
