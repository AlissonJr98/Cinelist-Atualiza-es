package com.example.cinelist

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.cinelist.shared.i18n.I18nCore

/**
 * Motor central de internacionalização (i18n) do CineList.
 * Fornece tradução reativa e instantânea para todos os textos da interface
 * nos idiomas suportados pelo aplicativo, integrado com o módulo compartilhado Multiplataforma.
 */
object I18n {

    // Estado reativo do idioma ativo (para que componentes Compose recomponham automaticamente)
    var idiomaAtivo by mutableStateOf("pt-BR")

    fun definirIdioma(tag: String) {
        idiomaAtivo = tag
        I18nCore.definirIdioma(tag)
    }

    /**
     * Traduz uma string em português para o idioma ativo com suporte inteligente
     * para caracteres de pontuação, emojis, contadores, separadores compostos e datas relativas.
     */
    fun traduzir(textoPt: String): String {
        return I18nCore.traduzir(textoPt, idiomaAtivo)
    }
}

/**
 * Extensão conveniente para traduzir qualquer string literal da UI do CineList.
 * Exemplo: "Minha Lista".t()
 */
fun String.t(): String {
    return I18n.traduzir(this)
}

/**
 * Formata temporada e episódio respeitando o idioma ativo do usuário.
 */
fun formatarTemporadaEpisodio(temporada: Int, episodio: Int): String {
    return I18nCore.formatarTemporadaEpisodio(temporada, episodio, I18n.idiomaAtivo)
}
