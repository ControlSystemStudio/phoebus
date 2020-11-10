CREATE TABLE IF NOT EXISTS snapshot_node_pv_large_store (
    id INTEGER PRIMARY KEY AUTO_INCREMENT,
    snapshot_node_id BIGINT UNSIGNED NOT NULL REFERENCES node(id) ON DELETE CASCADE,
    config_pv_id BIGINT UNSIGNED NOT NULL REFERENCES config_pv(id) ON DELETE CASCADE,
    value LONGTEXT,
    readback_value LONGTEXT,
    CONSTRAINT fk_ls_snapshot_node_id FOREIGN KEY (snapshot_node_id) REFERENCES node(id) ON DELETE CASCADE,
    CONSTRAINT fk_ls_config_pv_id FOREIGN KEY (config_pv_id) REFERENCES config_pv(id) ON DELETE CASCADE
) ENGINE=InnoDB;
