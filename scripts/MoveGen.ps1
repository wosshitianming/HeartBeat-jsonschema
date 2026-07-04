# MoveGen.ps1 - Pure ASCII, no Chinese in source
$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

# Build base dir using char codes
$chars = @(68,58,92,68,101,115,107,116,111,112,92,12958,23450,20041,25991,20214,22806,92,72,101,97,114,116,66,101,97,116,45,106,115,111,110,115,99,104,101,109,97)
$baseDir = -join ($chars | ForEach-Object { [char]$_ })

if (-not (Test-Path -LiteralPath $baseDir)) {
    Write-Host "Base dir not found: $baseDir" -ForegroundColor Red
    exit 1
}
Write-Host "Base: $baseDir"

$genE = Join-Path $baseDir "heartbeat-infrastructure\src\main\java\top\kx\heartbeat\infrastructure\persistence\entity\gen"
$parentE = Join-Path $baseDir "heartbeat-infrastructure\src\main\java\top\kx\heartbeat\infrastructure\persistence\entity"
$genM = Join-Path $baseDir "heartbeat-infrastructure\src\main\java\top\kx\heartbeat\infrastructure\persistence\mapper\gen"
$parentM = Join-Path $baseDir "heartbeat-infrastructure\src\main\java\top\kx\heartbeat\infrastructure\persistence\mapper"

Write-Host "gen entity: $genE"
Write-Host "gen mapper: $genM"

function Get-Domain([string]$name) {
    if ($name -like "FlowWaitState*") { return "event" }
    elseif ($name -like "SysInbox*") { return "event" }
    elseif ($name -like "SysOutbox*") { return "event" }
    elseif ($name -like "Auth*") { return "auth" }
    elseif ($name -like "Hb*") { return "flow" }
    elseif ($name -like "Wf*") { return "workflow" }
    elseif ($name -like "Pay*") { return "pay" }
    elseif ($name -like "Report*") { return "report" }
    elseif ($name -like "Mobile*") { return "mobile" }
    elseif ($name -like "Mp*") { return "mp" }
    elseif ($name -like "Structure*") { return "structure" }
    elseif ($name -like "Sys*") { return "sys" }
    else { return $null }
}

Write-Host ""
Write-Host "=== Create subdirs ===" -ForegroundColor Cyan
$domains = @("auth","flow","workflow","pay","report","mobile","mp","structure","sys","event","tool","common")
foreach ($d in $domains) {
    $te = Join-Path $parentE $d
    $tm = Join-Path $parentM $d
    if (-not (Test-Path -LiteralPath $te)) { New-Item -ItemType Directory -Path $te -Force | Out-Null }
    if (-not (Test-Path -LiteralPath $tm)) { New-Item -ItemType Directory -Path $tm -Force | Out-Null }
}

Write-Host ""
Write-Host "=== Move DO files ===" -ForegroundColor Cyan
$filesE = Get-ChildItem -LiteralPath $genE -File -ErrorAction Stop
Write-Host "Found $($filesE.Count) DO files"
$cnt = 0
foreach ($f in $filesE) {
    $bn = [System.IO.Path]::GetFileNameWithoutExtension($f.Name)
    $dom = Get-Domain $bn
    if (-not $dom) { Write-Host "  Skip: $($f.Name)" -ForegroundColor Yellow; continue }
    $td = Join-Path $parentE $dom
    $tf = Join-Path $td $f.Name
    $content = [System.IO.File]::ReadAllText($f.FullName, [System.Text.Encoding]::UTF8)
    $content = $content.Replace(
        "package top.kx.heartbeat.infrastructure.persistence.entity.gen;",
        "package top.kx.heartbeat.infrastructure.persistence.entity.$dom;"
    )
    [System.IO.File]::WriteAllText($tf, $content, (New-Object System.Text.UTF8Encoding $false))
    [System.IO.File]::Delete($f.FullName)
    $cnt++
    Write-Host "  $($f.Name) -> entity/$dom/"
}
Write-Host "Moved $cnt DO files"

Write-Host ""
Write-Host "=== Move Mapper files ===" -ForegroundColor Cyan
$filesM = Get-ChildItem -LiteralPath $genM -File -ErrorAction Stop
Write-Host "Found $($filesM.Count) Mapper files"
$cnt2 = 0
foreach ($f in $filesM) {
    $bn = [System.IO.Path]::GetFileNameWithoutExtension($f.Name)
    $dom = Get-Domain $bn
    if (-not $dom) { Write-Host "  Skip: $($f.Name)" -ForegroundColor Yellow; continue }
    $td = Join-Path $parentM $dom
    $tf = Join-Path $td $f.Name
    $content = [System.IO.File]::ReadAllText($f.FullName, [System.Text.Encoding]::UTF8)
    $content = $content.Replace(
        "package top.kx.heartbeat.infrastructure.persistence.mapper.gen;",
        "package top.kx.heartbeat.infrastructure.persistence.mapper.$dom;"
    )
    $content = $content.Replace(
        "top.kx.heartbeat.infrastructure.persistence.entity.gen.",
        "top.kx.heartbeat.infrastructure.persistence.entity.$dom."
    )
    [System.IO.File]::WriteAllText($tf, $content, (New-Object System.Text.UTF8Encoding $false))
    [System.IO.File]::Delete($f.FullName)
    $cnt2++
    Write-Host "  $($f.Name) -> mapper/$dom/"
}
Write-Host "Moved $cnt2 Mapper files"

Write-Host ""
Write-Host "=== Delete gen dirs ===" -ForegroundColor Cyan
[System.IO.Directory]::Delete($genE, $true)
[System.IO.Directory]::Delete($genM, $true)
Write-Host "  Done"

Write-Host ""
Write-Host "=== DONE ===" -ForegroundColor Green