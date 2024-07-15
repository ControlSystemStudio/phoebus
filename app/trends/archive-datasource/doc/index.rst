Archive Datasources
===================

Overview
--------
The archive datasources allow accessing historical data as a PV. There are two types of archive datasources:

1. `archive`: Retrieves a single archived value at a particular instant of time.
2. `replay`: Creates a PV that recreates changes in values based on data from the archive.

Archive PV Syntax
-----------------

The prefix for the datasource is ``archive://``, which can be omitted if configured as the default datasource.
The archiver PVs are read-only and constant.

- `archive://pv_name`: Retrieves the latest value in the archiver.
- `archive://pv_name(time)`: Retrieves the last value at or before the specified "time".

Replay PV Syntax
----------------

The prefix for this datasource is ``replay://``.
The replay PVs are read-only and constant.

- `replay://pv_name`: Retrieves the last 5 minutes of data for this PV from the archiver and replays them at 10Hz.
- `replay://pv_name(start, end)`: Recreates the PV value changes using the data from the archiver between the specific start and end times.
- `replay://pv_name(start, end, update_rate)`: Recreates the PV value changes using the data from the archiver between the specified start and end times. Updates occur at the rate specified by `update_rate` (a value defined in seconds).

