@echo off
chcp 65001 >nul 2>&1
setlocal enabledelayedexpansion

set JMETER=C:\jMeter\apache-jmeter-5.6.3\bin\jmeter.bat
set JMX=c:\java\perf-demo\jmeter\perf-demo-test-plan.jmx
set TEMP_JMX=%TEMP%\perf-demo-run.jmx
set HOST=localhost
set PORT=18081

if "%1"=="" (
    echo.
    echo  Usage: run-scenario.bat [TG번호] [port]
    echo.
    echo  Thread Groups:
    echo    1  = 단건 테스트 (1 VUser, 1회)        [기본 enabled]
    echo    2  = Baseline Before (10 VUser, 3분)
    echo    3  = Baseline After (10 VUser, 3분)
    echo    4  = Load Before (30 VUser, 5분)
    echo    5  = Load After (30 VUser, 5분)
    echo    61 = Stress 10 VUser (2분)
    echo    62 = Stress 20 VUser (2분)
    echo    63 = Stress 40 VUser (2분)
    echo    64 = Stress 80 VUser (2분)
    echo    7  = Endurance (20 VUser, 30분)
    echo.
    echo  Example:
    echo    run-scenario.bat 2          = Baseline Before only
    echo    run-scenario.bat 2 18082    = Baseline Before, port 18082
    echo.
    exit /b 0
)

set TG=%1
if not "%2"=="" set PORT=%2

:: Copy JMX and toggle enabled flags
copy /y "%JMX%" "%TEMP_JMX%" >nul

:: Disable ALL Thread Groups first
powershell -Command "(Get-Content '%TEMP_JMX%') -replace '(testclass=\"ThreadGroup\"[^>]*) enabled=\"true\"', '$1 enabled=\"false\"' | Set-Content '%TEMP_JMX%' -Encoding UTF8"

:: Enable only the selected one
if "%TG%"=="1"  powershell -Command "(Get-Content '%TEMP_JMX%') -replace '(testname=\"1\.[^\"]*\") enabled=\"false\"', '$1 enabled=\"true\"' | Set-Content '%TEMP_JMX%' -Encoding UTF8"
if "%TG%"=="2"  powershell -Command "(Get-Content '%TEMP_JMX%') -replace '(testname=\"2\.[^\"]*\") enabled=\"false\"', '$1 enabled=\"true\"' | Set-Content '%TEMP_JMX%' -Encoding UTF8"
if "%TG%"=="3"  powershell -Command "(Get-Content '%TEMP_JMX%') -replace '(testname=\"3\.[^\"]*\") enabled=\"false\"', '$1 enabled=\"true\"' | Set-Content '%TEMP_JMX%' -Encoding UTF8"
if "%TG%"=="4"  powershell -Command "(Get-Content '%TEMP_JMX%') -replace '(testname=\"4\.[^\"]*\") enabled=\"false\"', '$1 enabled=\"true\"' | Set-Content '%TEMP_JMX%' -Encoding UTF8"
if "%TG%"=="5"  powershell -Command "(Get-Content '%TEMP_JMX%') -replace '(testname=\"5\.[^\"]*\") enabled=\"false\"', '$1 enabled=\"true\"' | Set-Content '%TEMP_JMX%' -Encoding UTF8"
if "%TG%"=="61" powershell -Command "(Get-Content '%TEMP_JMX%') -replace '(testname=\"6-1\.[^\"]*\") enabled=\"false\"', '$1 enabled=\"true\"' | Set-Content '%TEMP_JMX%' -Encoding UTF8"
if "%TG%"=="62" powershell -Command "(Get-Content '%TEMP_JMX%') -replace '(testname=\"6-2\.[^\"]*\") enabled=\"false\"', '$1 enabled=\"true\"' | Set-Content '%TEMP_JMX%' -Encoding UTF8"
if "%TG%"=="63" powershell -Command "(Get-Content '%TEMP_JMX%') -replace '(testname=\"6-3\.[^\"]*\") enabled=\"false\"', '$1 enabled=\"true\"' | Set-Content '%TEMP_JMX%' -Encoding UTF8"
if "%TG%"=="64" powershell -Command "(Get-Content '%TEMP_JMX%') -replace '(testname=\"6-4\.[^\"]*\") enabled=\"false\"', '$1 enabled=\"true\"' | Set-Content '%TEMP_JMX%' -Encoding UTF8"
if "%TG%"=="7"  powershell -Command "(Get-Content '%TEMP_JMX%') -replace '(testname=\"7\.[^\"]*\") enabled=\"false\"', '$1 enabled=\"true\"' | Set-Content '%TEMP_JMX%' -Encoding UTF8"

:: Setup result directory
set RESULT_DIR=c:\java\perf-demo\jmeter\results\TG%TG%
if exist "%RESULT_DIR%" rd /s /q "%RESULT_DIR%"
mkdir "%RESULT_DIR%" 2>nul

echo ============================================
echo  Thread Group: %TG%
echo  Port: %PORT%
echo  Result: %RESULT_DIR%
echo ============================================

call "%JMETER%" -n -t "%TEMP_JMX%" -l "%RESULT_DIR%\result.jtl" -e -o "%RESULT_DIR%\report" -JHOST=%HOST% -JPORT=%PORT%

if %ERRORLEVEL% EQU 0 (
    echo.
    echo  Done! Report: %RESULT_DIR%\report\index.html
)

del "%TEMP_JMX%" 2>nul
endlocal
