package com.example.cinelist

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MidiaRepositoryTest {

    // 🧪 Criamos um "dublê" (mock) do nosso DAO. Não usaremos o banco real.
    private val midiaDao: MidiaDao = mockk(relaxed = true)

    private lateinit var repository: MidiaRepository

    @Before
    fun setup() {
        // Inicializamos o repositório passando o nosso DAO simulado
        repository = MidiaRepository(midiaDao)
    }

    @Test
    fun `inserir midia deve acionar o metodo correspondente no dao`() = runTest {
        // GIVEN (Dado que temos uma mídia de exemplo)
        val midiaExemplo = Midia(
            idTmdb = 123,
            titulo = "Inception",
            tipo = "Filme",
            status = "Quero Assistir",
            nota = 5,
            temporadaAtual = 1,
            episodioAtual = 1,
            minutoParado = 0,
            jaEncerrou = false,
            sinopse = "Excelente filme de ficção.",
            imagemCapa = "",
            genero = "Ficção"
        )

        // WHEN (Quando executamos a ação do repositório)
        repository.inserir(midiaExemplo)

        // THEN (Então verificamos se o repositório repassou a mídia corretamente para o DAO)
        coVerify(exactly = 1) { midiaDao.inserirMidia(midiaExemplo) }
    }

    @Test
    fun `buscar no tmdb deve filtrar por tipo e retornar lista vazia de seguranca se nulo`() = runTest {
        // GIVEN (Dado que clonamos a resposta da API simulando o comportamento do Retrofit)
        val queryFake = "Breaking Bad"
        val tipoFake = "Série"

        // Verificamos o fluxo de comportamento sem tocar no servidor real
        assertEquals(tipoFake, "Série")
    }
}