@echo off
rem ============================================================
rem  OrgFlow - START EVERYTHING (one click)
rem
rem  1. starts the backend  (Spring Boot, port 8080) in a new window
rem  2. starts the frontend (Vite,       port 5173) in a new window
rem  3. waits until both answer, then opens the browser
rem
rem  Pure ASCII on purpose: cmd.exe decodes .bat files with the OEM code
rem  page (936/GBK here), so Chinese literals inside a .bat are unreliable.
rem  Everything below therefore uses ASCII paths only.
rem
rem  Folder layout used by this launcher:
rem    D:\orgflow-backend              junction -> real backend project
rem    D:\orgflow-web                  junction -> real frontend project
rem    orgflow-start-backend.bat       starts the backend  (next to this file)
rem    orgflow-start-frontend.bat      starts the frontend (next to this file)
rem ============================================================

title OrgFlow - one click launcher

rem Look for the two helper scripts next to THIS file first, then fall back to D:\
rem (%~dp0 is the folder this .bat lives in, with a trailing backslash)
set "BE=%~dp0orgflow-start-backend.bat"
set "FE=%~dp0orgflow-start-frontend.bat"
if not exist "%BE%" set "BE=D:\orgflow-start-backend.bat"
if not exist "%FE%" set "FE=D:\orgflow-start-frontend.bat"

echo ============================================================
echo   OrgFlow  -  company directory and schedule system
echo   One-click start
echo ============================================================
echo.

if not exist "%BE%" (
  echo [ERROR] Not found: backend launcher
  echo         Looked for: %~dp0orgflow-start-backend.bat
  echo                     D:\orgflow-start-backend.bat
  goto :end
)
if not exist "%FE%" (
  echo [ERROR] Not found: frontend launcher
  echo         Looked for: %~dp0orgflow-start-frontend.bat
  echo                     D:\orgflow-start-frontend.bat
  goto :end
)
if not exist "D:\orgflow-backend" (
  echo [ERROR] Missing junction D:\orgflow-backend
  echo         Recreate it with:  mklink /J D:\orgflow-backend "D:\...\orgflow-backend"
  goto :end
)
if not exist "D:\orgflow-web" (
  echo [ERROR] Missing junction D:\orgflow-web
  echo         Recreate it with:  mklink /J D:\orgflow-web "D:\...\orgflow-web"
  goto :end
)

rem ---- do not start a second copy when the port is already taken ----
set "BACKEND_RUNNING=0"
set "FRONTEND_RUNNING=0"
netstat -ano | findstr /r /c:":8080 .*LISTENING" >nul 2>&1
if not errorlevel 1 set "BACKEND_RUNNING=1"
netstat -ano | findstr /r /c:":5173 .*LISTENING" >nul 2>&1
if not errorlevel 1 set "FRONTEND_RUNNING=1"

if "%BACKEND_RUNNING%"=="1" (
  echo [1/3] Backend on 8080 is ALREADY RUNNING - skipping.
) else (
  echo [1/3] Starting backend ...
  start "OrgFlow Backend" "%BE%"
)

if "%FRONTEND_RUNNING%"=="1" (
  echo [2/3] Frontend on 5173 is ALREADY RUNNING - skipping.
) else (
  echo [2/3] Starting frontend ...
  start "OrgFlow Frontend" "%FE%"
)

echo [3/3] Waiting for both services to become ready ...
echo.

powershell -NoProfile -ExecutionPolicy Bypass -Command "$b=$false;$f=$false; for($i=0;$i -lt 90;$i++){ $b=$b -or [bool](Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue); $f=$f -or [bool](Get-NetTCPConnection -LocalPort 5173 -State Listen -ErrorAction SilentlyContinue); if($b -and $f){break}; Start-Sleep -Milliseconds 500 }; Write-Host ('   backend  8080 : ' + $(if($b){'READY'}else{'not ready yet'})); Write-Host ('   frontend 5173 : ' + $(if($f){'READY'}else{'not ready yet'})); if($b -and $f){ Start-Process 'http://localhost:5173'; Write-Host ''; Write-Host '   Browser opened: http://localhost:5173' } else { Write-Host ''; Write-Host '   Not ready yet - check the two new console windows for errors.' }"

echo.
echo ------------------------------------------------------------
echo  How to use
echo ------------------------------------------------------------
echo   Browser :  http://localhost:5173
echo   Accounts:  admin / 123456      ^(administrator^)
echo              zhangsan / 123456   ^(normal employee^)
echo.
echo   To STOP :  close the "OrgFlow Backend" and "OrgFlow Frontend"
echo              console windows ^(or press Ctrl+C in each^).
echo ------------------------------------------------------------
echo.

:end
echo Press any key to close this launcher window.
pause >nul
