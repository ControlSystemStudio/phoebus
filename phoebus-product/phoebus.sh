#!/bin/sh
#
# Phoebus launcher for Linux or Mac OS X

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
if [ -f ${TOP}/product-${V}.jar ]
then
  JAR="${TOP}/product-${V}.jar"
else
  JAR="${TOP}/product-${V}-SNAPSHOT.jar"
fi

# To get one instance, use server mode
OPT="-server 4918"

java --add-modules=java.corba -jar $JAR $OPT "$@" &
