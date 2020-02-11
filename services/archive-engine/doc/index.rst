RDB Archive Engine Service
==========================

The RDB archive engine reads samples from PVs and writes them to an RDB.
The RDB may be MySQL, Posgres or Oracle.
For a production setup, the latter two offer a partitioned table space
that allows managing the data by time.
For smaller setups and to get started, MySQL is very straight forward.

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

     
Extract configuration into an XML file::

    archive-engine.sh -engine Demo -export Demo.xml

Modify the XML file or create a new one to list the channels
you want to archive and to configure how they should be samples.
For details on the 'scanned' and 'monitored' sample modes,
refer to the CS-Studio manual chapter
http://cs-studio.sourceforge.net/docbook/ch11.html

Finally, import the XML configuration into the RDB,
in this example replacing the original one::

    archive-engine.sh -engine Demo -import Demo.xml -port 4812 -replace_engine


Run the Archive Engine
----------------------

To start the archive engine for a configuration::

    archive-engine.sh -engine Demo -port 4812 -settings my_settings.ini
    
The engine name ('Demo') needs to match a previously imported configuration name,
and the port number (4812) needs to match the port number used when importing the configuration.
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
