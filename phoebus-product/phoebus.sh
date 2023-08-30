#!/bin/sh
#
# Phoebus launcher for Linux or Mac OS X

# When deploying, change "TOP"
# to the absolute installation path
# TOP="."
TOP="$( cd "$(dirname "$0")" ; pwd -P )"

# Ideally, assert that Java is found
# export JAVA_HOME=/opt/jdk-9
# export PATH="$JAVA_HOME/bin:$PATH"

if [ -d "${TOP}/target" ]
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


filter1="-help"
filter2="-main"

firstarg=$1;

if test "${firstarg#*$filter1}" != "$firstarg"; then
  # Run MEDM converter etc. in foreground
  java -Dfile.encoding=UTF-8 -jar $JAR $OPT "$@"
elif test "${firstarg#*$filter2}" != "$firstarg"; then
    java -Dfile.encoding=UTF-8 -jar $JAR $OPT "$@"
else
  # Run UI as separate thread
  java -Dfile.encoding=UTF-8 -jar $JAR $OPT "$@" &
fi
