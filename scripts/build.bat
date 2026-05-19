@echo off
REM Build and run the Cell Simulation application from CLI.
REM Usage: scripts\build.bat

cd /d "%~dp0\.."

echo ==^> Compiling...
call mvn clean compile
if errorlevel 1 exit /b 1

echo ==^> Launching JavaFX application...
call mvn javafx:run
