@echo off
cd /d "%~dp0"
title DUKEN - stop

rem --- 8080 portyndaghy serverdi toqtatu ---
powershell -NoProfile -Command "$c = Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue; if ($c) { $c | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue }; Write-Host 'Server toqtatyldy.' } else { Write-Host 'Server iske qosylmaghan.' }"

rem --- "DUKEN server" terezesin jabu ---
taskkill /fi "WINDOWTITLE eq DUKEN server*" /t /f >nul 2>&1

ping -n 3 127.0.0.1 >nul
