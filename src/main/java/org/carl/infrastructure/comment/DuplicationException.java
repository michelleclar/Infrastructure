package org.carl.infrastructure.comment;

public class DuplicationException extends BaseException {
    public DuplicationException(StatusType statusType) {
        super(statusType, 0L);
    }
}
