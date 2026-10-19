-- Run this once in your Supabase project SQL Editor if your project was set up with an
-- older supabase_schema.sql (before the `app` column existed). Safe to re-run.
--
-- Adds an `app` column (e.g. 'mangaworld', 'animeworld', 'novelworld') so apps signed into
-- the same account keep separate data. Existing rows keep app = NULL; after updating, each
-- app re-uploads its local data tagged with its own name on its next sync.
--
-- Update every OtakuWorld app before running this: older app versions can't sync afterwards.

-- ─── APP COLUMN ───────────────────────────────────────────────────────────────
ALTER TABLE favorite_items      ADD COLUMN IF NOT EXISTS app TEXT;
ALTER TABLE chapters_watched    ADD COLUMN IF NOT EXISTS app TEXT;
ALTER TABLE bookmarked_chapters ADD COLUMN IF NOT EXISTS app TEXT;
ALTER TABLE notes               ADD COLUMN IF NOT EXISTS app TEXT;
ALTER TABLE history             ADD COLUMN IF NOT EXISTS app TEXT;
ALTER TABLE custom_list_items   ADD COLUMN IF NOT EXISTS app TEXT;
ALTER TABLE custom_list_info    ADD COLUMN IF NOT EXISTS app TEXT;
ALTER TABLE heatmap_items       ADD COLUMN IF NOT EXISTS app TEXT;
ALTER TABLE activity_timer      ADD COLUMN IF NOT EXISTS app TEXT;

-- ─── UNIQUE KEYS ──────────────────────────────────────────────────────────────
-- Drop the old per-user unique keys (each table has exactly one), then re-add them with `app`.
DO $$
DECLARE r record;
BEGIN
    FOR r IN
        SELECT conrelid::regclass AS tbl, conname
        FROM pg_constraint
        WHERE contype = 'u'
          AND conrelid::regclass::text IN (
              'favorite_items', 'chapters_watched', 'bookmarked_chapters', 'notes', 'history',
              'custom_list_items', 'custom_list_info', 'heatmap_items', 'activity_timer'
          )
    LOOP
        EXECUTE format('ALTER TABLE %s DROP CONSTRAINT %I', r.tbl, r.conname);
    END LOOP;
END $$;

ALTER TABLE favorite_items      ADD CONSTRAINT favorite_items_user_app_url_key           UNIQUE (user_id, app, url);
ALTER TABLE chapters_watched    ADD CONSTRAINT chapters_watched_user_app_url_key         UNIQUE (user_id, app, url);
ALTER TABLE bookmarked_chapters ADD CONSTRAINT bookmarked_chapters_user_app_chapter_key  UNIQUE (user_id, app, chapter_url);
ALTER TABLE notes               ADD CONSTRAINT notes_user_app_item_url_key               UNIQUE (user_id, app, item_url);
ALTER TABLE history             ADD CONSTRAINT history_user_app_search_text_key          UNIQUE (user_id, app, search_text);
ALTER TABLE custom_list_items   ADD CONSTRAINT custom_list_items_user_app_uuid_key       UNIQUE (user_id, app, uuid);
ALTER TABLE custom_list_info    ADD CONSTRAINT custom_list_info_user_app_unique_id_key   UNIQUE (user_id, app, unique_id);
ALTER TABLE heatmap_items       ADD CONSTRAINT heatmap_items_user_app_time_key           UNIQUE (user_id, app, time);

-- activity_timer was keyed by user_id alone; give it its own id so each app gets a row.
ALTER TABLE activity_timer DROP CONSTRAINT IF EXISTS activity_timer_pkey;
ALTER TABLE activity_timer ADD COLUMN IF NOT EXISTS id UUID DEFAULT uuid_generate_v4();
UPDATE activity_timer SET id = uuid_generate_v4() WHERE id IS NULL;
ALTER TABLE activity_timer ADD PRIMARY KEY (id);
ALTER TABLE activity_timer ADD CONSTRAINT activity_timer_user_app_key UNIQUE (user_id, app);

-- ─── INDEXES ──────────────────────────────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_favorites_app_updated      ON favorite_items(user_id, app, updated_at);
CREATE INDEX IF NOT EXISTS idx_chapters_app_updated       ON chapters_watched(user_id, app, updated_at);
CREATE INDEX IF NOT EXISTS idx_bookmarks_app_updated      ON bookmarked_chapters(user_id, app, updated_at);
CREATE INDEX IF NOT EXISTS idx_notes_app_updated          ON notes(user_id, app, updated_at);
CREATE INDEX IF NOT EXISTS idx_history_app_updated        ON history(user_id, app, updated_at);
CREATE INDEX IF NOT EXISTS idx_custom_list_items_app_upd  ON custom_list_items(user_id, app, updated_at);
CREATE INDEX IF NOT EXISTS idx_custom_list_info_app_upd   ON custom_list_info(user_id, app, updated_at);
CREATE INDEX IF NOT EXISTS idx_heatmap_app_updated        ON heatmap_items(user_id, app, updated_at);

-- ─── OPTIONAL CLEANUP ─────────────────────────────────────────────────────────
-- Once every app you use has synced on the new version, the untagged legacy rows are
-- no longer read by anything. Uncomment to delete them.
-- DELETE FROM favorite_items      WHERE app IS NULL;
-- DELETE FROM chapters_watched    WHERE app IS NULL;
-- DELETE FROM bookmarked_chapters WHERE app IS NULL;
-- DELETE FROM notes               WHERE app IS NULL;
-- DELETE FROM history             WHERE app IS NULL;
-- DELETE FROM custom_list_items   WHERE app IS NULL;
-- DELETE FROM custom_list_info    WHERE app IS NULL;
-- DELETE FROM heatmap_items       WHERE app IS NULL;
-- DELETE FROM activity_timer      WHERE app IS NULL;
