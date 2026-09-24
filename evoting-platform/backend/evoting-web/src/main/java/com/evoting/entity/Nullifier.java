package com.evoting.entity;

import com.evoting.core.domain.enums.NullifierStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "nullifiers", schema = "public",
    uniqueConstraints = { @UniqueConstraint(name = "uq_election_nullifier", columnNames = {"election_id", "nullifier_hash"}) },
    indexes = {
        @Index(name = "idx_nullifier_hash", columnList = "nullifier_hash"),
        @Index(name = "idx_nullifier_election", columnList = "election_id")
    }
)
@Getter @Setter @NoArgsConstructor
public class Nullifier {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "election_id", nullable = false, foreignKey = @ForeignKey(name = "fk_nullifier_election"))
    private Election election;

    @Column(name = "nullifier_hash", nullable = false, length = 64)
    private String nullifierHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private NullifierStatus status = NullifierStatus.ACTIVE;

    @Column(name = "blockchain_tx_id", length = 100)
    private String blockchainTxId;

    @CreationTimestamp @Column(name = "first_seen_at", nullable = false, updatable = false)
    private Instant firstSeenAt;

    public Nullifier(Election election, String nullifierHash) {
        this.election = election;
        this.nullifierHash = nullifierHash;
    }
}
