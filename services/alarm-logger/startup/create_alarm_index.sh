#!/bin/sh
if [ $# -ne 1 ]
then
    echo "Usage: create_alarm_index.sh accelerator"
    exit 1
fi

es_host=localhost
es_port=9200

# Create the elastic index with the correct mapping for alarm state messages.
# Create the Index
# Set the mapping
curl -H 'Content-Type: application/json' -XPUT http://${es_host}:${es_port}/${1}_alarms -d'
{
  "mappings" : {  
    "alarm" : {
        "properties" : {
          "APPLICATION-ID" : {
            "type" : "text"
          },
          "CONFIG" : {
            "type" : "text"
          },
          "CURRENT_SEVERITY" : {
            "type" : "text"
          },
          "CURRENT_STATUS" : {
            "type" : "text"
          },
          "EVENTTIME" : {
            "type" : "date",
            "format" : "yyyy-MM-dd HH:mm:ss.SSS"
          },
          "HOST" : {
            "type" : "text"
          },
          "NAME" : {
            "type" : "text",
            "analyzer" : "keyword"
          },
          "SEVERITY" : {
            "type" : "text"
          },
          "STATUS" : {
            "type" : "text"
          },
          "TEXT" : {
            "type" : "text"
          },
          "TYPE" : {
            "type" : "text"
          },
          "USER" : {
            "type" : "text"
          },
          "VALUE" : {
            "type" : "text"
          }
        }
      }
  }
}'
