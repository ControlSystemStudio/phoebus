#!/bin/sh

if [ $# -ne 1 ]
then
    echo "Usage: delete_alarm_topics.sh Accelerator"
    exit 1
fi

config=$1

for topic in "$1" "${1}State" "${1}Command" "${1}Talk"
do
    kafka/bin/kafka-topics.sh  --zookeeper localhost:2181 --delete --topic $topic
done

