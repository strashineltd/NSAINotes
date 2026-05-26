@echo off
chcp 65001 >nul
title NSAI笔记 - 项目构建工具
echo ========================================
echo    NSAI笔记 - Android 项目构建工具
echo ========================================
echo.

:: Check Java
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo [错误] 未找到 Java！请安装 JDK 17+
    echo 下载地址: https://adoptium.net/
    echo.
    pause
    exit /b 1
)
echo [OK] Java 已安装

:: Check ANDROID_HOME
if "%ANDROID_HOME%"=="" (
    if exist "%LOCALAPPDATA%\Android\Sdk" (
        set ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk
        echo [OK] 发现 Android SDK: %ANDROID_HOME%
    ) else (
        echo [错误] 未找到 Android SDK！
        echo 请安装 Android Studio 或设置 ANDROID_HOME 环境变量
        pause
        exit /b 1
    )
) else (
    echo [OK] ANDROID_HOME: %ANDROID_HOME%
)

:: Create local.properties
if not exist "local.properties" (
    echo sdk.dir=%ANDROID_HOME:\=\\% > local.properties
    echo [OK] 已创建 local.properties
)

:: Generate gradle wrapper if missing
if not exist "gradle\wrapper\gradle-wrapper.jar" (
    echo [INFO] 正在生成 Gradle Wrapper...
    gradle wrapper --gradle-version=8.9
    if %errorlevel% neq 0 (
        :: Fallback: download manually
        echo [WARN] Gradle 未安装，尝试下载 wrapper...
        powershell -Command "Invoke-WebRequest -Uri 'https://services.gradle.org/distributions/gradle-8.9-bin.zip' -OutFile '%TEMP%\gradle.zip'"
        echo 请手动解压 gradle.zip 并将 gradle-8.9/bin 加入 PATH
        pause
        exit /b 1
    )
)

echo.
echo ========================================
echo  构建选项:
echo   1. Debug APK
echo   2. Release APK
echo   3. 运行单元测试
echo   4. 清理项目
echo   0. 退出
echo ========================================
echo.

set /p choice="请选择 [0-4]: "

if "%choice%"=="1" (
    echo 正在构建 Debug APK...
    gradlew.bat assembleDebug
    if %errorlevel% equ 0 (
        echo.
        echo [OK] Debug APK 构建成功！
        dir /b app\build\outputs\apk\debug\*.apk 2>nul
    ) else (
        echo [错误] 构建失败，请检查错误信息
    )
) else if "%choice%"=="2" (
    echo 正在构建 Release APK...
    gradlew.bat assembleRelease
    if %errorlevel% equ 0 (
        echo.
        echo [OK] Release APK 构建成功！
        dir /b app\build\outputs\apk\release\*.apk 2>nul
    ) else (
        echo [错误] 构建失败，请检查错误信息
    )
) else if "%choice%"=="3" (
    echo 正在运行单元测试...
    gradlew.bat testDebugUnitTest
    if %errorlevel% equ 0 (
        echo [OK] 所有测试通过！
    ) else (
        echo [错误] 测试失败，请检查错误信息
    )
) else if "%choice%"=="4" (
    gradlew.bat clean
    echo [OK] 已清理项目
) else (
    echo 退出
)

pause
