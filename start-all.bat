@echo off
chcp 65001 > nul
echo ============================================
echo  perf-demo: Scouter + 3 Instances Start All
echo ============================================
echo.
echo  1. Scouter Server   (Java 8)
echo  2. Scouter Webapp   (Java 8)
echo  3. Scouter Host Agent (Java 8)
echo  4. perf-demo-1 :18081 (Java 17)
echo  5. perf-demo-2 :18082 (Java 17)
echo  6. perf-demo-3 :18083 (Java 17)
echo.

:: ── Scouter 기동 ──
echo [1/6] Starting Scouter Server...
start "Scouter Server" cmd /c "%~dp0start-scouter-server.bat"
timeout /t 5 /nobreak > nul

echo [2/6] Starting Scouter Webapp (Paper)...
start "Scouter Webapp" cmd /c "%~dp0start-scouter-webapp.bat"
timeout /t 3 /nobreak > nul

echo [3/6] Starting Scouter Host Agent...
start "Scouter Host Agent" cmd /c "%~dp0start-scouter-host.bat"
timeout /t 2 /nobreak > nul

:: ── perf-demo 인스턴스 기동 ──
set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.18.8-hotspot
set JAVA="%JAVA_HOME%\bin\java.exe"
set SCOUTER_AGENT=C:\java\scouter\agent.java\scouter.agent.jar
set SCOUTER_OPTS=--add-opens=java.base/java.lang=ALL-UNNAMED --add-exports=java.base/sun.net=ALL-UNNAMED
set JAR_FILE=target\perf-demo-1.0.0.jar

if not exist "%JAR_FILE%" (
    echo [ERROR] %JAR_FILE% not found. Run "mvn clean package" first.
    pause
    exit /b 1
)

echo [4/6] Starting perf-demo-1 (port 18081)...
start "perf-demo-1" %JAVA% %SCOUTER_OPTS% -Dserver.port=18081 -Dscouter.config=conf\scouter-demo1.conf -javaagent:%SCOUTER_AGENT% -jar %JAR_FILE%
timeout /t 3 /nobreak > nul

echo [5/6] Starting perf-demo-2 (port 18082)...
start "perf-demo-2" %JAVA% %SCOUTER_OPTS% -Dserver.port=18082 -Dscouter.config=conf\scouter-demo2.conf -javaagent:%SCOUTER_AGENT% -jar %JAR_FILE%
timeout /t 3 /nobreak > nul

echo [6/6] Starting perf-demo-3 (port 18083)...
start "perf-demo-3" %JAVA% %SCOUTER_OPTS% -Dserver.port=18083 -Dscouter.config=conf\scouter-demo3.conf -javaagent:%SCOUTER_AGENT% -jar %JAR_FILE%

echo.
echo ============================================
echo  All services started!
echo.
echo  Scouter Paper:  http://localhost:6180/extweb/scouter-xlog.html
echo  perf-demo-1:    http://localhost:18081/swagger-ui.html
echo  perf-demo-2:    http://localhost:18082/swagger-ui.html
echo  perf-demo-3:    http://localhost:18083/swagger-ui.html
echo ============================================
pause
