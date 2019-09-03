#!/bin/sh

es_host=localhost
es_port=9200

# The mapping names used in here need to match those used in the ElasticClientHelper:
# "alarm", ""alarm_cmd", "alarm_config"

# Create the elastic template with the correct mapping for alarm state messages.
curl -XPUT http://${es_host}:${es_port}/_template/alarms_state_template -H 'Content-Type: application/json' -d'
{
  "index_patterns":["*_alarms_state*"],
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

# Create the elastic template with the correct mapping for alarm command messages.
curl -XPUT http://${es_host}:${es_port}/_template/alarms_cmd_template -H 'Content-Type: application/json' -d'
{
  "index_patterns":["*_alarms_cmd*"],
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

# Create the elastic template with the correct mapping for alarm config messages.
curl -XPUT http://${es_host}:${es_port}/_template/alarms_config_template -H 'Content-Type: application/json' -d'
{
  "index_patterns":["*_alarms_config*"],
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


echo "Alarm templates:"
curl -X GET "${es_host}:${es_port}/_template/*alarm*"

