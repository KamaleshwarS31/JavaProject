package com.evoting.entity;

import com.evoting.core.domain.enums.BallotState;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ballot_metadata", schema = "public",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_ballot_reference", columnNames = "ballot_reference"),
        @UniqueConstraint(name = "uq_ballot_commitment", columnNames = "commitment")
    }
)
@Getter @Setter @NoArgsConstructor
public class BallotMetadata {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "election_id", nullable = false, foreignKey = @ForeignKey(name = "fk_ballot_election"))
    private Election election;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", foreignKey = @ForeignKey(name = "fk_ballot_batch"))
    private MerkleBatch batch;

    @Column(name = "ballot_reference", nullable = false, unique = true, length = 64)
    private String ballotReference;

    @Column(name = "commitment", nullable = false, unique = true, length = 64)
    private String commitment;

    @Column(name = "encrypted_payload", columnDefinition = "TEXT")
    private String encryptedPayload;

    @Column(name = "leaf_index")
    private Integer leafIndex;

    @Enumerated(EnumType.STRING)
    @Column(name = "lifecycle_state", nullable = false, length = 30)
    private BallotState lifecycleState = BallotState.CREATED;

    @Column(name = "idempotency_key", length = 64)
    private String idempotencyKey;

    @CreationTimestamp @Column(name = "accepted_at", nullable = false, updatable = false)
    private Instant acceptedAt;

    @Column(name = "anchored_at")
    private Instant anchoredAt;
}
