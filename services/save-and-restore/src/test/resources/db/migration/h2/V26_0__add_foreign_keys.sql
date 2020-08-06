ALTER TABLE config_pv_relation MODIFY COLUMN config_id BIGINT UNSIGNED;
ALTER TABLE node_closure MODIFY COLUMN ancestor BIGINT UNSIGNED;
ALTER TABLE node_closure MODIFY COLUMN descendant BIGINT UNSIGNED;
ALTER TABLE property MODIFY COLUMN node_id BIGINT UNSIGNED;
ALTER TABLE snapshot_node_pv MODIFY COLUMN snapshot_node_id BIGINT UNSIGNED;
ALTER TABLE snapshot_node_pv MODIFY COLUMN config_pv_id BIGINT UNSIGNED;

ALTER TABLE config_pv_relation ADD CONSTRAINT fk_config_id FOREIGN KEY (config_id) REFERENCES node(id) ON DELETE CASCADE;
ALTER TABLE node_closure ADD CONSTRAINT fk_ancestor FOREIGN KEY (ancestor) REFERENCES node(id) ON DELETE CASCADE;
ALTER TABLE node_closure ADD CONSTRAINT fk_descendant FOREIGN KEY (descendant) REFERENCES node(id) ON DELETE CASCADE;
ALTER TABLE property ADD CONSTRAINT fk_node_id FOREIGN KEY (node_id) REFERENCES node(id) ON DELETE CASCADE;
ALTER TABLE snapshot_node_pv ADD CONSTRAINT fk_snapshot_node_id FOREIGN KEY (snapshot_node_id) REFERENCES node(id) ON DELETE CASCADE;
ALTER TABLE snapshot_node_pv ADD CONSTRAINT fk_config_pv_id FOREIGN KEY (config_pv_id) REFERENCES config_pv(id) ON DELETE CASCADE;