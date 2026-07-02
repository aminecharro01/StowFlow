# Verifie que les 3 conteneurs StowFlow tournent (apres docker compose up -d)
$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

$services = @("stowflow-postgres", "stowflow-api", "stowflow-web")
$ok = $true

Write-Host "`nStowFlow Docker status`n" -ForegroundColor Cyan

docker ps --filter "name=stowflow" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

foreach ($name in $services) {
    $running = docker inspect -f "{{.State.Running}}" $name 2>$null
    if ($running -ne "true") {
        Write-Host "MISSING or stopped: $name" -ForegroundColor Red
        $ok = $false
    }
}

if (-not $ok) {
    Write-Host "`nRun: docker compose up --build -d" -ForegroundColor Yellow
    exit 1
}

Write-Host "`nHealth checks..." -ForegroundColor Cyan

try {
    $pg = docker exec stowflow-postgres pg_isready -U postgres -d stowflow 2>&1
    Write-Host "  postgres: $pg"
} catch {
    Write-Host "  postgres: FAIL" -ForegroundColor Red
    $ok = $false
}

try {
    $api = Invoke-RestMethod -Uri "http://localhost:8000/actuator/health" -TimeoutSec 5
    Write-Host "  stowflow-api: $($api.status)"
} catch {
    Write-Host "  stowflow-api: FAIL (http://localhost:8000)" -ForegroundColor Red
    $ok = $false
}

try {
    $frontCode = curl.exe -s -o NUL -w "%{http_code}" http://localhost:3000 2>$null
    if ($frontCode -match "^(200|307)$") {
        Write-Host "  inventra-web: HTTP $frontCode"
    } else {
        Write-Host "  inventra-web: FAIL (HTTP $frontCode)" -ForegroundColor Red
        $ok = $false
    }
} catch {
    Write-Host "  inventra-web: FAIL (http://localhost:3000)" -ForegroundColor Red
    $ok = $false
}

if ($ok) {
    Write-Host "`nAll 3 services are up." -ForegroundColor Green
    Write-Host "Open http://localhost:3000`n"
} else {
    Write-Host "`nSome checks failed. Logs: docker compose logs -f`n" -ForegroundColor Yellow
    exit 1
}
