RDB Archive Engine Service
==========================

The RDB archive engine reads samples from PVs and writes them to an RDB.
The RDB may be MySQL, PostgreSQL or Oracle.
For smaller setups and to get started, MySQL is very straight forward
and will be described in here.
For a production setup, PostgreSQL or Oracle can use partitioned table space
that allows better data management over time.
See https://github.com/ControlSystemStudio/phoebus/tree/master/app/databrowser-timescale
for more on using PostgreSQL with the TimescaleDB extension to
partition data and get optimized data retrieval.

Once the RDB is configured with the archive table schema,
the archive engine is used both as a command line tool to configure the
archive settings and as a service to write samples from PVs to the RDB.
You can build the archive engine from sources or fetch a binary from
https://controlssoftware.sns.ornl.gov/css_phoebus


Install MySQL (Centos Example)
------------------------------

Install::

    sudo yum install mariadb-server

Start::

    sudo systemctl start mariadb

Set root password, which is initially empty::

    /usr/bin/mysql_secure_installation

In the following we assume you set the root password to ``$root``.
To start RDB when computer boots::

    sudo systemctl enable mariadb.service


Create archive tables
---------------------

Connect to mysql as root::

    mysql -u root -p'$root'

and then paste the commands shown in ``services/archive-engine/dbd/MySQL.dbd``
(available online as
https://github.com/ControlSystemStudio/phoebus/blob/master/services/archive-engine/dbd/MySQL.dbd )
to create the table setup for archiving PV samples.

The provided database schema is meant as an example, concentrating on the essential
tables. It uses a single large ``sample`` table. A production setup
might prefer to partition the table by for example creating a new partition each month.

The schema as provided does not rely on table constraints.
For example, while the ``chan_grp.eng_id`` should refer to a valid
``smpl_eng.eng_id``, there may not be a foreign key constraint to
enforce this.
This has been done to minimize RDB overhead, using the database simply
as "storage" and enforcing the correctness of the data inside the archive engine
when it is importing a configuration or adding samples.
For a production setup, you may want to add or remove constraints as desired.


View Archive Data
-----------------

The default settings for the Phoebus Data Browser check for archived data in
``mysql://localhost/archive``. To access MySQL on another host,
change these settings in your :ref:`preference_settings`  ::

    org.csstudio.trends.databrowser3/urls=jdbc:mysql://my.host.site.org/archive|RDB
    org.csstudio.trends.databrowser3/archives=jdbc:mysql://my.host.site.org/archive|RDB

The ``MySQL.dbd`` used to install the archive tables adds a few demo samples
for ``sim://sine(0, 10, 50, 0.1)`` around 2004-01-10 13:01, so you can simply
add that channel to a Data Browser and find data at that time.



List, Export and Import Configurations
--------------------------------------

List configurations::

    archive-engine.sh -list
    Archive Engine Configurations:
    ID  Name     Description        URL
     1  Demo     Demo Engine        http://localhost:4812


<<<<<<< Updated upstream
Extract configuration into an XML file::
=======
     
Extract configuration into an XML file::
>>>>>>> Stashed changes

    archive-engine.sh -engine Demo -export Demo.xml

For a description of the XML schema, see ``archive_config.xsd``.

Modify the XML file or create a new one to list the channels
you want to archive and to configure how they should be samples.
For details on the 'scanned' and 'monitored' sample modes,
refer to the CS-Studio manual chapter
http://cs-studio.sourceforge.net/docbook/ch11.html

Finally, import the XML configuration into the RDB,
in this example replacing the original one::

    archive-engine.sh -engine Demo -import Demo.xml -port 4812 -replace_engine


PV Name Details
---------------

The archive engine uses CS-Studio PV names.
"ca://xxxx" will force a Channel Access connection,
"pva://xxxx" will force a PV Access connection,
and just "xxxx" will use the default PV type
configurable via

    org.phoebus.pv/default=ca

Since EPICS 7, IOCs can support both protocols.
"xxxx", "ca://xxxx" and "pva://xxxx" will thus
refer to the same record on the IOC.

The preference setting

    org.csstudio.archive/equivalent_pv_prefixes=ca, pva

causes the archive engine to treat them equivalent as well.
For details, refer to the description of the
`equivalent_pv_prefixes` preference setting.


Run the Archive Engine
----------------------

To start the archive engine for a configuration::

    archive-engine.sh -engine Demo -port 4812 -settings my_settings.ini
<<<<<<< Updated upstream

The engine name ('Demo') needs to match a previously imported configuration name,
and the port number (4812) needs to match the port number used when importing the configuration.
=======
    
The engine name ('Demo') needs to match a previously imported configuration name,
and the port number (4812) needs to match the port number used when importing the configuration.
>>>>>>> Stashed changes
The settings (my_settings.ini) typically contain the EPICS CA address list settings
as well as archive engine configuration details, see archive engine settings
in :ref:`preference_settings`.

In a production setup, the archive engine is best run under ``procServ``
(https://github.com/ralphlange/procServ).

The running archive engine offers a simple shell::

    INFO Archive Configuration 'Demo'
    ...
    INFO Web Server : http://localhost:4812
    ...
    >
    > help
    Archive Engine Commands:
    help            -  Show commands
    disconnected    -  Show disconnected channels
    restart         -  Restart archive engine
    shutdown        -  Stop the archive engine

In addition, it has a web interface accessible under the URL shown at startup
for inspecting connection state, last archived value for each channel and more.
The engine can be shut down via either the ``shutdown`` command entered
on the shell, or by accessing the ``stop`` URL.
For the URL shown in the startup above that would be ``http://localhost:4812/stop``.
