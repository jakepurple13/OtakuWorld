-- Run this in your Supabase project SQL Editor once.
-- Requires: Authentication enabled in your Supabase project.

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ─── FAVORITES ────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS favorite_items (
    id                      UUID    DEFAULT uuid_generate_v4() PRIMARY KEY,
    user_id                 UUID    NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    url                     TEXT    NOT NULL,
    title                   TEXT    NOT NULL DEFAULT '',
    description             TEXT    NOT NULL DEFAULT '',
    image_url               TEXT    NOT NULL DEFAULT '',
    source                  TEXT    NOT NULL DEFAULT '',
    num_chapters            INTEGER NOT NULL DEFAULT 0,
    should_check_for_update BOOLEAN NOT NULL DEFAULT true,
    supabase_id             TEXT    NOT NULL DEFAULT '',
    created_at              BIGINT  NOT NULL DEFAULT 0,
    updated_at              BIGINT  NOT NULL DEFAULT 0,
    is_deleted              BOOLEAN NOT NULL DEFAULT false,
    is_dirty                BOOLEAN NOT NULL DEFAULT true,
    UNIQUE(user_id, url)
);
ALTER TABLE favorite_items ENABLE ROW LEVEL SECURITY;
CREATE POLICY "own_favorites" ON favorite_items FOR ALL USING (auth.uid() = user_id);
CREATE INDEX idx_favorites_updated ON favorite_items(user_id, updated_at);

-- ─── CHAPTERS WATCHED ─────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS chapters_watched (
    id           UUID   DEFAULT uuid_generate_v4() PRIMARY KEY,
    user_id      UUID   NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    url          TEXT   NOT NULL,
    name         TEXT   NOT NULL DEFAULT '',
    favorite_url TEXT   NOT NULL DEFAULT '',
    supabase_id  TEXT   NOT NULL DEFAULT '',
    created_at   BIGINT NOT NULL DEFAULT 0,
    updated_at   BIGINT NOT NULL DEFAULT 0,
    is_deleted   BOOLEAN NOT NULL DEFAULT false,
    is_dirty     BOOLEAN NOT NULL DEFAULT true,
    UNIQUE(user_id, url)
);
ALTER TABLE chapters_watched ENABLE ROW LEVEL SECURITY;
CREATE POLICY "own_chapters" ON chapters_watched FOR ALL USING (auth.uid() = user_id);
CREATE INDEX idx_chapters_updated ON chapters_watched(user_id, updated_at);

-- ─── BOOKMARKED CHAPTERS ──────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS bookmarked_chapters (
    id               UUID   DEFAULT uuid_generate_v4() PRIMARY KEY,
    user_id          UUID   NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    chapter_url      TEXT   NOT NULL,
    chapter_name     TEXT   NOT NULL DEFAULT '',
    parent_url       TEXT   NOT NULL DEFAULT '',
    parent_title     TEXT   NOT NULL DEFAULT '',
    parent_image_url TEXT   NOT NULL DEFAULT '',
    source           TEXT   NOT NULL DEFAULT '',
    timestamp        BIGINT NOT NULL DEFAULT 0,
    supabase_id      TEXT   NOT NULL DEFAULT '',
    created_at       BIGINT NOT NULL DEFAULT 0,
    updated_at       BIGINT NOT NULL DEFAULT 0,
    is_deleted       BOOLEAN NOT NULL DEFAULT false,
    is_dirty         BOOLEAN NOT NULL DEFAULT true,
    UNIQUE(user_id, chapter_url)
);
ALTER TABLE bookmarked_chapters ENABLE ROW LEVEL SECURITY;
CREATE POLICY "own_bookmarks" ON bookmarked_chapters FOR ALL USING (auth.uid() = user_id);
CREATE INDEX idx_bookmarks_updated ON bookmarked_chapters(user_id, updated_at);

-- ─── NOTES ────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS notes (
    id          UUID   DEFAULT uuid_generate_v4() PRIMARY KEY,
    user_id     UUID   NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    item_url    TEXT   NOT NULL,
    item_title  TEXT   NOT NULL DEFAULT '',
    content     TEXT   NOT NULL DEFAULT '',
    timestamp   BIGINT NOT NULL DEFAULT 0,
    supabase_id TEXT   NOT NULL DEFAULT '',
    created_at  BIGINT NOT NULL DEFAULT 0,
    updated_at  BIGINT NOT NULL DEFAULT 0,
    is_deleted  BOOLEAN NOT NULL DEFAULT false,
    is_dirty    BOOLEAN NOT NULL DEFAULT true,
    UNIQUE(user_id, item_url)
);
ALTER TABLE notes ENABLE ROW LEVEL SECURITY;
CREATE POLICY "own_notes" ON notes FOR ALL USING (auth.uid() = user_id);
CREATE INDEX idx_notes_updated ON notes(user_id, updated_at);

-- ─── HISTORY ──────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS history (
    id          UUID   DEFAULT uuid_generate_v4() PRIMARY KEY,
    user_id     UUID   NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    search_text TEXT   NOT NULL,
    time        BIGINT NOT NULL DEFAULT 0,
    supabase_id TEXT   NOT NULL DEFAULT '',
    created_at  BIGINT NOT NULL DEFAULT 0,
    updated_at  BIGINT NOT NULL DEFAULT 0,
    is_deleted  BOOLEAN NOT NULL DEFAULT false,
    is_dirty    BOOLEAN NOT NULL DEFAULT true,
    UNIQUE(user_id, search_text)
);
ALTER TABLE history ENABLE ROW LEVEL SECURITY;
CREATE POLICY "own_history" ON history FOR ALL USING (auth.uid() = user_id);
CREATE INDEX idx_history_updated ON history(user_id, updated_at);

-- ─── CUSTOM LIST ITEMS ────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS custom_list_items (
    id            UUID    DEFAULT uuid_generate_v4() PRIMARY KEY,
    user_id       UUID    NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    uuid          TEXT    NOT NULL,
    name          TEXT    NOT NULL DEFAULT '',
    time          BIGINT  NOT NULL DEFAULT 0,
    use_biometric BOOLEAN NOT NULL DEFAULT false,
    description   TEXT    NOT NULL DEFAULT '',
    supabase_id   TEXT    NOT NULL DEFAULT '',
    created_at    BIGINT  NOT NULL DEFAULT 0,
    updated_at    BIGINT  NOT NULL DEFAULT 0,
    is_deleted    BOOLEAN NOT NULL DEFAULT false,
    is_dirty      BOOLEAN NOT NULL DEFAULT true,
    UNIQUE(user_id, uuid)
);
ALTER TABLE custom_list_items ENABLE ROW LEVEL SECURITY;
CREATE POLICY "own_custom_list_items" ON custom_list_items FOR ALL USING (auth.uid() = user_id);
CREATE INDEX idx_custom_list_items_updated ON custom_list_items(user_id, updated_at);

-- ─── CUSTOM LIST INFO ─────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS custom_list_info (
    id          UUID   DEFAULT uuid_generate_v4() PRIMARY KEY,
    user_id     UUID   NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    unique_id   TEXT   NOT NULL,
    uuid        TEXT   NOT NULL,
    title       TEXT   NOT NULL DEFAULT '',
    description TEXT   NOT NULL DEFAULT '',
    url         TEXT   NOT NULL DEFAULT '',
    image_url   TEXT   NOT NULL DEFAULT '',
    source      TEXT   NOT NULL DEFAULT '',
    supabase_id TEXT   NOT NULL DEFAULT '',
    created_at  BIGINT NOT NULL DEFAULT 0,
    updated_at  BIGINT NOT NULL DEFAULT 0,
    is_deleted  BOOLEAN NOT NULL DEFAULT false,
    is_dirty    BOOLEAN NOT NULL DEFAULT true,
    UNIQUE(user_id, unique_id)
);
ALTER TABLE custom_list_info ENABLE ROW LEVEL SECURITY;
CREATE POLICY "own_custom_list_info" ON custom_list_info FOR ALL USING (auth.uid() = user_id);
CREATE INDEX idx_custom_list_info_updated ON custom_list_info(user_id, updated_at);

-- ─── RECOMMENDATIONS ──────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS recommendations (
    id          UUID   DEFAULT uuid_generate_v4() PRIMARY KEY,
    user_id     UUID   NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    title       TEXT   NOT NULL,
    description TEXT   NOT NULL DEFAULT '',
    reason      TEXT   NOT NULL DEFAULT '',
    genre       TEXT   NOT NULL DEFAULT '[]',  -- JSON array stored as TEXT
    supabase_id TEXT   NOT NULL DEFAULT '',
    created_at  BIGINT NOT NULL DEFAULT 0,
    updated_at  BIGINT NOT NULL DEFAULT 0,
    is_deleted  BOOLEAN NOT NULL DEFAULT false,
    is_dirty    BOOLEAN NOT NULL DEFAULT true,
    UNIQUE(user_id, title)
);
ALTER TABLE recommendations ENABLE ROW LEVEL SECURITY;
CREATE POLICY "own_recommendations" ON recommendations FOR ALL USING (auth.uid() = user_id);
CREATE INDEX idx_recommendations_updated ON recommendations(user_id, updated_at);

-- ─── HEATMAP ──────────────────────────────────────────────────────────────────
-- HeatMapItem.time is a LocalDate stored as JSON TEXT by Room's TypeConverter
CREATE TABLE IF NOT EXISTS heatmap_items (
    id          UUID    DEFAULT uuid_generate_v4() PRIMARY KEY,
    user_id     UUID    NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    time        TEXT    NOT NULL,  -- ISO-8601 date string e.g. "2026-06-17"
    day_count   INTEGER NOT NULL DEFAULT 0,
    supabase_id TEXT    NOT NULL DEFAULT '',
    created_at  BIGINT  NOT NULL DEFAULT 0,
    updated_at  BIGINT  NOT NULL DEFAULT 0,
    is_deleted  BOOLEAN NOT NULL DEFAULT false,
    is_dirty    BOOLEAN NOT NULL DEFAULT true,
    UNIQUE(user_id, time)
);
ALTER TABLE heatmap_items ENABLE ROW LEVEL SECURITY;
CREATE POLICY "own_heatmap" ON heatmap_items FOR ALL USING (auth.uid() = user_id);
CREATE INDEX idx_heatmap_updated ON heatmap_items(user_id, updated_at);

-- ─── STORAGE BUCKET ───────────────────────────────────────────────────────────
INSERT INTO storage.buckets (id, name, public)
VALUES ('otakuworld-backups', 'otakuworld-backups', false)
ON CONFLICT (id) DO NOTHING;

CREATE POLICY "own_backups"
    ON storage.objects FOR ALL
    USING (
        bucket_id = 'otakuworld-backups'
        AND auth.uid()::text = (storage.foldername(name))[2]
    );
