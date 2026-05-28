@echo off
chcp 65001 >nul
title Custom Pack Config - Build All

setlocal enabledelayedexpansion

set PROJECT_DIR=%~dp0
set JAVA17=C:\Program Files\Java\jdk-17
set JAVA21=D:\Program Files\Java\zulu21.32.17-ca-jdk21.0.2-win_x64
set JAVA25=%PROJECT_DIR%.gradle-tools\jdk-25.0.4+2

echo ============================================
echo   Custom Pack Config - Batch Build
echo ============================================
echo.

rem ---- forge-1.20.1 ----
echo [1/4] Building forge-1.20.1...
cd /d "%PROJECT_DIR%forge-1.20.1"
set "JAVA_HOME=%JAVA17%"
call gradlew clean build --no-daemon -x test 2>&1
if %errorlevel% neq 0 (
    echo [!] forge-1.20.1 BUILD FAILED
    set BUILD_FAILED=1
) else (
    echo [OK] forge-1.20.1 BUILD SUCCESS
)
echo.

rem ---- forge-1.20.4 ----
echo [2/4] Building forge-1.20.4...
cd /d "%PROJECT_DIR%forge-1.20.4"
set "JAVA_HOME=%JAVA17%"
call gradlew clean build --no-daemon -x test 2>&1
if %errorlevel% neq 0 (
    echo [!] forge-1.20.4 BUILD FAILED
    set BUILD_FAILED=1
) else (
    echo [OK] forge-1.20.4 BUILD SUCCESS
)
echo.

rem ---- forge-1.21.1 ----
echo [3/4] Building forge-1.21.1...
cd /d "%PROJECT_DIR%forge-1.21.1"
set "JAVA_HOME=%JAVA21%"
set "JAVA25_HOME=%JAVA25%"
call gradlew clean build --no-daemon -x test 2>&1
if %errorlevel% neq 0 (
    echo [!] forge-1.21.1 BUILD FAILED
    set BUILD_FAILED=1
) else (
    echo [OK] forge-1.21.1 BUILD SUCCESS
)
echo.

rem ---- forge-1.21.4 ----
echo [4/5] Building forge-1.21.4...
cd /d "%PROJECT_DIR%forge-1.21.4"
set "JAVA_HOME=%JAVA21%"
set "JAVA25_HOME=%JAVA25%"
call gradlew clean build --no-daemon -x test 2>&1
if %errorlevel% neq 0 (
    echo [!] forge-1.21.4 BUILD FAILED
    set BUILD_FAILED=1
) else (
    echo [OK] forge-1.21.4 BUILD SUCCESS
)
echo.

rem ---- forge-1.21.10 ----
echo [5/5] Building forge-1.21.10...
cd /d "%PROJECT_DIR%forge-1.21.10"
set "JAVA_HOME=%JAVA21%"
set "JAVA25_HOME=%JAVA25%"
call gradlew clean build --no-daemon -x test 2>&1
if %errorlevel% neq 0 (
    echo [!] forge-1.21.10 BUILD FAILED
    set BUILD_FAILED=1
) else (
    echo [OK] forge-1.21.10 BUILD SUCCESS
)
echo.

rem ---- Summary ----
echo ============================================
if "%BUILD_FAILED%"=="1" (
    echo  Some builds FAILED. Check logs above.
    exit /b 1
) else (
    echo  All 5 versions BUILT SUCCESSFULLY!
)
echo ============================================
echo.
pause
