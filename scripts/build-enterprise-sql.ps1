$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent $PSScriptRoot
$migrationDir = Join-Path $repoRoot 'heartbeat-start/src/main/resources/db/migration/mysql'
$outputPath = Join-Path $repoRoot 'heartbeat-start/src/main/resources/db/mysql/heartbeat-enterprise-all.sql'

$migrations = Get-ChildItem -Path $migrationDir -Filter 'V*.sql' |
    Sort-Object {
        if ($_.Name -match '^V(\d+)__') {
            [int]$Matches[1]
        } else {
            9999
        }
    }

$builder = New-Object System.Text.StringBuilder
[void]$builder.AppendLine('-- Generated from Flyway migrations. Do not edit manually.')
[void]$builder.AppendLine("SET NAMES utf8mb4;")
[void]$builder.AppendLine("SET time_zone = '+00:00';")
[void]$builder.AppendLine()

foreach ($migration in $migrations) {
    [void]$builder.AppendLine('-- =================================================================')
    [void]$builder.AppendLine('-- ' + $migration.Name)
    [void]$builder.AppendLine('-- =================================================================')
    [void]$builder.AppendLine((Get-Content -Path $migration.FullName -Raw -Encoding UTF8).Trim())
    [void]$builder.AppendLine()
}

$encoding = New-Object System.Text.UTF8Encoding($false)
[System.IO.File]::WriteAllText($outputPath, $builder.ToString(), $encoding)
Write-Host "Generated $outputPath"
