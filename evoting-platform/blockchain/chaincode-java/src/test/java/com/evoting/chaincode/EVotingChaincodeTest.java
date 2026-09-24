package com.evoting.chaincode;

import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EVotingChaincode Unit Tests")
class EVotingChaincodeTest {

    @Mock private Context ctx;
    @Mock private ChaincodeStub stub;

    private EVotingChaincode chaincode;

    private static final String ELECTION_ID = "550e8400-e29b-41d4-a716-446655440000";
    private static final String NULLIFIER_HASH = "a" .repeat(64); // 64 hex chars
    private static final String MERKLE_ROOT = "b".repeat(64);
    private static final String AUDIT_DIGEST = "c".repeat(64);
    private static final String TX_ID = "tx-abc-123";

    @BeforeEach
    void setUp() {
        chaincode = new EVotingChaincode();
        when(ctx.getStub()).thenReturn(stub);
        when(stub.getTxId()).thenReturn(TX_ID);
        when(stub.getTxTimestamp()).thenReturn(Instant.now());
    }

    @Test
    @DisplayName("registerElection: should store election record and return txId")
    void registerElection_success() {
        String metadataHash = "d".repeat(64);
        when(stub.getStringState("ELECTION::" + ELECTION_ID)).thenReturn("");

        String result = chaincode.registerElection(ctx, ELECTION_ID, metadataHash);

        assertThat(result).isEqualTo(TX_ID);
        verify(stub).putStringState(eq("ELECTION::" + ELECTION_ID), anyString());
        verify(stub).setEvent(eq("ElectionRegistered"), any());
    }

    @Test
    @DisplayName("registerElection: should reject duplicate election")
    void registerElection_duplicate() {
        when(stub.getStringState("ELECTION::" + ELECTION_ID))
            .thenReturn("{\"electionId\":\"..\"}");
        assertThatThrownBy(() -> chaincode.registerElection(ctx, ELECTION_ID, "d".repeat(64)))
            .isInstanceOf(ChaincodeException.class)
            .hasMessageContaining("already registered");
    }

    @Test
    @DisplayName("recordNullifier: should store nullifier and return txId")
    void recordNullifier_success() {
        when(stub.getStringState("ELECTION::" + ELECTION_ID)).thenReturn("{\"electionId\":\"test\"}");
        when(stub.getStringState("NULLIFIER::" + ELECTION_ID + "::" + NULLIFIER_HASH)).thenReturn("");

        String result = chaincode.recordNullifier(ctx, ELECTION_ID, NULLIFIER_HASH);

        assertThat(result).isEqualTo(TX_ID);
        verify(stub).putStringState(eq("NULLIFIER::" + ELECTION_ID + "::" + NULLIFIER_HASH), anyString());
    }

    @Test
    @DisplayName("recordNullifier: should reject duplicate nullifier (double-vote)")
    void recordNullifier_duplicate() {
        when(stub.getStringState("ELECTION::" + ELECTION_ID)).thenReturn("{\"electionId\":\"test\"}");
        when(stub.getStringState("NULLIFIER::" + ELECTION_ID + "::" + NULLIFIER_HASH))
            .thenReturn("{\"nullifierHash\":\"..\"}");

        assertThatThrownBy(() -> chaincode.recordNullifier(ctx, ELECTION_ID, NULLIFIER_HASH))
            .isInstanceOf(ChaincodeException.class)
            .hasMessageContaining("Double-vote");
    }

    @Test
    @DisplayName("anchorBatchRoot: should store Merkle root and return txId")
    void anchorBatchRoot_success() {
        String batchRef = "batch001";
        when(stub.getStringState("BATCH::" + ELECTION_ID + "::" + batchRef)).thenReturn("");

        String result = chaincode.anchorBatchRoot(ctx, ELECTION_ID, batchRef, MERKLE_ROOT, "100", null);

        assertThat(result).isEqualTo(TX_ID);
        verify(stub).putStringState(eq("BATCH::" + ELECTION_ID + "::" + batchRef), anyString());
        verify(stub).setEvent(eq("BatchAnchored"), any());
    }

    @Test
    @DisplayName("isNullifierUsed: should return false when not used")
    void isNullifierUsed_notUsed() {
        when(stub.getStringState("NULLIFIER::" + ELECTION_ID + "::" + NULLIFIER_HASH)).thenReturn("");
        assertThat(chaincode.isNullifierUsed(ctx, ELECTION_ID, NULLIFIER_HASH)).isFalse();
    }

    @Test
    @DisplayName("isNullifierUsed: should return true when used")
    void isNullifierUsed_used() {
        when(stub.getStringState("NULLIFIER::" + ELECTION_ID + "::" + NULLIFIER_HASH))
            .thenReturn("{\"nullifierHash\":\"..\"}");
        assertThat(chaincode.isNullifierUsed(ctx, ELECTION_ID, NULLIFIER_HASH)).isTrue();
    }

    @Test
    @DisplayName("recordNullifier: should reject invalid hex (non-64 char)")
    void recordNullifier_invalidHex() {
        when(stub.getStringState("ELECTION::" + ELECTION_ID)).thenReturn("{\"electionId\":\"test\"}");
        assertThatThrownBy(() -> chaincode.recordNullifier(ctx, ELECTION_ID, "notahex"))
            .isInstanceOf(ChaincodeException.class)
            .hasMessageContaining("64-character hex");
    }
}
