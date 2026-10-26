// Copied from recloudstream/cloudstream d9c55e2e5b020dde8e28c12a3e7802df962265ac, GPL-3.0
// OTAKU: only the nested AniList response classes plugins reuse (and the classes those reference).
// The AniList sync API itself is not shimmed, so the outer class does not extend SyncAPI.
package com.lagradost.cloudstream3.syncproviders.providers

import com.fasterxml.jackson.annotation.JsonProperty
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class AniListApi {
    @Serializable
    data class Title(
        @JsonProperty("english") @SerialName("english") val english: String?,
        @JsonProperty("romaji") @SerialName("romaji") val romaji: String?,
    )

    @Serializable
    data class CoverImage(
        @JsonProperty("medium") @SerialName("medium") val medium: String?,
        @JsonProperty("large") @SerialName("large") val large: String?,
        @JsonProperty("extraLarge") @SerialName("extraLarge") val extraLarge: String?,
    )

    @Serializable
    data class RecommendedMedia(
        @JsonProperty("id") @SerialName("id") val id: Int?,
        @JsonProperty("title") @SerialName("title") val title: MediaTitle?,
        @JsonProperty("coverImage") @SerialName("coverImage") val coverImage: MediaCoverImage?,
    )

    @Serializable
    data class RecommendationConnection(
        @JsonProperty("edges") @SerialName("edges") val edges: List<RecommendationEdge> = emptyList(),
        @JsonProperty("nodes") @SerialName("nodes") val nodes: List<Recommendation> = emptyList(),
    )

    @Serializable
    data class RecommendationEdge(
        @JsonProperty("node") @SerialName("node") val node: Recommendation,
    )

    @Serializable
    data class Recommendation(
        @JsonProperty("mediaRecommendation") @SerialName("mediaRecommendation") val mediaRecommendation: RecommendedMedia?,
    )

    @Serializable
    data class MediaCoverImage(
        @JsonProperty("extraLarge") @SerialName("extraLarge") val extraLarge: String?,
        @JsonProperty("large") @SerialName("large") val large: String?,
        @JsonProperty("medium") @SerialName("medium") val medium: String?,
        @JsonProperty("color") @SerialName("color") val color: String?,
    )

    @Serializable
    data class SeasonNextAiringEpisode(
        @JsonProperty("episode") @SerialName("episode") val episode: Int?,
        @JsonProperty("timeUntilAiring") @SerialName("timeUntilAiring") val timeUntilAiring: Int?,
    )

    @Serializable
    data class MediaTitle(
        @JsonProperty("romaji") @SerialName("romaji") val romaji: String?,
        @JsonProperty("english") @SerialName("english") val english: String?,
        @JsonProperty("native") @SerialName("native") val native: String?,
        @JsonProperty("userPreferred") @SerialName("userPreferred") val userPreferred: String?,
    )

    @Serializable
    data class LikePageInfo(
        @JsonProperty("total") @SerialName("total") val total: Int?,
        @JsonProperty("currentPage") @SerialName("currentPage") val currentPage: Int?,
        @JsonProperty("lastPage") @SerialName("lastPage") val lastPage: Int?,
        @JsonProperty("perPage") @SerialName("perPage") val perPage: Int?,
        @JsonProperty("hasNextPage") @SerialName("hasNextPage") val hasNextPage: Boolean?,
    )
}
