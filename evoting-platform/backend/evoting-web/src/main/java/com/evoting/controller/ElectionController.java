package com.evoting.controller;

import com.evoting.core.domain.enums.ElectionState;
import com.evoting.dto.request.CandidateRequest;
import com.evoting.dto.request.ElectionCreateRequest;
import com.evoting.dto.response.CandidateResponse;
import com.evoting.dto.response.ElectionResponse;
import com.evoting.service.ElectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Election management REST controller.
 * No business logic — all delegated to ElectionService.
 */
@RestController
@RequestMapping("/api/v1/elections")
@Tag(name = "Elections", description = "Election CRUD and state management")
public class ElectionController {

    private final ElectionService electionService;

    public ElectionController(ElectionService electionService) {
        this.electionService = electionService;
    }

    @GetMapping
    @Operation(summary = "List all elections (public)")
    public ResponseEntity<List<ElectionResponse>> getAllElections() {
        return ResponseEntity.ok(electionService.getAllElections());
    }

    @GetMapping("/active")
    @Operation(summary = "List currently active elections (public)")
    public ResponseEntity<List<ElectionResponse>> getActiveElections() {
        return ResponseEntity.ok(electionService.getActiveElections());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get election details including candidates (public)")
    public ResponseEntity<ElectionResponse> getElection(@PathVariable UUID id) {
        return ResponseEntity.ok(electionService.getElection(id));
    }

    @PostMapping
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Create a new election (Election Admin only)")
    public ResponseEntity<ElectionResponse> createElection(@Valid @RequestBody ElectionCreateRequest request) throws Exception {
        return ResponseEntity.status(HttpStatus.CREATED).body(electionService.createElection(request));
    }

    @PatchMapping("/{id}/state")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Transition election state (Election Admin only)")
    public ResponseEntity<ElectionResponse> transitionState(
            @PathVariable UUID id,
            @RequestParam ElectionState targetState) {
        return ResponseEntity.ok(electionService.transitionState(id, targetState));
    }

    @PostMapping("/{id}/candidates")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Add candidate to election (Election Admin only, DRAFT state)")
    public ResponseEntity<CandidateResponse> addCandidate(
            @PathVariable UUID id,
            @Valid @RequestBody CandidateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(electionService.addCandidate(id, request));
    }

    @GetMapping("/{id}/candidates")
    @Operation(summary = "List candidates for an election (public)")
    public ResponseEntity<List<CandidateResponse>> getCandidates(@PathVariable UUID id) {
        return ResponseEntity.ok(electionService.getCandidates(id));
    }
}
