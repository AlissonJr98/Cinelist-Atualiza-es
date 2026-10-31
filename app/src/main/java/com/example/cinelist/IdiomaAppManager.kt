package com.example.cinelist

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

/**
 * Utilitário global para recuperar com segurança a Activity a partir de qualquer contexto (mesmo ContextThemeWrapper).
 */
tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

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
 * Implementa o padrão oficial do Android (Per-App Language Preferences / Suporte a Diferentes Idiomas),
 * sincronizando a configuração do sistema operacional (Android 13+ / AndroidX), motor I18n do Compose
 * e chamadas de catálogo do TMDB.
 */
object IdiomaAppManager {

    private const val PREFS_NAME = "ConfiguracoesPerfil"
    private const val CHAVE_PREFERENCIA_IDIOMA = "idioma_selecionado_usuario"

    /**
     * Retorna o nome do idioma traduzido de acordo com a língua que o usuário está visualizando no app.
     * Exemplo: Se o app está em inglês, "Português (Brasil)" aparece como "Portuguese (Brazil)".
     * Se o app está em espanhol, aparece como "Portugués (Brasil)".
     */
    fun obterNomeTraduzido(codigo: String, idiomaVisualizacao: String = I18n.idiomaAtivo): String {
        val lang = idiomaVisualizacao.lowercase().take(2)
        return when (codigo) {
            "SISTEMA" -> when (lang) {
                "en" -> "System Default (Automatic)"
                "es" -> "Predeterminado del Celular (Automático)"
                "fr" -> "Par Défaut du Téléphone (Automatique)"
                "de" -> "Handy-Standard (Automatisch)"
                "it" -> "Predefinito del Telefono (Automatico)"
                "ja" -> "スマホのデフォルト (自動)"
                "ko" -> "스마트폰 기본값 (자동)"
                else -> "Padrão do Celular (Automático)"
            }
            "pt-BR" -> when (lang) {
                "en" -> "Portuguese (Brazil)"
                "es" -> "Portugués (Brasil)"
                "fr" -> "Portugais (Brésil)"
                "de" -> "Portugiesisch (Brasilien)"
                "it" -> "Portoghese (Brasile)"
                "ja" -> "ポルトガル語 (ブラジル)"
                "ko" -> "포르투갈어 (브라질)"
                else -> "Português (Brasil)"
            }
            "en-US" -> when (lang) {
                "es" -> "Inglés (Estados Unidos)"
                "fr" -> "Anglais (États-Unis)"
                "de" -> "Englisch (Vereinigte Staaten)"
                "it" -> "Inglese (Stati Uniti)"
                "ja" -> "英語 (米国)"
                "ko" -> "영어 (미국)"
                "pt" -> "Inglês (Estados Unidos)"
                else -> "English (United States)"
            }
            "es-ES" -> when (lang) {
                "en" -> "Spanish"
                "fr" -> "Espagnol"
                "de" -> "Spanisch"
                "it" -> "Spagnolo"
                "ja" -> "スペイン語"
                "ko" -> "스페인어"
                "pt" -> "Espanhol"
                else -> "Español"
            }
            "fr-FR" -> when (lang) {
                "en" -> "French"
                "es" -> "Francés"
                "de" -> "Französisch"
                "it" -> "Francese"
                "ja" -> "フランス語"
                "ko" -> "프랑스어"
                "pt" -> "Francês"
                else -> "Français"
            }
            "de-DE" -> when (lang) {
                "en" -> "German"
                "es" -> "Alemán"
                "fr" -> "Allemand"
                "it" -> "Tedesco"
                "ja" -> "ドイツ語"
                "ko" -> "독일어"
                "pt" -> "Alemão"
                else -> "Deutsch"
            }
            "it-IT" -> when (lang) {
                "en" -> "Italian"
                "es" -> "Italiano"
                "fr" -> "Italien"
                "de" -> "Italienisch"
                "ja" -> "イタリア語"
                "ko" -> "이탈리아어"
                "pt" -> "Italiano"
                else -> "Italiano"
            }
            "ja-JP" -> when (lang) {
                "en" -> "Japanese"
                "es" -> "Japonés"
                "fr" -> "Japonais"
                "de" -> "Japanisch"
                "it" -> "Giapponese"
                "ja" -> "日本語"
                "ko" -> "일본어"
                "pt" -> "Japonês"
                else -> "Japanese"
            }
            "ko-KR" -> when (lang) {
                "en" -> "Korean"
                "es" -> "Coreano"
                "fr" -> "Coréen"
                "de" -> "Koreanisch"
                "it" -> "Coreano"
                "ja" -> "韓国語"
                "ko" -> "한국어"
                "pt" -> "Coreano"
                else -> "Korean"
            }
            else -> IDIOMAS_DISPONIVEIS.find { it.codigo == codigo }?.nome?.t() ?: codigo
        }
    }

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
            "${opcao.bandeiraEmoji} ${opcao.nome.t()}"
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
     * Retorna a tag do TMDB e Locale efetivamente em uso ("pt-BR", "en-US", etc.).
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
     * Converte tag BCP47 ("pt-BR", "en-US", "es") em objeto Locale do Java.
     */
    fun tagParaLocale(tag: String): Locale {
        return try {
            if (tag.contains("-")) {
                val partes = tag.split("-")
                Locale(partes[0], partes[1])
            } else {
                Locale(tag)
            }
        } catch (_: Exception) {
            Locale.getDefault()
        }
    }

    /**
     * Salva a preferência escolhida pelo usuário e sincroniza com a API oficial
     * do Android (Per-App Language Preferences), Retrofit (TMDB), motor i18n
     * e resources do sistema operacional.
     */
    fun salvarPreferenciaEAplicar(context: Context, codigoEscolhido: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(CHAVE_PREFERENCIA_IDIOMA, codigoEscolhido).apply()

        val tagEfetiva = if (codigoEscolhido == "SISTEMA") {
            obterIdiomaPadraoDispositivo()
        } else {
            codigoEscolhido
        }

        // 1. Atualiza o motor i18n Compose
        I18n.definirIdioma(tagEfetiva)
        MidiaLocalizationManager.limparCache()

        // 2. Atualiza o cliente Retrofit do TMDB
        RetrofitClient.atualizarIdioma(tagEfetiva)

        // 3. Aplica o padrão oficial do Android (AppCompatDelegate e LocaleManager)
        try {
            if (codigoEscolhido == "SISTEMA") {
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
            } else {
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tagEfetiva))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 4. Aplica o Locale global na JVM e nos Resources do Context
        aplicarLocaleGlobal(context, tagEfetiva)
    }

    /**
     * Aplica o Locale nos Resources e na JVM.
     */
    fun aplicarLocaleGlobal(context: Context, tag: String) {
        try {
            val locale = tagParaLocale(tag)
            Locale.setDefault(locale)

            val resources = context.resources
            val config = Configuration(resources.configuration)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                config.setLocales(LocaleList(locale))
            } else {
                @Suppress("DEPRECATION")
                config.locale = locale
            }
            @Suppress("DEPRECATION")
            resources.updateConfiguration(config, resources.displayMetrics)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Cria e retorna um novo contexto configurado com a linguagem selecionada (útil em attachBaseContext).
     */
    fun aplicarContextLocale(baseContext: Context): Context {
        return try {
            val tag = obterTagTmdbEfetiva(baseContext)
            I18n.definirIdioma(tag)
            RetrofitClient.atualizarIdioma(tag)

            val locale = tagParaLocale(tag)
            Locale.setDefault(locale)

            val config = Configuration(baseContext.resources.configuration)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                config.setLocales(LocaleList(locale))
                baseContext.createConfigurationContext(config)
            } else {
                @Suppress("DEPRECATION")
                config.locale = locale
                @Suppress("DEPRECATION")
                baseContext.resources.updateConfiguration(config, baseContext.resources.displayMetrics)
                baseContext
            }
        } catch (_: Exception) {
            baseContext
        }
    }

    /**
     * Inicializa a linguagem na abertura do aplicativo.
     */
    fun inicializar(context: Context) {
        val tagEfetiva = obterTagTmdbEfetiva(context)
        I18n.definirIdioma(tagEfetiva)
        RetrofitClient.atualizarIdioma(tagEfetiva)
        aplicarLocaleGlobal(context, tagEfetiva)
    }
}
