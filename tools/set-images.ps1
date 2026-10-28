#!/usr/bin/env pwsh
# Builds the pushed theme from the owner's pictures and, when a TV is given, uploads it.
#
# Usage: tools/set-images.ps1 [-Wallpapers <folder>] [-Splash <picture>] [-Avatar <picture>]
#                             [-TvHost <ip>] [-PrepareOnly]
#
# With no arguments it reads personal-assets/wallpapers, personal-assets/splash and
# personal-assets/avatar.*, writes personal-assets/theme, and uploads it when TV_HOST (or -TvHost)
# is set. The originals are never changed and nothing is built into the APK.
param(
    [Parameter(Position = 0)][string]$Wallpapers = 'personal-assets\wallpapers',
    [Parameter(Position = 1)][string]$Splash = '',
    [string]$Avatar = '',
    [string]$TvHost = $env:TV_HOST,
    [switch]$PrepareOnly
)
$ErrorActionPreference = 'Stop'
Set-Location (Join-Path $PSScriptRoot '..')

$py = Join-Path $PSScriptRoot 'venv\Scripts\python.exe'
if (-not (Test-Path $py)) {
    Write-Output 'run: python -m venv tools/venv && tools\venv\Scripts\pip install -r tools\requirements.txt'
    exit 1
}

if (-not $Splash) {
    $found = Get-ChildItem -Path 'personal-assets\splash' -File -ErrorAction SilentlyContinue |
        Where-Object { $_.Extension -match '^\.(jpg|jpeg|png|webp|bmp)$' } |
        Sort-Object Name | Select-Object -First 1
    if ($found) { $Splash = $found.FullName }
}

$arguments = @('-I', 'tools\wallpapers.py', '--wallpapers', $Wallpapers)
if ($Splash) { $arguments += @('--splash', $Splash) }
if ($Avatar) { $arguments += @('--avatar', $Avatar) }
& $py @arguments
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

if ($PrepareOnly) {
    Write-Output 'Prepared personal-assets\theme. Push it with: tools\theme.ps1 -TvHost <tv ip> personal-assets\theme'
    exit 0
}
if (-not $TvHost) {
    Write-Output 'Prepared personal-assets\theme. Set TV_HOST or pass -TvHost to upload it now.'
    exit 0
}

$env:TV_HOST = $TvHost
& $py -I tools\theme_push.py --source personal-assets\theme
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
Write-Output 'Pushed. Uninstalling com.gathin.tvhome deletes the pushed theme; keep personal-assets/theme as the master copy.'
