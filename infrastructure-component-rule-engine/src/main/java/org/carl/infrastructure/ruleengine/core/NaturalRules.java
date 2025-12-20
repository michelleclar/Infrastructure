package org.carl.infrastructure.ruleengine.core;

import org.carl.infrastructure.ruleengine.api.Facts;
import org.carl.infrastructure.ruleengine.api.Rule;

import java.util.Collections;

/** This is Natural Rules */
public class NaturalRules extends CompositeRule {

    public static CompositeRule of(Rule... rules) {
        CompositeRule instance = new NaturalRules();
        Collections.addAll(instance.rules, rules);
        return instance;
    }

    @Override
    public boolean evaluate(Facts facts) {
        // 不支持, which means Natural Rules can not be the children of other rules
        throw new RuntimeException("evaluate not supported for natural composite");
    }

    @Override
    public void execute(Facts facts) {
        // 不支持, which means Natural Rules can not be the children of other rules
        throw new RuntimeException("execute not supported for natural composite");
    }

    @Override
    protected boolean doApply(Facts facts) {
        for (Rule rule : rules) {
            rule.apply(facts);
        }
        return true;
    }
}
