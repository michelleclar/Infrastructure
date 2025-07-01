package org.carl.infrastructure.component.web.ability;

public interface IWorkerAbility {
    /**
     * Worker Identification
     *
     * @return worker name
     */
    String getWorker();

    /**
     * workflow class
     *
     * @return class
     */
    Class<?>[] getWorkflowImplClasses();

    Object[] getActivityImplInstances();
}
