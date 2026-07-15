package org.carl.infra.ruleengine.api;

@FunctionalInterface
public interface Action {
    void execute(Facts facts);
}
