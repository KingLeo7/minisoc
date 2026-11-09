@echo off
setlocal
cd /d "%~dp0"

echo [MiniSOC] Checking build...
if not exist "out\minisoc\gui\SocGui.class" (
    echo [MiniSOC] Compiling Java classes...
    if not exist out mkdir out
    powershell -NoProfile -Command "javac -d out (Get-ChildItem -Recurse -Filter *.java src).FullName"
    if errorlevel 1 (
        echo [ERROR] Compilation failed. Please ensure JDK is installed and in PATH.
        pause
        exit /b 1
    )
)

echo [MiniSOC] Launching Security Operations Center GUI...
start javaw -cp out minisoc.gui.SocGui
if errorlevel 1 (
    java -cp out minisoc.gui.SocGui
)

