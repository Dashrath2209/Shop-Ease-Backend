ALTER TABLE categories ADD COLUMN updated_at DATETIME;
UPDATE categories SET updated_at = created_at WHERE updated_at IS NULL;