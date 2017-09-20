#!/bin/sh
#
# Phoebus launcher for Linux or Mac OS X

# When deploying, you might want to change "TOP"
# to the absolute installation path
TOP="."

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

# Will eventually need --add-modules=ALL-SYSTEM?
java -jar $JAR "$@"
