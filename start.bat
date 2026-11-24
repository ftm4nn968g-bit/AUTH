@echo off
setlocal EnableDelayedExpansion
cd /d "%~dp0"
title DUKEN - start

rem --- Java izdeu: JAVA_HOME -> PATH -> %USERPROFILE%\.jdks ---
if not defined JAVA_HOME (
    where java >nul 2>&1
    if errorlevel 1 (
        for /d %%D in ("%USERPROFILE%\.jdks\*") do set "JAVA_HOME=%%~fD"
    )
)
if not defined JAVA_HOME (
    where java >nul 2>&1
    if errorlevel 1 (
        echo [!] Java tabylmady. JDK 17+ ornatynyz.
        pause
        exit /b 1
    )
)

rem --- Server ali iske qosylyp tur ma? ---
powershell -NoProfile -Command "if (Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue) { exit 0 } else { exit 1 }"
if not errorlevel 1 (
    echo Server ali zhumys istep tur: http://localhost:8080
    start "" http://localhost:8080
    exit /b 0
)

rem --- .env failynan ADMIN_PASSWORD t.b. oqu (.env repozitoriige tuspeidi) ---
if exist ".env" (
    for /f "usebackq eol=# tokens=1,* delims==" %%A in (".env") do (
        if not "%%B"=="" set "%%A=%%B"
    )
)

echo Server iske qosyluda... (alghashqy ret 1-2 minut)
start "DUKEN server" /d "%~dp0" cmd /k ""%~dp0mvnw.cmd" spring-boot:run"

rem --- Server dayin bolghansha kutu, sosyn brauzerdi ashu ---
set /a tries=0
:wait
set /a tries+=1
if !tries! gtr 180 (
    echo [!] Server 3 minut ishinde iske qosylmady. "DUKEN server" terezesin qaranyz.
    pause
    exit /b 1
)
powershell -NoProfile -Command "if (Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue) { exit 0 } else { exit 1 }"
if errorlevel 1 (
    ping -n 2 127.0.0.1 >nul
    goto wait
)

echo Dayin: http://localhost:8080
start "" http://localhost:8080
ping -n 3 127.0.0.1 >nul
exit /b 0
