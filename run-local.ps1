# ============================================================
# E-Voting Platform — Local Startup Script (Windows PowerShell)
# Reads .env and starts Spring Boot with all environment variables
# ============================================================

$ErrorActionPreference = "Stop"

$envFile = Join-Path $PSScriptRoot ".env"
if (-not (Test-Path $envFile)) {
    Write-Host "ERROR: .env file not found at $envFile. Copy .env.example to .env and configure your values." -ForegroundColor Red
    exit 1
}

Write-Host "Loading environment variables from $envFile..." -ForegroundColor Cyan
Get-Content $envFile | ForEach-Object {
    $line = $_.Trim()
    if ($line -and -not $line.StartsWith("#")) {
        $idx = $line.IndexOf("=")
        if ($idx -gt 0) {
            $key = $line.Substring(0, $idx).Trim()
            $val = $line.Substring($idx + 1).Trim()
            [System.Environment]::SetEnvironmentVariable($key, $val, "Process")
        }
    }
}

Write-Host "Starting E-Voting Spring Boot Application on port $env:APP_PORT..." -ForegroundColor Green
mvn spring-boot:run -f (Join-Path $PSScriptRoot "evoting-platform\backend\evoting-web\pom.xml")
