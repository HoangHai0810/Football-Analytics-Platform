@REM ----------------------------------------------------------------------------
@REM Licensed to the Apache Software Foundation (ASF) under one
@REM or more contributor license agreements.
@REM ----------------------------------------------------------------------------
@echo off
setlocal

set DIRNAME=%~dp0
if "%DIRNAME%" == "" set DIRNAME=.
set APP_BASE_NAME=%~nx0
set APP_HOME=%DIRNAME%

set DEFAULT_JAVACMD=java.exe
if defined JAVA_HOME goto findJavaFromJavaHome
set JAVACMD=%DEFAULT_JAVACMD%
goto checkJavaCmd

:findJavaFromJavaHome
set JAVACMD=%JAVA_HOME%\bin\%DEFAULT_JAVACMD%
if exist "%JAVACMD%" goto checkJavaCmd
echo The JAVA_HOME environment variable is not defined correctly.
exit /b 1

:checkJavaCmd
if not exist "%JAVACMD%" (
  echo Error: JAVA_HOME is not set and no 'java' command could be found in your PATH.
  exit /b 1
)

set MAVEN_PROJECTBASEDIR=%APP_HOME%
set MAVEN_CMD_LINE_ARGS=%*
set WRAPPER_JAR="%APP_HOME%\.mvn\wrapper\maven-wrapper.jar"

if exist %WRAPPER_JAR% (
    "%JAVACMD%" -jar %WRAPPER_JAR% %MAVEN_CMD_LINE_ARGS%
) else (
    echo Maven wrapper jar not found, executing mvn if installed...
    mvn %MAVEN_CMD_LINE_ARGS%
)

endlocal
