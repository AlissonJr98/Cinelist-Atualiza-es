package com.example.cinelist

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Testes da lógica de "onde parei" / prioridade na lista.
 *
 * Estes testes cobrem apenas lógica pura (sem Android), por isso correm
 * rapidamente sem emulador.
 */
class MidiaPrioridadeTest {

    private fun serie(ep: Int = 1, temporada: Int = 1) = Midia(
        titulo = "Série Teste",
        tipo = "Série",
        temporadaAtual = temporada,
        episodioAtual = ep
    )

    // ===== ehSerieOuAnime =====

    @Test
    fun `series e anines sao considerados conteudo seriado`() {
        assertTrue(Midia(tipo = "Série").ehSerieOuAnime)
        assertTrue(Midia(tipo = "Anime").ehSerieOuAnime)
        assertTrue(Midia(tipo = "Novela").ehSerieOuAnime)
        assertTrue(Midia(tipo = "Dorama").ehSerieOuAnime)
    }

    @Test
    fun `filmes nao sao conteudo seriado`() {
        assertFalse(Midia(tipo = "Filme").ehSerieOuAnime)
    }

    // ===== temProgressoEpisodio =====

    @Test
    fun `serie no primeiro episodio nao tem progresso`() {
        assertFalse(serie(ep = 1, temporada = 1).temProgressoEpisodio)
    }

    @Test
    fun `serie com episodio avancado tem progresso`() {
        assertTrue(serie(ep = 7, temporada = 1).temProgressoEpisodio)
    }

    @Test
    fun `serie com temporada avancada tem progresso mesmo no episodio um`() {
        assertTrue(serie(ep = 1, temporada = 2).temProgressoEpisodio)
    }

    @Test
    fun `filme nunca tem progresso de episodio`() {
        val filme = Midia(tipo = "Filme", episodioAtual = 9, temporadaAtual = 3)
        assertFalse(filme.temProgressoEpisodio)
    }

    // ===== pesoPrioridade =====

    @Test
    fun `status assistindo tem prioridade maxima`() {
        val m = serie(ep = 1).copy(status = StatusMidia.ASSISTINDO.valor)
        assertEquals(3, m.pesoPrioridade)
    }

    @Test
    fun `serie com progresso fica acima de conteudo sem progresso`() {
        val comProgresso = serie(ep = 5)
        val semProgresso = serie(ep = 1)
        assertTrue(
            "O conteúdo com progresso deve aparecer primeiro",
            comProgresso.pesoPrioridade > semProgresso.pesoPrioridade
        )
    }

    @Test
    fun `concluido tem prioridade mais baixa que serie em progresso`() {
        val concluida = serie(ep = 10).copy(status = StatusMidia.CONCLUIDO.valor)
        val emProgresso = serie(ep = 10)
        assertTrue(
            "Concluído deve descer relativamente a quem está a ver",
            emProgresso.pesoPrioridade > concluida.pesoPrioridade
        )
    }

    @Test
    fun `ordenar por prioridade coloca o mais recente primeiro dentro do mesmo nivel`() {
        val antigo = serie(ep = 3).copy(atualizadoEm = 1_000L)
        val recente = serie(ep = 4).copy(atualizadoEm = 9_999L)

        val ordenado = listOf(antigo, recente)
            .sortedWith(
                compareByDescending<Midia> { it.pesoPrioridade }
                    .thenByDescending { it.atualizadoEm }
            )

        assertEquals("A série alterada mais recentemente deve ir primeiro", "T1 • Ep 4", "T1 • Ep ${ordenado.first().episodioAtual}")
    }

    @Test
    fun `conteudo assistindo precede serie com progresso`() {
        val assistindo = serie(ep = 2).copy(status = StatusMidia.ASSISTINDO.valor)
        val emProgresso = serie(ep = 9)

        val ordenado = listOf(emProgresso, assistindo)
            .sortedWith(compareByDescending<Midia> { it.pesoPrioridade })

        assertEquals(3, ordenado.first().pesoPrioridade)
    }
}
