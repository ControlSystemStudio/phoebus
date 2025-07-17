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

Docker compose files are provided to cover two use cases:

1. ```docker-compose-alarm-server-only-import.yml``` will run the alarm server for the
    purpose of importing a configuration. 
2. ```docker-compose-alarm-server-only.yml``` will run the alarm server. 

The docker compose files do **not** launch 3rd party services needed by the alarm service. User is advocated to
consult https://github.com/ControlSystemStudio/phoebus/blob/master/services/README.md for information on how
to prepare the required environment and alarm service settings files, and how launch these services through docker.

To launch docker, use:
```>docker compose --env-file <path-to-environment-file> -f docker-compose-alarm-server-only*.yml up```

**NOTE:** Accessing IOCs over pva (default mode in the alarm serv) works **only** if IOC is running on the
same host as the Docker container. Moreover, this has been verified to work only on Linux.
