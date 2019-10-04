-- The following makes sure Mysql uses 0 as a valid AUTO_INCREMENT value
-- It is needed to secure the same behavior as for instance Postgresql
SET @@session.sql_mode =
    CASE WHEN @@session.sql_mode NOT LIKE '%NO_AUTO_VALUE_ON_ZERO%'
        THEN CASE WHEN LENGTH(@@session.sql_mode)>0
            THEN CONCAT_WS(',',@@session.sql_mode,'NO_AUTO_VALUE_ON_ZERO')  -- added, wasn't empty
            ELSE 'NO_AUTO_VALUE_ON_ZERO'                                    -- replaced, was empty
        END
        ELSE @@session.sql_mode                                             -- unchanged, already had NO_AUTO_VALUE_ON_ZERO set
    END;

CREATE TABLE IF NOT EXISTS node(
  id INTEGER PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(50) NOT NULL,
  type VARCHAR(20) NOT NULL,
  created TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  last_modified TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY node_idx (id, name, type)
) ENGINE=InnoDB;


CREATE TABLE IF NOT EXISTS node_closure(
  ancestor INTEGER NOT NULL REFERENCES node(id) ON DELETE CASCADE,
  descendant INTEGER NOT NULL REFERENCES node(id) ON DELETE CASCADE,
  depth INTEGER NOT NULL
) ENGINE=InnoDB;

INSERT INTO node (id, name, type) values(0, 'Save & Restore Root', 'FOLDER');
INSERT INTO node_closure values(0, 0, 0);

CREATE TABLE IF NOT EXISTS config (
  node_id INTEGER REFERENCES node(id) ON DELETE CASCADE,
  active INTEGER NOT NULL DEFAULT 1,
  description TEXT NOT NULL,
  _system TEXT
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS config_pv (
  id INTEGER PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(50) NOT NULL,
  tags TEXT,
  groupName TEXT,
  readonly INT NOT NULL DEFAULT 0,
  UNIQUE(name),
  KEY pv_name_idx (name)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS config_pv_relation (
  config_id INTEGER NOT NULL  REFERENCES node(id) ON DELETE CASCADE,
  config_pv_id INTEGER NOT NULL  REFERENCES config_pv(id),
  KEY config_pv_idx (config_id, config_pv_id)
) ENGINE=InnoDB;


CREATE TABLE IF NOT EXISTS username(
  id INTEGER PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(50) NOT NULL,
  KEY username_idx (name)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS snapshot (
  id INTEGER PRIMARY KEY AUTO_INCREMENT,
  config_id INTEGER REFERENCES node(id) ON DELETE CASCADE,
  created TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  username_id INTEGER REFERENCES username(id) ON DELETE CASCADE,
  comment TEXT,
  approve BOOLEAN,
  KEY snapshot_config_idx (config_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS snapshot_pv (
  snapshot_id INTEGER REFERENCES snapshot(id)  ON DELETE CASCADE,
  config_pv_id INTEGER NOT NULL  REFERENCES config_pv(id) ON DELETE CASCADE,
  dtype INTEGER DEFAULT NULL,
  severity INTEGER DEFAULT NULL,
  status INTEGER DEFAULT NULL,
  time BIGINT DEFAULT NULL,
  timens INTEGER DEFAULT NULL,
  clazz TEXT DEFAULT NULL,
  value TEXT DEFAULT NULL,
  fetch_status BOOLEAN DEFAULT FALSE
) ENGINE=InnoDB;
