package org.carl.ruleengine;

import org.carl.infra.ruleengine.api.Rule;
import org.carl.infra.ruleengine.api.RuleEngine;
import org.carl.infra.ruleengine.core.DefaultRuleEngine;
import org.carl.infra.ruleengine.core.RuleBuilder;

public class HelloWorld {
    public static void main(String[] args) {
        RuleEngine ruleEngine = new DefaultRuleEngine();
        Rule rule =
                new RuleBuilder()
                        .name("hello world rule")
                        .description("always say hello world")
                        .priority(1)
                        .when(facts -> true)
                        .then(facts -> System.out.println("hello world"))
                        .build();

        ruleEngine.fire(rule, null);
    }
}
