package com.aiwishcoding.easymock.exception;

public class RuleValidationException extends RuntimeException {

    public RuleValidationException(String message) {
        super(message);
    }

    public RuleValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
