package com.aiwishcoding.easymock.exception;

public class RuleNotFoundException extends RuntimeException {

    public RuleNotFoundException(Long id) {
        super("Mock rule not found: " + id);
    }
}
