package com.evoting.entity;

import com.evoting.core.domain.enums.ElectionState;
import com.evoting.core.exception.ElectionStateException;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "elections", schema = "public",
    uniqueConstraints = { @UniqueConstraint(name = "uq_election_code", columnNames = "election_code") })
@Getter @Setter @NoArgsConstructor
public class Election {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @NotBlank @Size(max = 50)
    @Column(name = "election_code", nullable = false, unique = true, length = 50)
    private String electionCode;

    @NotBlank @Size(max = 255)
    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false, length = 20)
    private ElectionState state = ElectionState.DRAFT;

    @Column(name = "election_type", nullable = false, length = 30)
    private String electionType = "SINGLE_CHOICE";

    @Column(name = "public_key_pem", columnDefinition = "TEXT")
    private String publicKeyPem;

    @Column(name = "start_time")
    private Instant startTime;

    @Column(name = "end_time")
    private Instant endTime;

    @Column(name = "batch_size_limit", nullable = false)
    private int batchSizeLimit = 100;

    @Column(name = "blockchain_election_id", length = 100)
    private String blockchainElectionId;

    @OneToMany(mappedBy = "election", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Candidate> candidates = new ArrayList<>();

    @Version @Column(name = "version")
    private Long version;

    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public void transitionTo(ElectionState targetState) {
        if (!this.state.canTransitionTo(targetState)) {
            throw new ElectionStateException(this.state, targetState);
        }
        this.state = targetState;
    }

    public boolean isVotingOpen() {
        return ElectionState.ACTIVE == this.state
            && (startTime == null || Instant.now().isAfter(startTime))
            && (endTime == null || Instant.now().isBefore(endTime));
    }
}
