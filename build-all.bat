@echo off
setlocal
set ROOT=%~dp0
if exist "%ROOT%build\release" rmdir /s /q "%ROOT%build\release"
mkdir "%ROOT%build\release"

echo ==^> Building Scythe Mod family: legacy
call "%ROOT%builds\legacy\gradlew.bat" -p "%ROOT%builds\legacy" buildAndCollect
if errorlevel 1 exit /b %errorlevel%

echo ==^> Building Scythe Mod family: modern
call "%ROOT%builds\modern\gradlew.bat" -p "%ROOT%builds\modern" buildAndCollect
if errorlevel 1 exit /b %errorlevel%

echo ==^> Building Scythe Mod family: current
call "%ROOT%builds\current\gradlew.bat" -p "%ROOT%builds\current" buildAndCollect
if errorlevel 1 exit /b %errorlevel%

echo ==^> Release jars
dir /b "%ROOT%build\release\*.jar"
