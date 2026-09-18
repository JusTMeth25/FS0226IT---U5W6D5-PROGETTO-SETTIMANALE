@echo off
setlocal EnableExtensions
cd /d "%~dp0"

echo.
echo  Zaffiro - Mini WhatsApp
echo  ======================
echo.

rem ---- tools ----
where java >nul 2>&1 || (echo  [X] Java not found on PATH. Install a JDK 25 and try again. & goto :fail)
where npm  >nul 2>&1 || (echo  [X] npm not found on PATH. Install Node.js and try again. & goto :fail)

rem ---- database credentials ----
rem Read from .env when present. That file is git-ignored, so no password is committed.
if exist ".env" (
  for /f "usebackq eol=# tokens=1,* delims==" %%a in (".env") do (
    if /i "%%a"=="DB_USERNAME" set "DB_USERNAME=%%b"
    if /i "%%a"=="DB_PASSWORD" set "DB_PASSWORD=%%b"
  )
)

if "%DB_USERNAME%"=="" set "DB_USERNAME=postgres"

if "%DB_PASSWORD%"=="" (
  echo  PostgreSQL password for user "%DB_USERNAME%".
  echo  Put DB_PASSWORD in a .env file next to this script to skip this prompt.
  set /p "DB_PASSWORD=  Password: "
)

if "%DB_PASSWORD%"=="" (echo  [X] No password given, the backend cannot start. & goto :fail)

rem ---- frontend dependencies ----
if not exist "FE\node_modules" (
  echo  [1/3] Installing frontend dependencies, this only happens once...
  pushd FE
  call npm install || (popd & echo  [X] npm install failed. & goto :fail)
  popd
) else (
  echo  [1/3] Frontend dependencies already installed.
)

rem ---- both servers, one window each ----
rem .\ before mvnw.cmd: cmd does not always look in the working directory.
echo  [2/3] Starting the backend on http://localhost:8080 ...
start "Zaffiro BE" /D "%~dp0BE" cmd /k .\mvnw.cmd spring-boot:run

echo  [3/3] Starting the frontend on http://localhost:5173 ...
start "Zaffiro FE" /D "%~dp0FE" cmd /k npm run dev

rem ---- wait for both, then open a browser ----
rem The backend answers 401 without a session, which is enough to know it is up.
echo.
echo  Waiting for both servers...
set "BE_UP="
set "FE_UP="
for /l %%i in (1,1,120) do (
  if not defined BE_UP curl -s -o nul --max-time 2 http://localhost:8080/api/users >nul 2>&1 && set "BE_UP=1"
  if not defined FE_UP curl -s -o nul --max-time 2 http://localhost:5173 >nul 2>&1 && set "FE_UP=1"
  if defined BE_UP if defined FE_UP goto :ready
  ping -n 2 127.0.0.1 >nul
)

if not defined BE_UP echo  [!] The backend did not answer. Check the "Zaffiro BE" window, and that PostgreSQL is running with a "U5W6D5-PROGETTO-SETTIMANALE" database.
if not defined FE_UP echo  [!] The frontend did not answer. Check the "Zaffiro FE" window.
goto :done

:ready
echo  Both are up. Opening http://localhost:5173
start "" http://localhost:5173

:done
echo.
echo  Each server runs in its own window. Close those windows to stop them.
echo.
pause
exit /b 0

:fail
echo.
pause
exit /b 1
