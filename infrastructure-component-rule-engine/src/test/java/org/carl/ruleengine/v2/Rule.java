package org.carl.ruleengine.v2;

@FunctionalInterface
public interface Rule {
    String apply(int n);
}
