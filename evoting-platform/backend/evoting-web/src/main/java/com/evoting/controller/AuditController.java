package com.evoting.controller;

import com.evoting.dto.response.AuditResponse;
import com.evoting.service.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Public audit REST controller.
 * All endpoints publicly readable for independent verification.
 */
@RestController
@RequestMapping("/api/v1/audit")
@Tag(name = "Audit", description = "Election audit, Merkle batch, and blockchain anchor verification")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping("/elections/{electionId}")
    @Operation(summary = "Get full audit report for an election (public)")
    public ResponseEntity<AuditResponse> getAuditReport(@PathVariable UUID electionId) {
        return ResponseEntity.ok(auditService.getAuditReport(electionId));
    }

    @GetMapping("/elections")
    @Operation(summary = "Get audit summary for all elections (public)")
    public ResponseEntity<List<AuditResponse>> getAllAuditReports() {
        return ResponseEntity.ok(auditService.getAllAuditReports());
    }
}
