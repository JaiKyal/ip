<#
.SYNOPSIS
  Fills DB_URL, DB_USERNAME and DB_PASSWORD in .env from a Neon connection string.

.DESCRIPTION
  Paste the connection string that Neon's Connect dialog shows, for example
  postgresql://user:password@ep-xxxx.ap-southeast-1.aws.neon.tech/neondb?sslmode=require
  The paste is hidden while you type. The script converts it to the JDBC form Spring and Flyway
  need and updates only those three lines in .env. It never prints the user name or the password.

  Run from the project root:   .\scripts\set-neon-env.ps1
#>
param(
    [SecureString] $ConnectionString,
    [string] $EnvFile = (Join-Path $PSScriptRoot '..\.env')
)

$ErrorActionPreference = 'Stop'

if (-not $ConnectionString) {
    $ConnectionString = Read-Host 'Paste the Neon connection string (input is hidden)' -AsSecureString
}
$bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($ConnectionString)
try { $raw = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr).Trim().Trim('"').Trim("'") }
finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr) }

if ($raw -notmatch '^postgres(?:ql)?://([^:/@]+):([^@]+)@([^/:?]+)(?::(\d+))?/([^?]+)(?:\?.*)?$') {
    throw 'That does not look like a Neon connection string (postgresql://user:password@host/database?...). Nothing was changed.'
}
$user = [uri]::UnescapeDataString($Matches[1])
$pass = [uri]::UnescapeDataString($Matches[2])
$dbHost = $Matches[3]
$port = $Matches[4]
$database = $Matches[5]

if ($dbHost -match '-pooler') {
    throw "This is the POOLED host (it contains -pooler). Turn 'Connection pooling' OFF in Neon's Connect dialog and copy the string again. Nothing was changed."
}

$hostPart = if ($port) { "${dbHost}:$port" } else { $dbHost }
$jdbcUrl = "jdbc:postgresql://$hostPart/${database}?sslmode=require"

if (-not (Test-Path -LiteralPath $EnvFile)) {
    $example = Join-Path (Split-Path -Parent $EnvFile) '.env.example'
    if (-not (Test-Path -LiteralPath $example)) { throw "No .env and no .env.example next to it ($EnvFile)." }
    Copy-Item -LiteralPath $example -Destination $EnvFile
}

$values = [ordered]@{ DB_URL = $jdbcUrl; DB_USERNAME = $user; DB_PASSWORD = $pass }
$lines = [string[]](Get-Content -LiteralPath $EnvFile)
foreach ($key in $values.Keys) {
    $found = $false
    for ($i = 0; $i -lt $lines.Count; $i++) {
        if ($lines[$i] -match "^\s*$key\s*=") { $lines[$i] = "$key=$($values[$key])"; $found = $true; break }
    }
    if (-not $found) { $lines += "$key=$($values[$key])" }
}
[IO.File]::WriteAllLines((Resolve-Path -LiteralPath $EnvFile), $lines, (New-Object Text.UTF8Encoding($false)))

Write-Host "Updated DB_URL, DB_USERNAME and DB_PASSWORD in $EnvFile"
Write-Host "Database host: $dbHost   database: $database   (direct connection, sslmode=require)"
