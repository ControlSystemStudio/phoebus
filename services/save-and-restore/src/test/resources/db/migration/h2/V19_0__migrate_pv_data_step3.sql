ALTER TABLE config_pv DROP COLUMN name;
ALTER TABLE config_pv DROP COLUMN readback_name;

ALTER TABLE snapshot_node_pv ADD readback_time BIGINT;
ALTER TABLE snapshot_node_pv ADD readback_timens INTEGER;
ALTER TABLE snapshot_node_pv ADD readback_value TEXT;
ALTER TABLE snapshot_node_pv ADD readback_sizes TEXT;
ALTER TABLE snapshot_node_pv ADD readback_status TEXT;
ALTER TABLE snapshot_node_pv ADD readback_severity TEXT;
ALTER TABLE snapshot_node_pv ADD readback_data_type TEXT;
