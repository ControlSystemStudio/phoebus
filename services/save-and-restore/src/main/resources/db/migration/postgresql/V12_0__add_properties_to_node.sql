CREATE TABLE property(
	node_id INTEGER NOT NULL REFERENCES node(id) ON DELETE CASCADE,
	property_name VARCHAR NOT NULL,
	value VARCHAR NOT NULL,
	PRIMARY KEY(node_id, property_name)
);
