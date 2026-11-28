package com.programmersbox.kmpmodels

sealed interface RepoCheck {
    /** [name] is the repo's own name from repo.json; blank when unknown. */
    data class Found(val name: String, val remotes: List<KmpRemoteSources>) : RepoCheck
    data object NotThisFormat : RepoCheck
    data class Failed(val message: String) : RepoCheck
}
