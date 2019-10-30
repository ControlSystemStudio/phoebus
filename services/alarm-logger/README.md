# Alarm Logging

Logging alarm state and other messages to an elastic back end.

## Dependencies ##
1. elastic version 6+ ([to install elastic](https://www.elastic.co/products)).

### Start elasticsearch

    bin/elasticsearch

elasticsearch defaults to port 9200, however if elasticsearch is configured to use another port this can be set in

    /src/main/resources/alarm_logging_preferences.properties


### Create an elasticsearch index

Once, elasticsearch is set up, run the `create_alarm_index.sh` script if you intend to create just one large index,
or use `create_alarm_template.sh` if you indent to create a new index each day, week or month.

When using one index, the argument to the start up script should be the root of the alarm tree where all letters are lower case.

For example, with the alarm tree titled 'Accelerator', the script would be run like

    sh create_alarm_index.sh accelerator
    
this would result in an elasticsearch index titled 'accelerator_alarms' to be created.

When using the template, no argument is necessary.

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
java -jar target/alarm-logger-<version>.jar
```

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
