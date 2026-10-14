@echo off
rem ============================================================
rem  OrgFlow - start FRONTEND (Vite dev server, port 5173)
rem  Just double-click this file.
rem
rem  Pure ASCII on purpose (cmd.exe decodes .bat with the OEM code page).
rem  This script lives in the project root (whose folder name is Chinese),
rem  so all filesystem work goes through the ASCII junction
rem  D:\orgflow-web instead.
rem ============================================================

title OrgFlow Frontend - port 5173

set "NODE_BIN=C:\Users\20837\.dsh\dsh-runtimes\dsh-primary-runtime\dependencies\node\bin"
set "NODE_EXE=%NODE_BIN%\node.exe"
set "PNPM_JS=C:\Users\20837\.dsh\dsh-runtimes\dsh-primary-runtime\dependencies\pnpm\bin\pnpm.mjs"
set "WEB_DIR=D:\orgflow-web"

echo ============================================================
echo   OrgFlow FRONTEND  ^|  company directory + schedule system
echo ============================================================
echo.

if not exist "%NODE_EXE%" (
  echo [ERROR] node.exe not found at %NODE_EXE%
  echo         Fix the NODE_BIN path in this file.
  goto :end
)

if not exist "%WEB_DIR%\package.json" (
  echo [ERROR] Frontend project not found: %WEB_DIR%\package.json
  echo         The NTFS junction may be missing. Recreate it with:
  echo           mklink /J D:\orgflow-web "D:\<your-project>\orgflow-web"
  goto :end
)

if not exist "%WEB_DIR%\node_modules" (
  echo [INFO] node_modules is missing - installing dependencies now.
  echo        This needs internet access and may take a few minutes.
  echo        Registry is set to registry.npmmirror.com in .npmrc
  echo.
  pushd "%WEB_DIR%"
  "%NODE_EXE%" "%PNPM_JS%" install
  popd
  echo.
)

rem esbuild's postinstall script calls a literal "node", so node must be on PATH.
set "PATH=%NODE_BIN%;%PATH%"

netstat -ano | findstr /r /c:":5173 .*LISTENING" >nul 2>&1
if not errorlevel 1 (
  echo [WARN] Port 5173 is already in use - the frontend may already be running.
  echo        If so, do NOT start it twice. Just open http://localhost:5173
  echo.
  pause
)

cd /d "%WEB_DIR%"

echo Starting frontend dev server ...
echo.
echo   Open in your browser:   http://localhost:5173
echo   IMPORTANT: use "localhost", NOT "127.0.0.1"
echo              ^(Vite listens on IPv6 ::1 on this machine^)
echo.
echo   The backend must be running too, or the pages will show errors.
echo   Keep this window OPEN while using the system.
echo   Press Ctrl+C or close the window to stop the frontend.
echo ------------------------------------------------------------
echo.

"%NODE_EXE%" "%PNPM_JS%" run dev

echo.
echo ------------------------------------------------------------
echo Frontend stopped. Exit code: %ERRORLEVEL%
echo ------------------------------------------------------------

:end
echo.
echo Press any key to close this window.
pause >nul
