<%@ page contentType="text/html;charset=UTF-8" language="java" isELIgnored="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Login | E-Voting Platform</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet" integrity="sha384-T3c6CoIi6uLrA9TneNEoa7RxnatzjcDSCmG1MXxSR1GAsXEV/Dwwykc2MPK8M2HN" crossorigin="anonymous">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css" rel="stylesheet" crossorigin="anonymous">
</head>
<body class="bg-light">
<a href="#main-content" class="visually-hidden-focusable btn btn-primary position-absolute">Skip to main content</a>

<main id="main-content" class="d-flex align-items-center justify-content-center min-vh-100">
    <div class="card shadow-sm p-4" style="width:100%;max-width:420px;">
        <div class="text-center mb-4">
            <i class="bi bi-shield-lock-fill text-primary fs-1" aria-hidden="true"></i>
            <h1 class="h3 mt-2">Secure Login</h1>
            <p class="text-muted small">E-Voting Platform</p>
        </div>

        <c:if test="${not empty errorMessage}">
            <div class="alert alert-danger" role="alert">
                <i class="bi bi-exclamation-triangle-fill me-2" aria-hidden="true"></i>
                <c:out value="${errorMessage}"/>
            </div>
        </c:if>

        <!-- Login Form: Demonstrates Servlets, JavaBeans, JSP concepts -->
        <form action="/auth/login" method="post" autocomplete="off" novalidate id="loginForm">
            <div class="mb-3">
                <label for="username" class="form-label fw-semibold">
                    Username
                </label>
                <div class="input-group">
                    <span class="input-group-text" aria-hidden="true"><i class="bi bi-person"></i></span>
                    <input type="text" class="form-control" id="username" name="username"
                           required autocomplete="username"
                           aria-required="true"
                           aria-describedby="usernameHelp"
                           placeholder="Enter your username"
                           value="<c:out value='${param.username}'/>"/>
                </div>
                <div id="usernameHelp" class="form-text">Your registered username (3–50 characters)</div>
            </div>

            <div class="mb-4">
                <label for="password" class="form-label fw-semibold">
                    Password
                </label>
                <div class="input-group">
                    <span class="input-group-text" aria-hidden="true"><i class="bi bi-lock"></i></span>
                    <input type="password" class="form-control" id="password" name="password"
                           required autocomplete="current-password"
                           aria-required="true"
                           placeholder="Enter your password"/>
                    <button class="btn btn-outline-secondary" type="button"
                            onclick="togglePassword('password', this)"
                            aria-label="Show or hide password">
                        <i class="bi bi-eye" aria-hidden="true"></i>
                    </button>
                </div>
            </div>

            <div class="d-grid">
                <button type="submit" class="btn btn-primary btn-lg"
                        aria-label="Sign in to your account">
                    <i class="bi bi-box-arrow-in-right me-2" aria-hidden="true"></i>
                    Sign In
                </button>
            </div>
        </form>

        <hr class="my-4">
        <p class="text-center text-muted mb-0 small">
            Don't have an account?
            <a href="/auth/register" class="text-decoration-none">Register here</a>
        </p>
        <p class="text-center mt-2">
            <a href="/" class="text-muted small text-decoration-none">
                <i class="bi bi-arrow-left me-1" aria-hidden="true"></i>Back to Elections
            </a>
        </p>
    </div>
</main>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js" integrity="sha384-C6RzsynM9kWDrMNeT87bh95OGNyZPhcTNXj1NW7RuBCsyN/o0jlpcV8Qyq46cDfL" crossorigin="anonymous"></script>
<script>
function togglePassword(id, btn) {
    const input = document.getElementById(id);
    const isHidden = input.type === 'password';
    input.type = isHidden ? 'text' : 'password';
    btn.querySelector('i').className = isHidden ? 'bi bi-eye-slash' : 'bi bi-eye';
    btn.setAttribute('aria-label', isHidden ? 'Hide password' : 'Show password');
}
</script>
</body>
</html>
