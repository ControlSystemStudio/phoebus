TimescaleDB Details
===================

The main purpose of using a TimescaleDB hypertable instead of a plain RDB table
for storing the archive data samples is the support for partitioning aka chunking.
While nearly transparent to the archive engine as well as the data retrieval,
chunking improves the performance and maintainability of the archive.

The following is a collection of information on configuring and monitoring the chunking.
While certain aspects of the hypertable can be changed at runtime,
a complete reorganization of data into a new chunking setup is expensive.
The chunking setup should thus be "right" from the start.
At the time of this writing, however, there is no operational experience that would allow us to
make qualified suggestions for the "best" chunking configuration.
Details of the TimescaleDB options might also change.

More more, refer to https://docs.timescale.com/timescaledb/latest/


Connecting to the database
--------------------------

All the following examples use the `psql` command line tool.
Based on the initial setup described in [2 Configure Database](2_ConfigureDatabase.md),
examples that only require read access can be performed as

    /usr/pgsql-14/bin/psql -U report -W tsarch

with the password `$report`. For write access, use

    /usr/pgsql-14/bin/psql -Utsarch -W tsarch

with the password `$tsarch`. For complete access, use

    sudo su postgres
    psql tsarch


Version Info
------------

Postgres and the TimescaleDB extension versions,
which were 14.2 and 2.6.0 for the initial archive implementation:
 
    SELECT version() AS PSQL,
           extversion AS Timescale
    FROM pg_extension WHERE extname = 'timescaledb';

Partitioning
------------

The `sample` table must be partitioned by `smpl_time` to get basic time-series support.
In addition, it can be partitioned by `channel_id`, which creates an index by channel ID
which we would otherwise have to create separately. if we didn't

You need to pick a time period for the chunking based on time interval,
and a count for the number of different partitions by `channel_id`,
aka. "space partitions".

The basic options are

 * Hypertable with chunks by time, no `channel_id` partitions.
   This supports the basic time-series behavior,
   allows compressing older chunks,
   and you can easily delete old chunks to recover disk space.
 * Hypertable with chunks by time, further partitioned by `channel_id`.
   Results in smaller chunks which can fit into memory and speed up access,
   but creates more chunks, which eventually impacts performance,
   https://github.com/timescale/timescaledb/issues/2897
 * Hypertable with chunks by time and `channel_id`, spreading chunks across table spaces within one node.
   Table spaces on dedicated disks allow parallel access.
 * Hypertable with chunks by time and `channel_id`, spreading chunks across different nodes.
   Adds network latency but allows load balancing.

The best configuration is likely site-specific.
At this time we cannot offer much guidance beyond pointing to
https://docs.timescale.com/timescaledb/latest/how-to-guides/hypertables/best-practices:
 
 * "Time interval should be chosen such that recent chunk fits into memory."
 * "TimescaleDB does  _not_  benefit from a very large number of space partitions."
 * "In most cases, it is advised for users not to use space partitions."
 * "We recommend tying the number of space partitions to the number of disks and/or data nodes."
 * "With a RAID setup, no spatial partitioning is required on a single node."
 

Configure and Change Partitioning
---------------------------------

Initial setup is done in setup.sql when the `sample` table is turned into a hypertable,
for example like this:

    -- Partition by time (default: 7 days), then by channel ID (5 space partitions)
    SELECT * FROM create_hypertable('sample', 'smpl_time', 'channel_id', 5);

To check the current hypertable settings:

    SELECT * from timescaledb_information.dimensions;

The chunk time interval can be changed, affecting  _new_  chunks, like this:

    # Change to monthly time chunks
    SELECT set_chunk_time_interval('sample', INTERVAL '1 month');

Space partitioning, i.e., chunking based on the `channel_id`,
cannot easily be changed on a running system.
https://docs.timescale.com/api/latest/hypertable/add_dimension/ mentions
that space partitions can be added, but only on an empty hypertable.
There is no `remove_dimension` function.

As a workaround, which can also be used to re-chunk existing data for a new time interval,
you can update partitions by creating a new schema, copying data over, then renaming the tables and deleting the old one.

    -- In new schema, create a new 'sample' table with desired settings
    CREATE SCHEMA update;
    CREATE TABLE update.sample (....;
    -- 1 monthly chunk for all channels
    SELECT * FROM create_hypertable('update.sample', 'smpl_time', 'channel_id', 1);
    SELECT set_chunk_time_interval('update.sample', INTERVAL '1 month');
    SELECT * from timescaledb_information.dimensions;
   
    -- Import old data into new table, do this for example month by month:
    \timing on    
    INSERT INTO update.sample
      SELECT smpl_time, nanosecs, channel_id, severity_id, status_id, num_val, float_val, str_val
      FROM public.sample WHERE smpl_time >= '2019-01-01' AND smpl_time < '2019-02-01';
    -- With original example data, 1 month required about 15 minutes and 15 GB.
 
To swap the old/new table format, move them between schemata like this:

    -- Move old table out of 'public'
    CREATE SCHEMA old;
    ALTER TABLE public.sample SET SCHEMA old;
 
    -- Move updated table into 'public' and allow report account to read from it
    ALTER TABLE update.sample SET SCHEMA public;
    DROP SCHEMA update;
    GRANT SELECT ON sample TO report;
    
    -- Import more data, now that the 'public' table is the updated one
    \timing on    
    INSERT INTO sample
      SELECT smpl_time, nanosecs, channel_id, severity_id, status_id, num_val, float_val, str_val
      FROM old.sample WHERE smpl_time >= '2019-07-01' AND smpl_time < '2019-08-01';

    -- Compress older chunks as shown below

    -- When no longer needed, the old data can be removed
    DROP TABLE old.sample;
    DROP SCHEMA old;
    
    
Overall Statistics, Chunk Information
-------------------------------------

Channel count:

    SELECT count(*) from channel; 

How many samples are there?

    -- Full count takes _forever_:
    --  SELECT count(*) FROM sample;
    --
    -- This estimate is comparably fast
    -- First call to ANALIZE can take a while
    ANALYZE sample;
    -- and this is then instantaneous
    SELECT * FROM approximate_row_count('sample');
 
Overall `sample` size:

    # https://en.wikipedia.org/wiki/Gigabyte definition
    SELECT hypertable_size('sample') / 1e9 AS GigaByte;
    
    # Close to result of
    #   du -hc /var/lib/pgsql/14/data/base
    SELECT hypertable_size('sample') / (1024.0^3) AS GigaByte;

Average bytes per sample (170):

    SELECT hypertable_size('sample') * 1.0 / approximate_row_count('sample') AS Bytes_per_Sample;

Display chunk settings, which apply to newly added data:

    SELECT * from timescaledb_information.dimensions;

How many chunks are there?

    SELECT * FROM timescaledb_information.hypertables;

List all chunks:

    # Note 'Child tables:'
    \d+ sample
    
    # Just list the chunks:
    SELECT show_chunks('sample');

Details for each chunk:

    SELECT * FROM timescaledb_information.chunks ORDER BY range_end;
    
    SELECT hypertable_name, chunk_schema, chunk_name, range_start, range_end, is_compressed
    FROM timescaledb_information.chunks ORDER BY range_end;

Display chunk details for one year:
    
    SELECT * FROM timescaledb_information.chunks WHERE range_start >= '2021-01-01' AND range_end <= '2022-01-01' ORDER BY range_end;
    
Example result: In a setup with 2 chunks by `channel_id`,
you might find that 2 chunks hold data for the same time range.
Displaying the detail for those 2 chunks will show that they have the same `smpl_time` constraint but adjacent `channel_id` ranges:


    \d _timescaledb_internal._hyper_1_6_chunk
    ...
    CHECK (_timescaledb_internal.get_partition_hash(channel_id) < 1073741823)
    ...
    \d _timescaledb_internal._hyper_1_7_chunk
    ...
    CHECK (_timescaledb_internal.get_partition_hash(channel_id) >= 1073741823)
    ...


Chunk size:

    SELECT * FROM chunks_detailed_size('sample');
    
    SELECT min(total_bytes)/1e6 AS Min_MB, max(total_bytes)/1e6 AS Max_MB, avg(total_bytes)/1e6 AS Avg_MB FROM chunks_detailed_size('sample');
    
    SELECT c.chunk_schema || '.' || c.chunk_name AS Chunk, s.total_bytes/(1000^3) AS Gigabytes, c.range_start, c.range_end
    FROM timescaledb_information.chunks c
    JOIN chunks_detailed_size('sample') s ON s.chunk_schema = c.chunk_schema AND s.chunk_name = c.chunk_name
    ORDER BY c.range_end;
    

Size of chunks for one year

    SELECT c.chunk_schema, c.chunk_name, d.total_bytes/1e6 AS Megabytes
    FROM chunks_detailed_size('sample') d
    JOIN timescaledb_information.chunks c ON c.chunk_schema = d.chunk_schema AND c.chunk_name = d.chunk_name
    WHERE c.range_start >= '2021-01-01' AND c.range_end <= '2022-01-01'
    ORDER BY c.range_end;

    SELECT sum(d.total_bytes)/1e6 AS Megabytes
    FROM chunks_detailed_size('sample') d
    JOIN timescaledb_information.chunks c ON c.chunk_schema = d.chunk_schema AND c.chunk_name = d.chunk_name
    WHERE c.range_start >= '2021-01-01' AND c.range_end <= '2022-01-01';


Compression
-----------

Older chunks can be compressed, which conserves disk space
and re-orders data by `channel_id` to streamline access to samples for one channel at a time.
`EXPLAIN` shows that the normal indices are ignored when reading compressed chunks.

Manually compress older chunks
------------------------------
    
Enable compression:

    ALTER TABLE sample SET (
      timescaledb.compress,
      timescaledb.compress_segmentby = 'channel_id'
    );

Determine which chunks to compress:

    SELECT hypertable_name, chunk_schema, chunk_name, range_start, range_end, is_compressed
    FROM timescaledb_information.chunks ORDER BY range_end;

    SELECT show_chunks('sample');    
    SELECT show_chunks('sample', older_than => DATE '2021-02-01');
    
    -- Compressing takes a few minutes per chunk
    \timing on    
    SELECT compress_chunk('_timescaledb_internal._hyper_2_20_chunk', if_not_compressed=>TRUE);
    SELECT compress_chunk('_timescaledb_internal._hyper_2_21_chunk', if_not_compressed=>TRUE);
    
    -- Identify chunks within a time range ...
    SELECT show_chunks('sample', newer_than => DATE '2020-12-01', older_than => DATE '2021-02-02');
    -- .. and compress them
    SELECT compress_chunk(i, if_not_compressed=>TRUE) 
    FROM show_chunks('sample', newer_than => DATE '2020-12-01', older_than => DATE '2021-02-02') i;
    
Compression info:

    SELECT * FROM hypertable_compression_stats('sample');

    -- Compresses to about 10% of original size
    SELECT * FROM chunk_compression_stats('sample');
    SELECT chunk_name,
           after_compression_total_bytes * 100.0 / before_compression_total_bytes AS percent
      FROM chunk_compression_stats('sample')
      ORDER BY chunk_name;
      
    SELECT min(percent), max(percent), avg(percent)
    FROM ( SELECT after_compression_total_bytes * 100 / before_compression_total_bytes AS percent
           FROM chunk_compression_stats('sample') ) AS data;
      
      
Manually decompress chunks
--------------------------

Compressed chunks are read-only.
Don't compress older chunks if you plan to import data.
To "INSERT" or "DELETE" samples, if a backfill is necessary,
one option is to create a temporary table and then `CALL decompress_backfill(...)`.

Or manually decompress one or many chunks:

    SELECT decompress_chunk('_timescaledb_internal._hyper_2_21_chunk', if_compressed=>TRUE);

    SELECT decompress_chunk(i, if_compressed=>TRUE) from show_chunks('sample', newer_than => DATE '2021-02-01') i;

Automatically compress older chunks
-----------------------------------
    
    SELECT add_compression_policy('sample', INTERVAL '7 days');
    
    SELECT remove_compression_policy('sample');

    SELECT * FROM timescaledb_information.compression_settings;

Deleting older chunks
---------------------

Delete chunks with samples before some date:

    SELECT drop_chunks('sample', '2017-01-01'::date);

Identify and then drop specific chunk:

    SELECT * FROM timescaledb_information.chunks;
    DROP TABLE _timescaledb_internal._hyper_6_544_chunk;



Tablespace
----------

By default, data is kept under `/var/lib/pgsql/14/data/base/`.
The use of table spaces allows more control over the location on disk,
for example using a specific large vs. fast disk or an NFS mount;
https://docs.timescale.com/latest/using-timescaledb/data-tiering

Example for placing 2019 data in a specific table space:

    \! mkdir -p /var/lib/pgsql/chunks/2019

    CREATE TABLESPACE chunks2019 OWNER postgres LOCATION '/var/lib/pgsql/chunks/2019';

.. and then move chunks there:

    -- Show chunks, move single one
    SELECT show_chunks('sample', newer_than => '2019-01-01', older_than => '2020-01-01');
    SELECT move_chunk(chunk => '_timescaledb_internal._hyper_6_551_chunk', destination_tablespace => 'chunks2019', index_destination_tablespace => 'chunks2019', verbose => TRUE);

    -- Move chunks for one year
    SELECT  move_chunk(chunk => i, destination_tablespace => 'chunks2019', index_destination_tablespace => 'chunks2019', verbose => TRUE)
    FROM show_chunks('sample', newer_than => DATE '2019-01-01', older_than => DATE '2020-01-01') i;

    -- Show current chunk locations
    SELECT hypertable_schema, hypertable_name, chunk_schema, chunk_name, range_start, range_end, is_compressed
    FROM timescaledb_information.chunks WHERE hypertable_schema='public' ORDER BY range_end;

Note that there is no detailed control over the files within a table space.
While other databases like Oracle might offer a "Big File" approach
that results in one large file per table space, allowing backup and restore
of that file while the database instance is idle,
this is not possible with Postgres respectively TimescaleDB.
See https://www.postgresql.org/docs/10/manage-ag-tablespaces.html:

> Files in a tablespace cannot be treated as autonomous data files.
> They depend on metadata in the main data directory, and corrupting one
> will break all.
>  ...
> you cannot control the location of individual files within a logical file system.


Backup
------

No matter if the database resides in the default folder `/var/lib/pgsql/14/data/base/` or in
another table space, Postgres holds the data in several binary files. We can generally not
directly backup and restore these individual data files, in fact doing so might result in a corrupted
database and loss of all the data.

https://docs.timescale.com/timescaledb/latest/how-to-guides/backup-and-restore/ suggests
three options:

 1. Logical backup via the `pg_dump` tool that creates an SQL file with commands to re-create the entire database.
 2. Physical backup via `pg_basebackup` which creates archives of the database
    similar to a direct backup of the files in `/var/lib/pgsql/14/data/base/`.
 3. Ongoing physical backups that start similar to `pg_basebackup` and continually store the
    write-ahead log (WAL) files.

None of these approaches take advantage of the TimescaleDB chunking.
It is specifically not possible to backup/restore selected chunks.
https://docs.timescale.com/timescaledb/latest/how-to-guides/backup-and-restore/pg-dump-and-restore/#backup-entiredb specifically
warns

> Do not use the pg_dump command to backup individual hypertables.
> Dumps created using this method lack the necessary information
> to correctly restore the hypertable from backup.


Update
------

First experiments used PostgreSQL 12.6 and TimescaleDB 2.0.1, then 14.2 and 2.6.0.
While these setups where fully compatible in their API, requiring no changes
to table schema, stored procedures, archive engine or data retrieval code,
they where each newly created, separate database instances.

It is unclear how to update to a new version while keeping existing chunked data.
https://docs.timescale.com/timescaledb/latest/how-to-guides/migrate-data/different-db/#migrate-your-data-into-timescaledb
suggests a complete CSV dump of existing data, then import into new database.
An export of the channel information and then the data for each channel into monthly files
would basically look like this:

    psql -d tsarch -c "\COPY (SELECT * FROM channel) TO channel.csv DELIMITER ',' CSV"
    psql -d tsarch -c "\COPY (SELECT * FROM sample WHERE channel_id=2 AND smpl_time >= '2021-01-01' AND smpl_time < '2021-02-01') TO ch2_2021_01.csv DELIMITER ',' CSV"
    psql ...channel_id=2 .. TO ch2_2021_01.csv ...
    

Long-Term Data Maintenance
--------------------------

Long-term maintenance of the data would ideally allow us to

 * Compress older chunks
 * Delete older chunks
 * Move chunks to different disk locations
 * Backup one chunk at a time
 * Unlink older chunks from the live instance to save disk space
 * Restore older chunks from backup when the data is again needed
 * Re-link restored chunks into the database

Is it easy to compress or delete older chunks.
Chunks can also be moved to different disk locations (aka table spaces).
For the remaining operations, however, TimescaleDB appears to offers no advantage over
the plain Postgres database operations. The database is a "black box"
where we cannot add, remove, backup and restore individual chunks.
