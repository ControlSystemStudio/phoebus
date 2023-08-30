Services
========

The following sections describe available services.


.. toctree::
   :maxdepth: 1
   
   services/archive-engine/doc/index
   services/alarm-config-logger/doc/index
   services/save-and-restore/doc/index
   services/alarm-server/doc/index
   services/alarm-logger/doc/index

Settings configuration
~~~~~~~~~~~~~~~~~~~~~~

Example of settings.ini 

**Alarm Server**

| org.phoebus.email/mailhost=smtp.com
| org.phoebus.applications.alarm/server=localhost:9092
| org.phoebus.applications.alarm/enable_slot_time=false

**Alarm Logger**

| org.phoebus.applications.alarm.logging.ui/service_uri=http://localhost:8082
| org.phoebus.applications.alarm.logging.ui/results_max_size=10000

**Olog**

| #Logbook
| org.phoebus.logbook.ui/logbook_factory=olog-es
| org.phoebus.logbook/logbook_factory=olog-es
| org.phoebus.logbook.olog.ui/save_credentials=true
| #Olog
| org.phoebus.olog.es.api/olog_url=http://localhost:8081/Olog
| org.phoebus.olog.es.api/username=admin
| org.phoebus.olog.es.api/password=adminPass

**Archive Appliance**

| org.csstudio.trends.databrowser3/urls=pbraw://localhost:17668/retrieval

**Save & Restore**

| org.phoebus.applications.saveandrestore.datamigration.git/jmasar.service.url=http://localhost:8084

**Channel Finder**

| org.phoebus.channelfinder/serviceURL=http://localhost:8080/ChannelFinder
