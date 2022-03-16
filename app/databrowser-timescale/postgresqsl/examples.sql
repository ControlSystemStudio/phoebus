-------------------------
-- Readout Examples (update channel name and time range for your data)
-------------------------

-- Basic raw readout
SELECT s.smpl_time, s.nanosecs, channel.name, severity.name, status.name, s.num_val, s.float_val, s.str_val
 FROM sample s
 JOIN channel ON channel.channel_id = s.channel_id
 JOIN severity ON severity.severity_id = s.severity_id
 JOIN status ON status.status_id = s.status_id
 WHERE channel.name = 'BTF_MEBT_Mag:PS_QH33:B'
   AND smpl_time BETWEEN '2021-01-01 10:39' AND '2021-01-01 23:23'
 ORDER BY s.smpl_time;


-- Optimized readout example
SELECT smpl_time AS bucket, NULL AS min, NULL AS max, NULL AS avg, str_val, 1 AS N
  FROM sample                                                                                      
  WHERE channel_id = 4
    AND smpl_time BETWEEN '2019-05-30' AND '2019-06-02'
    AND str_val is not null
UNION ALL
  SELECT time_bucket(make_interval(secs=>4300), smpl_time) AS bucket,
         min(float_val) AS min,
         max(float_val) AS max,
         avg(float_val) AS avg,
         NULL AS str_val,
         COUNT(*) AS N
  FROM sample
  WHERE channel_id=33
    AND smpl_time BETWEEN '2021-01-01' AND '2021-06-01'
  GROUP BY bucket
ORDER BY bucket;




-- Returns 'read_optimized' column where each row holds a set
SELECT read_optimized(33, '2021-01-01', '2021-06-01', 4300);

-- Looks more like usual table query
SELECT * FROM read_optimized(33, '2021-01-01', '2021-06-01', 4300);
SELECT bucket, severity_id, status_id, min, max, avg, num_val, str_val, n
 FROM read_optimized(33, '2021-01-01', '2021-06-01', 4300);

-- Decode stat/sevr
SELECT bucket, severity.name AS severity, status.name AS STATUS, min, max, avg, num_val, str_val, n
 FROM read_optimized(33, '2021-01-01', '2021-06-01', 4300) data
 LEFT OUTER JOIN severity ON severity.severity_id = data.severity_id
 LEFT OUTER JOIN status ON status.status_id = data.status_id;


-- Find a channel ID:
SELECT * FROM channel WHERE name LIKE '...';
-- Get optimized data for that channel ID:
SELECT * FROM auto_optimize(33, '2021-01-01', '2021-06-02', 1000);

