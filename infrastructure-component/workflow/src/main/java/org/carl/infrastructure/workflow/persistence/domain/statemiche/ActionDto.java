package org.carl.infrastructure.workflow.persistence.domain.statemiche;

public class ActionDto<S, E, C> {
    private S from;
    private S to;
    private E event;
    private C context;

    public C getContext() {
        return context;
    }

    public ActionDto<S, E, C> setContext(C context) {
        this.context = context;
        return this;
    }

    public E getEvent() {
        return event;
    }

    public ActionDto<S, E, C> setEvent(E event) {
        this.event = event;
        return this;
    }

    public S getTo() {
        return to;
    }

    public ActionDto<S, E, C> setTo(S to) {
        this.to = to;
        return this;
    }

    public S getFrom() {
        return from;
    }

    public ActionDto<S, E, C> setFrom(S from) {
        this.from = from;
        return this;
    }
}
