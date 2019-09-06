CREATE SEQUENCE masar_seq;

ALTER TABLE node ALTER COLUMN id SET DEFAULT masar_seq.nextval;
ALTER TABLE config ALTER COLUMN node_id SET DEFAULT masar_seq.nextval;
ALTER TABLE config_pv ALTER COLUMN id SET DEFAULT masar_seq.nextval;

CREATE TABLE IF NOT EXISTS snapshot_node (
  node_id INTEGER  NOT NULL REFERENCES node(id) ON DELETE CASCADE,
  comment TEXT
);

CREATE TABLE IF NOT EXISTS snapshot_node_pv (
  snapshot_node_id INTEGER  NOT NULL REFERENCES node(id) ON DELETE CASCADE,
  config_pv_id INTEGER  NOT NULL REFERENCES config_pv(id) ON DELETE CASCADE,
  time BIGINT,
  timens INTEGER,
  value TEXT,
  fetch_status BOOLEAN DEFAULT FALSE,
  sizes TEXT,
  status TEXT,
  severity TEXT,
  data_type TEXT
);