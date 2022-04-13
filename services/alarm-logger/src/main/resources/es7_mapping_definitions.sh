#!/bin/sh

es_host=localhost
es_port=9200

###
# A temporary helper file for creating the alarm logging templates for elastic 7 for cases where elastic 6 cannot be used.
###

#Create the template for the alarm messages
curl -H 'Content-Type: application/json' -XPUT http://${es_host}:${es_port}/_template/alarms_state_template?include_type_name=true -d'
{
   "index_patterns":[
      "*_alarms_state*"
   ],
   "settings":{
      "number_of_shards":1
   },
   "mappings":{
      "alarm":{
         "properties":{
            "APPLICATION-ID":{
               "type":"text"
            },
            "config":{
               "type":"keyword"
            },
            "pv":{
               "type":"keyword"
            },
            "severity":{
               "type":"keyword"
            },
            "latch":{
               "type":"boolean"
            },
            "message":{
               "type":"keyword"
            },
            "value":{
               "type":"keyword"
            },
            "time":{
               "type":"date",
               "format":"yyyy-MM-dd HH:mm:ss.SSS"
            },
            "message_time":{
               "type":"date",
               "format":"yyyy-MM-dd HH:mm:ss.SSS"
            },
            "current_severity":{
               "type":"keyword"
            },
            "current_message":{
               "type":"keyword"
            },
            "mode":{
               "type":"keyword"
            }
         }
      }
   }
}'

#Create the template for the alarm command messages
curl -H 'Content-Type: application/json' -XPUT http://${es_host}:${es_port}/_template/alarms_cmd_template?include_type_name=true -d'
{
   "index_patterns":[
      "*_alarms_cmd*"
   ],
   "mappings":{
      "alarm_cmd":{
         "properties":{
            "APPLICATION-ID":{
               "type":"text"
            },
            "config":{
               "type":"keyword"
            },
            "user":{
               "type":"keyword"
            },
            "host":{
               "type":"keyword"
            },
            "command":{
               "type":"keyword"
            },
            "message_time":{
               "type":"date",
               "format":"yyyy-MM-dd HH:mm:ss.SSS"
            }
         }
      }
   }
}'

#Create the template for the alarm config messages
curl -H 'Content-Type: application/json' -XPUT http://${es_host}:${es_port}/_template/alarms_config_template?include_type_name=true -d'
{
   "index_patterns":[
      "*_alarms_config*"
   ],
   "mappings":{
      "alarm_config":{
         "properties":{
            "APPLICATION-ID":{
               "type":"text"
            },
            "config":{
               "type":"keyword"
            },
            "user":{
               "type":"keyword"
            },
            "host":{
               "type":"keyword"
            },
            "enabled":{
               "type":"keyword"
            },
            "latching":{
               "type":"keyword"
            },
            "config_msg":{
               "type":"keyword"
            },
            "message_time":{
               "type":"date",
               "format":"yyyy-MM-dd HH:mm:ss.SSS"
            }
         }
      }
   }
}'