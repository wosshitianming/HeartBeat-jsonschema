@echo off
REM Pure-ASCII batch file - paths use %~dp0 relative resolution
REM Will be called from within the project directory

setlocal enabledelayedexpansion

cd /d "%~dp0.."

REM All paths are now resolved relative to project root
set "GEN_E=%CD%\heartbeat-infrastructure\src\main\java\top\kx\heartbeat\infrastructure\persistence\entity\gen"
set "PARENT_E=%CD%\heartbeat-infrastructure\src\main\java\top\kx\heartbeat\infrastructure\persistence\entity"
set "GEN_M=%CD%\heartbeat-infrastructure\src\main\java\top\kx\heartbeat\infrastructure\persistence\mapper\gen"
set "PARENT_M=%CD%\heartbeat-infrastructure\src\main\java\top\kx\heartbeat\infrastructure\persistence\mapper"

echo GEN_E=%GEN_E%
echo GEN_M=%GEN_M%

REM Test if gen exists
if not exist "%GEN_E%" (
    echo entity/gen NOT FOUND
    exit /b 1
)
if not exist "%GEN_M%" (
    echo mapper/gen NOT FOUND
    exit /b 1
)

echo === Create subdirs ===
for %%D in (auth flow workflow pay report mobile mp structure sys event tool common) do (
    if not exist "%PARENT_E%\%%D" mkdir "%PARENT_E%\%%D"
    if not exist "%PARENT_M%\%%D" mkdir "%PARENT_M%\%%D"
)

REM Use a helper VBScript for the file rename (handles UTF-8 properly)
echo === Move DO files ===
set DO_COUNT=0
for %%F in ("%GEN_E%\*.java") do (
    set "BASENAME=%%~nF"
    set "DOMAIN="
    REM Try matching prefixes
    echo !BASENAME! | findstr /b "FlowWaitState" > nul 2>&1 && set "DOMAIN=event"
    if not defined DOMAIN echo !BASENAME! | findstr /b "SysInbox" > nul 2>&1 && set "DOMAIN=event"
    if not defined DOMAIN echo !BASENAME! | findstr /b "SysOutbox" > nul 2>&1 && set "DOMAIN=event"
    if not defined DOMAIN echo !BASENAME! | findstr /b "Auth" > nul 2>&1 && set "DOMAIN=auth"
    if not defined DOMAIN echo !BASENAME! | findstr /b "Hb" > nul 2>&1 && set "DOMAIN=flow"
    if not defined DOMAIN echo !BASENAME! | findstr /b "Wf" > nul 2>&1 && set "DOMAIN=workflow"
    if not defined DOMAIN echo !BASENAME! | findstr /b "Pay" > nul 2>&1 && set "DOMAIN=pay"
    if not defined DOMAIN echo !BASENAME! | findstr /b "Report" > nul 2>&1 && set "DOMAIN=report"
    if not defined DOMAIN echo !BASENAME! | findstr /b "Mobile" > nul 2>&1 && set "DOMAIN=mobile"
    if not defined DOMAIN echo !BASENAME! | findstr /b "Mp" > nul 2>&1 && set "DOMAIN=mp"
    if not defined DOMAIN echo !BASENAME! | findstr /b "Structure" > nul 2>&1 && set "DOMAIN=structure"
    if not defined DOMAIN echo !BASENAME! | findstr /b "Sys" > nul 2>&1 && set "DOMAIN=sys"

    if defined DOMAIN (
        echo   %%~nxF -^> entity\!DOMAIN!\
        set /a DO_COUNT+=1
    ) else (
        echo   SKIP: %%~nxF
    )
)
echo DO total: %DO_COUNT%

echo === Done listing only (actual move not performed by this batch) ===
endlocal