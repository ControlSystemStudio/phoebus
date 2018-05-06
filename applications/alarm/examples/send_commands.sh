#!/bin/sh

if [ $# -ne 1 ]
then
    echo "Usage: send_commands.sh Accelerator"
    exit 1
fi

topic="$1Command"

echo "Example commands:"
echo "dump!"
echo "dump!/some/path"
echo "pvs!"
echo "pvs!/some/path"
echo "pvs!disconnected"
echo "restart!"
echo "shutdown!"

kafka/bin/kafka-console-producer.sh --broker-list localhost:9092 --property 'parse.key=true' --property 'key.separator=!' --topic $topic


