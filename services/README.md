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
services in this repository depend:

* Elasticsearch
  * Used by Save-and-Restore, Alarm Logger, Channel Finder and Olog
* MongoDB 
  * Used by Olog
* Kafka + Zookeeper
  * Used by Alarm Server and Alarm Logger

The Docker compose file depends on the environment variable ```HOST_IP_ADDRESS```, which must be set
to the IP address of the host running the Docker container. Kafka clients must use this as the
```bootstrap-server``` IP address.

Docker supports environment variables to be set in a file (default ```.env``` in current directory) like so:

```HOST_IP_ADDRESS=1.2.3.4```
.  
.  
.

This may be preferable compared to setting environment variables on command line, e.g.

```>export HOST_IP_ADDRESS=1.2.3.4```.