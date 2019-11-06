# Save and restore service

The save-and-restore service implements the MASAR (MAchine Save And Restore) service as a collection
of REST endpoints. These can be used by clients to manage save sets (aka configurations) and
snapshots, to compare snapshots and to restore settings from snapshots.

The service depends on the app-save-and-restore-model module. 

Data is persisted by a relational database engine. The service has been verified on Postgresql and Mysql.

# Build

Build the Phoebus product.

# Run

NOTE: the service is based on Spring Boot using a default configuration. Consequently the build artifact contains 
all dependencies. As such it is launched as a stand-alone jar, see below. To convert the build artifact to a war file for 
deployment to an application server, see https://spring.io/guides/gs/convert-jar-to-war/ 

Run the service like so:

java [options] -jar service-save-and-restore-<version>.jar

Where mandatory [options] are:
-Ddbengine=[postgresql | mysql] 
-Dspring.datasource.username=<database user name>
-Dspring.datasource.password=<database password>
-Dspring.datasource.jdbcUrl=<database JDBC URL, e.g. jdbc:postgresql://localhost:5432/masar>
-Dhostname=<hostname/IP address of database>

# Features

* The service supports a tree structure of objects. Nodes in the tree are
folders, configurations (aka save sets) and snapshots.

* There is always a top level root node of type folder. This cannot be modified
in any manner.

* Child nodes of folder nodes are folder or configuration nodes. Child nodes
of configuration nodes are only snapshot nodes. Snapshot nodes do not contain
child nodes

* Snapshot nodes are associated with snapshot items (stored PV values) 
not part of the tree structure.

* Each node can be associated with an arbitrary number of string properties, e.g.
a "golden" property can be set on snapshot nodes.

* Each node has a created date and a last updated date, as well as a user name
attribute. This should identify the user creating or updating a node.

* Nodes in the tree can be renamed or deleted. When a folder or configuration
node is deleted, all its child nodes are deleted unconditionally.

* If PV is deleted from a save set, the corresponding PV values of all snapshots
associated with the save set are also deleted. 

* A folder or configuration node can be moved to another parent node. All
child nodes of the moved node remain child nodes of the moved node.

* Snapshot nodes cannot be moved as they are closely associated with the save set
defining the list of PVs in the snapshot.

* The service is built upon Spring Boot and depends on a persistence 
implementation. In its current version, persistence is implemented against
a RDB engine, using Spring's JdbcTemplate to manage SQL queries. It has been 
verified on Postgres 9.6 and Mysql 8.0, on Mac OS, as well as on
Postgres 9.6 on CentOS 7.4. Database 
connection parameters are found in src/main/resources.

* Flyway scripts for Postgres and Mysql are provided to set up the database. 
Flyway is run as part of the application startup, i.e. there is no need to 
run Flyway scripts manually. The Flyway scripts will create the top level folder.

* Unit tests rely on the H2 in-memory database and are independent of any
external database engine. Flyway scripts for the H2 database are found
in src/test/resources/db/migration. Running the unit tests will create the H2
database file (h2.db.mv.db) in a folder named db relative to the current directory.

Missing features:

* Security in terms of authentication and authorization.

* Search for configurations or snapshots.

* JPA as persistence framework, which would make transition to database engines
other than Postgresql or Mysql easier, and also hide some of the issues with
differences in SQL dialects.
