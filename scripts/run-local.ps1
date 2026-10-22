<#
.SYNOPSIS
  Runs the backend locally against the database in .env (Neon dev branch).

.DESCRIPTION
  Loads the variables from .env into this PowerShell process only (nothing is printed or saved),
  activates the "local" Spring profile and starts the app with the Maven Wrapper.
  Stop it with Ctrl+C. Run from the project root:   .\scripts\run-local.ps1
#>
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$envFile = Join-Path $root '.env'
if (-not (Test-Path -LiteralPath $envFile)) { throw "No .env found. Run .\scripts\set-neon-env.ps1 first." }

Get-Content -LiteralPath $envFile | ForEach-Object {
    if ($_ -match '^\s*([A-Z0-9_]+)\s*=(.*)$') {
        [Environment]::SetEnvironmentVariable($Matches[1], $Matches[2].Trim(), 'Process')
    }
}
if ($env:DB_URL -match '^<' -or $env:DB_PASSWORD -match '^<') { throw "DB_* in .env are still placeholders. Run .\scripts\set-neon-env.ps1." }
if (-not $env:JWT_SECRET -or $env:JWT_SECRET -match '^<' -or $env:JWT_SECRET.Length -lt 32) { throw "JWT_SECRET in .env must be a random string of 32+ characters." }

if (-not $env:JAVA_HOME) { $env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.10.7-hotspot' }
$env:SPRING_PROFILES_ACTIVE = 'local'

Set-Location (Join-Path $root 'backend')
& .\mvnw.cmd spring-boot:run
