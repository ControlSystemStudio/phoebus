#!/bin/sh

if [ $# -ne 1 ]
then
    echo "Usage: delete_alarm_topics.sh Accelerator"
	echo
    echo "Can be used to delete topics, for example to start over like this:"
	echo
    echo " 1. Stop alarm server,"
    echo " 2. Export config,"
    echo " 3. Delete topics,"
    echo " 4. Re-create topics,"
    echo " 5. Import config,"
    echo " 6. Start alarm server."
	echo
    echo "After deleting topics, they will no longer be shown by list_topics.sh."
    echo "The kafka/zookeeper log dir will rename the previous 'XXX' folders into 'XXX-delete'"
    echo "and soon remove them."

    exit 1
fi

config=$1

# ...State was used earlier.
# With recent setups, you might get a "... does not exist" error which can be ignored
for topic in "$1" "${1}Command" "${1}Talk"
do
    kafka/bin/kafka-topics.sh  --bootstrap-server localhost:9092 --delete --topic $topic
done

