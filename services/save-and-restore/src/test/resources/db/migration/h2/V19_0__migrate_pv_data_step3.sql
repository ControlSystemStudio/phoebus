ALTER TABLE config_pv DROP COLUMN name;
ALTER TABLE config_pv DROP COLUMN readback_name;

ALTER TABLE snapshot_node_pv ADD pv_id INTEGER REFERENCES pv(id);