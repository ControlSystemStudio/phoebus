# Services

Modules in this directory include CS Studio/Phoebus middleware services:

* Alarm Config Logger
* Alarm Logger
* Alarm Service
* Archive Engine
* Save-and-Restore 

Additional middleware services are maintained in other repositories:
* Phoebus Logbook Service (Olog): https://github.com/Olog/phoebus-olog
* Channel Finder Service: https://github.com/ChannelFinder/ChannelFinderService
* Archiver Appliance: https://github.com/archiver-appliance/epicsarchiverap

# Docker

The provided Docker compose file can be used to launch the full stack of 3rd party services upon which
Phoebus services depend:

* Elasticsearch
  * Used by Save-and-Restore, Alarm Logger, Channel Finder and Olog
* MongoDB 
  * Used by Olog (see https://github.com/Olog/phoebus-olog)
* Kafka + Zookeeper
  * Used by Alarm Server and Alarm Logger

To launch the docker compose file in this directory (and docker compose files in sub-directories), user
must create an environment file with the following contents:

```
HOST_IP_ADDRESS=<host IP address>
CONFIG_FILE=<path-to>/Accelerator.xml
CONFIG=Accelerator
ELASTIC_HOST_IP_ADDRESS=<host IP address>
KAFKA_HOST_IP_ADDRESS=<host IP address>
ALARM_SERVICE_SETTINGS_FILE=<path-to>/settings.properties
ALARM_TOPICS=Accelerator
EPICS_PVA_ADDR_LIST=<host IP address>
MONGO_HOST_IP_ADDRESS=<host IP address>
```

Where
* ```<host IP address>```: the IP address where docker is launched.
* ```<path-to>/Accelerator.xml```: absolute path to the ```Accelerator.xml``` alarm config file.
* ```<path-to>/settings.properties```: absolute path to the ```settings.properties``` file. 

The ```settings.properties``` file is needed to define the default EPICS protocol like so:
```org.phoebus.pv/default=pva```

To launch docker, use:

```>docker compose --env-file <path-to-environment-file> -f docker-compose-kafka-elastic-mongodb.yml up```

The ```--env-file <path-to-environment-file>``` can be omitted if the file resides in current directory and is named
exactly ```.env```.
