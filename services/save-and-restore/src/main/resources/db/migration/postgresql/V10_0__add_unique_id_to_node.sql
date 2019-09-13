ALTER TABLE node ADD unique_id TEXT;
ALTER TABLE node ADD CONSTRAINT node_unique_id_key UNIQUE (unique_id);
