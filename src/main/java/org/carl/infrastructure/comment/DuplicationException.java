package org.carl.infrastructure.comment;

public class DuplicationException extends BaseException {
    public DuplicationException(int errorCode, String message) {
        super(errorCode, message);
    }
}
