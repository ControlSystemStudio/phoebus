# Alarm Logging

Logging alarm state and other messages to an elastic back end.

## Dependencies ##
1. Elasticsearch version 8.x OS specific release can be found here:  
https://www.elastic.co/downloads/past-releases#elasticsearch  
The CI/CD pipeline is setup to test with elastic release 8.2.3

### Start elasticsearch

    bin/elasticsearch

elasticsearch defaults to port 9200, however if elasticsearch is configured to use another port this can be set in

    /src/main/resources/alarm_logging_preferences.properties

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

#### Configuration

The alarm logger can be configured via command line switches when running the jar, see option `-help` for details, 
or via properties documented in [here](https://github.com/ControlSystemStudio/phoebus/blob/master/services/alarm-logger/src/main/resources/alarm_logger.properties)




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

## Data Management

The  most common aspects for effectively configuring the alarm logger are: 

### Creating an elasticsearch index

The new elastic indices are created based on the templates which are automatically created
when the service is first launched.

### Index period

By default, the alarm logger will create a new index for each month, named
`{Alarm topic}_state_yyyy_mm_dd` and `..._cmd_...`.
This supports the removal of older data by simply and efficiently deleting older indices.

Queries can read from all these indices by using a pattern like `{Alarm topic}_state_*`,
so they are abstracted from the periodically structured indices.

In practice, monthly indexing, which is also the default, has been proven most useful,
keeping the indices for roughly one year.
While finer grained (weekly or even daily) indexing is possible, it will likely require more frequent index cleanup.

### Cleanup

Obsolete data should be periodically removed, this can be achieved by deleting the indices from elastic which contain
stale data. 

One or more indices can be deleted with the following command:

```
curl -X DELETE 'localhost:9200/accelerator_alarms_state_2019-02-*'
```
