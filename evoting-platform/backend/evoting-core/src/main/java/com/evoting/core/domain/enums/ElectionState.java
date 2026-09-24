package com.evoting.core.domain.enums;

import java.util.EnumSet;
import java.util.Set;

/**
 * Finite State Machine for Election lifecycle.
 * Invalid transitions must be rejected by the service layer AND blockchain chaincode.
 */
public enum ElectionState {

    DRAFT,
    READY,
    ACTIVE,
    CLOSED,
    TALLYING,
    FINALIZED;

    /**
     * Returns the valid next states from the current state.
     * Any transition not in this set MUST be rejected.
     */
    public Set<ElectionState> validNextStates() {
        return switch (this) {
            case DRAFT     -> EnumSet.of(READY);
            case READY     -> EnumSet.of(ACTIVE, DRAFT); // can revert to DRAFT if not yet activated
            case ACTIVE    -> EnumSet.of(CLOSED);
            case CLOSED    -> EnumSet.of(TALLYING);
            case TALLYING  -> EnumSet.of(FINALIZED);
            case FINALIZED -> EnumSet.noneOf(ElectionState.class); // terminal state
        };
    }

    /**
     * Validates whether a transition to the given target state is permitted.
     *
     * @param target the desired next state
     * @return true if the transition is allowed
     */
    public boolean canTransitionTo(ElectionState target) {
        return validNextStates().contains(target);
    }

    /** Returns true if voting is currently permitted in this election state. */
    public boolean isVotingAllowed() {
        return this == ACTIVE;
    }

    /** Returns true if the election configuration may be modified. */
    public boolean isConfigurationEditable() {
        return this == DRAFT;
    }

    /** Returns true if this is the terminal state. */
    public boolean isTerminal() {
        return this == FINALIZED;
    }
}
