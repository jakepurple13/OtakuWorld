# OtakuWorld

This is a modifiable app to handle syncing with whatever server you want!
So you don't have to change anything about each of the other apps, MangaWorld, AnimeWorld,
NovelWorld.
Right now, Firebase is being used for cloud syncing, however, that will soon be phased out. (Not
sure when, just know its coming.) Since this cannot be used everywhere, this app was made.
There is no official alternative right now that we can just switch to, however, this app gives you
the tools to find a possible alternative.
If someone wants to help and setup a server, we can work together on that, but until then, this is a
warning.

The current setup is for the custom made
server [OtakuWorld Server](https://github.com/jakepurple13/OtakuWorldServer).
But anyone can modify this to point to any server they want as well as handle the data however they
want.
Syncing, the ui, whatever. BUT! This readme will explain how the basics of this works and why one
might want to leave some things as is.

## Account Management

This project is set up with
the [Account Manager](https://developer.android.com/reference/android/accounts/AccountManager), a
built in way to handle accounts in Android.
This is set up for a number of reasons.

1. We get access
   to [Sync Adapters](https://developer.android.com/training/sync-adapters/creating-sync-adapter)!
    - This means that whenever a favorite is added, removed, or modified, we will automatically sync
      with the server.
2. You can manually sync with the server in a single place!

## Content Providers

Each of the OtakuWorld apps are now equipped with Content Providers that OtakuWorld reads and writes
to.
This is what actually allows us to make the Sync Adapters work!
We can view all of the data from each app in one place, in case you want to look at them without
going to the actual app.
This might seem a bit...unintuitive, and you are right! However, I wanted to make a ui in case I
wanted to add some settings or any other interesting stuff.

## So what do I need to do?

Really? Only two things. I know that sounds easy, but there is a lot more to it.

### Step 1

Modify the ServerHandling class in the ServerHandler file. Set it up however you want to get and
push data to the server.

### Step 2

Modify the LoginActivity however you want to make sure the UI is how you like it to log in!

### Optional Steps

If you want a different sync strategy, that's completely fine! All you really need to do is:

1. Go to the FavoritesSyncAdapter and change the sync strategy to however you want!
2. Go to the ListSyncAdapter and change the sync strategy to however you want!