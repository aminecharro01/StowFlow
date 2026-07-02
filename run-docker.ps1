# Demarre StowFlow via Docker Compose (PostgreSQL + API + front)
$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    Write-Error "Docker n'est pas installe. Installez Docker Desktop: https://www.docker.com/products/docker-desktop/"
}

if (-not (Test-Path ".env.docker")) {
    Write-Host "Note: .env.docker absent — copiez .env.docker.example si vous utilisez Gemini." -ForegroundColor Yellow
}

Write-Host ""
Write-Host "StowFlow Docker — 3 services:" -ForegroundColor Cyan
Write-Host "  [1] postgres       -> localhost:5433  (database: stowflow, internal:5432)"
Write-Host "  [2] stowflow-api   -> localhost:8000  (Spring Boot REST API)"
Write-Host "  [3] inventra-web   -> localhost:3000  (Next.js frontend)"
Write-Host ""
Write-Host "App:     http://localhost:3000"
Write-Host "API:     http://localhost:8000"
Write-Host "Swagger: http://localhost:8000/swagger-ui.html"
Write-Host "Demo:    stock@default.demo / password"
Write-Host ""

docker compose up --build @args
