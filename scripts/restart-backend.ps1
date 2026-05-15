$ErrorActionPreference = 'Continue'

$root = Split-Path -Parent $PSScriptRoot
$logsDir = "$root\logs"
if (-not (Test-Path $logsDir)) {
    New-Item -ItemType Directory -Path $logsDir | Out-Null
}

# 8080 쓰는 process 죽이기 (PID 파일 우선, 없으면 포트로 검색)
$pidFile = "$logsDir\backend.pid"
if (Test-Path $pidFile) {
    $oldPid = Get-Content $pidFile | Select-Object -First 1
    if ($oldPid) {
        Stop-Process -Id $oldPid -Force -ErrorAction SilentlyContinue
    }
    Remove-Item $pidFile -ErrorAction SilentlyContinue
}
$conn = Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue
if ($conn) {
    Stop-Process -Id $conn.OwningProcess -Force -ErrorAction SilentlyContinue
    Write-Host "Killed leftover PID $($conn.OwningProcess) on 8080"
}
Start-Sleep -Seconds 2

# Hidden 모드로 백엔드 재시작
$env:Path = [Environment]::GetEnvironmentVariable('Path', 'User') + ';' + [Environment]::GetEnvironmentVariable('Path', 'Machine')
$env:JAVA_HOME = [Environment]::GetEnvironmentVariable('JAVA_HOME', 'User')
$env:MAVEN_HOME = [Environment]::GetEnvironmentVariable('MAVEN_HOME', 'User')

$backendCmd = "cd /d `"$root\campus-fit-api`" && mvn spring-boot:run > `"$logsDir\backend.log`" 2>&1"
$proc = Start-Process -FilePath 'cmd.exe' -ArgumentList '/c', $backendCmd -WindowStyle Hidden -PassThru
$proc.Id | Out-File "$logsDir\backend.pid"
Write-Host "Backend restarted (PID $($proc.Id)) → logs\backend.log"
