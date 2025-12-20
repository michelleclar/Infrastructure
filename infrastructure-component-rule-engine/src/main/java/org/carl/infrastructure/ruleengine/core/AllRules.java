package org.carl.infrastructure.ruleengine.core;

import org.carl.infrastructure.ruleengine.api.Facts;
import org.carl.infrastructure.ruleengine.api.Rule;

import java.util.Collections;

public class AllRules extends CompositeRule {

    public static CompositeRule allOf(Rule... rules) {
        CompositeRule instance = new AllRules();
        Collections.addAll(instance.rules, rules);
        return instance;
    }

    @Override
    public boolean evaluate(Facts facts) {
        if (rules.stream().allMatch(rule -> rule.evaluate(facts))) return true;
        return false;
    }

    @Override
    public void execute(Facts facts) {
        for (Rule rule : rules) {
            rule.execute(facts);
        }
    }

    @Override
    protected boolean doApply(Facts facts) {
        System.out.println("start AND composite rule apply");
        if (evaluate(facts)) {
            for (Rule rule : rules) {
                // 所有的rules都执行
                rule.execute(facts);
            }
            return true;
        }
        return false;
    }
}
