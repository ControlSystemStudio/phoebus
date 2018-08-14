#!/bin/sh
#
# Scan Server launcher for Linux or Mac OS X

# When deploying, change "TOP"
# to the absolute installation path
TOP="."

# Ideally, assert that Java is found
# export JAVA_HOME=/opt/jdk-9
# export PATH="$JAVA_HOME/bin:$PATH"

if [ -d $TOP/target ]
then
  TOP="$TOP/target"
fi

V="0.0.1"

# Use ant or maven jar?
if [ -f ${TOP}/service-scan-server-${V}.jar ]
then
  JAR="${TOP}/service-scan-server-${V}.jar"
else
  JAR="${TOP}/service-scan-server-${V}-SNAPSHOT.jar"
fi

java --add-modules=java.corba -jar $JAR "$@"
