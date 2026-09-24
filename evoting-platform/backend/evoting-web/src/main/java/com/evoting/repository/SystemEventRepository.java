package com.evoting.repository;

import com.evoting.entity.SystemEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SystemEventRepository extends JpaRepository<SystemEvent, UUID> {
    List<SystemEvent> findByElectionIdOrderByEventTimestampDesc(UUID electionId);
    List<SystemEvent> findByEventTypeOrderByEventTimestampDesc(String eventType);
}
