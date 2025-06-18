@REM Phoebus launcher for Windows
@REM Uses a JDK that's located next to this folder,
@REM otherwise assumes JDK is on the PATH

set TOP=%~P0
setlocal ENABLEDELAYEDEXPANSION

@IF EXIST "%TOP%target" (
    set TOP=%TOP%target\
)

@IF EXIST "%TOP%..\jdk" (
    set JAVA_HOME=%TOP%..\jdk
    @path !JAVA_HOME!\bin
    @ECHO Found JDK !JAVA_HOME!
)

@IF EXIST "%TOP%update" (
    @ECHO Installing update...
    @cd "%TOP%"
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
FOR /F "tokens=* USEBACKQ" %%F IN (`dir /B /S "%TOP%product*.jar"`) DO (SET JAR=%%F)
echo on

@REM CA_DISABLE_REPEATER=true: Don't start CA repeater (#494)
@REM To get one instance, use server mode by adding `-server 4918`
@java -DCA_DISABLE_REPEATER=true -Dfile.encoding=UTF-8 -jar "%JAR%" %*

