package org.carl.infrastructure.ruleengine;

import org.carl.infrastructure.ruleengine.api.Facts;
import org.carl.infrastructure.ruleengine.api.Rule;
import org.carl.infrastructure.ruleengine.api.RuleEngine;
import org.carl.infrastructure.ruleengine.core.RuleBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link IRuleEngineAbility}.
 */
class IRuleEngineAbilityTest {

    /** Minimal concrete implementation for testing. */
    static class Subject implements IRuleEngineAbility {}

    // -----------------------------------------------------------------------
    // getRuleEngine()
    // -----------------------------------------------------------------------

    @Test
    void getRuleEngine_returnsNonNull() {
        Subject subject = new Subject();
        RuleEngine engine = subject.getRuleEngine();
        assertNotNull(engine, "getRuleEngine() must return a non-null engine");
    }

    @Test
    void getRuleEngine_returnsSameStaticInstance() {
        Subject a = new Subject();
        Subject b = new Subject();
        assertSame(a.getRuleEngine(), b.getRuleEngine(),
                "getRuleEngine() must return the shared static DEFAULT_ENGINE");
    }

    // -----------------------------------------------------------------------
    // fireRule() — happy path: delegates to engine
    // -----------------------------------------------------------------------

    @Test
    void fireRule_executesActionWhenConditionIsTrue() {
        Subject subject = new Subject();
        boolean[] actionFired = {false};

        Rule rule = new RuleBuilder()
                .when(facts -> true)
                .then(facts -> actionFired[0] = true)
                .build();

        Facts facts = new Facts();
        subject.fireRule(rule, facts);

        assertTrue(actionFired[0], "fireRule() must delegate to the engine and execute the rule's action");
    }

    @Test
    void fireRule_doesNotExecuteActionWhenConditionIsFalse() {
        Subject subject = new Subject();
        boolean[] actionFired = {false};

        Rule rule = new RuleBuilder()
                .when(facts -> false)
                .then(facts -> actionFired[0] = true)
                .build();

        Facts facts = new Facts();
        subject.fireRule(rule, facts);

        assertFalse(actionFired[0], "fireRule() must not execute the action when the condition is false");
    }

    // -----------------------------------------------------------------------
    // fireRule() — null guard
    // -----------------------------------------------------------------------

    @Test
    void fireRule_throwsIllegalArgumentExceptionForNullRule() {
        Subject subject = new Subject();
        assertThrows(IllegalArgumentException.class,
                () -> subject.fireRule(null, new Facts()),
                "fireRule() must throw IllegalArgumentException when rule is null");
    }
}
