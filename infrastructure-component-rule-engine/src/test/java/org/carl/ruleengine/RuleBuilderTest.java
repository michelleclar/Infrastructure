package org.carl.ruleengine;

import org.carl.infrastructure.ruleengine.api.Facts;
import org.carl.infrastructure.ruleengine.api.Rule;
import org.carl.infrastructure.ruleengine.api.RuleEngine;
import org.carl.infrastructure.ruleengine.core.*;
import org.junit.jupiter.api.Test;

public class RuleBuilderTest {

    @Test
    public void testFizzBuzz() {
        RuleEngine fizzBuzzEngine = new DefaultRuleEngine();

        // create rules
        Rule fizzRule =
                new RuleBuilder()
                        .name("fizzRule")
                        .description("fizz rule when input times 3, output is Fizz")
                        .priority(10)
                        .when(facts -> (int) facts.get("number") % 3 == 0)
                        .then(facts -> System.out.print("Fizz"))
                        .build();

        Rule buzzRule =
                new RuleBuilder()
                        .name("buzzRule")
                        .description("buzz rule when input times 5, output is buzz")
                        .priority(1)
                        .when(facts -> (int) facts.get("number") % 5 == 0)
                        .then(facts -> System.out.print("Buzz"))
                        .build();

        Rule fizzBuzzRule = AllRules.allOf(fizzRule, buzzRule).name("fizzBuzzRule").priority(30);

        Rule defaultRule =
                new RuleBuilder()
                        .name("defaultRule")
                        .description("default rule, output number")
                        .priority(40)
                        .when(facts -> true)
                        .then(facts -> System.out.print((int) facts.get("number")))
                        .build();

        Rule rule = AnyRules.anyOf(fizzBuzzRule, fizzRule, buzzRule, defaultRule).name("anyRule");

        // fire rules
        Facts facts = new Facts();
        facts.put("number", 15);
        fizzBuzzEngine.fire(rule, facts);
    }
}
