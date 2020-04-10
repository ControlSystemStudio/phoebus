ALTER TABLE snapshot DROP COLUMN username_id;
ALTER TABLE snapshot ADD username TEXT;
UPDATE snapshot SET username='Developer';

ALTER TABLE node ADD COLUMN username TEXT;
UPDATE node SET username='Developer';

DROP TABLE username;
