package com.example.cinelist

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeneroLocalizationHelperTest {

    @Test
    fun testTraducaoGenerosPtBr() {
        I18n.definirIdioma("pt-BR")
        val resultado = GeneroLocalizationHelper.formatarGeneros("Drama, Action & Adventure, Mystery", "pt-BR")
        assertEquals("Drama, Ação e Aventura, Mistério", resultado)
    }

    @Test
    fun testTraducaoGenerosComDelimitadoresVariados() {
        I18n.definirIdioma("pt-BR")
        val resultado1 = GeneroLocalizationHelper.formatarGeneros("Action & Adventure • Mystery / Animation", "pt-BR")
        assertEquals("Ação e Aventura, Mistério, Animação", resultado1)

        val resultado2 = GeneroLocalizationHelper.formatarGeneros("Comédia | Ficção Científica", "pt-BR")
        assertEquals("Comédia, Ficção Científica", resultado2)
    }

    @Test
    fun testTraducaoGenerosEnUs() {
        I18n.definirIdioma("en-US")
        val resultado = GeneroLocalizationHelper.formatarGeneros("Drama, Action & Adventure, Mystery", "en-US")
        assertEquals("Drama, Action & Adventure, Mystery", resultado)
    }

    @Test
    fun testTraducaoGenerosEs() {
        I18n.definirIdioma("es-ES")
        val resultado = GeneroLocalizationHelper.formatarGeneros("Drama, Action & Adventure, Mystery", "es-ES")
        assertEquals("Drama, Acción y Aventura, Misterio", resultado)
    }

    @Test
    fun testTraducaoGenerosDe() {
        I18n.definirIdioma("de-DE")
        val resultado = GeneroLocalizationHelper.formatarGeneros("Drama, Action & Adventure, Mystery", "de-DE")
        assertEquals("Drama, Action & Abenteuer, Mystery", resultado)
    }

    @Test
    fun testTraducaoGenerosFr() {
        I18n.definirIdioma("fr-FR")
        val resultado = GeneroLocalizationHelper.formatarGeneros("Drama, Action & Adventure, Mystery", "fr-FR")
        assertEquals("Drame, Action & Aventure, Mystère", resultado)
    }

    @Test
    fun testTraducaoGenerosJa() {
        I18n.definirIdioma("ja-JP")
        val resultado = GeneroLocalizationHelper.formatarGeneros("Action & Adventure, Animation", "ja-JP")
        assertEquals("アクション＆アドベンチャー, アニメーション", resultado)
    }

    @Test
    fun testNotificacaoAtualizacaoTituloEn() {
        I18n.definirIdioma("en-US")
        val notif = NotificacaoEntity(
            id = 1,
            tipo = "ATUALIZACAO",
            titulo = "Nova Versão v2.0.60 Disponível",
            mensagem = "Novidades da versão 2.0.60:\nMelhorias gerais e correções de desempenho."
        )
        val tituloFormatado = NotificacaoLocalizationHelper.obterTituloFormatado(notif)
        val mensagemFormatada = NotificacaoLocalizationHelper.obterMensagemFormatada(notif)
        assertEquals("New Version v2.0.60 Available", tituloFormatado)
        assertTrue(mensagemFormatada.contains("What's new in version 2.0.60:"))
        assertTrue(mensagemFormatada.contains("General improvements and performance fixes."))
    }

    @Test
    fun testNotificacaoAtualizacaoTituloEs() {
        I18n.definirIdioma("es-ES")
        val notif = NotificacaoEntity(
            id = 1,
            tipo = "ATUALIZACAO",
            titulo = "Nova Versão v2.0.60 Disponível",
            mensagem = "Novidades da versão 2.0.60:\nMelhorias gerais e correções de desempenho."
        )
        val tituloFormatado = NotificacaoLocalizationHelper.obterTituloFormatado(notif)
        val mensagemFormatada = NotificacaoLocalizationHelper.obterMensagemFormatada(notif)
        assertEquals("Nueva Versión v2.0.60 Disponible", tituloFormatado)
        assertTrue(mensagemFormatada.contains("Novedades de la versión 2.0.60:"))
        assertTrue(mensagemFormatada.contains("Mejoras generales y correcciones de rendimiento."))
    }
}
