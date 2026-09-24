package com.evoting.core.domain.enums;

import java.util.EnumSet;
import java.util.Set;

/**
 * Finite State Machine for individual Ballot lifecycle.
 */
public enum BallotState {

    CREATED,
    PROOF_VALIDATED,
    BALLOT_ACCEPTED,
    BATCHED,
    BLOCKCHAIN_ANCHORED,
    TALLY_INCLUDED,
    FINALIZED,
    REJECTED;

    public Set<BallotState> validNextStates() {
        return switch (this) {
            case CREATED            -> EnumSet.of(PROOF_VALIDATED, REJECTED);
            case PROOF_VALIDATED    -> EnumSet.of(BALLOT_ACCEPTED, REJECTED);
            case BALLOT_ACCEPTED    -> EnumSet.of(BATCHED);
            case BATCHED            -> EnumSet.of(BLOCKCHAIN_ANCHORED);
            case BLOCKCHAIN_ANCHORED-> EnumSet.of(TALLY_INCLUDED);
            case TALLY_INCLUDED     -> EnumSet.of(FINALIZED);
            case FINALIZED          -> EnumSet.noneOf(BallotState.class);
            case REJECTED           -> EnumSet.noneOf(BallotState.class);
        };
    }

    public boolean canTransitionTo(BallotState target) {
        return validNextStates().contains(target);
    }

    public boolean isTerminal() {
        return this == FINALIZED || this == REJECTED;
    }
}
