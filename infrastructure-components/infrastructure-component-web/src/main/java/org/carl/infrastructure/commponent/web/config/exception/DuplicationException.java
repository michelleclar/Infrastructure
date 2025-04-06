package org.carl.infrastructure.commponent.web.config.exception;

import org.carl.infrastructure.commponent.web.config.StatusType;

public class DuplicationException extends BaseException {
    public DuplicationException(StatusType statusType) {
        super(statusType, 0L);
    }
}
