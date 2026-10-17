package com.example.cinelist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel responsável pelo modo Match (swipe de mídias em grupo).
 */
@HiltViewModel
class MatchViewModel @Inject constructor(
    private val repository: MidiaRepository
) : ViewModel() {

    private val _casalIdAtivo = MutableStateFlow("")

    val popularesTmdbMatch: StateFlow<List<Midia>> = MutableStateFlow(emptyList())

    private val _popularesTmdbMatch = MutableStateFlow<List<Midia>>(emptyList())
    val popularesTmdbMatchFlow: StateFlow<List<Midia>> = _popularesTmdbMatch.asStateFlow()

    private var paginaTmdbAtual = 1
    private var carregandoPopulares = false

    val matchesDoGrupo: Flow<List<MatchMidia>> = _casalIdAtivo.flatMapLatest { grupoId ->
        if (grupoId.isBlank()) {
            kotlinx.coroutines.flow.flowOf(emptyList())
        } else {
            repository.observarMatchesDoGrupo(grupoId)
        }
    }

    fun carregarMaisPopularesMatch() {
        if (carregandoPopulares) return
        carregandoPopulares = true
        viewModelScope.launch {
            val novos = repository.buscarPopularesParaMatch(paginaTmdbAtual)
            if (novos.isNotEmpty()) {
                _popularesTmdbMatch.value = _popularesTmdbMatch.value + novos
                paginaTmdbAtual++
            }
            carregandoPopulares = false
        }
    }

    fun votarMatch(midia: Midia, curtiu: Boolean) {
        val grupo = _casalIdAtivo.value
        if (grupo.isNotBlank()) {
            viewModelScope.launch {
                repository.votarNoMatch(grupo, midia, curtiu)
            }
        }
    }

    fun salvarMidiaMatchNaSala(match: MatchMidia) {
        val grupo = _casalIdAtivo.value
        if (grupo.isNotBlank()) {
            viewModelScope.launch {
                val existentes = repository.buscarMidiasPorGrupo(grupo).let { flow ->
                    var lista: List<Midia> = emptyList()
                    flow.collect { lista = it; return@collect }
                    lista
                }
                val jaExiste = existentes.any {
                    (it.idTmdb != 0 && it.idTmdb == match.idTmdb) ||
                            it.titulo.trim().equals(match.titulo.trim(), ignoreCase = true)
                }

                if (!jaExiste) {
                    val novaMidia = Midia(
                        idTmdb = match.idTmdb,
                        titulo = match.titulo,
                        tipo = match.tipo,
                        status = "Quero Assistir",
                        nota = 0,
                        temporadaAtual = 1,
                        episodioAtual = 1,
                        minutoParado = 0,
                        jaEncerrou = false,
                        sinopse = match.sinopse,
                        imagemCapa = match.imagemCapa,
                        genero = match.genero,
                        plataforma = if (match.tipo.equals("Filme", ignoreCase = true)) "Cinema" else "TV / Original",
                        favorito = false,
                        listaCustomizada = "Geral",
                        isCasal = true,
                        casalId = grupo,
                        adicionadoPor = "Modo Match",
                        statusSugestao = "APROVADO"
                    )
                    repository.inserir(novaMidia)
                }
            }
        }
    }

    fun resetarRodadaMatch() {
        val grupo = _casalIdAtivo.value
        if (grupo.isNotBlank()) {
            viewModelScope.launch {
                repository.reiniciarRodadaMatch(grupo)
            }
        }
        paginaTmdbAtual = 1
        _popularesTmdbMatch.value = emptyList()
        carregarMaisPopularesMatch()
    }

    fun setCasalIdAtivo(grupoId: String) {
        _casalIdAtivo.value = grupoId
    }
}
