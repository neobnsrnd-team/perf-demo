@echo off
chcp 65001 > nul
title Scouter Webapp (Paper)
echo ========================================
echo  Scouter Webapp + Paper (Java 8)
echo ========================================

set JAVA_HOME=C:\Program Files\Amazon Corretto\jdk1.8.0_482
set SCOUTER_HOME=C:\java\scouter\webapp

cd /d %SCOUTER_HOME%
"%JAVA_HOME%\bin\java" -cp scouter.webapp.jar;lib/* scouterx.webapp.main.WebAppMain

pause
