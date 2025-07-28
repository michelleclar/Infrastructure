package org.carl.infrastructure.ruleengine.api;

@FunctionalInterface
public interface Action {
    void execute(Facts facts);
}
