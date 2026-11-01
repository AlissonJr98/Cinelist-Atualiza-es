package com.example.cinelist

import android.content.Context
import android.telephony.TelephonyManager
import java.util.Locale

data class ItemPais(
    val codigoIso: String,
    val nomePt: String,
    val bandeiraEmoji: String
)

object PaisRegiaoHelper {

    private const val PREFS_NAME = "cinelist_prefs"
    private const val CHAVE_PAIS = "pais_origem_iso"

    val PAISES_DISPONIVEIS = listOf(
        ItemPais("AUTO", "Automático (Localizar)", "🌐"),
        ItemPais("BR", "Brasil", "🇧🇷"),
        ItemPais("US", "Estados Unidos", "🇺🇸"),
        ItemPais("KR", "Coreia do Sul", "🇰🇷"),
        ItemPais("JP", "Japão", "🇯🇵"),
        ItemPais("PT", "Portugal", "🇵🇹"),
        ItemPais("ES", "Espanha", "🇪🇸"),
        ItemPais("FR", "França", "🇫🇷"),
        ItemPais("DE", "Alemanha", "🇩🇪"),
        ItemPais("IT", "Itália", "🇮🇹"),
        ItemPais("GB", "Reino Unido", "🇬🇧"),
        ItemPais("CA", "Canadá", "🇨🇦"),
        ItemPais("MX", "México", "🇲🇽"),
        ItemPais("AR", "Argentina", "🇦🇷"),
        ItemPais("CL", "Chile", "🇨🇱"),
        ItemPais("CO", "Colômbia", "🇨🇴"),
        ItemPais("PE", "Peru", "🇵🇪"),
        ItemPais("UY", "Uruguai", "🇺🇾"),
        ItemPais("AU", "Austrália", "🇦🇺"),
        ItemPais("CN", "China", "🇨🇳"),
        ItemPais("IN", "Índia", "🇮🇳")
    )

    fun obterPaisConfigurado(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(CHAVE_PAIS, "AUTO") ?: "AUTO"
    }

    fun salvarPaisConfigurado(context: Context, codigoIso: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(CHAVE_PAIS, codigoIso.uppercase()).apply()
    }

    /**
     * Retorna o código ISO de 2 letras real do país do usuário, respeitando a configuração
     * salva ou detectando automaticamente via TelephonyManager / Locale / Idioma.
     */
    fun obterPaisUsuario(context: Context? = null): String {
        if (context != null) {
            val salvo = obterPaisConfigurado(context)
            if (salvo.isNotBlank() && salvo != "AUTO") {
                return salvo.uppercase()
            }
            return detectarPaisAutomaticamente(context)
        }

        try {
            val localeCountry = Locale.getDefault().country?.trim()?.uppercase()
            if (!localeCountry.isNullOrBlank() && localeCountry.length == 2) {
                return localeCountry
            }
        } catch (_: Exception) {}

        val tag = I18n.idiomaAtivo.lowercase()
        return when {
            tag.startsWith("ko") -> "KR"
            tag.startsWith("ja") -> "JP"
            tag.startsWith("pt") -> "BR"
            tag.startsWith("en") -> "US"
            tag.startsWith("es") -> "ES"
            tag.startsWith("fr") -> "FR"
            tag.startsWith("de") -> "DE"
            tag.startsWith("it") -> "IT"
            else -> "BR"
        }
    }

    fun detectarPaisAutomaticamente(context: Context): String {
        try {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            val networkCountry = tm?.networkCountryIso?.trim()?.uppercase()
            if (!networkCountry.isNullOrBlank() && networkCountry.length == 2) {
                return networkCountry
            }
            val simCountry = tm?.simCountryIso?.trim()?.uppercase()
            if (!simCountry.isNullOrBlank() && simCountry.length == 2) {
                return simCountry
            }
        } catch (_: Exception) {}

        try {
            val localeCountry = Locale.getDefault().country?.trim()?.uppercase()
            if (!localeCountry.isNullOrBlank() && localeCountry.length == 2) {
                return localeCountry
            }
        } catch (_: Exception) {}

        // Fallback inteligente baseado no idioma selecionado no CineList
        val tag = I18n.idiomaAtivo.lowercase()
        return when {
            tag.startsWith("ko") -> "KR"
            tag.startsWith("ja") -> "JP"
            tag.startsWith("pt") -> "BR"
            tag.startsWith("en") -> "US"
            tag.startsWith("es") -> "ES"
            tag.startsWith("fr") -> "FR"
            tag.startsWith("de") -> "DE"
            tag.startsWith("it") -> "IT"
            else -> "BR"
        }
    }

    fun obterEmojiBandeira(codigoIso: String?): String {
        if (codigoIso.isNullOrBlank()) return ""
        val codigo = codigoIso.trim().uppercase()
        if (codigo == "AUTO") return "🌐"
        if (codigo.length != 2) return ""
        val primeiro = codigo[0]
        val segundo = codigo[1]
        if (primeiro !in 'A'..'Z' || segundo !in 'A'..'Z') return ""

        val flagOffset = 0x1F1E6
        val asciiOffset = 0x41
        val firstChar = Character.toChars(flagOffset + (primeiro.code - asciiOffset))
        val secondChar = Character.toChars(flagOffset + (segundo.code - asciiOffset))
        return String(firstChar) + String(secondChar)
    }

    fun obterNomeTraduzido(codigoIso: String): String {
        val item = PAISES_DISPONIVEIS.find { it.codigoIso.equals(codigoIso, ignoreCase = true) }
        val nomeBase = item?.nomePt ?: codigoIso
        return nomeBase.t()
    }
}
