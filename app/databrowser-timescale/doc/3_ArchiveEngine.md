
Configuring an Archive Engine to use TimescaleDB
================================================

The archive engine can write to the TimescaleDB `sample` table
using the existing generic RDB implementation.
For example settings see `engine_settings.ini`.

List configurations:

    archive-engine.sh -settings /path/to/engine_settings.ini -list

Export/Import config:

    archive-engine.sh -settings /path/to/engine_settings.ini -export `pwd`/test.xml
    archive-engine.sh -settings /path/to/engine_settings.ini -import `pwd`/test.xml -engine Test

Run:

    archive-engine.sh -settings /path/to/engine_settings.ini -engine Test


Importing existing data
-----------------------

CopyRDBToTimestampDB can be used to copy samples from an existing
RDB into TimescaleDB.
