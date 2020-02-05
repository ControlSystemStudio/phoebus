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

JAR=`echo "${TOP}/service-scan-server-*.jar"`
java -jar $JAR "$@"
