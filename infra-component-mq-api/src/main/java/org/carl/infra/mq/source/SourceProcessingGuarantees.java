package org.carl.infra.mq.source;

public enum SourceProcessingGuarantees {
    ATLEAST_ONCE,
    ATMOST_ONCE,
    EFFECTIVELY_ONCE,
    MANUAL
}
