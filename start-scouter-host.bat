@echo off
title Scouter Host Agent
echo ========================================
echo  Scouter Host Agent (Java 8)
echo ========================================

set JAVA_HOME=C:\Program Files\Amazon Corretto\jdk1.8.0_482
set SCOUTER_HOME=C:\java\scouter\agent.host

cd /d %SCOUTER_HOME%
"%JAVA_HOME%\bin\java" -classpath ./scouter.host.jar scouter.boot.Boot ./lib

pause
