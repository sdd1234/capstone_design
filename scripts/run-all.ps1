$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

$env:JAVA_HOME = [Environment]::GetEnvironmentVariable('JAVA_HOME', 'User')
$env:MAVEN_HOME = [Environment]::GetEnvironmentVariable('MAVEN_HOME', 'User')
$env:Path = [Environment]::GetEnvironmentVariable('Path', 'User') + ';' + [Environment]::GetEnvironmentVariable('Path', 'Machine')

# ── MySQL ────────────────────────────────────────────────────────
$mysqlPing = mysqladmin -u root ping 2>&1
if ($mysqlPing -notlike '*alive*') {
    Write-Host 'Starting MySQL...'
    $mysqlIni = "$(scoop prefix mysql-lts)\my.ini"
    Start-Process -FilePath 'mysqld' -ArgumentList "--defaults-file=`"$mysqlIni`"" -WindowStyle Hidden
    Start-Sleep -Seconds 3
    Write-Host 'MySQL started.'
}
else {
    Write-Host 'MySQL is already running.'
}

# ── Backend & Frontend ───────────────────────────────────────────
Start-Process powershell -ArgumentList '-NoExit', '-Command', "Set-Location '$root\campus-fit-api'; mvn spring-boot:run"
Start-Sleep -Seconds 2
Start-Process powershell -ArgumentList '-NoExit', '-Command', "Set-Location '$root\campus-fit-web'; npm.cmd run dev"

Write-Host 'Started backend and frontend in separate PowerShell windows.'
