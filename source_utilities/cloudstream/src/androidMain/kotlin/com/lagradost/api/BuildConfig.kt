// OTAKU: replaces the BuildConfig object upstream generates with the buildkonfig plugin from
// local.properties (`mdl.key`, `trakt.id`). We ship no keys, so the metaproviders that read them
// (MyDramaList, Trakt) get the same "null" string upstream bakes in when no key is configured.
package com.lagradost.api

object BuildConfig {
    val MDL_API_KEY: String = "null"
    val TRAKT_CLIENT_ID: String = "null"
}
