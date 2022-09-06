# alarm config logging
Log the configurationData changes made to the alarm server configurationData

### Dependencies ###

### Running ###

1. Run the jar

```
mvn clean install
java -jar target/alarm-config-logger-<version>.jar
```

2. Using spring boot  
```mvn spring-boot:run```

### Description ###

The alarm config model creates a git repository, sharing the same name as the alarm topic, which is used to keep track of the alarm configurationData changes.

The repo structure is as follows.

<pre>
Accelerator/  
    .restore-script/config.xml  # It consists of an XMl dump of the alarm server configurationData after each config change  
    Node1/  
        alarmconfig.json 	# A json representation of the alarm configurationData of this node  
        PV:alarmPV1/  
            alarmconfig.json	# A json representation of the alarm configurationData of this pv  
        PV:alarmPV1/  
        PV:alarmPV1/  
</pre>

The split between the config.xml and the file structure is to simplify the process of auditing the changes associated with a single pv of node within the alarm tree. The use of only the version controlled config.xml would require sifting through all the changes on the alarm tree.
