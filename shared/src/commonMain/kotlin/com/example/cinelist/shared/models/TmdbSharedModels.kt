package com.example.cinelist.shared.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TmdbPagedResponse<T>(
    @SerialName("page") val page: Int = 1,
    @SerialName("results") val results: List<T> = emptyList(),
    @SerialName("total_pages") val totalPages: Int = 1,
    @SerialName("total_results") val totalResults: Int = 0
)

@Serializable
data class TmdbMediaItem(
    @SerialName("id") val id: Int = 0,
    @SerialName("title") val title: String? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("original_title") val originalTitle: String? = null,
    @SerialName("original_name") val originalName: String? = null,
    @SerialName("overview") val overview: String = "",
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("media_type") val mediaType: String? = null,
    @SerialName("genre_ids") val genreIds: List<Int> = emptyList(),
    @SerialName("vote_average") val voteAverage: Double = 0.0,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("first_air_date") val firstAirDate: String? = null
) {
    val displayTitle: String
        get() = title?.takeIf { it.isNotBlank() } ?: name ?: ""

    val displayReleaseDate: String
        get() = releaseDate?.takeIf { it.isNotBlank() } ?: firstAirDate ?: ""

    val releaseYear: String
        get() = displayReleaseDate.take(4).takeIf { it.isNotBlank() } ?: ""

    val isTvShow: Boolean
        get() = mediaType.equals("tv", ignoreCase = true) || (title == null && name != null)

    val posterUrl: String
        get() = posterPath?.let { "https://image.tmdb.org/t/p/w500$it" } ?: ""

    val backdropUrl: String
        get() = backdropPath?.let { "https://image.tmdb.org/t/p/w1280$it" } ?: ""
}

@Serializable
data class TmdbGenre(
    @SerialName("id") val id: Int = 0,
    @SerialName("name") val name: String = ""
)

@Serializable
data class TmdbGenreResponse(
    @SerialName("genres") val genres: List<TmdbGenre> = emptyList()
)
