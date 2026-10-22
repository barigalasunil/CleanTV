#!/usr/bin/env pwsh
# Puts the pictures from personal-assets into the launcher, ready to be built into the APK.
#
# Usage: tools/set-images.ps1 [wallpaper folder] [splash picture]
#
# With no arguments it uses personal-assets/wallpapers and the first picture in
# personal-assets/splash. Every picture is scaled down to at most 1920x1080 and written as a JPEG
# into launcher/app/src/main/assets; the originals are never changed. The launcher then shows the
# wallpapers one after the other, and the splash picture on the welcome screen.
$ErrorActionPreference = 'Stop'
Set-Location (Join-Path $PSScriptRoot '..')

$py = Join-Path $PSScriptRoot 'venv\Scripts\python.exe'
if (-not (Test-Path $py)) {
    Write-Output 'run: python -m venv tools/venv && tools\venv\Scripts\pip install -r tools\requirements.txt'
    exit 1
}

$wallDir = if ($args.Count -ge 1) { $args[0] } else { 'personal-assets\wallpapers' }
$splash = if ($args.Count -ge 2) { $args[1] } else { $null }

if (-not $splash) {
    $found = Get-ChildItem -Path 'personal-assets\splash' -File -ErrorAction SilentlyContinue |
        Where-Object { $_.Extension -match '^\.(jpg|jpeg|png|webp)$' } |
        Sort-Object Name | Select-Object -First 1
    if ($found) { $splash = $found.FullName }
}

if (-not (Test-Path $wallDir)) { Write-Output "no such folder: $wallDir"; exit 1 }

$argList = @('-I', 'tools\prepare-images.py', '--wallpapers', $wallDir, '--clean')
if ($splash) { $argList += @('--splash', $splash) }

& $py @argList
Write-Output 'Now build and install: launcher\build-and-install.ps1'
