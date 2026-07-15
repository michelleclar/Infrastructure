package org.carl.infra.ruleengine;

import org.carl.infra.ruleengine.api.Facts;
import org.carl.infra.ruleengine.api.Rule;
import org.carl.infra.ruleengine.api.RuleEngine;
import org.carl.infra.ruleengine.core.DefaultRuleEngine;

/**
 * Ability interface that grants implementing classes access to a shared {@link RuleEngine}.
 *
 * <p>The default engine is a shared static constant — implementors need not allocate a new engine
 * on every call.
 */
public interface IRuleEngineAbility {

    /** Shared, reusable engine instance (interface field is implicitly public static final). */
    RuleEngine DEFAULT_ENGINE = new DefaultRuleEngine();

    /**
     * Return the rule engine to use. Defaults to {@link #DEFAULT_ENGINE}.
     *
     * @return the {@link RuleEngine} instance
     */
    default RuleEngine getRuleEngine() {
        return DEFAULT_ENGINE;
    }

    /**
     * Fire the given rule against the provided facts using this ability's engine.
     *
     * @param rule  the rule to fire; must not be null
     * @param facts the facts to evaluate against
     * @throws IllegalArgumentException if {@code rule} is null
     */
    default void fireRule(Rule rule, Facts facts) {
        if (rule == null) {
            throw new IllegalArgumentException("rule must not be null");
        }
        getRuleEngine().fire(rule, facts);
    }
}
