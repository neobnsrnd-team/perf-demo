@echo off
chcp 65001 >nul 2>&1
setlocal enabledelayedexpansion

set JMETER_HOME=C:\jMeter\apache-jmeter-5.6.3
set JMETER=%JMETER_HOME%\bin\jmeter.bat
set JMETER_DIR=%~dp0
set RESULT_DIR=%JMETER_DIR%results

if not exist "%JMETER%" (
    echo [ERROR] JMeter not found: %JMETER%
    pause
    exit /b 1
)

echo ============================================
echo  perf-demo JMeter CLI Runner
echo ============================================
echo.
echo  Available test plans:
echo    1. S1~S5 Basic        (perf-demo-test-plan.jmx)
echo    2. S6~S9 Advanced     (perf-demo-advanced-test-plan.jmx)
echo    3. S7 OOM Only        (perf-demo-s7-oom-test.jmx)
echo.
echo  Usage: run-test.bat [1^|2^|3] [port]
echo    port: target server port (default: 18081)
echo.

:: Parse arguments
set PLAN=%1
set PORT=%2
if "%PLAN%"=="" set PLAN=1
if "%PORT%"=="" set PORT=18081

:: Select JMX file
if "%PLAN%"=="1" (
    set JMX_FILE=%JMETER_DIR%perf-demo-test-plan.jmx
    set PLAN_NAME=S1-S5_Basic
)
if "%PLAN%"=="2" (
    set JMX_FILE=%JMETER_DIR%perf-demo-advanced-test-plan.jmx
    set PLAN_NAME=S6-S9_Advanced
)
if "%PLAN%"=="3" (
    set JMX_FILE=%JMETER_DIR%perf-demo-s7-oom-test.jmx
    set PLAN_NAME=S7_OOM
)

if not exist "%JMX_FILE%" (
    echo [ERROR] JMX file not found: %JMX_FILE%
    pause
    exit /b 1
)

:: Create timestamped result directory
for /f "tokens=1-6 delims=/-: " %%a in ("%date% %time%") do (
    set TIMESTAMP=%%a%%b%%c_%%d%%e%%f
)
set TIMESTAMP=%TIMESTAMP: =0%
set RUN_DIR=%RESULT_DIR%\%PLAN_NAME%_%TIMESTAMP%
set LOG_FILE=%RUN_DIR%\result.jtl

mkdir "%RUN_DIR%" 2>nul

echo ============================================
echo  Plan  : %PLAN_NAME%
echo  JMX   : %JMX_FILE%
echo  Port  : %PORT%
echo  Output: %RUN_DIR%
echo ============================================
echo.
echo Running JMeter (Non-GUI mode)...
echo.

call "%JMETER%" -n -t "%JMX_FILE%" -l "%LOG_FILE%" -e -o "%RUN_DIR%\report" -JPORT=%PORT% -JHOST=localhost

echo.
if %ERRORLEVEL% EQU 0 (
    echo ============================================
    echo  Test completed successfully!
    echo  Results : %LOG_FILE%
    echo  Report  : %RUN_DIR%\report\index.html
    echo ============================================
) else (
    echo [ERROR] JMeter exited with error code %ERRORLEVEL%
)

endlocal
