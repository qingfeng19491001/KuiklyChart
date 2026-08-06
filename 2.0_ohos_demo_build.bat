@echo off
setlocal enabledelayedexpansion

echo ========================================
echo KuiklyChart Ohos Demo Build (Windows)
echo ========================================

if defined DEVECO_SDK_HOME (
  for /f "delims=" %%A in ('echo %DEVECO_SDK_HOME%') do set "RESOLVED_SDK_HOME=%%A"
) else if exist "C:\Program Files\Huawei\DevEco Studio\sdk" (
  set "RESOLVED_SDK_HOME=C:\Program Files\Huawei\DevEco Studio\sdk"
) else (
  echo [Error] DEVECO_SDK_HOME not set and DevEco Studio sdk not found.
  exit /b 1
)

if exist "!RESOLVED_SDK_HOME!\default\openharmony" (
  set "OHOS_SDK_HOME=!RESOLVED_SDK_HOME!\default\openharmony"
) else if exist "!RESOLVED_SDK_HOME!\openharmony" (
  set "OHOS_SDK_HOME=!RESOLVED_SDK_HOME!\openharmony"
) else (
  echo [Error] OpenHarmony SDK not found under !RESOLVED_SDK_HOME!
  exit /b 1
)

set "DEVECO_SDK_HOME=!RESOLVED_SDK_HOME!"
set "KUIKLY_AGP_VERSION=7.4.2"
set "KUIKLY_KOTLIN_VERSION=2.0.21-KBA-010"
echo [Step 0] DEVECO_SDK_HOME=!DEVECO_SDK_HOME!
echo [Step 0] OHOS_SDK_HOME=!OHOS_SDK_HOME!

set "GRADLE_BIN=%USERPROFILE%\.gradle\wrapper\dists\gradle-8.7-bin\af3un6e4ivqgjcdo5lfa5efog\gradle-8.7\bin\gradle.bat"
if not exist "%GRADLE_BIN%" (
  set "GRADLE_BIN=%~dp0gradlew.bat"
)

echo [Step 1] linkDebugSharedOhosArm64 via %GRADLE_BIN%
call "%GRADLE_BIN%" -c settings.ohos.gradle.kts :shared:linkDebugSharedOhosArm64 --stacktrace
set BUILD_RESULT=%ERRORLEVEL%
if not "%BUILD_RESULT%"=="0" (
  echo [Error] Kotlin/Native ohos build failed: %BUILD_RESULT%
  exit /b %BUILD_RESULT%
)

echo [Step 2] Copy artifacts into ohosApp...
set TARGET_SO_PATH=shared\build\bin\ohosArm64\debugShared\libshared.so
if not exist "%TARGET_SO_PATH%" set TARGET_SO_PATH=shared\build\bin\ohosArm64\sharedDebugShared\libshared.so

set TARGET_H_PATH=shared\build\bin\ohosArm64\debugShared\libshared_api.h
if not exist "%TARGET_H_PATH%" set TARGET_H_PATH=shared\build\bin\ohosArm64\sharedDebugShared\libshared_api.h

set OHO_SO_DIR=ohosApp\entry\libs\arm64-v8a
set OHO_H_DIR=ohosApp\entry\src\main\cpp
if not exist "%OHO_SO_DIR%" mkdir "%OHO_SO_DIR%"

if not exist "%TARGET_SO_PATH%" (
  echo [Error] libshared.so not found
  exit /b 1
)
if not exist "%TARGET_H_PATH%" (
  echo [Error] libshared_api.h not found
  exit /b 1
)

copy /y "%TARGET_SO_PATH%" "%OHO_SO_DIR%\" >nul
copy /y "%TARGET_H_PATH%" "%OHO_H_DIR%\" >nul
echo   libshared.so -^> %OHO_SO_DIR%
echo   libshared_api.h -^> %OHO_H_DIR%

echo ========================================
echo Kotlin ohos artifact ready.
echo Next: open ohosApp in DevEco Studio, configure signing, Run entry
echo ========================================
endlocal
