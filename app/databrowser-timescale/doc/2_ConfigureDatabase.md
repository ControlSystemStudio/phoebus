
Configuring TimescaleDB for Archived Data
=========================================

To store archived data in TimescaleDB, you need to create
the appropriate tables, including a "sample" table
that's a TimescaleDB hypertable.

For the archive table schema, see `postgresqsl/setup.sql`.

Should then be able to connect like this:


    /usr/pgsql-14/bin/psql -U tsarch -W tsarch
    Password: $tsarch

or

    /usr/pgsql-14/bin/psql -U report -W tsarch
    Password: $report
