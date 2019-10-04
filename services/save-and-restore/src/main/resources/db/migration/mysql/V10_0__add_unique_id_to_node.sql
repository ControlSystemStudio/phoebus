ALTER TABLE node ADD unique_id VARCHAR(100);
ALTER TABLE node ADD CONSTRAINT node_unique_id_key UNIQUE (unique_id);
