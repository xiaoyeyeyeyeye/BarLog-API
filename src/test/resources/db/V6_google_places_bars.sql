ALTER TABLE bars ADD COLUMN IF NOT EXISTS google_place_id VARCHAR(128);
ALTER TABLE bars ADD COLUMN IF NOT EXISTS source VARCHAR(16) NOT NULL DEFAULT 'seed';
ALTER TABLE bars ADD COLUMN IF NOT EXISTS synced_at TIMESTAMP;
CREATE UNIQUE INDEX IF NOT EXISTS idx_bars_google_place_id ON bars(google_place_id)
    WHERE google_place_id IS NOT NULL;
UPDATE bars SET source = 'seed' WHERE source IS NULL;
