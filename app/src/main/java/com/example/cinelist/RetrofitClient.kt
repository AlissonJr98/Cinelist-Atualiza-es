package com.example.cinelist

import android.content.Context
import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Cliente Retrofit para a API do TMDB.
 *
 * O token de leitura (v4) é obtido dinamicamente com a seguinte precedência:
 * 1. Token salvo nas preferências locais pelo usuário no aplicativo.
 * 2. Token de compilação do `BuildConfig.TMDB_TOKEN` (definido em `local.properties` ou `TMDB_TOKEN` no CI).
 */
object RetrofitClient {
    private const val BASE_URL = "https://api.themoviedb.org/3/"
    private const val TAG = "RetrofitClient"
    private const val PREFS_NAME = "cinelist_tmdb_prefs"
    private const val KEY_TOKEN_CUSTOMIZADO = "tmdb_token_customizado"

    @Volatile
    private var tokenMemoria: String? = null

    /** Inicializa o token a partir das preferências do aplicativo. */
    fun inicializar(context: Context) {
        runCatching {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val salvo = prefs.getString(KEY_TOKEN_CUSTOMIZADO, null)?.trim()
            if (!salvo.isNullOrBlank() && salvo.startsWith("eyJ")) {
                tokenMemoria = salvo
                Log.d(TAG, "Token TMDB customizado carregado com sucesso.")
            }
        }.onFailure { Log.w(TAG, "Falha ao inicializar token TMDB das preferências", it) }
    }

    /** Salva um novo token TMDB (deve ser um JWT válido iniciando com 'eyJ'). */
    fun salvarToken(context: Context, novoToken: String): Boolean {
        val limpo = novoToken.trim()
        if (!limpo.startsWith("eyJ")) return false
        tokenMemoria = limpo
        return runCatching {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_TOKEN_CUSTOMIZADO, limpo)
                .commit()
        }.getOrDefault(false)
    }

    /** Retorna o token TMDB ativo. */
    fun obterToken(): String {
        return tokenMemoria?.takeIf { it.isNotBlank() }
            ?: BuildConfig.TMDB_TOKEN.trim().takeIf { it.isNotBlank() }
            ?: ""
    }

    /** Verdadeiro quando o token foi corretamente configurado. */
    val tokenConfigurado: Boolean get() = obterToken().startsWith("eyJ")

    val TOKEN_AUTENTICACAO: String get() = "Bearer ${obterToken()}"

    /** Idioma atual ativo nas consultas ao TMDB (inicializado conforme o dispositivo ou preferência). */
    @Volatile
    var idiomaAtual: String = "pt-BR"
        private set

    fun atualizarIdioma(novoIdioma: String) {
        if (novoIdioma.isNotBlank()) {
            idiomaAtual = novoIdioma
            Log.d(TAG, "Idioma do TMDB atualizado para: $novoIdioma")
        }
    }

    /** Lê o corpo da resposta em caso de erro sem consumir a stream (usando peekBody). */
    private class LoggingInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            val response = chain.proceed(request)

            if (!response.isSuccessful) {
                val corpoErro = runCatching { response.peekBody(1024 * 1024).string() }.getOrNull()
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
                val urlOriginal = requestOriginal.url

                // Injeta automaticamente o idioma configurado quando a requisição usar o idioma padrão ou não tiver definido
                val paramIdioma = urlOriginal.queryParameter("language")
                val urlBuilder = urlOriginal.newBuilder()
                if (paramIdioma == null || paramIdioma == "pt-BR") {
                    urlBuilder.setQueryParameter("language", idiomaAtual)
                }

                val novaRequest = requestOriginal.newBuilder()
                    .url(urlBuilder.build())
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
        appendLine("Token (início): ${if (tokenConfigurado) obterToken().take(12) + "..." else "VAZIO"}")
    }
}
