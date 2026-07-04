$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

# 用 [char[]] 数组构造路径,避免 PowerShell 字符串解析问题
$pathChars = [char[]]@(
    'D', ':', '\', 'D', 'e', 's', 'k', 't', 'o', 'p', '\',
    [char]0x81EA, [char]0x5B9A, [char]0x4E49, [char]0x6587, [char]0x4EF6, [char]0x5939, '\',
    'H', 'e', 'a', 'r', 't', 'B', 'e', 'a', 't', '-', 'j', 's', 'o', 'n', 's', 'c', 'h', 'e', 'm', 'a'
)
$baseDir = -join $pathChars
Write-Host "BASE: $baseDir"

if (-not (Test-Path -LiteralPath $baseDir)) {
    Write-Host "BASE 不存在!" -ForegroundColor Red
    exit 1
}

Set-Location -LiteralPath $baseDir
Write-Host "PWD: $(Get-Location)"

$genE = Join-Path $baseDir "heartbeat-infrastructure\src\main\java\top\kx\heartbeat\infrastructure\persistence\entity\gen"
$parentE = Join-Path $baseDir "heartbeat-infrastructure\src\main\java\top\kx\heartbeat\infrastructure\persistence\entity"
$genM = Join-Path $baseDir "heartbeat-infrastructure\src\main\java\top\kx\heartbeat\infrastructure\persistence\mapper\gen"
$parentM = Join-Path $baseDir "heartbeat-infrastructure\src\main\java\top\kx\heartbeat\infrastructure\persistence\mapper"

Write-Host "entity/gen: $genE"
Write-Host "mapper/gen: $genM"

function Get-DomainForDO([string]$name) {
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
Write-Host "=== 创建子包目录 ===" -ForegroundColor Cyan
$allDomains = @("auth", "flow", "workflow", "pay", "report", "mobile", "mp", "structure", "sys", "event", "tool", "common")
foreach ($d in $allDomains) {
    $targetE = Join-Path $parentE $d
    $targetM = Join-Path $parentM $d
    if (-not (Test-Path -LiteralPath $targetE)) {
        New-Item -ItemType Directory -Path $targetE -Force | Out-Null
    }
    if (-not (Test-Path -LiteralPath $targetM)) {
        New-Item -ItemType Directory -Path $targetM -Force | Out-Null
    }
}
Write-Host "  子包目录创建完成"

Write-Host ""
Write-Host "=== 移动并重写 DO 文件 ===" -ForegroundColor Cyan
$genFiles = Get-ChildItem -LiteralPath $genE -File -ErrorAction Stop
Write-Host "找到 $($genFiles.Count) 个 DO 文件"
$countE = 0
foreach ($f in $genFiles) {
    $baseName = [System.IO.Path]::GetFileNameWithoutExtension($f.Name)
    $domain = Get-DomainForDO $baseName
    if (-not $domain) {
        Write-Host "  未匹配: $($f.Name)" -ForegroundColor Yellow
        continue
    }
    $targetDir = Join-Path $parentE $domain
    $targetFile = Join-Path $targetDir $f.Name

    $content = [System.IO.File]::ReadAllText($f.FullName, [System.Text.Encoding]::UTF8)
    $oldPkg = "package top.kx.heartbeat.infrastructure.persistence.entity.gen;"
    $newPkg = "package top.kx.heartbeat.infrastructure.persistence.entity.$domain;"
    $content = $content.Replace($oldPkg, $newPkg)

    [System.IO.File]::WriteAllText($targetFile, $content, (New-Object System.Text.UTF8Encoding $false))
    [System.IO.File]::Delete($f.FullName)
    $countE++
    Write-Host "  $($f.Name) -> entity/$domain/"
}
Write-Host "移动了 $countE 个 DO 文件"

Write-Host ""
Write-Host "=== 移动并重写 Mapper 文件 ===" -ForegroundColor Cyan
$mapperFiles = Get-ChildItem -LiteralPath $genM -File -ErrorAction Stop
Write-Host "找到 $($mapperFiles.Count) 个 Mapper 文件"
$countM = 0
foreach ($f in $mapperFiles) {
    $baseName = [System.IO.Path]::GetFileNameWithoutExtension($f.Name)
    $domain = Get-DomainForDO $baseName
    if (-not $domain) {
        Write-Host "  未匹配: $($f.Name)" -ForegroundColor Yellow
        continue
    }
    $targetDir = Join-Path $parentM $domain
    $targetFile = Join-Path $targetDir $f.Name

    $content = [System.IO.File]::ReadAllText($f.FullName, [System.Text.Encoding]::UTF8)
    $oldPkg = "package top.kx.heartbeat.infrastructure.persistence.mapper.gen;"
    $newPkg = "package top.kx.heartbeat.infrastructure.persistence.mapper.$domain;"
    $content = $content.Replace($oldPkg, $newPkg)

    $oldEntityPrefix = "top.kx.heartbeat.infrastructure.persistence.entity.gen."
    $newEntityPrefix = "top.kx.heartbeat.infrastructure.persistence.entity.$domain."
    $content = $content.Replace($oldEntityPrefix, $newEntityPrefix)

    [System.IO.File]::WriteAllText($targetFile, $content, (New-Object System.Text.UTF8Encoding $false))
    [System.IO.File]::Delete($f.FullName)
    $countM++
    Write-Host "  $($f.Name) -> mapper/$domain/"
}
Write-Host "移动了 $countM 个 Mapper 文件"

Write-Host ""
Write-Host "=== 删除空的 gen 目录 ===" -ForegroundColor Cyan
[System.IO.Directory]::Delete($genE, $true)
[System.IO.Directory]::Delete($genM, $true)
Write-Host "  删除完成"

Write-Host ""
Write-Host "=== 完成 ===" -ForegroundColor Green