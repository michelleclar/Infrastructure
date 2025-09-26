package org.carl.infrastructure.pulsar.core;

public interface ITransactional {
    void commit() throws Exception;

    void rollback() throws Exception;
}
