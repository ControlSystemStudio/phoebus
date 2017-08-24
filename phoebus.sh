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

java -jar $JAR --add-modules=ALL-SYSTEM "$@"
