package org.carl.infrastructure.mq.source;

public enum SourceProcessingGuarantees {
    ATLEAST_ONCE,
    ATMOST_ONCE,
    EFFECTIVELY_ONCE,
    MANUAL
}
