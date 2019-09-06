ALTER TABLE node ADD unique_id VARCHAR;
ALTER TABLE node ADD UNIQUE KEY node_unique_key(unique_id);

