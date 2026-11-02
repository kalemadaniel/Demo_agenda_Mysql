@echo off
cd /d "%~dp0"
if not exist "dist\agenda.jar" (
  echo Compilez d'abord avec Construire.ps1.
  pause
  exit /b 1
)
java -jar "dist\agenda.jar"
if errorlevel 1 pause
