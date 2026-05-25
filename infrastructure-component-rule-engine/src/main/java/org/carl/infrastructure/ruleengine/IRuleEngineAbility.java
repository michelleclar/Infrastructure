package org.carl.infrastructure.ruleengine;

import org.carl.infrastructure.ruleengine.api.Facts;
import org.carl.infrastructure.ruleengine.api.Rule;
import org.carl.infrastructure.ruleengine.api.RuleEngine;
import org.carl.infrastructure.ruleengine.core.DefaultRuleEngine;

/**
 * Mixin interface that provides rule engine evaluation capability.
 *
 * <p>Implement this interface on a context or service class to gain access to a
 * {@link RuleEngine} without direct field injection. Following the Ability pattern,
 * the default implementation returns a stateless {@link DefaultRuleEngine} instance.
 *
 * <p>Override {@link #getRuleEngine()} to supply a custom or shared engine instance.
 */
public interface IRuleEngineAbility {

    /**
     * Returns the {@link RuleEngine} instance used for evaluating rules.
     *
     * @return a non-null {@link RuleEngine}
     */
    default RuleEngine getRuleEngine() {
        return new DefaultRuleEngine();
    }

    /**
     * Convenience method: fire the given rule against the provided facts using
     * the ability's rule engine.
     *
     * @param rule  the rule to evaluate and execute
     * @param facts the facts to pass to the rule
     */
    default void fireRule(Rule rule, Facts facts) {
        getRuleEngine().fire(rule, facts);
    }
}
