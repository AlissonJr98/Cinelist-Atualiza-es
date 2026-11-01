package com.example.cinelist.shared.network

import com.example.cinelist.shared.models.TmdbGenreResponse
import com.example.cinelist.shared.models.TmdbMediaItem
import com.example.cinelist.shared.models.TmdbPagedResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter

/**
 * Cliente de API oficial do TMDB para Kotlin Multiplatform (Android & iOS).
 */
class TmdbMultiplatformClient(
    val client: HttpClient = createPlatformHttpClient(),
    val apiToken: String = DEFAULT_TMDB_TOKEN
) {
    companion object {
        const val BASE_URL = "https://api.themoviedb.org/3"
        const val DEFAULT_TMDB_TOKEN = "eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiI2MDI0NDJiZTA2MGIzZWI0NjM5ZWFlOTYyNmZkYTNmNSIsIm5iZiI6MTc3OTU2NzA0NS41MDU5OTk4LCJzdWIiOiI2YTEyMDljNTE2MDRkMmYxOTg0YjRjNTgiLCJzY29wZXMiOlsiYXBpX3JlYWQiXSwidmVyc2lvbiI6MX0.T26K9YdHxxNUXOdHkUGsXZnzZAhLl2VmvnoPz4Cj8SQ"
    }

    suspend fun getTrending(
        timeWindow: String = "week",
        language: String = "pt-BR",
        page: Int = 1
    ): TmdbPagedResponse<TmdbMediaItem> {
        return client.get("$BASE_URL/trending/all/$timeWindow") {
            header("Authorization", "Bearer $apiToken")
            parameter("language", language)
            parameter("page", page)
        }.body()
    }

    suspend fun searchMulti(
        query: String,
        language: String = "pt-BR",
        page: Int = 1,
        includeAdult: Boolean = false
    ): TmdbPagedResponse<TmdbMediaItem> {
        return client.get("$BASE_URL/search/multi") {
            header("Authorization", "Bearer $apiToken")
            parameter("query", query)
            parameter("language", language)
            parameter("page", page)
            parameter("include_adult", includeAdult)
        }.body()
    }

    suspend fun getPopularMovies(
        language: String = "pt-BR",
        page: Int = 1
    ): TmdbPagedResponse<TmdbMediaItem> {
        return client.get("$BASE_URL/movie/popular") {
            header("Authorization", "Bearer $apiToken")
            parameter("language", language)
            parameter("page", page)
        }.body()
    }

    suspend fun getPopularTvShows(
        language: String = "pt-BR",
        page: Int = 1
    ): TmdbPagedResponse<TmdbMediaItem> {
        return client.get("$BASE_URL/tv/popular") {
            header("Authorization", "Bearer $apiToken")
            parameter("language", language)
            parameter("page", page)
        }.body()
    }

    suspend fun getMovieGenres(
        language: String = "pt-BR"
    ): TmdbGenreResponse {
        return client.get("$BASE_URL/genre/movie/list") {
            header("Authorization", "Bearer $apiToken")
            parameter("language", language)
        }.body()
    }

    suspend fun getTvGenres(
        language: String = "pt-BR"
    ): TmdbGenreResponse {
        return client.get("$BASE_URL/genre/tv/list") {
            header("Authorization", "Bearer $apiToken")
            parameter("language", language)
        }.body()
    }
}
