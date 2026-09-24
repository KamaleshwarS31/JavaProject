package com.evoting.core.exception;

/**
 * Thrown when a nullifier has already been used for this election.
 * This indicates a duplicate voting attempt (or credential replay attack).
 */
public class NullifierDuplicateException extends RuntimeException {

    private final String nullifierHash;
    private final String electionId;

    public NullifierDuplicateException(String nullifierHash, String electionId) {
        super("Nullifier already recorded for election: " + electionId);
        this.nullifierHash = nullifierHash;
        this.electionId = electionId;
    }

    public String getNullifierHash() { return nullifierHash; }
    public String getElectionId() { return electionId; }
}
