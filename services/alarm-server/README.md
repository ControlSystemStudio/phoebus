# Alarm Server

An alarm server for the epics control system which monitors and processes the states of hundreds or thousands of pv's and produces well described, manageable, and actionable alarms for users.

It was developed based on experience with the original EPICS alarm systems like ALH and BEAST combined with ideas from the book Alarm Management: Seven Effective Methods for Optimum Performance by B. Hollifield and E. Habibi, published by ISA in 2007.


## Building ##

### Requirements
 - [JDK11 or later, suggested is OpenJDK](http://jdk.java.net/11).
   (For the time being, Phoebus still builds with Oracle JDK 9 and 10,
    but it must be a JDK that includes JavaFX).
 - [maven 2.x](https://maven.apache.org/) or [ant](http://ant.apache.org/)

The alarm server can be build using maven or ant.

### Build with maven

```
mvn clean install
```

### Build with ant

```
ant clean build
```
### Start Alarm server

Before starting the alarm server you will need to ensure that you have set up the kafka cluster as described here https://github.com/ControlSystemStudio/phoebus/tree/master/app/alarm

With the Kafka cluster running you can start the alarm server using the provided script

```
./alarm-server.sh
```

some useful startup arguments include

```
-help                       - Help
-server   localhost:9092    - Location of Kafka server
-config   Accelerator       - Alarm configuration
-settings settings.xml      - Import preferences (PV connectivity) from property format file
-export   config.xml        - Export alarm configuration to file
-import   config.xml        - Import alarm configruation from file
-logging logging.properties - Load log settings
```

## Docker

Docker compose files are provided for convenience to cover two use cases:

1. ```docker-compose-alarm-server-only.yml``` will run the alarm server. Required environment variables:
    * ```KAFKA_HOST_IP_ADDRESS``` must be set to identify the Kafka server external IP address.
    * ```CONFIG``` identifies the configuration (topic) name. 
    * ```ALARM_SERVICE_SETTINGS_FILE``` file in user's home directory specifying EPICS related settings, e.g.
      ```org.phoebus.pv/default=pva```.
2. ```docker-compose-alarm-server-only-import.yml``` will run the alarm server for the
    purpose of importing a configuration. Environment variables must be se as in case 1.
    Additionally, the environment variable ```CONFIG_FILE``` must identify an alarm
    configuration file in the current user's home directory.

Docker supports environment variables to be set in a file (default ```.env``` in current directory) like so:

```KAFKA_HOST_IP_ADDRESS=1.2.3.4```  
```CONFIG=Accelerator```  
.  
.  
.  

This may be preferable compared to setting environment variables on command line, e.g. 

```>export KAFKA_HOST_IP_ADDRESS=1.2.3.4```.

**NOTE:** Accessing IOCs over pva (default mode in the Docker compose files) works **only** if IOC is running on the
same host as the Docker container. Moreover, this has been verified to work only on Linux.