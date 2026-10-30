# Windows version of theme.sh: push a theme folder to the launcher on the TV and restart Home.
# A theme is a folder with config.json and optionally avatar.jpg, splash.jpg, wallpapers/,
# thumbs/ and wallpapers.json. See themes/README.md for the keys and the folder layout.
#
# Usage: tools\theme.ps1 -TvHost 192.168.1.50 personal-assets\theme
#        tools\theme.ps1 -TvHost 192.168.1.50 -Reset
param(
    [Parameter(Position = 0)][string]$Theme,
    [switch]$Reset,
    [string]$TvHost = $env:TV_HOST
)
$ErrorActionPreference = 'Stop'
$OutputEncoding = New-Object System.Text.UTF8Encoding $false
if (-not $TvHost) { throw 'pass -TvHost <tv ip> or set $env:TV_HOST' }
$env:TV_HOST = $TvHost
# Resolve the theme folder against the caller's directory before moving into tools\.
if ($Theme) { $Theme = (Resolve-Path $Theme).Path }
Set-Location $PSScriptRoot
$py = 'venv\Scripts\python.exe'
if (-not (Test-Path $py)) { throw 'run: python -m venv tools\venv; tools\venv\Scripts\python.exe -m pip install -r tools\requirements.txt' }

$arguments = @('-I', 'theme_push.py')
if ($Reset) { $arguments += '--reset' }
else {
    if (-not $Theme) { throw 'usage: theme.ps1 <theme folder> | -Reset' }
    $arguments += @('--source', $Theme)
}
& $py @arguments
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
if (-not $Reset) {
    Write-Host "Theme $(Split-Path $Theme -Leaf) is on the TV. Undo with: tools\theme.ps1 -TvHost $TvHost -Reset"
}
