<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Election Details | E-Voting Platform</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet" integrity="sha384-T3c6CoIi6uLrA9TneNEoa7RxnatzjcDSCmG1MXxSR1GAsXEV/Dwwykc2MPK8M2HN" crossorigin="anonymous">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css" rel="stylesheet" crossorigin="anonymous">
</head>
<body class="bg-light">
<a href="#main-content" class="visually-hidden-focusable btn btn-primary position-absolute">Skip to main content</a>

<nav class="navbar navbar-dark bg-primary">
    <div class="container">
        <a class="navbar-brand fw-bold" href="/">
            <i class="bi bi-shield-lock-fill me-2"></i>E-Voting Platform
        </a>
        <div class="navbar-nav flex-row">
            <a class="nav-link text-white me-3" href="/elections">All Elections</a>
            <a class="nav-link text-white me-3" href="/auth/login">Login</a>
        </div>
    </div>
</nav>

<main id="main-content" class="container py-4" tabindex="-1">
    <!-- Election header loaded dynamically -->
    <div id="electionHeader" class="mb-4">
        <div class="placeholder-glow">
            <h1 class="placeholder col-6"></h1>
            <p class="placeholder col-8"></p>
        </div>
    </div>

    <div class="row">
        <!-- Election Info -->
        <div class="col-md-8">
            <div class="card shadow-sm mb-4">
                <div class="card-header">
                    <h2 class="h5 mb-0"><i class="bi bi-people-fill me-2" aria-hidden="true"></i>Candidates</h2>
                </div>
                <div class="card-body">
                    <div id="candidatesContainer">
                        <div class="text-center py-3">
                            <div class="spinner-border spinner-border-sm" role="status">
                                <span class="visually-hidden">Loading candidates...</span>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <!-- Election Stats & Actions -->
        <div class="col-md-4">
            <div class="card shadow-sm mb-4">
                <div class="card-header">
                    <h2 class="h5 mb-0"><i class="bi bi-bar-chart-fill me-2" aria-hidden="true"></i>Statistics</h2>
                </div>
                <div class="card-body" id="statsContainer">
                    <div class="placeholder-glow">
                        <p class="placeholder col-10"></p>
                        <p class="placeholder col-8"></p>
                    </div>
                </div>
            </div>

            <div id="voteButtonContainer"></div>

            <div class="card shadow-sm">
                <div class="card-header">
                    <h2 class="h5 mb-0"><i class="bi bi-shield-check me-2" aria-hidden="true"></i>Verify Your Vote</h2>
                </div>
                <div class="card-body">
                    <p class="small text-muted">Have your ballot reference? Check your vote status and get a Merkle proof.</p>
                    <div class="input-group">
                        <input type="text" class="form-control form-control-sm" id="verifyRefInput"
                               placeholder="Ballot reference..." aria-label="Ballot reference for verification"/>
                        <button class="btn btn-outline-secondary btn-sm" onclick="verifyBallot()">
                            Verify
                        </button>
                    </div>
                    <div id="verifyResult" class="mt-2" aria-live="polite"></div>
                </div>
            </div>
        </div>
    </div>
</main>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js" integrity="sha384-C6RzsynM9kWDrMNeT87bh95OGNyZPhcTNXj1NW7RuBCsyN/o0jlpcV8Qyq46cDfL" crossorigin="anonymous"></script>
<script>
const electionId = window.location.pathname.split('/')[2];
const stateColors = {
    ACTIVE: 'success', DRAFT: 'secondary', READY: 'info',
    CLOSED: 'warning', TALLYING: 'primary', FINALIZED: 'dark'
};

document.addEventListener('DOMContentLoaded', () => {
    fetch(`/api/v1/elections/${electionId}`)
        .then(r => r.ok ? r.json() : Promise.reject(r))
        .then(e => {
            renderHeader(e);
            renderCandidates(e.candidates || []);
            renderStats(e);
            renderVoteButton(e);
        })
        .catch(() => {
            document.getElementById('electionHeader').innerHTML =
                '<div class="alert alert-danger">Failed to load election details.</div>';
        });
});

function renderHeader(e) {
    document.getElementById('electionHeader').innerHTML = `
        <nav aria-label="breadcrumb">
            <ol class="breadcrumb">
                <li class="breadcrumb-item"><a href="/elections">Elections</a></li>
                <li class="breadcrumb-item active">${escapeHtml(e.title)}</li>
            </ol>
        </nav>
        <h1 class="display-6 fw-bold">${escapeHtml(e.title)}
            <span class="badge bg-${stateColors[e.state] || 'secondary'} fs-6 align-middle">${e.state}</span>
        </h1>
        <p class="text-muted">${escapeHtml(e.description || '')}</p>
    `;
    document.title = escapeHtml(e.title) + ' | E-Voting Platform';
}

function renderCandidates(candidates) {
    const container = document.getElementById('candidatesContainer');
    if (!candidates.length) {
        container.innerHTML = '<p class="text-muted">No candidates registered.</p>';
        return;
    }
    container.innerHTML = `<div class="list-group list-group-flush">
        ${candidates.map(c => `
            <div class="list-group-item">
                <div class="d-flex align-items-center">
                    <div class="me-3">
                        <span class="badge bg-primary rounded-pill">${escapeHtml(c.candidateCode)}</span>
                    </div>
                    <div>
                        <h3 class="mb-0 h6">${escapeHtml(c.displayName)}</h3>
                        ${c.partyName ? `<small class="text-muted">${escapeHtml(c.partyName)}</small>` : ''}
                    </div>
                </div>
                ${c.biography ? `<p class="mt-2 mb-0 small text-muted">${escapeHtml(c.biography)}</p>` : ''}
            </div>
        `).join('')}
    </div>`;
}

function renderStats(e) {
    document.getElementById('statsContainer').innerHTML = `
        <ul class="list-unstyled mb-0">
            <li class="mb-2"><i class="bi bi-hash me-2 text-primary"></i>
                <strong>${e.totalBallotsAccepted}</strong> ballots accepted
            </li>
            <li class="mb-2"><i class="bi bi-diagram-3 me-2 text-success"></i>
                <strong>${e.totalBatchesAnchored}</strong> batches anchored
            </li>
            ${e.startTime ? `<li class="mb-2"><i class="bi bi-calendar-event me-2"></i>${new Date(e.startTime).toLocaleString()}</li>` : ''}
            ${e.endTime ? `<li><i class="bi bi-calendar-x me-2"></i>${new Date(e.endTime).toLocaleString()}</li>` : ''}
        </ul>
    `;
}

function renderVoteButton(e) {
    const container = document.getElementById('voteButtonContainer');
    if (e.votingOpen) {
        container.innerHTML = `
            <div class="card shadow-sm mb-4 border-success">
                <div class="card-body text-center">
                    <p class="fw-semibold mb-2">Voting is now open!</p>
                    <a href="/elections/${electionId}/vote" class="btn btn-success btn-lg w-100">
                        <i class="bi bi-check2-square me-2"></i>Cast My Vote
                    </a>
                </div>
            </div>`;
    } else {
        container.innerHTML = `
            <div class="alert alert-secondary mb-4 text-center">
                <i class="bi bi-clock me-2"></i>Voting is not currently open.
            </div>`;
    }
}

function verifyBallot() {
    const ref = document.getElementById('verifyRefInput').value.trim();
    if (!ref) return;
    const div = document.getElementById('verifyResult');
    div.innerHTML = '<div class="spinner-border spinner-border-sm"></div>';
    fetch(`/api/v1/verify/ballot/${encodeURIComponent(ref)}`)
        .then(r => r.json())
        .then(data => {
            div.innerHTML = `<div class="alert alert-${data.lifecycleState === 'BLOCKCHAIN_ANCHORED' || data.lifecycleState === 'FINALIZED' ? 'success' : 'info'} small">
                <strong>${data.lifecycleState}</strong><br>
                Commitment: <code>${data.commitment ? data.commitment.substring(0,16) + '...' : 'N/A'}</code>
            </div>`;
        })
        .catch(() => {
            div.innerHTML = '<div class="alert alert-danger small">Ballot reference not found.</div>';
        });
}

function escapeHtml(s) {
    if (!s) return '';
    return s.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
}
</script>
</body>
</html>
