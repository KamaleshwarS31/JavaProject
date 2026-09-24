package com.evoting.scheduler;

import com.evoting.core.crypto.MerkleTreeEngine;
import com.evoting.core.domain.enums.BatchStatus;
import com.evoting.entity.BallotMetadata;
import com.evoting.entity.MerkleBatch;
import com.evoting.repository.BallotMetadataRepository;
import com.evoting.repository.MerkleBatchRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Scheduled service that:
 * 1. Computes Merkle roots for PENDING_ANCHOR batches
 * 2. Anchors Merkle roots to Hyperledger Fabric (or logs if Fabric is not connected)
 *
 * Runs every 5 minutes (configurable via evoting.merkle.anchor-interval-seconds).
 */
@Component
public class MerkleBatchScheduler {

    private static final Logger log = LoggerFactory.getLogger(MerkleBatchScheduler.class);

    private final MerkleBatchRepository merkleBatchRepository;
    private final BallotMetadataRepository ballotMetadataRepository;
    private final MerkleTreeEngine merkleTreeEngine;

    @Value("${evoting.merkle.batch-size:100}")
    private int batchSize;

    public MerkleBatchScheduler(
            MerkleBatchRepository merkleBatchRepository,
            BallotMetadataRepository ballotMetadataRepository,
            MerkleTreeEngine merkleTreeEngine) {
        this.merkleBatchRepository = merkleBatchRepository;
        this.ballotMetadataRepository = ballotMetadataRepository;
        this.merkleTreeEngine = merkleTreeEngine;
    }

    /**
     * Every 5 minutes: compute Merkle roots for PENDING_ANCHOR batches.
     */
    @Scheduled(fixedDelayString = "${evoting.merkle.anchor-interval-seconds:300}000")
    @Transactional
    public void processPendingBatches() {
        log.debug("[Merkle Scheduler] Checking for pending batches...");

        List<MerkleBatch> pendingBatches = merkleBatchRepository.findAll().stream()
            .filter(b -> b.getStatus() == BatchStatus.PENDING_ANCHOR || 
                        (b.getStatus() == BatchStatus.OPEN && b.getLeafCount() > 0))
            .collect(Collectors.toList());

        if (pendingBatches.isEmpty()) {
            log.debug("[Merkle Scheduler] No pending batches.");
            return;
        }

        for (MerkleBatch batch : pendingBatches) {
            try {
                computeMerkleRoot(batch);
            } catch (Exception e) {
                log.error("[Merkle Scheduler] Failed to process batch {}: {}",
                    batch.getBatchReference(), e.getMessage());
            }
        }
    }

    private void computeMerkleRoot(MerkleBatch batch) {
        List<BallotMetadata> ballots = ballotMetadataRepository.findByBatchId(batch.getId())
            .stream()
            .sorted(Comparator.comparingInt(b -> b.getLeafIndex() != null ? b.getLeafIndex() : 0))
            .collect(Collectors.toList());

        if (ballots.isEmpty()) {
            log.warn("[Merkle Scheduler] Batch {} has no ballots, skipping.", batch.getBatchReference());
            return;
        }

        List<String> commitments = ballots.stream()
            .map(BallotMetadata::getCommitment)
            .collect(Collectors.toList());

        String merkleRoot = merkleTreeEngine.computeRoot(commitments);
        batch.setMerkleRoot(merkleRoot);
        batch.setStatus(BatchStatus.PENDING_ANCHOR);

        // TODO: Anchor to Hyperledger Fabric via FabricGatewayService
        // String txId = fabricGatewayService.anchorBatchRoot(batch.getBatchReference(), merkleRoot);
        // batch.setBlockchainTxId(txId);
        // batch.setStatus(BatchStatus.ANCHORED);
        // batch.setAnchoredAt(Instant.now());

        // For now, log the Merkle root (Fabric integration in next phase)
        log.info("[Merkle Scheduler] Computed Merkle root for batch {}: {}",
            batch.getBatchReference(), merkleRoot);

        merkleBatchRepository.save(batch);
    }
}
