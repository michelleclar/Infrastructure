package org.carl.infrastructure.workflow.config;

import io.quarkus.runtime.StartupEvent;

import jakarta.enterprise.event.Observes;

import java.util.*;

/**
 * not work
 *
 * <p>need disable quarkus.temporal.workflow.disable-eager-execution=false
 */
// @ApplicationScoped
public class WorkflowLifecycle {
    //
    //    private static final Logger LOGGER = Logger.getLogger(WorkflowLifecycle.class);
    //    @Inject WorkflowClient client;
    //    @Inject Instance<IStateMachineAbility<?, ?, ?>> stateMachineAbilityInstance;
    //
    void onStart(@Observes StartupEvent ev) {
        //        LOGGER.info("beg init worker");
        //        WorkerFactory factory = WorkerFactory.newInstance(client);
        //        WorkerManger.workflowClient = client;
        //        Set<String> machineIds = StateMachineFactory.getMachineIds();
        //        for (IStateMachineAbility<?, ?, ?> ability : stateMachineAbilityInstance) {
        //            ability.machineId();
        //            Worker worker = factory.newWorker(ability.machineId());
        //            LOGGER.infof("TaskQueue [%s] is Completed", ability.machineId());
        //        }
        //        for (String machine : machineIds) {
        //            Worker worker = factory.newWorker(machine);
        //            LOGGER.infof("TaskQueue [%s] is Completed", machine);
        //        }
        //        factory.start();
        //        LOGGER.infof("TaskQueues [%s] is started", machineIds);
    }
}
