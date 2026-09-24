<%@ page contentType="text/html;charset=UTF-8" language="java" isELIgnored="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Audit Portal | E-Voting Platform</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet" integrity="sha384-T3c6CoIi6uLrA9TneNEoa7RxnatzjcDSCmG1MXxSR1GAsXEV/Dwwykc2MPK8M2HN" crossorigin="anonymous">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css" rel="stylesheet" crossorigin="anonymous">
</head>
<body class="bg-light">
<a href="#main-content" class="visually-hidden-focusable btn btn-primary position-absolute">Skip to main content</a>

<nav class="navbar navbar-dark bg-dark">
    <div class="container">
        <a class="navbar-brand fw-bold" href="/">
            <i class="bi bi-shield-check me-2"></i>Election Audit Portal
        </a>
        <a class="nav-link text-white" href="/elections">Back to Elections</a>
    </div>
</nav>

<main id="main-content" class="container py-4" tabindex="-1">
    <div class="row mb-4">
        <div class="col">
            <h1 class="display-6 fw-bold">
                <i class="bi bi-clipboard-data-fill text-dark me-2" aria-hidden="true"></i>
                Election Audit Portal
            </h1>
            <p class="lead text-muted">
                Independently verify election integrity. All audit data is publicly available.
            </p>
            <div class="alert alert-info">
                <i class="bi bi-info-circle me-2"></i>
                <strong>How it works:</strong> Each batch of ballots has a Merkle root anchored to Hyperledger Fabric.
                You can verify any ballot's commitment is included in the published Merkle tree
                without revealing who voted or how.
            </div>
        </div>
    </div>

    <!-- Individual ballot verification -->
    <div class="card shadow-sm mb-4">
        <div class="card-header bg-primary text-white">
            <h2 class="h5 mb-0"><i class="bi bi-search me-2" aria-hidden="true"></i>Verify Your Ballot</h2>
        </div>
        <div class="card-body">
            <div class="row g-3">
                <div class="col-md-8">
                    <label for="ballotRefInput" class="form-label fw-semibold">Ballot Reference</label>
                    <input type="text" class="form-control" id="ballotRefInput"
                           placeholder="Enter your ballot reference from your receipt"
                           aria-label="Ballot reference" aria-describedby="ballotRefHelp"/>
                    <div id="ballotRefHelp" class="form-text">Found on your voting receipt after casting your ballot.</div>
                </div>
                <div class="col-md-4 d-flex align-items-end">
                    <button class="btn btn-primary w-100" onclick="verifyBallot()">
                        <i class="bi bi-search me-2"></i>Verify Ballot
                    </button>
                </div>
            </div>
            <div id="verifyResult" class="mt-3" role="status" aria-live="polite"></div>
        </div>
    </div>

    <!-- Elections audit list -->
    <div class="card shadow-sm">
        <div class="card-header bg-dark text-white">
            <h2 class="h5 mb-0"><i class="bi bi-diagram-3 me-2" aria-hidden="true"></i>Election Merkle Batches</h2>
        </div>
        <div class="card-body">
            <div id="auditContainer">
                <div class="text-center py-4">
                    <div class="spinner-border" role="status" aria-label="Loading audit data">
                        <span class="visually-hidden">Loading...</span>
                    </div>
                </div>
            </div>
        </div>
    </div>
</main>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js" integrity="sha384-C6RzsynM9kWDrMNeT87bh95OGNyZPhcTNXj1NW7RuBCsyN/o0jlpcV8Qyq46cDfL" crossorigin="anonymous"></script>
<script>
document.addEventListener('DOMContentLoaded', () => {
    fetch('/api/v1/audit/elections')
        .then(r => r.json())
        .then(reports => renderAuditData(reports))
        .catch(() => {
            document.getElementById('auditContainer').innerHTML =
                '<div class="alert alert-danger">Failed to load audit data.</div>';
        });
});

function renderAuditData(reports) {
    const container = document.getElementById('auditContainer');
    if (!reports || reports.length === 0) {
        container.innerHTML = '<p class="text-muted">No elections to audit yet.</p>';
        return;
    }

    const html = reports.map(r => `
        <div class="mb-4">
            <h3 class="h6 fw-bold">${escapeHtml(r.title)}
                <span class="badge bg-secondary ms-2">${r.state}</span>
                ${r.auditMismatchDetected ? '<span class="badge bg-danger ms-1">MISMATCH DETECTED</span>' : ''}
            </h3>
            <div class="row text-center mb-2">
                <div class="col"><strong>${r.totalBallotsAccepted}</strong><div class="text-muted small">Ballots</div></div>
                <div class="col"><strong>${r.totalBatchesAnchored}</strong><div class="text-muted small">Batches</div></div>
            </div>
            ${r.batches && r.batches.length > 0 ? `
            <div class="table-responsive">
                <table class="table table-sm table-bordered" aria-label="Merkle batches for ${escapeHtml(r.title)}">
                    <thead class="table-dark">
                        <tr>
                            <th scope="col">Batch Reference</th>
                            <th scope="col">Ballots</th>
                            <th scope="col">Merkle Root</th>
                            <th scope="col">Status</th>
                            <th scope="col">Blockchain TX</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${r.batches.map(b => `
                        <tr>
                            <td><code class="small">${escapeHtml(b.batchReference ? b.batchReference.substring(0,12) + '...' : 'N/A')}</code></td>
                            <td>${b.leafCount}</td>
                            <td><code class="small">${b.merkleRoot ? b.merkleRoot.substring(0,12) + '...' : 'Pending'}</code></td>
                            <td><span class="badge bg-${b.status === 'ANCHORED' ? 'success' : 'warning'}">${b.status}</span></td>
                            <td><code class="small">${b.blockchainTxId ? b.blockchainTxId.substring(0,12) + '...' : 'Pending'}</code></td>
                        </tr>
                        `).join('')}
                    </tbody>
                </table>
            </div>` : '<p class="text-muted small">No batches yet.</p>'}
            <hr>
        </div>
    `).join('');
    container.innerHTML = html;
}

function verifyBallot() {
    const ref = document.getElementById('ballotRefInput').value.trim();
    if (!ref) return;
    const div = document.getElementById('verifyResult');
    div.innerHTML = '<div class="spinner-border spinner-border-sm" role="status"><span class="visually-hidden">Verifying...</span></div>';

    Promise.all([
        fetch(`/api/v1/verify/ballot/${encodeURIComponent(ref)}`).then(r => r.json()),
        fetch(`/api/v1/verify/merkle-proof/${encodeURIComponent(ref)}`).then(r => r.json())
    ]).then(([status, proof]) => {
        const verified = proof.verified;
        div.innerHTML = `
            <div class="card border-${verified ? 'success' : 'warning'}">
                <div class="card-body">
                    <h4 class="card-title h6">
                        <i class="bi bi-${verified ? 'check-circle-fill text-success' : 'clock text-warning'} me-2"></i>
                        ${verified ? 'Ballot Verified — Included in Merkle Tree' : 'Ballot Found — Merkle Proof Not Yet Available'}
                    </h4>
                    <dl class="row mb-0 small">
                        <dt class="col-4">Status</dt><dd class="col-8">${status.lifecycleState}</dd>
                        <dt class="col-4">Commitment</dt><dd class="col-8"><code>${status.commitment ? status.commitment.substring(0,20) + '...' : 'N/A'}</code></dd>
                        <dt class="col-4">Merkle Root</dt><dd class="col-8"><code>${proof.merkleRoot || 'Pending...'}</code></dd>
                        <dt class="col-4">Blockchain TX</dt><dd class="col-8"><code>${proof.blockchainTxId || 'Pending...'}</code></dd>
                    </dl>
                    <p class="text-muted small mt-2 mb-0">${escapeHtml(proof.verificationMessage || '')}</p>
                </div>
            </div>`;
    }).catch(() => {
        div.innerHTML = '<div class="alert alert-danger">Ballot reference not found or verification failed.</div>';
    });
}

function escapeHtml(s) {
    if (!s) return '';
    return s.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
}
</script>
</body>
</html>
