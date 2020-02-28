@REM Phoebus launcher for Windows
@REM Uses a JDK that's located next to this folder,
@REM otherwise assumes JDK is on the PATH

@cd %~P0

@IF EXIST "%~P0%..\jdk" (
    set JAVA_HOME=%~P0%..\jdk
    @path %JAVA_HOME%\bin
    @ECHO Found JDK %JAVA_HOME%
)

@if EXIST "update" (
    @ECHO Installing update...
    @rd /S/Q doc
    @rd /S/Q lib
    @del product*.jar
    @move /Y update\*.* .
    @move /Y update\doc .
    @move /Y update\lib .
    @rmdir update
    @ECHO Updated.
)

@java -version
echo off
FOR /F "tokens=* USEBACKQ" %%F IN (`dir /B product*.jar`) DO (SET JAR=%%F)
echo on

@REM To get one instance, use server mode
@java -jar %JAR% -server 4918 %*

