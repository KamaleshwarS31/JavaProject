package com.evoting.core.exception;

import com.evoting.core.domain.enums.ElectionState;

/**
 * Thrown when an invalid election state transition is attempted.
 */
public class ElectionStateException extends RuntimeException {

    private final ElectionState currentState;
    private final ElectionState targetState;

    public ElectionStateException(ElectionState currentState, ElectionState targetState) {
        super(String.format("Invalid election state transition: %s -> %s", currentState, targetState));
        this.currentState = currentState;
        this.targetState = targetState;
    }

    public ElectionState getCurrentState() { return currentState; }
    public ElectionState getTargetState() { return targetState; }
}
