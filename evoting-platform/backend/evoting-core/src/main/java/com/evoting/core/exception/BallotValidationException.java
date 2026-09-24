package com.evoting.core.exception;

import java.util.List;

/**
 * Thrown when ballot validation fails at the gateway.
 */
public class BallotValidationException extends RuntimeException {

    private final List<String> validationErrors;

    public BallotValidationException(String message) {
        super(message);
        this.validationErrors = List.of(message);
    }

    public BallotValidationException(List<String> errors) {
        super(String.join("; ", errors));
        this.validationErrors = List.copyOf(errors);
    }

    public List<String> getValidationErrors() { return validationErrors; }
}
