# Cloud sync

Sync keeps your favorites, history, lists, notes, bookmarks, and read or watched chapters the same
on all your devices. It's optional. Everything works without it.

Sync uses your own [Supabase](https://supabase.com) project, so your data is stored in an account
you control, not on an OtakuWorld server. Supabase's free plan is enough.

Setup takes about 10 minutes and you only do it once. After that, adding another device takes a
QR code scan.

## What you need

- A free Supabase account
- A computer with a web browser, for the setup steps
- One of the OtakuWorld apps installed

## Step 1: Create a Supabase project

1. Go to [supabase.com](https://supabase.com) and sign up.
2. Click **New project**. Give it any name, such as `otakuworld`, and set a database password.
   Save that password somewhere; the app doesn't need it.
3. Pick the region closest to you and click **Create new project**. It takes a minute or two to
   start.

## Step 2: Create the tables

The app needs a few tables in your project to store your data.

1. Open the
   [setup script](https://github.com/jakepurple13/OtakuWorld/blob/develop/docs/supabase/supabase_schema.sql)
   on GitHub and click the **Copy raw file** button.
2. In Supabase, open **SQL Editor** in the left sidebar.
3. Paste the script and click **Run**. You should see **Success. No rows returned**.

## Step 3: Turn on instant sync

This step lets changes show up on your other devices right away on Wi-Fi. If you skip it, sync
still works but only checks for changes every few minutes.

In **SQL Editor**, click **New query**, paste this, and click **Run**:

```sql
ALTER TABLE favorite_items       REPLICA IDENTITY FULL;
ALTER TABLE chapters_watched     REPLICA IDENTITY FULL;
ALTER TABLE bookmarked_chapters  REPLICA IDENTITY FULL;
ALTER TABLE notes                REPLICA IDENTITY FULL;
ALTER TABLE history              REPLICA IDENTITY FULL;
ALTER TABLE custom_list_items    REPLICA IDENTITY FULL;
ALTER TABLE custom_list_info     REPLICA IDENTITY FULL;
ALTER TABLE heatmap_items        REPLICA IDENTITY FULL;

ALTER PUBLICATION supabase_realtime ADD TABLE
    favorite_items, chapters_watched, bookmarked_chapters, notes,
    history, custom_list_items, custom_list_info, heatmap_items;
```

## Step 4: Copy your project URL and key

1. In Supabase, open **Project Settings** › **API**.
2. Copy the **Project URL**. It looks like `https://abcdefghijkl.supabase.co`.
3. Copy the **anon public** key. It's a long line of letters and numbers.

The anon key is safe to put in the app. Don't use the **service_role** key; it can bypass every
safety rule on your data.

## Step 5: Connect the app

1. In the app, go to **Settings › Integrations › Supabase** and open **Supabase Config**.
2. Paste the **Project URL** and **Anon Key**.
3. Tap **Test Connection**. When it succeeds, tap **Save**.
4. Go back and open **Supabase Auth**. Tap **Sign Up**, enter an email and password, and create
   your account. You can also use **Send Magic Link** to sign in from an email instead of a
   password.

If Supabase asks you to confirm your email, open the email it sent and tap the link, then sign
in.

Your existing favorites, lists, and history upload on the first sync. You can watch it in
**Supabase Sync Status**.

## Adding another device

You don't need to copy the URL and key again.

1. On the device that's already set up, open **Supabase Config** and tap **Share via QR**.
2. On the new device, open **Supabase Config** and tap **Scan QR**. Point the camera at the code.
3. Sign in with the same email and password.

Do this for each app separately. MangaWorld, AnimeWorld, and NovelWorld can all use the same
Supabase project and account, and each one keeps its own data apart.

## Choosing what syncs

Open **Supabase Backup Choices** to turn sync on or off for each kind of data, such as history or
notes. This is set per device.

## Keeping the project tidy (optional)

When you delete something, the app leaves a small marker in Supabase so your other devices know
to delete it too. These markers are tiny, but they pile up over time. This step removes ones older
than 30 days, every night.

1. In Supabase, open **Database › Extensions** and turn on **pg_cron**.
2. In **SQL Editor**, run the cleanup script from the
   [cleanup section of the setup guide](https://github.com/jakepurple13/OtakuWorld/blob/develop/docs/supabase/SETUP.md#tombstone-cleanup-supabase-cron-job).

If a device stays offline for more than 30 days, things you deleted elsewhere can come back on
it. Change `30 days` to `90 days` in the script if that's likely for you.

## Upgrading from an older version

If you set up sync before version 33.0.0, run the
[migration script](https://github.com/jakepurple13/OtakuWorld/blob/develop/docs/supabase/migration_add_app_column.sql)
once in **SQL Editor** after updating **all** your apps. Until you do, the app shows
**Supabase schema is out of date**. Older app versions can't sync after you run it.

## Troubleshooting

**Test Connection fails.** Check that the URL starts with `https://` and ends in `.supabase.co`,
and that you copied the whole anon key. Free Supabase projects pause after a week without use;
open your project in Supabase and click **Restore** if you see a paused banner.

**I signed in but nothing shows on my other device.** Make sure both devices use the same app,
the same project, and the same account. Then tap **Sync Now** in **Supabase Sync Status**.

**Changes take a few minutes to show up.** Step 3 wasn't run, or you're on mobile data. On mobile
data the app checks every few minutes to save battery and data.

**Recommendations don't sync.** That's expected for now. Recommendations stay on each device.
