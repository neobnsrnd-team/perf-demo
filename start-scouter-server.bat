@echo off
title Scouter Server
echo ========================================
echo  Scouter Server (Java 8)
echo ========================================

set JAVA_HOME=C:\Program Files\Amazon Corretto\jdk1.8.0_482
set SCOUTER_HOME=C:\java\scouter\server

cd /d %SCOUTER_HOME%
"%JAVA_HOME%\bin\java" -Xmx1024m -classpath ./scouter-server-boot.jar scouter.boot.Boot ./lib

pause
