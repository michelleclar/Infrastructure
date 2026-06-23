package org.carl.infrastructure.mq.source;

import org.carl.infrastructure.mq.common.ex.SourceAdminException;

public interface SourceAdmin extends AutoCloseable {

    void create(SourceDefinition source) throws SourceAdminException;

    void update(SourceDefinition source) throws SourceAdminException;

    void delete(SourceReference source) throws SourceAdminException;

    void start(SourceReference source) throws SourceAdminException;

    void start(SourceInstanceReference sourceInstance) throws SourceAdminException;

    void stop(SourceReference source) throws SourceAdminException;

    void stop(SourceInstanceReference sourceInstance) throws SourceAdminException;

    void restart(SourceReference source) throws SourceAdminException;

    void restart(SourceInstanceReference sourceInstance) throws SourceAdminException;

    SourceStatus getStatus(SourceReference source) throws SourceAdminException;

    SourceInstanceStatus getStatus(SourceInstanceReference sourceInstance) throws SourceAdminException;

    @Override
    default void close() throws SourceAdminException {}
}
