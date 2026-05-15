$ErrorActionPreference = 'Continue'

$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

$env:JAVA_HOME = [Environment]::GetEnvironmentVariable('JAVA_HOME', 'User')
$env:MAVEN_HOME = [Environment]::GetEnvironmentVariable('MAVEN_HOME', 'User')
$env:Path = [Environment]::GetEnvironmentVariable('Path', 'User') + ';' + [Environment]::GetEnvironmentVariable('Path', 'Machine')

# ── MySQL ────────────────────────────────────────────────────────
$mysqlPing = ''
try {
    $mysqlPing = (& mysqladmin -u root ping 2>&1 | Out-String)
} catch {
    $mysqlPing = ''
}
if ($mysqlPing -notlike '*alive*') {
    Write-Host 'Starting MySQL...'
    $mysqlIni = "$(scoop prefix mysql-lts)\my.ini"
    Start-Process -FilePath 'mysqld' -ArgumentList "--defaults-file=`"$mysqlIni`"" -WindowStyle Hidden
    Start-Sleep -Seconds 5
    Write-Host 'MySQL started.'
}
else {
    Write-Host 'MySQL is already running.'
}

# ── Backend & Frontend (Hidden window + 로그 파일) ─────────────────
# 새 창 안 띄우고 백그라운드로. 로그는 logs/ 디렉토리.
$logsDir = "$root\logs"
if (-not (Test-Path $logsDir)) {
    New-Item -ItemType Directory -Path $logsDir | Out-Null
}

# 기존 PID 파일 정리
$pidDir = "$root\logs"

# Backend
$backendCmd = "cd /d `"$root\campus-fit-api`" && mvn spring-boot:run > `"$logsDir\backend.log`" 2>&1"
$backendProc = Start-Process -FilePath 'cmd.exe' -ArgumentList '/c', $backendCmd -WindowStyle Hidden -PassThru
$backendProc.Id | Out-File "$logsDir\backend.pid"
Write-Host "Backend started (PID $($backendProc.Id)) → logs\backend.log"

Start-Sleep -Seconds 2

# Frontend
$frontendCmd = "cd /d `"$root\campus-fit-web`" && npm.cmd run dev > `"$logsDir\frontend.log`" 2>&1"
$frontendProc = Start-Process -FilePath 'cmd.exe' -ArgumentList '/c', $frontendCmd -WindowStyle Hidden -PassThru
$frontendProc.Id | Out-File "$logsDir\frontend.pid"
Write-Host "Frontend started (PID $($frontendProc.Id)) → logs\frontend.log"

Write-Host ''
Write-Host '✅ 백엔드/프론트는 hidden 모드로 실행 중. 로그:'
Write-Host '   logs\backend.log  (tail -f로 진행 확인)'
Write-Host '   logs\frontend.log'
Write-Host '   종료: scripts\stop-all.ps1 또는 taskkill /PID <PID>'
