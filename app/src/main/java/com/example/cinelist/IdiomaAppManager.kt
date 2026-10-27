package com.example.cinelist

import android.content.Context
import java.util.Locale

/**
 * Representa uma opção de idioma para a interface e requisições do TMDB.
 */
data class IdiomaOpcao(
    val codigo: String,        // "SISTEMA", "pt-BR", "en-US", "es-ES", etc.
    val nome: String,          // "Português (Brasil)", "English (US)", etc.
    val bandeiraEmoji: String, // "🌐", "🇧🇷", "🇺🇸", etc.
    val tagTmdb: String        // "pt-BR", "en-US", etc.
)

/**
 * Gerenciador central de idiomas e localização do CineList.
 * Reconhece a língua padrão do dispositivo móvel como principal e permite
 * customização manual no menu de configurações.
 */
object IdiomaAppManager {

    private const val PREFS_NAME = "ConfiguracoesPerfil"
    private const val CHAVE_PREFERENCIA_IDIOMA = "idioma_selecionado_usuario"

    val IDIOMAS_DISPONIVEIS = listOf(
        IdiomaOpcao(
            codigo = "SISTEMA",
            nome = "Padrão do Celular (Automático)",
            bandeiraEmoji = "🌐",
            tagTmdb = "" // Resolvido dinamicamente pelo dispositivo
        ),
        IdiomaOpcao(
            codigo = "pt-BR",
            nome = "Português (Brasil)",
            bandeiraEmoji = "🇧🇷",
            tagTmdb = "pt-BR"
        ),
        IdiomaOpcao(
            codigo = "en-US",
            nome = "English (United States)",
            bandeiraEmoji = "🇺🇸",
            tagTmdb = "en-US"
        ),
        IdiomaOpcao(
            codigo = "es-ES",
            nome = "Español",
            bandeiraEmoji = "🇪🇸",
            tagTmdb = "es-ES"
        ),
        IdiomaOpcao(
            codigo = "fr-FR",
            nome = "Français",
            bandeiraEmoji = "🇫🇷",
            tagTmdb = "fr-FR"
        ),
        IdiomaOpcao(
            codigo = "de-DE",
            nome = "Deutsch",
            bandeiraEmoji = "🇩🇪",
            tagTmdb = "de-DE"
        ),
        IdiomaOpcao(
            codigo = "it-IT",
            nome = "Italiano",
            bandeiraEmoji = "🇮🇹",
            tagTmdb = "it-IT"
        ),
        IdiomaOpcao(
            codigo = "ja-JP",
            nome = "日本語 (Japonês)",
            bandeiraEmoji = "🇯🇵",
            tagTmdb = "ja-JP"
        ),
        IdiomaOpcao(
            codigo = "ko-KR",
            nome = "한국어 (Coreano)",
            bandeiraEmoji = "🇰🇷",
            tagTmdb = "ko-KR"
        )
    )

    /**
     * Identifica a língua padrão atualmente configurada no sistema operacional do celular.
     */
    fun obterIdiomaPadraoDispositivo(): String {
        return try {
            val locale = Locale.getDefault()
            val lang = locale.language.lowercase()
            val country = locale.country.uppercase()

            when (lang) {
                "pt" -> "pt-BR"
                "en" -> if (country == "GB" || country == "UK") "en-GB" else "en-US"
                "es" -> if (country.isNotBlank()) "es-$country" else "es-ES"
                "fr" -> "fr-FR"
                "de" -> "de-DE"
                "it" -> "it-IT"
                "ja" -> "ja-JP"
                "ko" -> "ko-KR"
                else -> "pt-BR"
            }
        } catch (_: Exception) {
            "pt-BR"
        }
    }

    /**
     * Nome descritivo e amigável da língua padrão do celular.
     */
    fun obterNomeAmigavelIdiomaDispositivo(): String {
        val tag = obterIdiomaPadraoDispositivo()
        val opcao = IDIOMAS_DISPONIVEIS.find { it.codigo == tag }
        return if (opcao != null) {
            "${opcao.bandeiraEmoji} ${opcao.nome}"
        } else {
            "🌐 ${Locale.getDefault().displayName}"
        }
    }

    /**
     * Retorna a chave salva pelo usuário ("SISTEMA", "pt-BR", etc.).
     */
    fun obterChaveConfigurada(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(CHAVE_PREFERENCIA_IDIOMA, "SISTEMA") ?: "SISTEMA"
    }

    /**
     * Retorna a tag do TMDB efetivamente em uso ("pt-BR", "en-US", etc.).
     */
    fun obterTagTmdbEfetiva(context: Context): String {
        val chave = obterChaveConfigurada(context)
        return if (chave == "SISTEMA") {
            obterIdiomaPadraoDispositivo()
        } else {
            chave
        }
    }

    /**
     * Salva a preferência escolhida pelo usuário e sincroniza com o cliente HTTP do TMDB e Locale do app.
     */
    fun salvarPreferencia(context: Context, codigoEscolhido: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(CHAVE_PREFERENCIA_IDIOMA, codigoEscolhido).apply()

        val tagEfetiva = if (codigoEscolhido == "SISTEMA") {
            obterIdiomaPadraoDispositivo()
        } else {
            codigoEscolhido
        }

        // Atualiza o cliente Retrofit em tempo real
        RetrofitClient.atualizarIdioma(tagEfetiva)
    }

    /**
     * Inicializa a linguagem na abertura do aplicativo.
     */
    fun inicializar(context: Context) {
        val tagEfetiva = obterTagTmdbEfetiva(context)
        RetrofitClient.atualizarIdioma(tagEfetiva)
    }
}
