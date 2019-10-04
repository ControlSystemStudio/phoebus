ALTER TABLE node ALTER COLUMN id SET DEFAULT nextval('node_id_seq') ;
ALTER TABLE config ALTER COLUMN node_id SET DEFAULT nextval('node_id_seq');
ALTER TABLE config ALTER COLUMN description DROP NOT NULL;
ALTER TABLE config_pv ALTER COLUMN id SET DEFAULT nextval('node_id_seq');

CREATE TABLE IF NOT EXISTS snapshot_node (
  node_id INTEGER REFERENCES node(id) ON DELETE CASCADE NOT NULL,
  comment TEXT
);

CREATE TABLE IF NOT EXISTS snapshot_node_pv (
  snapshot_node_id INTEGER REFERENCES node(id) ON DELETE CASCADE NOT NULL,
  config_pv_id INTEGER REFERENCES config_pv(id) ON DELETE CASCADE NOT NULL,
  time BIGINT,
  timens INTEGER,
  value TEXT,
  fetch_status BOOLEAN DEFAULT FALSE,
  sizes TEXT,
  status TEXT,
  severity TEXT,
  data_type TEXT
);