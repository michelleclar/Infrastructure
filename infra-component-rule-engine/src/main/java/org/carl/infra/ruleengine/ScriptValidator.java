package org.carl.infra.ruleengine;

import com.alibaba.qlexpress4.Express4Runner;
import com.alibaba.qlexpress4.InitOptions;

/** Syntax only: does not execute scripts or validate business data. */
public final class ScriptValidator {
    private final Express4Runner runner = new Express4Runner(InitOptions.DEFAULT_OPTIONS);

    /** Preserves QLExpress syntax exceptions, including line, column and error code. */
    public void check(String script) {
        if (script == null || script.isBlank()) {
            throw new IllegalArgumentException("script must not be blank");
        }
        runner.check(script);
    }
}
