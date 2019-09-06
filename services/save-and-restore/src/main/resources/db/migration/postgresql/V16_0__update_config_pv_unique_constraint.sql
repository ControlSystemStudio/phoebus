ALTER TABLE config_pv DROP CONSTRAINT config_pv_name_key;
ALTER TABLE config_pv ADD CONSTRAINT config_pv_name_key UNIQUE(name, readback_name);
