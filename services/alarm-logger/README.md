# Alarm Logging Service

The alarm logging service (aka alarm-logger) records all alarm messages to create an archive of all alarm state changes and the associated actions.
This is an elasticsearch back end.

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

#### Standalone mode

With the `-standalone true` option on the command line or in `application.properties`, you can run the alarm logging service independently of an alarm server.
It may be useful to troubleshoot the system independently from production alarm services using the alarm log service backup for the long alarm log history.

#### Configuration

The alarm logger can be configured via command line switches when running the jar, see option `-help` for details, 
or via properties documented in [here](https://github.com/ControlSystemStudio/phoebus/blob/master/services/alarm-logger/src/main/resources/alarm_logger.properties)

#### Check Service Status

```
http://localhost:8080
```

Will provide information regarding the version of the service along with information about the connection status with the elastic backend.

e.g.

```
{
    "name": "Alarm logging Service",
    "version": "4.7.4-SNAPSHOT",
    "elastic": {
        "status": "Connected",
        "clusterName": "elasticsearch",
        "clusterUuid": "hwn6nGDwTKSm8vzVTqR9Sw",
        "version": "co.elastic.clients.elasticsearch._types.ElasticsearchVersionInfo@51e5581d"
    }
}
```

### Query the Data

Alarm logs can be retrieved using the alarm logging services query REST API's

```
curl -X GET 'localhost:8080/search/alarm?pv=*'
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

## Release

**Prepare the release**  
`mvn release:prepare`  
In this step will ensure there are no uncommitted changes, ensure the versions number are correct, tag the scm, etc.
A full list of checks is documented [here](https://maven.apache.org/maven-release/maven-release-plugin/examples/prepare-release.html).

**Perform the release**  
`mvn -Darguments="-Dskip-executable-jar" -Pdocs,releases release:perform`  
Checkout the release tag, build, sign and push the build binaries to sonatype. The `docs` profile is needed in order
to create required javadocs jars.

# Docker

The latest version of the service is available as a Docker image (ghcr.io/controlsystemstudio/phoebus/service-alarm-logger:master). 
Pushes to the master branch into this directory will trigger a new build of the image.

Docker compose file is provided. It requires the following environment variable to be set:

```KAFKA_HOST_IP_ADDRESS=1.2.3.4```
```ELASTIC_HOST_IP_ADDRESS=1.2.3.4```  
```ALARM_TOPICS```: comma-separated list of alarm topics subscribed to by the service

This may be preferable compared to setting environment variables on command line, e.g.

```>export KAFKA_HOST_IP_ADDRESS=1.2.3.4```.
