package com.evoting.entity;

import com.evoting.core.domain.enums.BatchStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "merkle_batches", schema = "public",
    uniqueConstraints = { @UniqueConstraint(name = "uq_batch_reference", columnNames = "batch_reference") })
@Getter @Setter @NoArgsConstructor
public class MerkleBatch {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "election_id", nullable = false, foreignKey = @ForeignKey(name = "fk_batch_election"))
    private Election election;

    @Column(name = "batch_reference", nullable = false, unique = true, length = 64)
    private String batchReference;

    @Column(name = "leaf_count", nullable = false)
    private int leafCount = 0;

    @Column(name = "merkle_root", length = 64)
    private String merkleRoot;

    @Column(name = "previous_batch_root", length = 64)
    private String previousBatchRoot;

    @Column(name = "blockchain_tx_id", length = 100)
    private String blockchainTxId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private BatchStatus status = BatchStatus.OPEN;

    @OneToMany(mappedBy = "batch", fetch = FetchType.LAZY)
    private List<BallotMetadata> ballots = new ArrayList<>();

    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "anchored_at")
    private Instant anchoredAt;
}
