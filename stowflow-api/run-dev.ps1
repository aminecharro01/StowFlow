# Démarre stowflow-api sur le port 8000 (libère le port si déjà occupé).
$port = 8000
Write-Host "Recherche d'un processus sur le port $port..."

$connections = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue
if ($connections) {
  $pids = $connections | Select-Object -ExpandProperty OwningProcess -Unique
  foreach ($pid in $pids) {
    Write-Host "Arret du processus PID $pid (port $port occupe)."
    Stop-Process -Id $pid -Force -ErrorAction SilentlyContinue
  }
  Start-Sleep -Seconds 2
}

Write-Host "Demarrage de stowflow-api sur http://localhost:$port ..."
Set-Location $PSScriptRoot
$env:SPRING_PROFILES_ACTIVE = "local"
& .\mvnw.cmd spring-boot:run
