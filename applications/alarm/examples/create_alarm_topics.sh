#!/bin/sh

if [ $# -ne 1 ]
then
    echo "Usage: create_alarm_topics.sh Accelerator"
    exit 1
fi

config=$1

for topic in "$1" "${1}State" "${1}Command" "{1}Talk"
do
    kafka/bin/kafka-topics.sh  --zookeeper localhost:2181 --create --replication-factor 1 --partitions 1 --topic $topic
    kafka/bin/kafka-configs.sh --zookeeper localhost:2181 --entity-type topics --alter --entity-name $topic \
           --add-config cleanup.policy=compact,segment.ms=10000,min.cleanable.dirty.ratio=0.01
done

