ALTER TABLE config_pv DROP COLUMN name;
ALTER TABLE config_pv DROP COLUMN readback_name;
DROP TABLE snapshot_node;

ALTER TABLE snapshot_node_pv ADD pv_id INTEGER REFERENCES pv(id);