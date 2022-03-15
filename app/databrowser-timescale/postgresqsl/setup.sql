-- (Re-)Create TimescaleDB and PostgreSQL tables for archived data
--
-- In principle, this can be run as a whole via
--
--   psql -U postgres -h localhost -f setup.sql
--
-- but note that some sections might require adaptation
-- to local needs.
-- We suggest to copy/paste commands from this document
-- section by section into a psql session
-- to better verify success or see error messages right away.

-- Assume you are connected as the 'postgres' super user:
--
--   sudo su postgres
--   psql -U postgres -h localhost 

-- Suggested database name is 'tsarch'.
-- The original plain RDB archive setup suggested an 'archive' database.
-- The tables we use with TimescaleDB are similar, and in principle you
-- can run both the plain RDB 'archive' and the new TimescaleDB 'tsarch'
-- within the same Postgres server as long as they use different
-- database names like 'archive' vs. 'tsarch'.

DROP DATABASE IF EXISTS tsarch;
CREATE DATABASE tsarch;
\connect tsarch

-- Activate timescaledb support
CREATE EXTENSION IF NOT EXISTS timescaledb;

-- Disable reporting of telemetry which is by default on
ALTER SYSTEM SET timescaledb.telemetry_level=off;


-- Note:
-- Not 100% sure which user should perform the following CREATE ..
-- statements. "postgres" or "tsarch"?
-- In here we assume "postgres",
-- and towards the end we create accounts "tsarch" and "report"
-- which are granted access (read-only for "report")



-------------------------
-- Sample Engines
-------------------------

DROP TABLE IF EXISTS smpl_eng;
DROP SEQUENCE IF EXISTS smpl_eng_engid_seq;

CREATE SEQUENCE smpl_eng_engid_seq;

CREATE TABLE  smpl_eng
(
   eng_id BIGINT NOT NULL PRIMARY KEY DEFAULT nextval('smpl_eng_engid_seq'),
   name VARCHAR(100) NOT NULL,
   descr VARCHAR(100) NOT NULL,
   url VARCHAR(100) NOT NULL
);
INSERT INTO smpl_eng VALUES (1, 'Demo', 'Demo Engine', 'http://localhost:4812');
SELECT * FROM smpl_eng;



-------------------------
-- Channel groups
-------------------------

CREATE SEQUENCE chan_grp_grpid_seq;

DROP TABLE IF EXISTS chan_grp;
CREATE TABLE  chan_grp
(
   grp_id BIGINT NOT NULL PRIMARY KEY DEFAULT nextval('chan_grp_grpid_seq'),
   name VARCHAR(100) NOT NULL,
   eng_id BIGINT NOT NULL,
   descr VARCHAR(100) NULL,
   enabling_chan_id BIGINT NULL
);
INSERT INTO chan_grp VALUES (1, 'Demo', 1, 'Demo Group', NULL);
SELECT * FROM chan_grp;



-------------------------
-- Sample modes
-------------------------

DROP TABLE IF EXISTS smpl_mode;
CREATE TABLE smpl_mode
(
   smpl_mode_id INT NOT NULL PRIMARY KEY,
   name VARCHAR(100) NOT NULL,
   descr VARCHAR(100) NOT NULL
);
INSERT INTO smpl_mode VALUES (1, 'Monitor', 'Store every received update');
INSERT INTO smpl_mode VALUES (2, 'Scan', 'Periodic scan');
SELECT * FROM smpl_mode;



-------------------------
-- Channel names and info
-------------------------

DROP TABLE IF EXISTS channel;
DROP SEQUENCE IF EXISTS channel_chid;

CREATE SEQUENCE channel_chid;

CREATE TABLE channel
(
   channel_id BIGINT NOT NULL PRIMARY KEY DEFAULT nextval('channel_chid'),
   name VARCHAR(100) NOT NULL,
   descr VARCHAR(100) NULL,
   grp_id BIGINT NULL,
   smpl_mode_id INT NULL,
   smpl_val DOUBLE PRECISION NULL,
   smpl_per DOUBLE PRECISION NULL, 
   retent_id INT NULL,
   retent_val DOUBLE PRECISION NULL   
);

-- Speed up lookup of channel_id by name
CREATE INDEX channel_name ON channel(name) INCLUDE (channel_id);

EXPLAIN SELECT channel_id FROM channel WHERE name='test';

INSERT INTO channel(name) VALUES ('test');

-- channel_id is automatically assigned
SELECT * FROM channel;
-- INSERT INTO channel(name) VALUES ('test2') RETURNING channel_id;



-------------------------
-- Severities
-------------------------

DROP TABLE IF EXISTS severity;
DROP SEQUENCE IF EXISTS severity_sevid;

CREATE SEQUENCE severity_sevid; 

CREATE TABLE severity
(
   severity_id SMALLINT NOT NULL PRIMARY KEY default nextval('severity_sevid'),
   name VARCHAR(100) NOT NULL
);

-- If you already have an existing plain RDB 'archive',
-- populate severities table with the same IDs and names
-- to simplify data import.
-- Example values from SNS database
INSERT INTO severity(severity_id, name)
  VALUES (1,'OK'),
         (2,'INVALID'),
         (3,'MAJOR'),
         (4,'MINOR'),
         (5,'MINOR_ACK'),
         (6,'MAJOR_ACK'),
         (7,'INVALID_ACK'),
         (8,'NONE'),
         (9,'UNDEFINED'),
         (10,'UNDEFINED_ACK');

SELECT * FROM severity;



------------------------
-- Status values
-------------------------

DROP TABLE IF EXISTS status;
DROP SEQUENCE IF EXISTS status_statid;

CREATE SEQUENCE status_statid;

CREATE TABLE  status
(
   status_id SMALLINT PRIMARY KEY default nextval('status_statid'),
   name VARCHAR(100) NOT NULL UNIQUE
);

-- If you already have an existing plain RDB 'archive',
-- populate status table with the same IDs and names
-- to simplify data import.
-- Example values from SNS database
INSERT INTO status (status_id, name)
  VALUES (1,'DISABLE_ALARM'),
         (2,'Archive_Off'),
         (3,'OK'),
         (4,'HIHI_ALARM'),
         (5,'UDF_ALARM'),
         (6,'HIGH_ALARM'),
         (7,'SCAN_ALARM'),
         (8,'READ_ALARM'),
         (9,'LINK_ALARM'),
         (10,'STATE_ALARM'),
         (11,'LOW_ALARM'),
         (12,'Disconnected'),
         (13,'LOLO_ALARM'),
         (14,'Write_Error'),
         (15,'WRITE_ALARM'),
         (16,'NaN'),
         (17,'SOFT_ALARM'),
         (18,'CALC_ALARM'),
         (19,'TIMEOUT_ALARM'),
         (20,'COS_ALARM'),
         (21,'Test'),
         (22,'Disabled'),
         (23,'No Connection'),
         (24,'High'),
         (25,'Way High'),
         (26,'Low'),
         (27,'Way Low'),
         (28,'COMM_ALARM'),
         (29,'HW_LIMIT_ALARM'),
         (30,'BAD_SUB_ALARM'),
         (31,'SIMM_ALARM'),
         (32,'READ_ACCESS_ALARM'),
         (33,'WRITE_ACCESS_ALARM'),
         (34,'Archive_Disabled'),
         (35,'NO_ALARM'),
         (36,'NONE'),
         (37,'Starting');

SELECT * FROM status;



-------------------------
-- Channel Meta data: Units etc. for numeric channels
-------------------------

DROP TABLE IF EXISTS num_metadata;
CREATE TABLE  num_metadata
(
   channel_id BIGINT  NOT NULL PRIMARY KEY,
   low_disp_rng DOUBLE PRECISION NULL,
   high_disp_rng DOUBLE PRECISION NULL,
   low_warn_lmt DOUBLE PRECISION NULL,
   high_warn_lmt DOUBLE PRECISION NULL,
   low_alarm_lmt DOUBLE PRECISION NULL,
   high_alarm_lmt DOUBLE PRECISION NULL,
   prec INT NULL,
   unit VARCHAR(100) NOT NULL
);
INSERT INTO num_metadata VALUES (1, 0, 10, 2, 8, 1, 9, 2, 'mA');
SELECT * FROM num_metadata;

-- Enumerated channels have a sample.num_val that can also be interpreted
-- as an enumeration string via this table

DROP TABLE IF EXISTS enum_metadata;
CREATE TABLE enum_metadata
(
   channel_id BIGINT  NOT NULL,
   enum_nbr INT NULL,
   enum_val VARCHAR(120) NULL
);



-------------------------
-- Samples
-------------------------

DROP TABLE IF EXISTS sample;
CREATE TABLE sample
(
  smpl_time TIMESTAMPTZ NOT NULL,
  nanosecs INTEGER  NOT NULL,
  channel_id BIGINT NOT NULL,
  severity_id SMALLINT NOT NULL,
  status_id SMALLINT  NOT NULL,
  num_val INTEGER NULL,
  float_val DOUBLE PRECISION NULL,
  str_val VARCHAR(120) NULL,
  datatype CHAR(1) NULL DEFAULT ' ',
  array_val BYTEA  NULL
);

-- ******************************************** --
-- Turn plain table into TimescaleDB hypertable --
-- ******************************************** --
-- 
-- At this time, the 'tsarch' table structure is fully compatible with
-- a plain RDB 'archive' setup. Now the 'sample' table is instrumented
-- for use with TimescaleDB.
--
-- Unclear what the best parameters are.
-- Certainly need to partition the 'sample' table on the 'smpl_time' time stamp
-- to get time series support.
-- But what time interval?
--
-- We can additionally partition on channel_id, so that data is indexed by channel,
-- and compressed data can be arranged by channel.
-- But how many chunks?
-- From https://docs.timescale.com/timescaledb/latest/how-to-guides/hypertables/best-practices:
--  * "Time interval should be chosen such that more recent chunk fits into memory."
--  * "TimescaleDB does _not_ benefit from a very large number of space partitions."
--  * "In most cases, it is advised for users not to use space partitions."
--  * "We recommend tying the number of space partitions to the number of disks and/or data nodes."
--  * "With a RAID setup, no spatial partitioning is required on a single node."
--
-- ==> Creating 2 space partitions by channel_id since many partitions aren't suggested
--     but want to be able to see partitioning in action for initial tests.
--     See 5_TimescaleDB_Details.md for more.

\d+ sample

SELECT * FROM create_hypertable('sample', 'smpl_time', 'channel_id', 2);

-- Default time interval is 7 days
SELECT * from timescaledb_information.dimensions;

-- This is likely too short for a long-running setup.
-- Should create monthly, not weekly chunks to reduce number of chunks
SELECT set_chunk_time_interval('sample', INTERVAL '1 month');

-- This creates two indices: 
--    "sample_channel_id_smpl_time_idx" btree (channel_id, smpl_time DESC)
--    "sample_smpl_time_idx" btree (smpl_time DESC)
--
-- If the hypertable is NOT partitioned by channel_id, need to manually create that index,
-- for example:
-- CREATE INDEX sample_id ON sample (channel_id);
-- CREATE INDEX sample_id_time ON sample (channel_id, smpl_time, nanosecs);
  
-- TODO What about nanosecs? Include in index?

\d+ sample


-- The combined one is what we want, so no need for a manual creation:
-- CREATE INDEX ON sample (channel_id, smpl_time DESC);
--
-- The other one just on time is not needed, so delete?
-- DROP INDEX sample_smpl_time_idx;
-- .. or add back in if you deleted it?
-- CREATE INDEX sample_smpl_time_idx ON sample(smpl_time DESC);
-- \d+ sample


-- Example for adding data.
-- Batch inserts much faster than single-row inserts.
-- Inserts at end of time range faster than inserting older time intervals.
INSERT INTO sample (channel_id, smpl_time,  nanosecs, severity_id, status_id, float_val)
   VALUES (1, '2004-01-10 13:01:17', 1,  3, 2, 3.16),
          (1, '2004-01-10 13:01:11', 2,  1, 1, 3.16),
          (1, '2004-01-10 13:01:10', 3, 1, 2, 3.15),
          (1, '2004-01-10 13:01:10', 4, 1, 2, 3.14);

EXPLAIN (ANALYZE on, BUFFERS on)
SELECT * FROM sample WHERE channel_id=1 ORDER BY smpl_time DESC LIMIT 10;



-------------------------
-- Default users
-------------------------

-- Example for user with full write access
DROP USER IF EXISTS tsarch;
CREATE USER tsarch WITH PASSWORD '$tsarch';

GRANT SELECT, INSERT, UPDATE, DELETE
  ON smpl_eng, smpl_mode, chan_grp, channel, severity, status, sample, num_metadata, enum_metadata  
  TO tsarch;

GRANT USAGE ON SEQUENCE
  channel_chid 
  TO tsarch;


-- Example for user with only read access
DROP USER IF EXISTS report;
CREATE USER report WITH PASSWORD '$report';

GRANT SELECT
  ON  smpl_eng, smpl_mode, chan_grp, channel, severity, status, sample, num_metadata, enum_metadata
  TO report;


-- Show table access privileges
SELECT * FROM pg_user;
SELECT grantee, privilege_type FROM information_schema.role_table_grants WHERE table_name='sample';
\dp


-------------------------
-- Functions for optimized readout
-------------------------

-- https://www.postgresql.org/docs/current/xfunc-sql.html

-- Plain 'sql' function that performs the optimized readout, always.
-- Client would check the result, and if there are only a few samples,
-- it can follow up by reading raw data

-- DROP FUNCTION read_optimized;

CREATE OR REPLACE FUNCTION read_optimized(p_channel_id BIGINT, p_start TIMESTAMPTZ, p_end TIMESTAMPTZ, p_bucket_secs BIGINT)
RETURNS TABLE(bucket TIMESTAMPTZ, severity_id SMALLINT, status_id SMALLINT, min DOUBLE PRECISION, max DOUBLE PRECISION, avg DOUBLE PRECISION, num_val INTEGER, str_val VARCHAR(120), N BIGINT)
LANGUAGE sql
STABLE
RETURNS NULL ON NULL INPUT
PARALLEL SAFE
AS
$$
    SELECT smpl_time AS bucket, severity_id, status_id, NULL AS min, NULL AS max, NULL AS avg, num_val, str_val, 1 AS N
    FROM sample
    WHERE channel_id=p_channel_id
      AND smpl_time BETWEEN p_start AND p_end
      AND float_val IS NULL

  UNION ALL

    SELECT time_bucket(make_interval(secs=>p_bucket_secs), smpl_time) AS bucket,
           NULL AS severity_id,
           NULL AS status_id,
           MIN(float_val) AS min,
           MAX(float_val) AS max,
           AVG(float_val) AS avg,
           NULL AS num_val,
           NULL AS str_val,
           COUNT(*) AS N
    FROM sample
    WHERE channel_id=p_channel_id
      AND smpl_time BETWEEN p_start AND p_end
      AND float_val IS NOT null
    GROUP BY bucket
  ORDER BY bucket
$$;

-- Returns 'read_optimized' column where each row holds a set
-- SELECT read_optimized(4, '2019-05-30', '2019-06-02', 4300);

-- Looks more like usual table query
-- SELECT * FROM read_optimized(4, '2019-05-30', '2019-06-02', 4300);
-- SELECT bucket, severity_id, status_id, min, max, avg, num_val, str_val, n FROM read_optimized(4, '2019-01-01', '2019-06-02', 24*60*60);

-- Decode stat/sevr
-- SELECT bucket, severity.name AS severity, status.name AS STATUS, min, max, avg, num_val, str_val, n
--  FROM read_optimized(4, '2019-01-01', '2019-06-02', 24*60*60) data
--  LEFT OUTER JOIN severity ON severity.severity_id = data.severity_id
--  LEFT OUTER JOIN status ON status.status_id = data.status_id;


-- More complex PL/pgsql function that automatically
-- selects raw or optimized readout.
-- Client simply calls this function, no need to fall back
-- to another query for raw data

-- DROP FUNCTION auto_optimize;

CREATE OR REPLACE FUNCTION auto_optimize(p_channel_id BIGINT, p_start TIMESTAMPTZ, p_end TIMESTAMPTZ, p_bucket_count BIGINT)
RETURNS TABLE(bucket TIMESTAMPTZ, severity_id SMALLINT, status_id SMALLINT, min DOUBLE PRECISION, max DOUBLE PRECISION, avg DOUBLE PRECISION, num_val INTEGER, str_val VARCHAR(120), N BIGINT)
LANGUAGE plpgsql
STABLE
RETURNS NULL ON NULL INPUT
PARALLEL SAFE
AS
$$
DECLARE
   initial TIMESTAMPTZ;
   count BIGINT;
   dummy DOUBLE PRECISION := NULL;
   bucket_size INTERVAL;
BEGIN
    -- Determine size of one bucket based on original start..end range and count
    bucket_size := (p_end-p_start) / p_bucket_count;

    -- Determine actual start time, last sample at-or-before requested start
    SELECT smpl_time INTO initial FROM sample WHERE channel_id=p_channel_id AND smpl_time<=p_start ORDER BY smpl_time DESC LIMIT 1;
    IF FOUND THEN
        p_start := initial;
    END IF;
    
    -- Determine how many raw samples there are
    SELECT COUNT(*) INTO count FROM sample WHERE channel_id=p_channel_id AND smpl_time BETWEEN p_start AND p_end;    

    -- Fewer samples than requested, or the buckets are small?
    IF NOT FOUND  OR  count <= p_bucket_count  OR  bucket_size < make_interval(secs=>10) THEN
        RAISE NOTICE 'Adjusted range % .. %, % buckets sized %, % samples found -> returning raw data', p_start, p_end, p_bucket_count, bucket_size, count;
        -- Return raw data in a format that matches the optimized query, NULL for unused min, max, count of 1 per row
        count := 1;
        RETURN QUERY SELECT s.smpl_time AS bucket, s.severity_id, s.status_id, dummy, dummy, s.float_val, s.num_val, s.str_val, count
                     FROM sample s
                     WHERE s.channel_id=p_channel_id
                       AND s.smpl_time BETWEEN p_start AND p_end
                     ORDER BY bucket;            
    ELSE
        RAISE NOTICE 'Adjusted range % .. %, % buckets sized %, % samples found -> returning optimized data', p_start, p_end, p_bucket_count, bucket_size, count;
        RETURN QUERY -- Select non-float samples "as is" with NULL for min/max
                     SELECT s.smpl_time AS bucket, s.severity_id, s.status_id, dummy AS min, dummy AS max, dummy AS avg, s.num_val, s.str_val, 1 AS N
                     FROM sample s
                     WHERE s.channel_id=p_channel_id
                       AND s.smpl_time BETWEEN p_start AND p_end
                       AND s.float_val IS NULL
                     UNION ALL
                         -- Fetch optimized min/max/average/count buckets
                         SELECT time_bucket(bucket_size, s.smpl_time) AS bucket,
                                NULL AS severity_id,
                                NULL AS status_id,
                                MIN(s.float_val),
                                MAX(s.float_val),
                                AVG(s.float_val),
                                NULL AS num_val,
                                NULL AS str_val,
                                COUNT(*)
                         FROM sample s
                         WHERE s.channel_id=p_channel_id
                           AND s.smpl_time BETWEEN p_start AND p_end
                           AND s.float_val IS NOT null
                         GROUP BY bucket
                     ORDER BY bucket;
   END IF;
END;
$$;


-- Find a channel ID:
-- SELECT * FROM channel WHERE name LIKE '...';
-- Get optimized data for that channel ID:
-- SELECT * FROM auto_optimize(33, '2021-01-01', '2021-06-02', 1000);

   

