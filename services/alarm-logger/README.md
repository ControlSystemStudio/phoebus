# Alarm Logging

Logging alarm state and other messages to an elastic back end.

## Dependencies ##
1. elastic version 6+ ([to install elastic](https://www.elastic.co/products)).

### Start elasticsearch

    bin/elasticsearch

elasticsearch defaults to port 9200, however if elasticsearch is configured to use another port this can be set in

    /src/main/resources/alarm_logging_preferences.properties


### Create an elasticsearch index

Once, elasticsearch is set up, run the create\_alarm\_index.sh script.

The  argument to the start up script should be the root of the alarm tree where all letters are lower case.

For example, with the alarm tree titled 'Accelerator', the script would be run like

    sh create_alarm_index.sh accelerator
    
this would result in an elasticsearch index titled 'accelerator_alarms' to be created.

### Run the alarm logging service

Start the alarm logging service
    
    java AlarmService
    
    java -jar service-alarm-logger.jar
    
    ...
