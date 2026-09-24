<%@ page contentType="text/html;charset=UTF-8" language="java" isELIgnored="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Register | E-Voting Platform</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet" integrity="sha384-T3c6CoIi6uLrA9TneNEoa7RxnatzjcDSCmG1MXxSR1GAsXEV/Dwwykc2MPK8M2HN" crossorigin="anonymous">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css" rel="stylesheet" crossorigin="anonymous">
</head>
<body class="bg-light">
<a href="#main-content" class="visually-hidden-focusable btn btn-primary position-absolute">Skip to main content</a>

<main id="main-content" class="d-flex align-items-center justify-content-center min-vh-100 py-5">
    <div class="card shadow-sm p-4" style="width:100%;max-width:480px;">
        <div class="text-center mb-4">
            <i class="bi bi-person-plus-fill text-primary fs-1" aria-hidden="true"></i>
            <h1 class="h3 mt-2">Create Account</h1>
            <p class="text-muted small">Voter Registration</p>
        </div>

        <c:if test="${not empty errorMessage}">
            <div class="alert alert-danger" role="alert">
                <i class="bi bi-exclamation-triangle-fill me-2" aria-hidden="true"></i>
                <c:out value="${errorMessage}"/>
            </div>
        </c:if>

        <form action="/auth/register" method="post" autocomplete="off" novalidate id="registerForm">
            <div class="mb-3">
                <label for="username" class="form-label fw-semibold">Username <span aria-hidden="true" class="text-danger">*</span></label>
                <input type="text" class="form-control" id="username" name="username"
                       required minlength="3" maxlength="50"
                       pattern="[a-zA-Z0-9_-]+"
                       aria-required="true"
                       aria-describedby="usernameHelp"
                       value="<c:out value='${param.username}'/>"
                       placeholder="3–50 alphanumeric characters"/>
                <div id="usernameHelp" class="form-text">Letters, numbers, hyphens, and underscores only.</div>
            </div>

            <div class="mb-3">
                <label for="email" class="form-label fw-semibold">Email <span aria-hidden="true" class="text-danger">*</span></label>
                <input type="email" class="form-control" id="email" name="email"
                       required autocomplete="email"
                       aria-required="true"
                       value="<c:out value='${param.email}'/>"
                       placeholder="your@email.com"/>
            </div>

            <div class="mb-3">
                <label for="password" class="form-label fw-semibold">Password <span aria-hidden="true" class="text-danger">*</span></label>
                <div class="input-group">
                    <input type="password" class="form-control" id="password" name="password"
                           required minlength="12" maxlength="128"
                           autocomplete="new-password"
                           aria-required="true"
                           aria-describedby="passwordHelp"
                           placeholder="Minimum 12 characters"/>
                    <button class="btn btn-outline-secondary" type="button"
                            onclick="togglePassword('password', this)" aria-label="Show or hide password">
                        <i class="bi bi-eye" aria-hidden="true"></i>
                    </button>
                </div>
                <div id="passwordHelp" class="form-text">At least 12 characters. Use a strong, unique password.</div>
            </div>

            <div class="mb-4">
                <label for="confirmPassword" class="form-label fw-semibold">Confirm Password <span aria-hidden="true" class="text-danger">*</span></label>
                <div class="input-group">
                    <input type="password" class="form-control" id="confirmPassword" name="confirmPassword"
                           required autocomplete="new-password"
                           aria-required="true"
                           placeholder="Repeat password"/>
                    <button class="btn btn-outline-secondary" type="button"
                            onclick="togglePassword('confirmPassword', this)" aria-label="Show or hide confirm password">
                        <i class="bi bi-eye" aria-hidden="true"></i>
                    </button>
                </div>
            </div>

            <div class="d-grid">
                <button type="submit" class="btn btn-primary btn-lg">
                    <i class="bi bi-person-check me-2" aria-hidden="true"></i>
                    Create Account
                </button>
            </div>
        </form>

        <hr class="my-4">
        <p class="text-center text-muted mb-0 small">
            Already have an account?
            <a href="/auth/login" class="text-decoration-none">Sign in</a>
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
