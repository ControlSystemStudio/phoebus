CREATE TABLE IF NOT EXISTS global_sequence (
    number BIGINT UNSIGNED NOT NULL
);

INSERT INTO global_sequence VALUES(0);

DELIMITER //
DROP TRIGGER IF EXISTS trigger_id_node //
CREATE TRIGGER trigger_id_node BEFORE INSERT ON node
    FOR EACH ROW BEGIN
        DECLARE incremented_number BIGINT UNSIGNED;
        UPDATE global_sequence SET number = number + 1;
        SELECT number INTO incremented_number
            FROM global_sequence;
        SET new.id := incremented_number;
    END//
DELIMITER ;

DELIMITER //
DROP TRIGGER IF EXISTS trigger_id_config_pv //
CREATE TRIGGER trigger_id_config_pv BEFORE INSERT ON config_pv
    FOR EACH ROW BEGIN
        DECLARE incremented_number BIGINT UNSIGNED;
        UPDATE global_sequence SET number = number + 1;
        SELECT number INTO incremented_number
            FROM global_sequence;
        SET new.id := incremented_number;
    END//
DELIMITER ;

ALTER TABLE node MODIFY COLUMN id SERIAL;
ALTER TABLE config_pv MODIFY COLUMN id SERIAL;

CREATE TABLE IF NOT EXISTS snapshot_node (
  node_id INTEGER NOT NULL REFERENCES node(id) ON DELETE CASCADE,
  comment TEXT
);

CREATE TABLE IF NOT EXISTS snapshot_node_pv (
  snapshot_node_id INTEGER NOT NULL REFERENCES node(id) ON DELETE CASCADE,
  config_pv_id INTEGER NOT NULL REFERENCES config_pv(id) ON DELETE CASCADE,
  time BIGINT,
  timens INTEGER,
  value TEXT,
  fetch_status BOOLEAN DEFAULT FALSE,
  sizes TEXT,
  status TEXT,
  severity TEXT,
  data_type TEXT
);