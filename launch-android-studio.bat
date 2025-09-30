@echo off
echo Starting Android Studio...
echo Java JDK Location: C:\JavaJDK25
echo Android Studio Location: C:\androidstudio

REM Set Java environment for this session
set JAVA_HOME=C:\JavaJDK25
set PATH=C:\JavaJDK25\bin;%PATH%

REM Launch Android Studio
start "" "C:\androidstudio\bin\studio64.exe"

echo Android Studio is starting...
echo If this is your first time, you'll need to complete the setup wizard.
pause
