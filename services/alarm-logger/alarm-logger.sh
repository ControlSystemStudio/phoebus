#!/bin/sh
#
# Alarm Server launcher for Linux or Mac OS X

# When deploying, change "TOP"
# to the absolute installation path
TOP="."

if [ -d $TOP/target ]
then
  TOP="$TOP/target"
fi

V="0.0.1"

# Use ant or maven jar?
if [ -f ${TOP}/service-alarm-logger-${V}.jar ]
then
  JAR="${TOP}/service-alarm-logger-${V}.jar"
else
  JAR="${TOP}/service-alarm-logger-${V}-SNAPSHOT-jar-with-dependencies.jar"
fi

java -jar $JAR "$@"
