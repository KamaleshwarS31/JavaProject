<%@ page contentType="text/html;charset=UTF-8" language="java" isELIgnored="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Cast Vote | E-Voting Platform</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet" integrity="sha384-T3c6CoIi6uLrA9TneNEoa7RxnatzjcDSCmG1MXxSR1GAsXEV/Dwwykc2MPK8M2HN" crossorigin="anonymous">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css" rel="stylesheet" crossorigin="anonymous">
</head>
<body class="bg-light">
<a href="#main-content" class="visually-hidden-focusable btn btn-primary position-absolute">Skip to main content</a>

<nav class="navbar navbar-dark bg-primary">
    <div class="container">
        <a class="navbar-brand fw-bold" href="/"><i class="bi bi-shield-lock-fill me-2" aria-hidden="true"></i>E-Voting Platform</a>
    </div>
</nav>

<main id="main-content" class="container py-5" tabindex="-1">
    <div class="row justify-content-center">
        <div class="col-md-8 col-lg-6">
            <div class="card shadow">
                <div class="card-header bg-primary text-white">
                    <h1 class="h4 mb-0">
                        <i class="bi bi-check2-square me-2" aria-hidden="true"></i>
                        Cast Your Vote
                    </h1>
                </div>
                <div class="card-body">
                    <div class="alert alert-info small" role="note">
                        <i class="bi bi-shield-shaded me-2" aria-hidden="true"></i>
                        <strong>Privacy Protected:</strong> Your vote is encrypted in your browser before being sent.
                        Your identity is never linked to your vote.
                    </div>

                    <div id="electionTitle" class="mb-3">
                        <div class="placeholder-glow"><span class="placeholder col-8"></span></div>
                    </div>

                    <fieldset id="candidateSection">
                        <legend class="fw-semibold mb-3">Select a Candidate</legend>
                        <div id="candidatesContainer">
                            <div class="text-center py-3">
                                <div class="spinner-border spinner-border-sm" role="status">
                                    <span class="visually-hidden">Loading candidates...</span>
                                </div>
                            </div>
                        </div>
                    </fieldset>

                    <!-- Credential input -->
                    <div class="mb-3 mt-4">
                        <label for="credentialToken" class="form-label fw-semibold">
                            Credential Token
                            <span class="text-danger" aria-hidden="true">*</span>
                        </label>
                        <input type="password" class="form-control" id="credentialToken"
                               required aria-required="true"
                               aria-describedby="credHelp"
                               placeholder="Enter your one-time credential token"/>
                        <div id="credHelp" class="form-text">
                            This is the anonymous credential issued by the Eligibility Authority.
                        </div>
                    </div>

                    <div class="d-grid gap-2">
                        <button type="button" class="btn btn-success btn-lg" id="submitVoteBtn"
                                onclick="submitVote()"
                                aria-label="Submit encrypted vote">
                            <i class="bi bi-send-fill me-2" aria-hidden="true"></i>
                            Submit Encrypted Vote
                        </button>
                    </div>

                    <div id="statusMessage" class="mt-3" role="status" aria-live="polite"></div>
                </div>
            </div>
        </div>
    </div>
</main>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js" integrity="sha384-C6RzsynM9kWDrMNeT87bh95OGNyZPhcTNXj1NW7RuBCsyN/o0jlpcV8Qyq46cDfL" crossorigin="anonymous"></script>
<script>
const electionId = window.location.pathname.split('/')[2];
let electionPublicKey = null;
let candidates = [];

document.addEventListener('DOMContentLoaded', () => {
    fetch(`/api/v1/elections/${electionId}`)
        .then(r => r.json())
        .then(election => {
            document.getElementById('electionTitle').innerHTML =
                `<h2 class="h5 text-primary">${escapeHtml(election.title)}</h2>`;
            electionPublicKey = election.publicKeyPem;
            candidates = election.candidates || [];
            renderCandidates(candidates);
        })
        .catch(() => showStatus('Failed to load election data.', 'danger'));
});

function renderCandidates(cands) {
    const container = document.getElementById('candidatesContainer');
    if (!cands.length) {
        container.innerHTML = '<p class="text-muted">No candidates registered yet.</p>';
        return;
    }
    container.innerHTML = cands.map((c, i) => `
        <div class="form-check mb-3 border rounded p-3 candidate-option" role="listitem">
            <input class="form-check-input" type="radio" name="candidate" id="cand_${i}"
                   value="${escapeHtml(c.candidateCode)}"
                   aria-describedby="cand_desc_${i}"/>
            <label class="form-check-label" for="cand_${i}">
                <strong>${escapeHtml(c.displayName)}</strong>
                <c:if test="${not empty c.partyName}">
                    <span class="text-muted ms-2 small">${escapeHtml(c.partyName || '')}</span>
                </c:if>
            </label>
            <div id="cand_desc_${i}" class="small text-muted mt-1">${escapeHtml(c.biography || '')}</div>
        </div>
    `).join('');
}

async function submitVote() {
    const selectedCandidate = document.querySelector('input[name="candidate"]:checked');
    const credentialToken = document.getElementById('credentialToken').value.trim();

    if (!selectedCandidate) { showStatus('Please select a candidate.', 'warning'); return; }
    if (!credentialToken) { showStatus('Please enter your credential token.', 'warning'); return; }
    if (!electionPublicKey) { showStatus('Election key not loaded. Please refresh.', 'danger'); return; }

    try {
        document.getElementById('submitVoteBtn').disabled = true;
        showStatus('Encrypting your vote in the browser…', 'info');

        // Client-side encryption using Web Crypto API
        const ballotJson = JSON.stringify({ candidateCode: selectedCandidate.value });
        const encryptedBallot = await encryptBallot(ballotJson, electionPublicKey);

        // Generate salt and commitment
        const salt = generateHex(32);
        const commitment = await computeCommitment(encryptedBallot, salt);

        // Generate nullifier (in production this uses the credential secret)
        const nullifier = await computeNullifier(credentialToken, electionId);

        const requestId = generateHex(16);

        showStatus('Submitting encrypted ballot…', 'info');

        const token = localStorage.getItem('jwtToken');
        const response = await fetch('/api/v1/ballots/cast', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`,
                'X-Request-Id': requestId
            },
            body: JSON.stringify({
                electionId, credentialToken,
                nullifier, encryptedBallot,
                commitment, requestId
            })
        });

        const data = await response.json();
        if (response.ok) {
            localStorage.setItem('lastReceipt', JSON.stringify(data));
            window.location.href = `/ballot/receipt?ref=${data.ballotReference}`;
        } else {
            showStatus(data.message || 'Vote submission failed.', 'danger');
            document.getElementById('submitVoteBtn').disabled = false;
        }
    } catch(e) {
        showStatus('Encryption error. Please try again.', 'danger');
        document.getElementById('submitVoteBtn').disabled = false;
    }
}

async function encryptBallot(plaintext, publicKeyBase64) {
    const binaryKey = Uint8Array.from(atob(publicKeyBase64), c => c.charCodeAt(0));
    const publicKey = await window.crypto.subtle.importKey(
        'spki', binaryKey.buffer,
        { name: 'RSA-OAEP', hash: 'SHA-256' },
        false, ['encrypt']
    );
    const enc = new TextEncoder();
    const encrypted = await window.crypto.subtle.encrypt(
        { name: 'RSA-OAEP' }, publicKey, enc.encode(plaintext)
    );
    return btoa(String.fromCharCode(...new Uint8Array(encrypted)));
}

async function computeCommitment(encryptedBallot, salt) {
    const enc = new TextEncoder();
    const data = enc.encode(`EVOTING_COMMITMENT_V1|${encryptedBallot}|${salt}`);
    const hash = await window.crypto.subtle.digest('SHA-256', data);
    return Array.from(new Uint8Array(hash)).map(b => b.toString(16).padStart(2,'0')).join('');
}

async function computeNullifier(credentialToken, electionId) {
    const enc = new TextEncoder();
    const data = enc.encode(`EVOTING_NULLIFIER_V1|${electionId}|${credentialToken}`);
    const hash = await window.crypto.subtle.digest('SHA-256', data);
    return Array.from(new Uint8Array(hash)).map(b => b.toString(16).padStart(2,'0')).join('');
}

function generateHex(bytes) {
    const arr = new Uint8Array(bytes);
    window.crypto.getRandomValues(arr);
    return Array.from(arr).map(b => b.toString(16).padStart(2,'0')).join('');
}

function showStatus(msg, type) {
    document.getElementById('statusMessage').innerHTML =
        `<div class="alert alert-${type}">${escapeHtml(msg)}</div>`;
}

function escapeHtml(s) {
    if (!s) return '';
    return s.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
}
</script>
</body>
</html>
