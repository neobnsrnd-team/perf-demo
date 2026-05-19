@echo off
setlocal

set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.18.8-hotspot"
set "APP_JAR=%~dp0target\perf-demo-1.0.0.jar"
set "JFR_FILE=%~dp0profiling.jfr"

echo ============================================================
echo  perf-demo with Java Flight Recorder (JFR)
echo ============================================================
echo  Recording to: %JFR_FILE%
echo  App:          http://localhost:18081
echo  Swagger:      http://localhost:18081/swagger-ui.html
echo ============================================================
echo.
echo  Press Ctrl+C to stop. The .jfr file will be saved.
echo  Open it with: jmc (JDK Mission Control) or IntelliJ Profiler
echo ============================================================
echo.

"%JAVA_HOME%\bin\java.exe" ^
    -XX:StartFlightRecording=filename="%JFR_FILE%",dumponexit=true,settings=profile ^
    -jar "%APP_JAR%"

endlocal
pause
