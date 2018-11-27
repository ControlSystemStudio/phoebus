#!/bin/sh
#
# Send text to topic.
# Originally created for commands, but can also be used to create test messages
# by for example connecting to the Accelerator (config) topic and sending
# /Accelerator/Main/SomePV!{"user":"me","host":"my.host","description":"SomePV"}

if [ $# -ne 1 ]
then
    echo "Usage: send_commands.sh Accelerator"
    exit 1
fi

topic="$1"

echo "Example commands:"
echo "dump!"
echo "dump!/some/path"
echo "pvs!"
echo "pvs!/some/path"
echo "pvs!disconnected"
echo "pv!name_of_PV"
echo "restart!"
echo "shutdown!"

kafka/bin/kafka-console-producer.sh --broker-list localhost:9092 --property 'parse.key=true' --property 'key.separator=!' --topic $topic


