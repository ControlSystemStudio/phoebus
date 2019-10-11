Save-and-restore service
========================

The save-and-restore service implements the MASAR (MAchine Save And Restore) service as a collection
of REST endpoints. These can be used by clients to manage save sets (aka configurations) and
snapshots, to compare snapshots and to restore settings from snapshots.

The service is packaged as a self-contained Spring Boot jar file, i.e. there are no external dependencies besides the
JVM and the database engine persisting the data. The service is verified for Postgresql and Mysql, but alternative
engines can be added with moderate effort, see below for details.

Running the service
-------------------

To run the service, connection parameters for the database must be specified on the command line, or
in existing property files (mysql.properties or postgresql.properties). Typical command line would be:

``java -Ddbengine=[postgresql|mysql]
-Dspring.datasource.username=<DB user name>
-Dspring.datasource.password=<DB password>
-Dspring.datasource.jdbcUrl=<DB engine URL>
-jar /path/to/service-save-and-restore-<version>.jar``

Where

``-Ddbengine`` must be specified to either of the supported database engines. This parameter selects the properties
file containing other settings (mysql.properties or postgresql.propties).

``-Dspring.datasource.username`` specifies the database engine user name. Can be specified in the properties file.

``-Dspring.datasource.password`` specifies the database engine password. Can be specified in the properties file.

``-Dspring.datasource.jdbcUrl`` specifies the database URL required by the JDBC driver. Can be specified in the
properties file.

Database setup
--------------

In order to deploy the service, one must create a database (schema) in the selected database engine matching the
connection paramaters. When the service is started, Flyway scripts will create the required tables. New versions
of the service that require changes to the database structure will also use Flyway scripts to perform necessary
actions on the database.

Alternative database engines
----------------------------

Currently the save-and-restore service does not use an ORM layer (e.g. Hibernate). To support a database engine
other than Postgresql or Mysql, use this checklist:

- Include the required JDBC driver.
- Create a <my favourite DB engine>.properties file containig the driver class name and paths to Flyway scripts.
  The name of the file must match the dbengine value on the command line.
- Create Flyway scripts for the database. Use existing as starting point.
- Configure command line paramaters.
- Verify.

Note that the persistence layer contains hard coded SQL which may be invalid for other database engines. If
there is a need to modify the SQL statement, please discuss this with the community as addition of ORM may be a
better alternative.
