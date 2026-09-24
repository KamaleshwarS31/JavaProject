package com.evoting.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "security_events", schema = "public",
    indexes = {
        @Index(name = "idx_security_event_type", columnList = "event_type"),
        @Index(name = "idx_security_event_severity", columnList = "severity"),
        @Index(name = "idx_security_event_timestamp", columnList = "event_timestamp")
    }
)
@Getter @Setter @NoArgsConstructor
public class SecurityEvent {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "severity", nullable = false, length = 20)
    private String severity;

    @Column(name = "client_ip", length = 45)
    private String clientIp;

    @Column(name = "correlation_id", length = 64)
    private String correlationId;

    @Column(name = "metadata_json", columnDefinition = "TEXT")
    private String metadataJson;

    @Column(name = "event_timestamp", nullable = false)
    private Instant eventTimestamp;

    @PrePersist
    protected void onPersist() {
        if (eventTimestamp == null) eventTimestamp = Instant.now();
    }
}
