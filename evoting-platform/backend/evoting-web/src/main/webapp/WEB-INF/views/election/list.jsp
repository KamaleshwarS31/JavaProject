<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Elections | E-Voting Platform</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet" integrity="sha384-T3c6CoIi6uLrA9TneNEoa7RxnatzjcDSCmG1MXxSR1GAsXEV/Dwwykc2MPK8M2HN" crossorigin="anonymous">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css" rel="stylesheet" crossorigin="anonymous">
</head>
<body class="bg-light">
<a href="#main-content" class="visually-hidden-focusable btn btn-primary position-absolute">Skip to main content</a>

<nav class="navbar navbar-expand-lg navbar-dark bg-primary">
    <div class="container">
        <a class="navbar-brand fw-bold" href="/">
            <i class="bi bi-shield-lock-fill me-2" aria-hidden="true"></i>E-Voting Platform
        </a>
        <div class="navbar-nav ms-auto">
            <a class="nav-link text-white" href="/auth/login">Login</a>
            <a class="nav-link text-white" href="/auth/register">Register</a>
        </div>
    </div>
</nav>

<main id="main-content" class="container py-5" tabindex="-1">
    <div class="row mb-4">
        <div class="col">
            <h1 class="display-5 fw-bold">
                <i class="bi bi-ballot-fill text-primary me-2" aria-hidden="true"></i>
                Active Elections
            </h1>
            <p class="text-muted lead">View and participate in currently open elections.</p>
        </div>
    </div>

    <!-- Elections are loaded dynamically from the database via API -->
    <div id="electionsContainer">
        <div class="text-center py-5">
            <div class="spinner-border text-primary" role="status" aria-label="Loading elections">
                <span class="visually-hidden">Loading...</span>
            </div>
        </div>
    </div>
</main>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js" integrity="sha384-C6RzsynM9kWDrMNeT87bh95OGNyZPhcTNXj1NW7RuBCsyN/o0jlpcV8Qyq46cDfL" crossorigin="anonymous"></script>
<script>
document.addEventListener('DOMContentLoaded', () => {
    fetch('/api/v1/elections')
        .then(r => r.json())
        .then(elections => renderElections(elections))
        .catch(err => {
            document.getElementById('electionsContainer').innerHTML =
                '<div class="alert alert-danger">Failed to load elections. Please refresh.</div>';
        });
});

function renderElections(elections) {
    const container = document.getElementById('electionsContainer');
    if (!elections || elections.length === 0) {
        container.innerHTML = '<div class="alert alert-info"><i class="bi bi-info-circle me-2"></i>No elections currently available.</div>';
        return;
    }

    const stateColors = {
        ACTIVE: 'success', DRAFT: 'secondary', READY: 'info',
        CLOSED: 'warning', TALLYING: 'primary', FINALIZED: 'dark'
    };

    const cards = elections.map(e => `
        <div class="col-md-6 col-lg-4 mb-4">
            <div class="card h-100 shadow-sm">
                <div class="card-header d-flex justify-content-between align-items-center">
                    <span class="badge bg-${stateColors[e.state] || 'secondary'} fs-6">${e.state}</span>
                    <small class="text-muted">${e.electionCode}</small>
                </div>
                <div class="card-body">
                    <h2 class="card-title h5">${escapeHtml(e.title)}</h2>
                    <p class="card-text text-muted small">${escapeHtml(e.description || '')}</p>
                    <ul class="list-unstyled small">
                        <li><i class="bi bi-people-fill me-1" aria-hidden="true"></i>
                            <strong>${e.totalBallotsAccepted}</strong> ballots accepted
                        </li>
                        <li><i class="bi bi-diagram-3-fill me-1" aria-hidden="true"></i>
                            <strong>${e.totalBatchesAnchored}</strong> batches anchored
                        </li>
                    </ul>
                </div>
                <div class="card-footer">
                    <a href="/elections/${e.id}" class="btn btn-primary btn-sm w-100"
                       aria-label="View election: ${escapeHtml(e.title)}">
                        <i class="bi bi-arrow-right me-1" aria-hidden="true"></i>
                        ${e.state === 'ACTIVE' ? 'Vote Now' : 'View Details'}
                    </a>
                </div>
            </div>
        </div>
    `).join('');

    container.innerHTML = `<div class="row">${cards}</div>`;
}

function escapeHtml(str) {
    if (!str) return '';
    return str.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
}
</script>
</body>
</html>
