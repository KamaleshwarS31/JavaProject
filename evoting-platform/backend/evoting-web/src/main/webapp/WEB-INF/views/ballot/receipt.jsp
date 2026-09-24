<%@ page contentType="text/html;charset=UTF-8" language="java" isELIgnored="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Voting Receipt | E-Voting Platform</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet" integrity="sha384-T3c6CoIi6uLrA9TneNEoa7RxnatzjcDSCmG1MXxSR1GAsXEV/Dwwykc2MPK8M2HN" crossorigin="anonymous">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css" rel="stylesheet" crossorigin="anonymous">
</head>
<body class="bg-light">
<a href="#main-content" class="visually-hidden-focusable btn btn-primary position-absolute">Skip to main content</a>

<nav class="navbar navbar-dark bg-success">
    <div class="container">
        <a class="navbar-brand fw-bold" href="/"><i class="bi bi-shield-check me-2"></i>E-Voting Platform</a>
    </div>
</nav>

<main id="main-content" class="container py-5" tabindex="-1">
    <div class="row justify-content-center">
        <div class="col-md-8">
            <div class="card shadow border-success">
                <div class="card-header bg-success text-white">
                    <h1 class="h4 mb-0">
                        <i class="bi bi-check-circle-fill me-2"></i>Vote Successfully Cast
                    </h1>
                </div>
                <div class="card-body">
                    <div class="alert alert-success">
                        <strong>Your ballot has been accepted.</strong>
                        Save this receipt to verify your vote was counted.
                    </div>

                    <div class="alert alert-warning small">
                        <i class="bi bi-exclamation-triangle me-2"></i>
                        <strong>Privacy Notice:</strong> This receipt does NOT reveal your vote choice.
                        It cannot be used to prove how you voted to anyone else.
                    </div>

                    <dl class="row" id="receiptDetails">
                        <dt class="col-sm-4">Ballot Reference</dt>
                        <dd class="col-sm-8"><code id="ballotRef">Loading...</code></dd>

                        <dt class="col-sm-4">Election</dt>
                        <dd class="col-sm-8" id="electionTitle">Loading...</dd>

                        <dt class="col-sm-4">Commitment Hash</dt>
                        <dd class="col-sm-8"><code id="commitment" class="text-break">Loading...</code></dd>

                        <dt class="col-sm-4">Batch Reference</dt>
                        <dd class="col-sm-8"><code id="batchRef">Pending anchoring...</code></dd>

                        <dt class="col-sm-4">Blockchain TX</dt>
                        <dd class="col-sm-8"><code id="blockchainTx">Pending...</code></dd>

                        <dt class="col-sm-4">Verification URL</dt>
                        <dd class="col-sm-8"><a id="verifyUrl" href="#">Loading...</a></dd>
                    </dl>

                    <div class="d-flex gap-2">
                        <a href="/elections" class="btn btn-outline-primary">
                            <i class="bi bi-arrow-left me-1"></i>Back to Elections
                        </a>
                        <button class="btn btn-outline-secondary" onclick="window.print()">
                            <i class="bi bi-printer me-1"></i>Print Receipt
                        </button>
                    </div>
                </div>
            </div>
        </div>
    </div>
</main>

<script>
document.addEventListener('DOMContentLoaded', () => {
    const receipt = JSON.parse(localStorage.getItem('lastReceipt') || 'null');
    if (receipt) {
        document.getElementById('ballotRef').textContent = receipt.ballotReference || 'N/A';
        document.getElementById('electionTitle').textContent = receipt.electionTitle || 'N/A';
        document.getElementById('commitment').textContent = receipt.commitment || 'N/A';
        document.getElementById('batchRef').textContent = receipt.batchReference || 'Pending anchoring...';
        document.getElementById('blockchainTx').textContent = receipt.blockchainTxId || 'Pending...';
        const url = receipt.verificationUrl || '#';
        const link = document.getElementById('verifyUrl');
        link.href = url;
        link.textContent = url;
    }
});
</script>
</body>
</html>
