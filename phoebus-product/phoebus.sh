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

if [ -d "${TOP}/update" ]
then
  echo "Installing update..."
  cd ${TOP}
  rm -rf doc lib
  mv update/* .
  rmdir update
  echo "Updated."
fi


JAR=`echo ${TOP}/product-*.jar`

# To get one instance, use server mode
OPT="-server 4918"

if [ "x$1" == "x-main" ]
then
  # Run MEDM converter etc. in foreground
  java -jar $JAR $OPT "$@"
else
  # Run UI as separate thread
  java -jar $JAR $OPT "$@" &
fi
