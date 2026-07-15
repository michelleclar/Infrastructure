package org.carl.infra.persistence.function;

import java.sql.Connection;

@FunctionalInterface
public interface ConnectionRunnable {
    void run(Connection var1) throws Throwable;
}
