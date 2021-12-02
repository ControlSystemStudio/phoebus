#!/bin/sh
if [ $# -ne 1 ]
then
    echo "Usage: create_alarm_index.sh Accelerator"
    exit 1
fi

es_host=localhost
es_port=9200

# Delete the elastic index with the correct mapping for alarm state messages.
curl -XDELETE http://${es_host}:${es_port}/${1}_alarms_state

# Delete the elastic index with the correct mapping for alarm cmd messages.
curl -XDELETE http://${es_host}:${es_port}/${1}_alarms_cmd

# Delete the elastic index with the correct mapping for alarm config messages.
curl -XDELETE http://${es_host}:${es_port}/${1}_alarms_config