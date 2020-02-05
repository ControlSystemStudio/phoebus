ALTER TABLE config_pv DROP COLUMN name;
ALTER TABLE config_pv DROP COLUMN readback_name;
DROP TABLE snapshot_node;

ALTER TABLE snapshot_node_pv
    ADD readback_time BIGINT,
    ADD readback_timens INTEGER,
    ADD readback_value TEXT,
    ADD readback_sizes TEXT,
    ADD readback_status TEXT,
    ADD readback_severity TEXT,
    ADD readback_data_type TEXT;
