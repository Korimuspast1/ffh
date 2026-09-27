@echo off
setlocal
set APP_HOME=%~dp0
where gradle >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
  echo Install Gradle 8.10.2 or run the Unix gradlew script in a shell with curl and unzip available.
  exit /b 1
)
gradle %*
