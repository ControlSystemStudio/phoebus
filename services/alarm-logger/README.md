# Alarm Logging

Logging alarm state and other messages to an elastic back end.

## Dependencies ##
1. Elasticsearch version 6.x. As the current major version is 7, you will need to find an archived 6.x release here:
https://www.elastic.co/downloads/past-releases#elasticsearch

### Start elasticsearch

    bin/elasticsearch

elasticsearch defaults to port 9200, however if elasticsearch is configured to use another port this can be set in

    /src/main/resources/alarm_logging_preferences.properties


### Create an elasticsearch index

Use `/startup/create_alarm_template.sh` if you indent to create a new index each day, week or month.

### Run the alarm logging service

#### Build the alarm server

``` 
mvn clean install
```
or

```
ant clean dist
```

#### Start the alarm logging service

1. Run the jar

```
java -jar target/alarm-logger-<version>.jar -topics MY,ALARM,CONFIGS
```
The argument for the ```-topics``` switch is the comma separated list of alarm configurations you wish to be 
logged. An alarm configuration is the value specified 
for the ```-config``` switch when starting an alarm server instance. See the README.md file in the alarm-server module.

2. Using spring boot  

```
mvn spring-boot:run
```

Run with `-help` to see command line options,
including those used to create daily, weekly or monthly indices.

### Query the Data

For more elaborate queries, use Kibana or other elasticsearch clients.
For a first test, this should list one or more `*alarm*` indices:

```
curl -X GET 'http://localhost:9200/_cat/indices?v'
```

This dumps records from one specific index:

```
curl -X GET 'http://localhost:9200/accelerator_alarms_state_2019-02-01/_search?format=json&pretty'
```

### Cleanup

In case you want to delete older data:

```
curl -X DELETE 'localhost:9200/accelerator_alarms_state_2019-02-*'
```
