package org.carl.infrastructure.ruleengine.core;

import org.carl.infrastructure.ruleengine.api.Action;
import org.carl.infrastructure.ruleengine.api.Condition;
import org.carl.infrastructure.ruleengine.api.Rule;

import java.util.ArrayList;
import java.util.List;

public class RuleBuilder {

    private final List<Action> actions = new ArrayList<>();
    private String name = Rule.DEFAULT_NAME;
    private String description = Rule.DEFAULT_DESCRIPTION;
    private int priority = Rule.DEFAULT_PRIORITY;
    private Condition condition = Condition.FALSE;

    /**
     * Set rule name.
     *
     * @param name of the rule
     * @return the builder instance
     */
    public RuleBuilder name(String name) {
        this.name = name;
        return this;
    }

    /**
     * Set rule description.
     *
     * @param description of the rule
     * @return the builder instance
     */
    public RuleBuilder description(String description) {
        this.description = description;
        return this;
    }

    /**
     * Set rule priority.
     *
     * @param priority of the rule
     * @return the builder instance
     */
    public RuleBuilder priority(int priority) {
        this.priority = priority;
        return this;
    }

    /**
     * Set rule condition.
     *
     * @param condition of the rule
     * @return the builder instance
     */
    public RuleBuilder when(Condition condition) {
        this.condition = condition;
        return this;
    }

    /**
     * Add an action to the rule.
     *
     * @param action to add
     * @return the builder instance
     */
    public RuleBuilder then(Action action) {
        this.actions.add(action);
        return this;
    }

    /**
     * Create a new {@link Rule}.
     *
     * @return a new rule instance
     */
    public Rule build() {
        return new DefaultRule(name, description, priority, condition, actions);
    }
}
