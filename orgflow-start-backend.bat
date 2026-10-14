@echo off
rem ============================================================
rem  OrgFlow - start BACKEND (Spring Boot, port 8080)
rem  Just double-click this file.
rem
rem  Pure ASCII on purpose: cmd.exe decodes .bat files with the OEM
rem  code page (936/GBK on this machine), so Chinese literals inside a
rem  batch file are unreliable. This script lives in the project root
rem  (whose folder name is Chinese), so all filesystem work goes through
rem  the ASCII junction D:\orgflow-backend instead.
rem ============================================================

title OrgFlow Backend - port 8080

set "JAVA_EXE=D:\software\java 17\bin\java.exe"
set "PROJ_DIR=D:\orgflow-backend"
set "JAR_FILE=%PROJ_DIR%\target\orgflow-backend.jar"

echo ============================================================
echo   OrgFlow BACKEND  ^|  company directory + schedule system
echo ============================================================
echo.

if not exist "%JAVA_EXE%" (
  echo [ERROR] JDK 17 not found at %JAVA_EXE%
  echo         Fix the JAVA_EXE path in this file.
  goto :end
)

if not exist "%PROJ_DIR%" (
  echo [ERROR] Project folder not found: %PROJ_DIR%
  echo         The NTFS junction is missing. Recreate it with:
  echo           mklink /J D:\orgflow-backend "D:\<your-project>\orgflow-backend"
  goto :end
)

if not exist "%JAR_FILE%" (
  echo [ERROR] Backend jar not found: %JAR_FILE%
  echo.
  echo         Build it first ^(one time, needs internet^):
  echo           cd /d "%PROJ_DIR%"
  echo           set JAVA_HOME=D:\software\java 17
  echo           "D:\IntelliJ IDEA 2025.1\plugins\maven\lib\maven3\bin\mvn.cmd" -B -DskipTests clean package
  goto :end
)

netstat -ano | findstr /r /c:":8080 .*LISTENING" >nul 2>&1
if not errorlevel 1 (
  echo [WARN] Port 8080 is already in use - the backend may already be running.
  echo        If so, do NOT start it twice. Just open http://localhost:5173
  echo.
  pause
)

echo Starting backend on http://localhost:8080 ...
echo Wait until you see: "Started OrgflowBackendApplication in x.x seconds"
echo.
echo   Demo accounts:  admin / 123456      ^(administrator^)
echo                   zhangsan / 123456   ^(normal employee^)
echo.
echo Keep this window OPEN while using the system.
echo Press Ctrl+C or close the window to stop the backend.
echo ------------------------------------------------------------
echo.

cd /d "%PROJ_DIR%"
"%JAVA_EXE%" -jar "%JAR_FILE%"

echo.
echo ------------------------------------------------------------
echo Backend stopped. Exit code: %ERRORLEVEL%
echo ------------------------------------------------------------

:end
echo.
echo Press any key to close this window.
pause >nul
