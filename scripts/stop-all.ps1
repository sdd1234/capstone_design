$ErrorActionPreference = 'Continue'

$root = Split-Path -Parent $PSScriptRoot
$logsDir = "$root\logs"

# Backend
$pidFile = "$logsDir\backend.pid"
if (Test-Path $pidFile) {
    $bp = Get-Content $pidFile | Select-Object -First 1
    if ($bp) { Stop-Process -Id $bp -Force -ErrorAction SilentlyContinue; Write-Host "Backend stopped (PID $bp)" }
    Remove-Item $pidFile -ErrorAction SilentlyContinue
}
$conn = Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue
if ($conn) { Stop-Process -Id $conn.OwningProcess -Force -ErrorAction SilentlyContinue }

# Frontend
$pidFile = "$logsDir\frontend.pid"
if (Test-Path $pidFile) {
    $fp = Get-Content $pidFile | Select-Object -First 1
    if ($fp) { Stop-Process -Id $fp -Force -ErrorAction SilentlyContinue; Write-Host "Frontend stopped (PID $fp)" }
    Remove-Item $pidFile -ErrorAction SilentlyContinue
}
$conn = Get-NetTCPConnection -LocalPort 5173 -ErrorAction SilentlyContinue
if ($conn) { Stop-Process -Id $conn.OwningProcess -Force -ErrorAction SilentlyContinue }

Write-Host "All stopped (MySQL은 그대로 유지)."
