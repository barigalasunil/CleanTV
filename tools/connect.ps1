#!/usr/bin/env pwsh
# Connect to the TV over ADB and wait until it is really there.
#
# Usage: tools/connect.ps1 [host]        (default: $env:TV_HOST)
#
# It never waits forever: the wait stops after 60 seconds and the script says what is wrong
# (no answer, refused, unauthorised, or more than one device). The TV has to have
# "ADB debugging" switched on in its developer options, and its IP can change when it
# reconnects to the network: find the new one under Settings - Network.
$ErrorActionPreference = 'Stop'
Set-Location (Join-Path $PSScriptRoot '..')

$host1 = if ($args.Count -ge 1) { $args[0] } else { $env:TV_HOST }
if (-not $host1) { Write-Output 'usage: connect.ps1 <TV IP>   (or set TV_HOST)'; exit 2 }
$port = if ($env:TV_PORT) { $env:TV_PORT } else { '5555' }
$serial = "$host1`:$port"
$limit = 60      # seconds to wait in total
$poll = 2         # seconds between tries
$adb = if ($env:ADB) { $env:ADB } else { 'adb' }

if (-not (Get-Command $adb -ErrorAction SilentlyContinue)) {
    Write-Output 'adb not found: install the Android platform-tools'
    exit 3
}

function Test-Device([string]$target) {
    $line = (& $adb devices 2>$null) | Where-Object { $_ -match "^$([regex]::Escape($target))\s" } | Select-Object -First 1
    if (-not $line) { return $null }
    if ($line -match '\sdevice$') { return 'device' }
    if ($line -match '\bunauthorized$') { return 'unauthorized' }
    if ($line -match '\boffline$') { return 'offline' }
    return 'unknown'
}

$state = Test-Device $serial
if ($state -eq 'device') { Write-Output "connected: $serial"; exit 0 }
if ($state -eq 'unauthorized') {
    Write-Output "$serial is unauthorised: open the TV and accept the 'Allow USB debugging?' prompt."
    Write-Output "If no prompt appears, revoke the USB debugging authorisation in the TV's developer options and reconnect."
    exit 4
}
if ($state -eq 'offline') {
    Write-Output "$serial is offline: the TV is connected but not answering. Try again in a moment."
    exit 5
}

Write-Output "connecting to $serial ..."
$start = Get-Date
$tries = 0
while ($true) {
    & $adb connect $serial *> $null
    if ((Test-Device $serial) -eq 'device') { Write-Output "connected: $serial"; exit 0 }
    $tries++
    $elapsed = [int]((Get-Date) - $start).TotalSeconds
    if ($elapsed -ge $limit) {
        Write-Output "gave up after ${limit}s: $serial did not come up."
        Write-Output 'Check that the TV is on the same network, that ADB debugging is on, and that the IP is right.'
        exit 6
    }
    if ($tries % 5 -eq 1) { Write-Output "  still trying (${elapsed}s of ${limit}s) ..." }
    Start-Sleep -Seconds $poll
}
