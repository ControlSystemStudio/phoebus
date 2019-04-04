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
curl -H 'Content-Type: application/json' -XPUT http://${es_host}:${es_port}/${1}_alarms_state -d'
{
  "mappings" : {  
    "alarm" : {
        "properties" : {
          "APPLICATION-ID" : {
            "type" : "text"
          },
          "config" : {
            "type" : "keyword"
          },
          "pv" : {
            "type" : "keyword"
          },
          "severity" : {
            "type" : "keyword"
          },
          "latch" : {
            "type" : "boolean"
          },
          "message" : {
            "type" : "text",
            "fields": {
              "keyword": { 
                "type": "keyword"
              }
            }
          },
          "value" : {
            "type" : "text"
          },
          "time" : {
            "type" : "date",
            "format" : "yyyy-MM-dd HH:mm:ss.SSS"
          },
          "message_time" : {
            "type" : "date",
            "format" : "yyyy-MM-dd HH:mm:ss.SSS"
          },
          "current_severity" : {
            "type" : "keyword"
          },
          "current_message" : {
            "type" : "text",
            "fields": {
              "keyword": { 
                "type": "keyword"
              }
            }
          },
          "mode" : {
            "type" : "keyword"
          }
        }
      }
  }
}
'

# Create the elastic index with the correct mapping for alarm command messages.
# Create the Index
# Set the mapping
curl -H 'Content-Type: application/json' -XPUT http://${es_host}:${es_port}/${1}_alarms_cmd -d'
{
  "mappings" : {  
    "alarm_cmd" : {
        "properties" : {
          "APPLICATION-ID" : {
            "type" : "text"
          },
          "config" : {
            "type" : "keyword"
          },
          "user" : {
            "type" : "keyword"
          },
          "host" : {
            "type" : "keyword"
          },
          "command" : {
            "type" : "keyword"
          },
          "message_time" : {
            "type" : "date",
            "format" : "yyyy-MM-dd HH:mm:ss.SSS"
          }
        }
      }
  }
}
'

# Create the elastic index with the correct mapping for alarm config messages.
# Create the Index
# Set the mapping
curl -H 'Content-Type: application/json' -XPUT http://${es_host}:${es_port}/${1}_alarms_config -d'
{
  "mappings" : {  
    "alarm_config" : {
        "properties" : {
          "APPLICATION-ID" : {
            "type" : "text"
          },
          "config" : {
            "type" : "keyword"
          },
          "user" : {
            "type" : "keyword"
          },
          "host" : {
            "type" : "keyword"
          },
          "enabled" : {
            "type" : "keyword"
          },
          "latching" : {
            "type" : "keyword"
          },
          "config_msg" : {
            "type" : "keyword"
          },
          "message_time" : {
            "type" : "date",
            "format" : "yyyy-MM-dd HH:mm:ss.SSS"
          }
        }
      }
  }
}
'
