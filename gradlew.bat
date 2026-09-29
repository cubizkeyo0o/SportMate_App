@echo off

set DIR=%~dp0

if "%DIR%"=="" set DIR=.

set CLASSPATH=%DIR%\gradle\wrapper\gradle-wrapper.jar

if defined JAVA_HOME (
    set "JAVACMD=%JAVA_HOME%\bin\java.exe"
) else (
    set "JAVACMD=java"
)

if defined JAVA_HOME (
    if not exist "%JAVACMD%" (
        echo ERROR: JAVA_HOME is set to "%JAVA_HOME%" but java.exe was not found.
        exit /b 1
    )
) else (
    where java >nul 2>nul

    if errorlevel 1 (
        echo ERROR: Java was not found. Install JDK 17 and set JAVA_HOME or add java to PATH.
        exit /b 1
    )
)

"%JAVACMD%" -Dorg.gradle.appname=gradlew -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*