package org.carl.infrastructure.ruleengine.core;

import org.carl.infrastructure.ruleengine.api.Facts;
import org.carl.infrastructure.ruleengine.api.Rule;
import org.carl.infrastructure.ruleengine.api.RuleEngine;

public class DefaultRuleEngine implements RuleEngine {

    @Override
    public void fire(Rule rule, Facts facts) {
        if (rule == null) {
            System.err.println("Rules is null! Nothing to apply");
            return;
        }
        rule.apply(facts);
    }
}
