package com.example.cinelist

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val BASE_URL = "https://api.themoviedb.org/3/"

    // ⚠️ SUBSTITUA O TEXTO ABAIXO PELO SEU TOKEN DE LEITURA (v4) DO TMDB
    private const val TOKEN_AUTENTICACAO = "Bearer eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiI2MDI0NDJiZTA2MGIzZWI0NjM5ZWFlOTYyNmZkYTNmNSIsIm5iZiI6MTc3OTU2NzA0NS41MDU5OTk4LCJzdWIiOiI2YTEyMDljNTE2MDRkMmYxOTg0YjRjNTgiLCJzY29wZXMiOlsiYXBpX3JlYWQiXSwidmVyc2lvbiI6MX0.T26K9YdHxxNUXOdHkUGsXZnzZAhLl2VmvnoPz4Cj8SQ"

    val apiService: TmdbApiService by lazy {
        val client = okhttp3.OkHttpClient.Builder()
            .addInterceptor { chain ->
                val requestOriginal = chain.request()
                // Injeta o token de segurança em cada chamada automaticamente
                val novaRequest = requestOriginal.newBuilder()
                    .header("Authorization", TOKEN_AUTENTICACAO)
                    .header("accept", "application/json")
                    .build()
                chain.proceed(novaRequest)
            }
            .build()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create()) // Traduz JSON para Kotlin
            .build()
            .create(TmdbApiService::class.java)
    }
}