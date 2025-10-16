#!/bin/sh
#
# Send talk message

if [ $# -ne 2 ]
then
    echo "Usage: talk.sh Accelerator Message"
    exit 1
fi

topic="$1"
text="$2"

kafka/bin/kafka-console-producer.sh --broker-list localhost:9092 --property 'parse.key=true' --property key.separator=" : "  --topic ${topic}Talk <<END
talk:/$topic/pv : {"severity":"MAJOR", "standout":true, "talk":"$text"}
END


