ALTER TABLE snapshot DROP CONSTRAINT snapshot_username_id_fkey;

ALTER TABLE snapshot DROP COLUMN username_id;
ALTER TABLE snapshot ADD username TEXT;
UPDATE snapshot SET username='Developer';

ALTER TABLE node ADD COLUMN username TEXT;
UPDATE node SET username='Developer';

DROP TABLE username;
