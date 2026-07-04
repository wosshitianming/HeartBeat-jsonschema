param(
    [string]$BackendImage = "heartbeat:1.0.0",
    [string]$WebImage = "heartbeat-web:1.0.0",
    [string]$ClassFinalPassword = $env:CLASSFINAL_PASSWORD,
    [switch]$DisableWebObfuscation
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot

if ([string]::IsNullOrWhiteSpace($ClassFinalPassword)) {
    throw "ClassFinalPassword is required. Pass -ClassFinalPassword or set CLASSFINAL_PASSWORD."
}

Write-Host "Building protected backend image: $BackendImage"
docker build `
    --build-arg CLASSFINAL_PASSWORD="$ClassFinalPassword" `
    -t "$BackendImage" `
    -f "$repoRoot\Dockerfile" `
    "$repoRoot"

$webBuildArgs = @()
if ($DisableWebObfuscation) {
    $webBuildArgs += "--build-arg"
    $webBuildArgs += "VITE_DISABLE_OBFUSCATION=true"
}

Write-Host "Building web image: $WebImage"
docker build `
    @webBuildArgs `
    -t "$WebImage" `
    -f "$repoRoot\heartbeat-web\Dockerfile" `
    "$repoRoot\heartbeat-web"

Write-Host "Done."
