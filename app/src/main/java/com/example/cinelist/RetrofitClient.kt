package com.example.cinelist

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Cliente Retrofit para a API do TMDB.
 *
 * O token de leitura (v4) é obtido do `BuildConfig`, que é gerado a partir
 * da propriedade `tmdb.token` no `local.properties` (ou da variável de
 * ambiente `TMDB_TOKEN` no CI).
 */
object RetrofitClient {
    private const val BASE_URL = "https://api.themoviedb.org/3/"
    private const val TAG = "RetrofitClient"

    // Token lido do BuildConfig (configurado via local.properties)
    private val tokenBruto: String = BuildConfig.TMDB_TOKEN.trim()

    /** Verdadeiro quando o token foi corretamente configurado. */
    val tokenConfigurado: Boolean get() = tokenBruto.startsWith("eyJ")

    private val TOKEN_AUTENTICACAO: String = "Bearer $tokenBruto"



    /** Lê o corpo da resposta em caso de erro, para diagnosis nas logs. */
    private class LoggingInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            val response = chain.proceed(request)

            if (!response.isSuccessful) {
                val corpoErro = runCatching { response.body?.string() }.getOrNull()
                Log.e(
                    TAG,
                    "TMDB ${response.code} em ${request.url.encodedPath}: ${corpoErro.orEmpty().take(300)}"
                )
            }
            return response
        }
    }

    val apiService: TmdbApiService by lazy {
        val client = okhttp3.OkHttpClient.Builder()
            // Timeouts generosos: sem isto, chamadas lentas travam a UI
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .addInterceptor { chain ->
                val requestOriginal = chain.request()
                // Injeta o token de segurança em cada chamada automaticamente
                val novaRequest = requestOriginal.newBuilder()
                    .header("Authorization", TOKEN_AUTENTICACAO)
                    .header("accept", "application/json")
                    .build()
                chain.proceed(novaRequest)
            }
            .addInterceptor(LoggingInterceptor())
            .build()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create()) // Traduz JSON para Kotlin
            .build()
            .create(TmdbApiService::class.java)
    }

    /** Verifica a saúde da configuração. Útil para depuração e testes. */
    fun diagnostico(): String = buildString {
        appendLine("TMDB base: $BASE_URL")
        appendLine("Token configurado: $tokenConfigurado")
        appendLine("Token (início): ${if (tokenConfigurado) tokenBruto.take(12) + "..." else "VAZIO"}")
    }
}
