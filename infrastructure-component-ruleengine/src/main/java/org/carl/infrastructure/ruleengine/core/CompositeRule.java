package org.carl.infrastructure.ruleengine.core;

import org.carl.infrastructure.ruleengine.api.Facts;
import org.carl.infrastructure.ruleengine.api.Rule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class CompositeRule extends AbstractRule {

    protected List<Rule> rules = new ArrayList<>();

    private boolean isSorted = false;

    public CompositeRule() {}

    public CompositeRule priority(int priority) {
        this.priority = priority;
        return this;
    }

    public CompositeRule name(String name) {
        this.name = name;
        return this;
    }

    @Override
    public boolean apply(Facts facts) {
        sort();
        return doApply(facts);
    }

    protected abstract boolean doApply(Facts facts);

    protected void sort() {
        if (!isSorted) {
            System.out.println(this.name + " before sort:" + rules);
            Collections.sort(rules);
            System.out.println(this.name + " after sort:" + rules);
            isSorted = true;
        }
    }
}
