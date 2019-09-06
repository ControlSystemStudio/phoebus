ALTER TABLE config_pv ADD readback_name TEXT;
ALTER TABLE config_pv ADD readonly BOOLEAN DEFAULT FALSE;
	
ALTER TABLE snapshot_node_pv ADD readback BOOLEAN DEFAULT FALSE;
ALTER TABLE snapshot_node_pv DROP COLUMN fetch_status;

DROP TABLE snapshot_pv;
DROP TABLE config;
DROP TABLE snapshot;
