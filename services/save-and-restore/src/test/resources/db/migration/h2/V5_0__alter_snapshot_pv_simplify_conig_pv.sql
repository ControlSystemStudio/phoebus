ALTER TABLE snapshot_pv DROP COLUMN dtype;
ALTER TABLE snapshot_pv DROP COLUMN clazz;
ALTER TABLE snapshot_pv DROP COLUMN status;
ALTER TABLE snapshot_pv DROP COLUMN severity;
ALTER TABLE snapshot_pv ADD sizes VARCHAR;
ALTER TABLE snapshot_pv ADD status VARCHAR;
ALTER TABLE snapshot_pv ADD severity VARCHAR;
ALTER TABLE snapshot_pv ADD data_type VARCHAR;
ALTER TABLE config_pv DROP COLUMN readonly;
ALTER TABLE config_pv DROP COLUMN groupname;
ALTER TABLE config_pv DROP COLUMN tags;

