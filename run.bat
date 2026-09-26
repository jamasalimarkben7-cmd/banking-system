@echo off
setlocal enabledelayedexpansion

:: ============================================================
:: Banking System - Windows Runner (run.bat)
:: Self-contained: downloads Maven wrapper, runs Spring Boot
:: ============================================================

title Banking System - Spring Boot Runner

echo.
echo ============================================================
echo  Banking System - Spring Boot Application
echo ============================================================
echo.

:: ------------------------------------------------------------
:: 1. Locate Java (prefer JAVA_HOME, fallback to 'java' in PATH)
:: ------------------------------------------------------------
set "JAVA_CMD="
if defined JAVA_HOME (
    if exist "%JAVA_HOME%\bin\java.exe" (
        set "JAVA_CMD=%JAVA_HOME%\bin\java.exe"
        echo [INFO] Using Java from JAVA_HOME: %JAVA_HOME%
    )
)
if not defined JAVA_CMD (
    where java >nul 2>&1
    if !errorlevel! equ 0 (
        for /f "tokens=*" %%a in ('where java') do set "JAVA_CMD=%%a"
        echo [INFO] Using Java from PATH: !JAVA_CMD!
    )
)
if not defined JAVA_CMD (
    echo [ERROR] Java not found!
    echo.
    echo Please install Java 25:
    echo   winget install --id EclipseAdoptium.Temurin.25.JDK --exact --source winget
    echo.
    echo Then close and reopen this terminal.
    pause
    exit /b 1
)

:: Verify Java version
for /f "tokens=3" %%v in ('"%JAVA_CMD%" -version 2^>^&1 ^| findstr /i "version"') do set "JAVA_VER=%%v"
echo [INFO] Java version: !JAVA_VER!

:: ------------------------------------------------------------
:: 2. Project directory & wrapper setup
:: ------------------------------------------------------------
set "PROJECT_DIR=%~dp0"
if "%PROJECT_DIR:~-1%"=="\" set "PROJECT_DIR=%PROJECT_DIR:~0,-1%"

set "WRAPPER_DIR=%PROJECT_DIR%\.mvn\wrapper"
set "WRAPPER_JAR=%WRAPPER_DIR%\maven-wrapper.jar"
set "WRAPPER_URL=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar"

if not exist "%WRAPPER_DIR%" mkdir "%WRAPPER_DIR%"

:: ------------------------------------------------------------
:: 3. Download maven-wrapper.jar if missing
:: ------------------------------------------------------------
if not exist "%WRAPPER_JAR%" (
    echo [INFO] Downloading Maven wrapper...
    powershell -NoProfile -Command ^
        "try { Invoke-WebRequest -Uri '%WRAPPER_URL%' -OutFile '%WRAPPER_JAR%' -ErrorAction Stop; Write-Host '[INFO] Download complete.' } catch { Write-Error $_; exit 1 }"
    if errorlevel 1 (
        echo [ERROR] Failed to download maven-wrapper.jar
        echo Check internet connection or download manually from:
        echo %WRAPPER_URL%
        pause
        exit /b 1
    )
)

:: ------------------------------------------------------------
:: 4. Database environment variables (set defaults if not provided)
:: ------------------------------------------------------------
:: Default to H2 in-memory database for zero-config development
if not defined DB_USERNAME set "DB_USERNAME=sa"
if not defined DB_PASSWORD set "DB_PASSWORD="
if not defined DB_URL set "DB_URL=jdbc:h2:mem:banking_system;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
if not defined DB_DRIVER set "DB_DRIVER=org.h2.Driver"

echo [INFO] Database: %DB_URL%
echo [INFO] User: %DB_USERNAME%
echo [INFO] Driver: %DB_DRIVER%

:: ------------------------------------------------------------
:: 5. Run Spring Boot with Maven Wrapper
:: ------------------------------------------------------------
echo.
echo [INFO] Starting Spring Boot application...
echo [INFO] Press Ctrl+C to stop
echo.

:: Critical: -Dmaven.multiModuleProjectDirectory must be set
"%JAVA_CMD%" ^
    -Dmaven.multiModuleProjectDirectory="%PROJECT_DIR%" ^
    -cp "%WRAPPER_JAR%" ^
    org.apache.maven.wrapper.MavenWrapperMain ^
    spring-boot:run

:: ------------------------------------------------------------
:: 6. Exit handling
:: ------------------------------------------------------------
echo.
if errorlevel 1 (
    echo [ERROR] Application exited with code %errorlevel%
) else (
    echo [INFO] Application stopped.
)
pause