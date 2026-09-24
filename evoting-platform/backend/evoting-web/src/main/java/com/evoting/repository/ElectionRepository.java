package com.evoting.repository;

import com.evoting.entity.Election;
import com.evoting.core.domain.enums.ElectionState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ElectionRepository extends JpaRepository<Election, UUID> {

    Optional<Election> findByElectionCode(String electionCode);

    List<Election> findByState(ElectionState state);

    List<Election> findByStateIn(List<ElectionState> states);

    @Query("SELECT e FROM Election e WHERE e.state = 'ACTIVE' ORDER BY e.startTime ASC")
    List<Election> findActiveElections();

    boolean existsByElectionCode(String electionCode);
}
