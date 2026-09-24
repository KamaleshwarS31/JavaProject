package com.evoting.core.domain.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("ElectionState State Machine Tests")
class ElectionStateTest {

    @Test
    @DisplayName("DRAFT -> READY should be valid")
    void testDraftToReady() {
        assertThat(ElectionState.DRAFT.canTransitionTo(ElectionState.READY)).isTrue();
    }

    @Test
    @DisplayName("DRAFT -> FINALIZED should be INVALID")
    void testDraftToFinalizedInvalid() {
        assertThat(ElectionState.DRAFT.canTransitionTo(ElectionState.FINALIZED)).isFalse();
    }

    @Test
    @DisplayName("ACTIVE -> CLOSED should be valid")
    void testActiveToClosedValid() {
        assertThat(ElectionState.ACTIVE.canTransitionTo(ElectionState.CLOSED)).isTrue();
    }

    @Test
    @DisplayName("ACTIVE -> DRAFT should be INVALID")
    void testActiveToDraftInvalid() {
        assertThat(ElectionState.ACTIVE.canTransitionTo(ElectionState.DRAFT)).isFalse();
    }

    @Test
    @DisplayName("CLOSED -> ACTIVE should be INVALID (election cannot reopen)")
    void testClosedToActiveInvalid() {
        assertThat(ElectionState.CLOSED.canTransitionTo(ElectionState.ACTIVE)).isFalse();
    }

    @Test
    @DisplayName("FINALIZED should be terminal — no valid next states")
    void testFinalizedIsTerminal() {
        assertThat(ElectionState.FINALIZED.isTerminal()).isTrue();
        assertThat(ElectionState.FINALIZED.validNextStates()).isEmpty();
    }

    @Test
    @DisplayName("Only ACTIVE state should allow voting")
    void testVotingAllowedOnlyWhenActive() {
        assertThat(ElectionState.ACTIVE.isVotingAllowed()).isTrue();
        assertThat(ElectionState.DRAFT.isVotingAllowed()).isFalse();
        assertThat(ElectionState.CLOSED.isVotingAllowed()).isFalse();
        assertThat(ElectionState.FINALIZED.isVotingAllowed()).isFalse();
    }
}
