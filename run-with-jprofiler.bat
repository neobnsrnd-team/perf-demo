@echo off
setlocal

REM ============================================================
REM  JProfiler Launch Script for perf-demo
REM  Adjust JPROFILER_HOME if your installation path differs.
REM ============================================================

REM --- Configuration ---
set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.18.8-hotspot"
set "JPROFILER_HOME=C:\Program Files\jprofiler16"
set "JPROFILER_PORT=8849"
set "APP_JAR=%~dp0target\perf-demo-1.0.0.jar"

REM --- Validate Java ---
if not exist "%JAVA_HOME%\bin\java.exe" (
    echo [ERROR] Java not found at: %JAVA_HOME%
    echo         Please set JAVA_HOME correctly.
    pause
    exit /b 1
)

REM --- Validate JAR ---
if not exist "%APP_JAR%" (
    echo [ERROR] Application JAR not found: %APP_JAR%
    echo         Run 'mvn package -DskipTests' first.
    pause
    exit /b 1
)

REM --- Validate JProfiler ---
set "JPROFILER_AGENT=%JPROFILER_HOME%\bin\windows-x64\jprofilerti.dll"
if not exist "%JPROFILER_AGENT%" (
    echo [WARNING] JProfiler agent not found at: %JPROFILER_AGENT%
    echo.
    echo Common installation paths:
    echo   - C:\Program Files\jprofiler16
    echo   - C:\Program Files\jprofiler15
    echo   - C:\jprofiler16
    echo.
    set /p "JPROFILER_HOME=Enter your JProfiler installation path: "
    set "JPROFILER_AGENT=!JPROFILER_HOME!\bin\windows-x64\jprofilerti.dll"
)

REM --- Enable delayed expansion for the user-input path ---
setlocal enabledelayedexpansion
set "JPROFILER_AGENT=!JPROFILER_HOME!\bin\windows-x64\jprofilerti.dll"

if not exist "!JPROFILER_AGENT!" (
    echo [ERROR] JProfiler agent DLL still not found: !JPROFILER_AGENT!
    pause
    exit /b 1
)

echo ============================================================
echo  perf-demo with JProfiler
echo ============================================================
echo  Java:      %JAVA_HOME%
echo  JProfiler: !JPROFILER_HOME!
echo  Agent:     !JPROFILER_AGENT!
echo  Port:      %JPROFILER_PORT%
echo  JAR:       %APP_JAR%
echo ============================================================
echo.
echo  After startup:
echo    App:      http://localhost:18081
echo    Swagger:  http://localhost:18081/swagger-ui.html
echo    JProfiler: attach to localhost:%JPROFILER_PORT%
echo ============================================================
echo.

"%JAVA_HOME%\bin\java.exe" ^
    -agentpath:"!JPROFILER_AGENT!=port=%JPROFILER_PORT%" ^
    -jar "%APP_JAR%"

endlocal
endlocal
pause
