<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <!-- Security Headers (also set by Spring Security) -->
    <meta http-equiv="Content-Security-Policy" content="default-src 'self'; script-src 'self' 'unsafe-inline' 'unsafe-eval' https://cdn.jsdelivr.net; style-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net; font-src 'self' data: https://cdn.jsdelivr.net; img-src 'self' data:; connect-src 'self'; object-src 'none'; frame-ancestors 'none'">
    <title><c:out value="${pageTitle}" default="E-Voting Platform"/> | Privacy-Preserving E-Voting</title>

    <!-- Bootstrap 5.3 -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css"
          rel="stylesheet"
          integrity="sha384-T3c6CoIi6uLrA9TneNEoa7RxnatzjcDSCmG1MXxSR1GAsXEV/Dwwykc2MPK8M2HN"
          crossorigin="anonymous">
    <!-- Bootstrap Icons -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css"
          rel="stylesheet"
          crossorigin="anonymous">
    <!-- Custom styles -->
    <link href="/css/evoting.css" rel="stylesheet">
</head>
<body class="bg-light">

<!-- Skip navigation for WCAG 2.2 AA accessibility -->
<a href="#main-content" class="visually-hidden-focusable btn btn-primary position-absolute">
    Skip to main content
</a>

<!-- Navbar -->
<nav class="navbar navbar-expand-lg navbar-dark bg-primary" role="navigation" aria-label="Main navigation">
    <div class="container">
        <a class="navbar-brand fw-bold" href="/">
            <i class="bi bi-shield-lock-fill me-2" aria-hidden="true"></i>
            E-Voting Platform
        </a>
        <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#navMenu"
                aria-controls="navMenu" aria-expanded="false" aria-label="Toggle navigation">
            <span class="navbar-toggler-icon"></span>
        </button>
        <div class="collapse navbar-collapse" id="navMenu">
            <ul class="navbar-nav me-auto mb-2 mb-lg-0">
                <li class="nav-item">
                    <a class="nav-link" href="/elections" aria-current="page">Elections</a>
                </li>
                <li class="nav-item">
                    <a class="nav-link" href="/audit-portal">Audit Portal</a>
                </li>
                <li class="nav-item">
                    <a class="nav-link" href="/api/v1/verify">Verify Ballot</a>
                </li>
            </ul>
            <ul class="navbar-nav">
                <c:choose>
                    <c:when test="${not empty sessionUser}">
                        <li class="nav-item">
                            <span class="navbar-text me-2">
                                <i class="bi bi-person-circle" aria-hidden="true"></i>
                                <c:out value="${sessionUser.username}"/>
                            </span>
                        </li>
                        <li class="nav-item">
                            <a class="nav-link" href="/auth/logout">Logout</a>
                        </li>
                    </c:when>
                    <c:otherwise>
                        <li class="nav-item">
                            <a class="nav-link" href="/auth/login">Login</a>
                        </li>
                        <li class="nav-item">
                            <a class="nav-link btn btn-outline-light btn-sm px-3" href="/auth/register">Register</a>
                        </li>
                    </c:otherwise>
                </c:choose>
            </ul>
        </div>
    </div>
</nav>

<!-- Main Content -->
<main id="main-content" class="container py-4" tabindex="-1">
    <!-- Flash messages -->
    <c:if test="${not empty successMessage}">
        <div class="alert alert-success alert-dismissible fade show" role="alert">
            <i class="bi bi-check-circle-fill me-2" aria-hidden="true"></i>
            <c:out value="${successMessage}"/>
            <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
        </div>
    </c:if>
    <c:if test="${not empty errorMessage}">
        <div class="alert alert-danger alert-dismissible fade show" role="alert">
            <i class="bi bi-exclamation-triangle-fill me-2" aria-hidden="true"></i>
            <c:out value="${errorMessage}"/>
            <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
        </div>
    </c:if>

    <jsp:include page="${contentPage}"/>
</main>

<!-- Footer -->
<footer class="bg-dark text-light py-4 mt-auto" role="contentinfo">
    <div class="container">
        <div class="row">
            <div class="col-md-6">
                <p class="mb-1"><strong>E-Voting Platform</strong></p>
                <p class="text-muted small mb-0">
                    Academic Research Prototype — Not certified for production elections.
                </p>
            </div>
            <div class="col-md-6 text-md-end">
                <a href="/api/v1/verify" class="text-light me-3 small">Verify Ballot</a>
                <a href="/audit-portal" class="text-light me-3 small">Audit Portal</a>
                <a href="/api-docs" class="text-light small">API Docs</a>
            </div>
        </div>
    </div>
</footer>

<!-- Bootstrap JS -->
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"
        integrity="sha384-C6RzsynM9kWDrMNeT87bh95OGNyZPhcTNXj1NW7RuBCsyN/o0jlpcV8Qyq46cDfL"
        crossorigin="anonymous"></script>
</body>
</html>
