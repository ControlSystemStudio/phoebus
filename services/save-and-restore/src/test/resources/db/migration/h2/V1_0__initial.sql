CREATE TABLE IF NOT EXISTS node(
  id SERIAL PRIMARY KEY,
  name TEXT NOT NULL,
  type TEXT NOT NULL,
  created TIMESTAMP NOT NULL,
  last_modified TIMESTAMP NOT NULL
);

CREATE TRIGGER IF NOT EXISTS node_updated_trigger
  BEFORE UPDATE
  ON node
  FOR EACH ROW CALL "org.phoebus.service.saveandrestore.persistence.h2.H2Trigger";

CREATE TABLE IF NOT EXISTS node_closure(
  ancestor INTEGER NOT NULL REFERENCES node(id) ON DELETE CASCADE,
  descendant INTEGER NOT NULL REFERENCES node(id) ON DELETE CASCADE,
  depth INTEGER NOT NULL
);

INSERT INTO node values(0, 'Save & Restore Root', 'FOLDER', CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP());
INSERT INTO node_closure values(0, 0, 0);

CREATE TABLE IF NOT EXISTS config (
  node_id INTEGER REFERENCES node(id) ON DELETE CASCADE,
  active INTEGER NOT NULL DEFAULT 1,
  description TEXT NOT NULL,
  _system TEXT
);

CREATE TABLE IF NOT EXISTS node_closure(
  ancestor INTEGER NOT NULL REFERENCES node(id) ON DELETE CASCADE,
  descendant INTEGER NOT NULL REFERENCES node(id) ON DELETE CASCADE,
  depth INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS config_pv (
  id SERIAL PRIMARY KEY,
  name TEXT NOT NULL,
  tags TEXT,
  groupName TEXT,
  readonly INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS config_pv_relation (
  config_id INTEGER NOT NULL REFERENCES node(id) ON DELETE CASCADE,
  config_pv_id INTEGER NOT NULL REFERENCES config_pv(id)
);

CREATE INDEX IF NOT EXISTS config_pv_idx ON config_pv_relation(config_id, config_pv_id);

CREATE TABLE IF NOT EXISTS username(
  id SERIAL PRIMARY KEY,
  name TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS snapshot (
  id SERIAL PRIMARY KEY,
  config_id INTEGER NOT NULL REFERENCES node(id) ON DELETE CASCADE,
  created TIMESTAMP NOT NULL,
  username_id INTEGER REFERENCES username(id) ON DELETE CASCADE,
  comment VARCHAR,
  approve BOOLEAN
);

CREATE INDEX IF NOT EXISTS snapshot_config_idx ON snapshot(config_id);

CREATE TABLE IF NOT EXISTS snapshot_pv (
  snapshot_id INTEGER NOT NULL REFERENCES snapshot(id) ON DELETE CASCADE,
  config_pv_id INTEGER NOT NULL REFERENCES config_pv(id) ON DELETE CASCADE,
  dtype INTEGER NOT NULL,
  severity INTEGER NOT NULL,
  status INTEGER NOT NULL,
  time BIGINT,
  timens INTEGER,
  clazz TEXT NOT NULL,
  value TEXT,
  fetch_status BOOLEAN
);
