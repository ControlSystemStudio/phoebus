#!/bin/sh

if [ $# -ne 1 ]
then
    echo "Usage: monitor_topic.sh Accelerator (or AcceleratorCommand, AcceleratorTalk)"
    exit 1
fi

topic=$1

kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --property print.timestamp=true --property print.key=true --property key.separator=": " --topic $topic --from-beginning  

