param(
    [string]$BackendImage = "heartbeat:1.0.0",
    [string]$WebImage = "heartbeat-web:1.0.0",
    [string]$ClassFinalPassword = $env:CLASSFINAL_PASSWORD,
    [switch]$DisableWebObfuscation,
    [switch]$EnableWebObfuscation
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot

if ([string]::IsNullOrWhiteSpace($ClassFinalPassword)) {
    throw "ClassFinalPassword is required. Pass -ClassFinalPassword or set CLASSFINAL_PASSWORD."
}

if ($EnableWebObfuscation) {
    throw "Web obfuscation is temporarily unavailable because it breaks Vite lazy-loaded chunks. Build without -EnableWebObfuscation."
}

Write-Host "Building protected backend image: $BackendImage"
docker build `
    --build-arg CLASSFINAL_PASSWORD="$ClassFinalPassword" `
    -t "$BackendImage" `
    -f "$repoRoot\Dockerfile" `
    "$repoRoot"

$webBuildArgs = @(
    "--build-arg",
    "VITE_ENABLE_OBFUSCATION=false",
    "--build-arg",
    "VITE_DISABLE_OBFUSCATION=true"
)
if ($DisableWebObfuscation) {
    Write-Warning "-DisableWebObfuscation is no longer required; web obfuscation is disabled by default."
}

Write-Host "Building web image: $WebImage"
docker build `
    @webBuildArgs `
    -t "$WebImage" `
    -f "$repoRoot\heartbeat-web\Dockerfile" `
    "$repoRoot\heartbeat-web"

Write-Host "Done."
