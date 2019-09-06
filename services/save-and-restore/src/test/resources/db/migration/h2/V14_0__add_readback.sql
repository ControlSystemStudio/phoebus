ALTER TABLE config_pv ADD readback_name TEXT;
ALTER TABLE config_pv ADD readonly INT DEFAULT 0;
	
ALTER TABLE snapshot_node_pv ADD readback INT DEFAULT 0;
ALTER TABLE snapshot_node_pv DROP COLUMN fetch_status;

DROP TABLE snapshot_pv;
DROP TABLE config;
DROP TABLE snapshot;
