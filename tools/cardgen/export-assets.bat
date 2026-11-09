@echo off
setlocal
cd /d "%~dp0..\.."
set "PROJ=Graphics\OpenGL Template GLUT1\OpenGL Template GLUT1"
java "tools\cardgen\CardRenderer.java" "treasures\src\main\resources\fish_seed.csv" "%PROJ%"
if errorlevel 1 exit /b 1
java "tools\cardgen\BannerRenderer.java" "%PROJ%"
if errorlevel 1 exit /b 1
copy /y "treasures\src\main\resources\fish_seed.csv" "%PROJ%\" >nul
if errorlevel 1 exit /b 1
endlocal
