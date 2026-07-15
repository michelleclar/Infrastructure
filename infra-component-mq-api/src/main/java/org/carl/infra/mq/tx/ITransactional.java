package org.carl.infra.mq.tx;

public interface ITransactional {
    void commit() throws Exception;

    void rollback() throws Exception;
}
