package org.carl.ruleengine.v2;

@FunctionalInterface
public interface Action {
    String execute(int n);
}
