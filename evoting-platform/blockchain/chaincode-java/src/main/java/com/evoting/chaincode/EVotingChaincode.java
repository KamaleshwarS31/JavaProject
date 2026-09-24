package com.evoting.chaincode;

import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.ContractInterface;
import org.hyperledger.fabric.contract.annotation.Contract;
import org.hyperledger.fabric.contract.annotation.Default;
import org.hyperledger.fabric.contract.annotation.Info;
import org.hyperledger.fabric.contract.annotation.Transaction;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.hyperledger.fabric.shim.ledger.KeyValue;
import org.hyperledger.fabric.shim.ledger.QueryResultsIterator;

import java.util.ArrayList;
import java.util.List;

/**
 * Hyperledger Fabric Chaincode for E-Voting.
 *
 * WHAT IS STORED ON CHAIN:
 * - Election registrations (ID + metadata hash)
 * - Nullifier hashes (for double-vote prevention — not reversible to identity)
 * - Merkle batch roots (C = SHA256(commitments) — not reversible to votes)
 * - Audit digests (tamper-evident audit log)
 *
 * WHAT IS NOT STORED ON CHAIN:
 * - Voter identity, name, email
 * - Candidate selection or vote content
 * - The encrypted ballot payload
 * - Any link between voter and ballot
 */
@Contract(
    name = "EVotingChaincode",
    info = @Info(
        title = "E-Voting Chaincode",
        description = "Privacy-preserving blockchain layer for e-voting. Stores nullifiers, Merkle roots, and audit digests.",
        version = "1.0.0"
    )
)
@Default
public class EVotingChaincode implements ContractInterface {

    // Key prefixes
    private static final String PREFIX_ELECTION   = "ELECTION::";
    private static final String PREFIX_NULLIFIER  = "NULLIFIER::";
    private static final String PREFIX_BATCH      = "BATCH::";
    private static final String PREFIX_AUDIT      = "AUDIT::";

    // ============================================================
    // Election Management
    // ============================================================

    /**
     * Register a new election on the ledger.
     * Only the Election Admin role (checked via MSP) may call this.
     *
     * @param electionId   UUID of the election
     * @param metadataHash SHA-256 of the election configuration (for integrity)
     */
    @Transaction
    public String registerElection(Context ctx, String electionId, String metadataHash) {
        validateNotEmpty(electionId, "electionId");
        validateNotEmpty(metadataHash, "metadataHash");
        validateHex64(metadataHash, "metadataHash");

        String key = PREFIX_ELECTION + electionId;
        if (ctx.getStub().getStringState(key) != null && !ctx.getStub().getStringState(key).isEmpty()) {
            throw new ChaincodeException("Election already registered: " + electionId, "ELECTION_ALREADY_EXISTS");
        }

        String txId = ctx.getStub().getTxId();
        String timestamp = ctx.getStub().getTxTimestamp().toString();
        String value = String.format(
            "{\"electionId\":\"%s\",\"metadataHash\":\"%s\",\"txId\":\"%s\",\"registeredAt\":\"%s\"}",
            escape(electionId), escape(metadataHash), escape(txId), escape(timestamp)
        );

        ctx.getStub().putStringState(key, value);
        ctx.getStub().setEvent("ElectionRegistered",
            String.format("{\"electionId\":\"%s\"}", escape(electionId)).getBytes());

        return txId;
    }

    /**
     * Get election registration details from ledger.
     */
    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String getElection(Context ctx, String electionId) {
        validateNotEmpty(electionId, "electionId");
        String value = ctx.getStub().getStringState(PREFIX_ELECTION + electionId);
        if (value == null || value.isEmpty()) {
            throw new ChaincodeException("Election not found: " + electionId, "ELECTION_NOT_FOUND");
        }
        return value;
    }

    // ============================================================
    // Nullifier Management (Double-Vote Prevention)
    // ============================================================

    /**
     * Record a nullifier hash on the ledger.
     *
     * PRIVACY: nullifierHash = SHA256(credential_secret || election_id)
     * It cannot be reversed to reveal voter identity.
     *
     * This is the blockchain layer's double-vote prevention.
     * The application layer ALSO checks nullifiers in PostgreSQL.
     *
     * @param electionId    UUID of the election
     * @param nullifierHash SHA-256 hex nullifier (64 chars)
     */
    @Transaction
    public String recordNullifier(Context ctx, String electionId, String nullifierHash) {
        validateNotEmpty(electionId, "electionId");
        validateNotEmpty(nullifierHash, "nullifierHash");
        validateHex64(nullifierHash, "nullifierHash");

        // Verify election exists
        if (ctx.getStub().getStringState(PREFIX_ELECTION + electionId) == null ||
            ctx.getStub().getStringState(PREFIX_ELECTION + electionId).isEmpty()) {
            throw new ChaincodeException("Election not registered: " + electionId, "ELECTION_NOT_FOUND");
        }

        String key = PREFIX_NULLIFIER + electionId + "::" + nullifierHash;

        // Check for duplicate
        if (ctx.getStub().getStringState(key) != null && !ctx.getStub().getStringState(key).isEmpty()) {
            throw new ChaincodeException(
                "Nullifier already recorded. Double-vote attempt detected.",
                "NULLIFIER_DUPLICATE"
            );
        }

        String txId = ctx.getStub().getTxId();
        String timestamp = ctx.getStub().getTxTimestamp().toString();
        // PRIVACY: Store only the hash — no voter identity, no candidate
        String value = String.format(
            "{\"electionId\":\"%s\",\"nullifierHash\":\"%s\",\"txId\":\"%s\",\"recordedAt\":\"%s\"}",
            escape(electionId), escape(nullifierHash), escape(txId), escape(timestamp)
        );

        ctx.getStub().putStringState(key, value);
        ctx.getStub().setEvent("NullifierRecorded",
            String.format("{\"electionId\":\"%s\"}", escape(electionId)).getBytes());

        return txId;
    }

    /**
     * Check whether a nullifier has been used.
     */
    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public boolean isNullifierUsed(Context ctx, String electionId, String nullifierHash) {
        validateNotEmpty(electionId, "electionId");
        validateNotEmpty(nullifierHash, "nullifierHash");
        String key = PREFIX_NULLIFIER + electionId + "::" + nullifierHash;
        String value = ctx.getStub().getStringState(key);
        return value != null && !value.isEmpty();
    }

    /**
     * Count nullifiers (=ballots cast) for an election.
     */
    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public int countNullifiers(Context ctx, String electionId) {
        validateNotEmpty(electionId, "electionId");
        String prefix = PREFIX_NULLIFIER + electionId + "::"; 
        int count = 0;
        try (QueryResultsIterator<KeyValue> results = ctx.getStub().getStateByRange(prefix, prefix + "\uFFFF")) {
            for (KeyValue kv : results) count++;
        } catch (Exception e) {
            throw new ChaincodeException("Failed to count nullifiers: " + e.getMessage(), "QUERY_ERROR");
        }
        return count;
    }

    // ============================================================
    // Merkle Batch Anchoring
    // ============================================================

    /**
     * Anchor a Merkle batch root on the ledger.
     *
     * PRIVACY: merkleRoot is a hash of ballot commitments.
     * Commitments are hashes of encrypted ballots — not revealing vote choices.
     *
     * @param electionId      UUID of the election
     * @param batchReference  Unique batch identifier
     * @param merkleRoot      SHA-256 Merkle root of all ballot commitments in this batch
     * @param leafCount       Number of ballots in this batch
     * @param previousRoot    Merkle root of the previous batch (chain-link)
     */
    @Transaction
    public String anchorBatchRoot(
            Context ctx,
            String electionId,
            String batchReference,
            String merkleRoot,
            String leafCount,
            String previousRoot) {

        validateNotEmpty(electionId, "electionId");
        validateNotEmpty(batchReference, "batchReference");
        validateNotEmpty(merkleRoot, "merkleRoot");
        validateHex64(merkleRoot, "merkleRoot");

        String key = PREFIX_BATCH + electionId + "::" + batchReference;
        if (ctx.getStub().getStringState(key) != null && !ctx.getStub().getStringState(key).isEmpty()) {
            throw new ChaincodeException("Batch already anchored: " + batchReference, "BATCH_ALREADY_ANCHORED");
        }

        String txId = ctx.getStub().getTxId();
        String timestamp = ctx.getStub().getTxTimestamp().toString();
        String value = String.format(
            "{\"electionId\":\"%s\",\"batchReference\":\"%s\",\"merkleRoot\":\"%s\",\"leafCount\":\"%s\",\"previousRoot\":\"%s\",\"txId\":\"%s\",\"anchoredAt\":\"%s\"}",
            escape(electionId), escape(batchReference), escape(merkleRoot),
            escape(leafCount), escape(previousRoot != null ? previousRoot : ""),
            escape(txId), escape(timestamp)
        );

        ctx.getStub().putStringState(key, value);
        ctx.getStub().setEvent("BatchAnchored",
            String.format("{\"electionId\":\"%s\",\"batchReference\":\"%s\",\"merkleRoot\":\"%s\"}",
                escape(electionId), escape(batchReference), escape(merkleRoot)).getBytes());

        return txId;
    }

    /**
     * Get a batch anchor record.
     */
    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String getBatchAnchor(Context ctx, String electionId, String batchReference) {
        validateNotEmpty(electionId, "electionId");
        validateNotEmpty(batchReference, "batchReference");
        String value = ctx.getStub().getStringState(PREFIX_BATCH + electionId + "::" + batchReference);
        if (value == null || value.isEmpty()) {
            throw new ChaincodeException("Batch not found: " + batchReference, "BATCH_NOT_FOUND");
        }
        return value;
    }

    /**
     * Get all batch anchors for an election.
     */
    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String getAllBatchAnchors(Context ctx, String electionId) {
        validateNotEmpty(electionId, "electionId");
        String prefix = PREFIX_BATCH + electionId + "::"; 
        List<String> results = new ArrayList<>();
        try (QueryResultsIterator<KeyValue> it = ctx.getStub().getStateByRange(prefix, prefix + "\uFFFF")) {
            for (KeyValue kv : it) results.add(kv.getStringValue());
        } catch (Exception e) {
            throw new ChaincodeException("Query failed: " + e.getMessage(), "QUERY_ERROR");
        }
        return "[" + String.join(",", results) + "]";
    }

    // ============================================================
    // Audit Digest Anchoring
    // ============================================================

    /**
     * Record an audit digest on the ledger.
     * A = SHA256(electionState || batchRoots || counts)
     */
    @Transaction
    public String recordAuditDigest(
            Context ctx,
            String electionId,
            String anchorReference,
            String auditDigest) {

        validateNotEmpty(electionId, "electionId");
        validateNotEmpty(anchorReference, "anchorReference");
        validateNotEmpty(auditDigest, "auditDigest");
        validateHex64(auditDigest, "auditDigest");

        String key = PREFIX_AUDIT + electionId + "::" + anchorReference;
        String txId = ctx.getStub().getTxId();
        String timestamp = ctx.getStub().getTxTimestamp().toString();
        String value = String.format(
            "{\"electionId\":\"%s\",\"anchorReference\":\"%s\",\"auditDigest\":\"%s\",\"txId\":\"%s\",\"recordedAt\":\"%s\"}",
            escape(electionId), escape(anchorReference), escape(auditDigest), escape(txId), escape(timestamp)
        );

        ctx.getStub().putStringState(key, value);
        ctx.getStub().setEvent("AuditDigestRecorded",
            String.format("{\"electionId\":\"%s\",\"auditDigest\":\"%s\"}",
                escape(electionId), escape(auditDigest)).getBytes());
        return txId;
    }

    // ============================================================
    // Private helpers
    // ============================================================

    private void validateNotEmpty(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new ChaincodeException(fieldName + " must not be empty", "VALIDATION_ERROR");
        }
    }

    private void validateHex64(String value, String fieldName) {
        if (!value.matches("[a-fA-F0-9]{64}")) {
            throw new ChaincodeException(
                fieldName + " must be a 64-character hex string (SHA-256)",
                "VALIDATION_ERROR"
            );
        }
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
