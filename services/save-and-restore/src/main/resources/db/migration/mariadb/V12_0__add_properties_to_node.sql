CREATE TABLE property(
	node_id INTEGER NOT NULL REFERENCES node(id) ON DELETE CASCADE,
	property_name VARCHAR(100) NOT NULL,
	value VARCHAR(100) NOT NULL,
	PRIMARY KEY(node_id, property_name)
);
