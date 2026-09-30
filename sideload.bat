@echo off
REM ============================================================
REM  Andraste Tablet — sideload to Amazon Fire HD 8 (onyx)
REM  Builds the debug APK and installs it over ADB.
REM ============================================================
setlocal
set PROJ=%~dp0
set ADB=%PROJ%..\platform-tools\adb.exe
set APK=%PROJ%app\build\outputs\apk\debug\app-armeabi-v7a-debug.apk

REM Use Android Studio's bundled JDK for the build
if "%JAVA_HOME%"=="" set JAVA_HOME=D:\Android\Android Studio\jbr

echo.
echo [1/3] Building debug APK...
call "%PROJ%gradlew.bat" :app:assembleDebug
if errorlevel 1 goto :fail

echo.
echo [2/3] Waiting for device...
"%ADB%" wait-for-device
"%ADB%" devices -l

echo.
echo [3/3] Installing %APK%
"%ADB%" install -r "%APK%"
if errorlevel 1 goto :fail

echo.
echo Done. Launch "Andraste" from the Fire home screen.
goto :eof

:fail
echo.
echo BUILD OR INSTALL FAILED — see output above.
exit /b 1
