@echo off
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0Build.ps1" -RuntimeHome "%USERPROFILE%\Downloads\server"
if errorlevel 1 pause

