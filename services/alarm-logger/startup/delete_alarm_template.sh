#!/bin/sh

es_host=localhost
es_port=9200

# Delete the elastic template with the correct mapping for alarm state messages.
curl -XDELETE http://${es_host}:${es_port}/_template/alarms_state_template

# Delete the elastic template with the correct mapping for alarm cmd messages.
curl -XDELETE http://${es_host}:${es_port}/_template/alarms_cmd_template

# Delete the elastic template with the correct mapping for alarm cmd messages.
curl -XDELETE http://${es_host}:${es_port}/_template/alarms_config_template

echo "Alarm templates:"
curl -X GET "${es_host}:${es_port}/_template/*alarm*"