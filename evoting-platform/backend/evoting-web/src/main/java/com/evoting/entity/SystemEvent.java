package com.evoting.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "system_events", schema = "public",
    indexes = {
        @Index(name = "idx_event_type", columnList = "event_type"),
        @Index(name = "idx_event_election", columnList = "election_id"),
        @Index(name = "idx_event_timestamp", columnList = "event_timestamp")
    }
)
@Getter @Setter @NoArgsConstructor
public class SystemEvent {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "actor_reference", length = 64)
    private String actorReference;

    @Column(name = "election_id")
    private UUID electionId;

    @Column(name = "event_hash", length = 64)
    private String eventHash;

    @Column(name = "metadata_json", columnDefinition = "TEXT")
    private String metadataJson;

    @Column(name = "correlation_id", length = 64)
    private String correlationId;

    @Column(name = "event_timestamp", nullable = false)
    private Instant eventTimestamp;

    @PrePersist
    protected void onPersist() {
        if (eventTimestamp == null) eventTimestamp = Instant.now();
    }
}
