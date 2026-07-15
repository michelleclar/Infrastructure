package org.carl.infra.ruleengine.core;

import org.carl.infra.ruleengine.api.Facts;
import org.carl.infra.ruleengine.api.Rule;
import org.carl.infra.ruleengine.api.RuleEngine;

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
