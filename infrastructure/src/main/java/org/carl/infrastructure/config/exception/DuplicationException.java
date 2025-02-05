package org.carl.infrastructure.config.exception;

import org.carl.infrastructure.config.StatusType;

public class DuplicationException extends BaseException {
    public DuplicationException(StatusType statusType) {
        super(statusType, 0L);
    }
}
