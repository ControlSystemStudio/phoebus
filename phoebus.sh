#!/bin/sh
#
# Phoebus launcher for Linux or Mac OS X

cd phoebus-product/target

V="0.0.1"

# Use ant or maven jar?
if [ -f product-${V}.jar ]
then
  JAR="product-${V}.jar"
else
  JAR="product-${V}-SNAPSHOT.jar"
fi

# Will eventually need --add-modules=ALL-SYSTEM?
java -jar $JAR "$@"
