package org.carl.infrastructure.config.exception;

import org.carl.infrastructure.comment.StatusType;

public class DuplicationException extends BaseException {
    public DuplicationException(StatusType statusType) {
        super(statusType, 0L);
    }
}
