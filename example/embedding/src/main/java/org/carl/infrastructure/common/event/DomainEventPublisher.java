package org.carl.infrastructure.common.event;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class DomainEventPublisher {

    public void publish(Object domainEvent) {
        //eventBus.fire(domainEvent);
    }
}
