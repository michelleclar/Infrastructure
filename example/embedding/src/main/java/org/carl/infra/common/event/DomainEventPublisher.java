package org.carl.infra.common.event;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class DomainEventPublisher {

    public void publish(Object domainEvent) {
        //eventBus.fire(domainEvent);
    }
}
