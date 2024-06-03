Archive Datasource
==================

Overview
--------
The archive datasource allows accessing historical data as a pv


PV syntax
---------

The standard prefix for the datasource is ``archive://`` which can be omitted if configured as the default datasource.
The archiver PV's are readonly and constants.

archive://pv_name

Retrieves the latest value in the archiver

archive://pv_name(time)

Retrieves the last value at or before the "time"
