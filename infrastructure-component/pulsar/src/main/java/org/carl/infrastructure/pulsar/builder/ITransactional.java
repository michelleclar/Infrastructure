package org.carl.infrastructure.pulsar.builder;

public interface ITransactional {
    void commit() throws Exception;

    void rollback() throws Exception;
}
