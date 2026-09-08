package com.example.cinelist

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MidiaViewModel @Inject constructor(
    private val repository: MidiaRepository,
    private val notificacaoRepository: NotificacaoRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val todasAsMidias: Flow<List<Midia>> = repository.todasAsMidias

    // ESTADO E CONTROLE DE NOTIFICAÇÕES
    val todasNotificacoes: Flow<List<NotificacaoEntity>> = notificacaoRepository.todasNotificacoes
    val quantidadeNaoLidas: Flow<Int> = notificacaoRepository.quantidadeNaoLidas

    fun marcarNotificacaoComoLida(id: Int) {
        viewModelScope.launch { notificacaoRepository.marcarComoLida(id) }
    }

    fun marcarTodasNotificacoesComoLidas() {
        viewModelScope.launch { notificacaoRepository.marcarTodasComoLidas() }
    }

    fun deletarNotificacao(notificacao: NotificacaoEntity) {
        viewModelScope.launch { notificacaoRepository.deletar(notificacao) }
    }

    // ESTADO E CONTROLE DE ATUALIZAÇÃO SILENCIOSA OTA
    private val _updatePendente = MutableStateFlow<InfoAtualizacao?>(null)
    val updatePendente: StateFlow<InfoAtualizacao?> = _updatePendente.asStateFlow()

    init {
        verificarAtualizacaoSilenciosa()
    }

    fun verificarAtualizacaoSilenciosa() {
        viewModelScope.launch {
            val update = UpdateManager.checarAtualizacaoSilenciosa()
            if (update != null) {
                _updatePendente.value = update

                // Dispara o alerta com as novidades direto na barra de notificações do Android
                UpdateManager.exibirNotificacaoAtualizacao(context, update)

                // Salva no banco de dados da aba interna de notificações (apenas 1x por versão)
                salvarNotificacaoInternaSeNecessario(update)
            }
        }
    }

    private suspend fun salvarNotificacaoInternaSeNecessario(update: InfoAtualizacao) {
        val prefs = context.getSharedPreferences("ControleAtualizacoes", Context.MODE_PRIVATE)
        val chaveRegistrada = "notificacao_registrada_${update.versaoCode}"

        if (!prefs.getBoolean(chaveRegistrada, false)) {
            val corpo = if (update.notasDaVersao.isNotBlank()) {
                "Novidades da v${update.versaoNome}:\n${update.notasDaVersao}"
            } else {
                "Uma nova versão (v${update.versaoNome}) está pronta para download com melhorias gerais."
            }

            val novaNotificacao = NotificacaoEntity(
                titulo = "Nova Versão v${update.versaoNome} Disponível",
                mensagem = corpo,
                dataCriacao = System.currentTimeMillis(),
                lida = false,
                tipo = "ATUALIZACAO",
                idReferencia = update.versaoCode
            )

            notificacaoRepository.inserir(novaNotificacao)
            prefs.edit().putBoolean(chaveRegistrada, true).apply()
        }
    }

    fun dispensarUpdate() {
        _updatePendente.value = null
    }

    private val _resultadosBuscaApi = MutableStateFlow<List<TmdbFilme>>(emptyList())
    val resultadosBuscaApi: StateFlow<List<TmdbFilme>> = _resultadosBuscaApi

    private val _carregandoApi = MutableStateFlow(false)
    val carregandoApi: StateFlow<Boolean> = _carregandoApi

    private val _queryPaginada = MutableStateFlow("")
    val queryPaginada: StateFlow<String> = _queryPaginada

    private val _tipoPaginado = MutableStateFlow("Filme")
    val tipoPaginado: StateFlow<String> = _tipoPaginado

    private val _provedorSelecionadoId = MutableStateFlow<Int?>(null)
    val provedorSelecionadoId: StateFlow<Int?> = _provedorSelecionadoId

    private val _generoSelecionadoId = MutableStateFlow<Int?>(null)
    val generoSelecionadoId: StateFlow<Int?> = _generoSelecionadoId

    private val _ordenacaoSelecionada = MutableStateFlow("popularity.desc")
    val ordenacaoSelecionada: StateFlow<String> = _ordenacaoSelecionada

    @OptIn(ExperimentalCoroutinesApi::class)
    val resultadosBuscaPaginadaApi: Flow<PagingData<TmdbFilme>> = combine(
        _queryPaginada,
        _tipoPaginado,
        _provedorSelecionadoId,
        _generoSelecionadoId,
        _ordenacaoSelecionada
    ) { query, tipo, provedor, genero, ordenacao ->
        repository.buscarNoTmdbPaginado(
            query = query,
            tipo = tipo,
            provedorId = provedor,
            generoId = genero,
            sortBy = ordenacao
        )
    }.flatMapLatest { flow -> flow }.cachedIn(viewModelScope)

    fun atualizarQueryEFiltrarPaginado(novaQuery: String, novoTipo: String) {
        _queryPaginada.value = novaQuery
        _tipoPaginado.value = novoTipo
    }

    fun selecionarProvedorStreaming(provedorId: Int?) {
        _provedorSelecionadoId.value = if (_provedorSelecionadoId.value == provedorId) null else provedorId
    }

    fun selecionarGenero(generoId: Int?) {
        _generoSelecionadoId.value = if (_generoSelecionadoId.value == generoId) null else generoId
    }

    fun selecionarTipo(tipo: String) {
        _tipoPaginado.value = tipo
    }

    fun selecionarOrdenacao(novaOrdenacao: String) {
        _ordenacaoSelecionada.value = novaOrdenacao
    }

    fun limparBuscaApi() {
        _resultadosBuscaApi.value = emptyList()
        _queryPaginada.value = ""
        _provedorSelecionadoId.value = null
        _generoSelecionadoId.value = null
        _ordenacaoSelecionada.value = "popularity.desc"
    }

    fun inserir(midia: Midia) {
        viewModelScope.launch { repository.inserir(midia) }
    }

    fun atualizar(midia: Midia) {
        viewModelScope.launch { repository.atualizar(midia) }
    }

    fun deletar(midia: Midia) {
        viewModelScope.launch { repository.deletar(midia) }
    }

    fun buscarFilmeNoTmdb(nome: String, tipo: String) {
        if (nome.isBlank()) {
            limparBuscaApi()
            return
        }
        viewModelScope.launch {
            _carregandoApi.value = true
            try {
                _resultadosBuscaApi.value = repository.buscarNoTmdb(nome, tipo)
            } catch (e: Exception) {
                e.printStackTrace()
                limparBuscaApi()
            } finally {
                _carregandoApi.value = false
            }
        }
    }

    private val _detalhesEstendidosApi = MutableStateFlow<TmdbDetalhesEstendidos?>(null)
    val detalhesEstendidosApi: StateFlow<TmdbDetalhesEstendidos?> = _detalhesEstendidosApi

    private val _provedoresStreaming = MutableStateFlow<List<ItemProvedor>>(emptyList())
    val provedoresStreaming: StateFlow<List<ItemProvedor>> = _provedoresStreaming

    private val _elencoMidia = MutableStateFlow<List<TmdbAtor>>(emptyList())
    val elencoMidia: StateFlow<List<TmdbAtor>> = _elencoMidia

    private val _chaveTrailerYoutube = MutableStateFlow<String?>(null)
    val chaveTrailerYoutube: StateFlow<String?> = _chaveTrailerYoutube

    private val _recomendacoesMidia = MutableStateFlow<List<TmdbFilme>>(emptyList())
    val recomendacoesMidia: StateFlow<List<TmdbFilme>> = _recomendacoesMidia

    fun buscarDetalhesEstendidos(idTmdb: Int, tipo: String) {
        if (idTmdb == 0) return

        viewModelScope.launch {
            try {
                val ehSerieOuAnime = tipo.equals("Série", ignoreCase = true) || tipo.equals("Anime", ignoreCase = true)

                val detalhes = if (ehSerieOuAnime) {
                    RetrofitClient.apiService.obterDetalhesSerieOuAnime(idSerie = idTmdb)
                } else {
                    RetrofitClient.apiService.obterDetalhesFilme(idFilme = idTmdb)
                }
                _detalhesEstendidosApi.value = detalhes

                val creditos = if (ehSerieOuAnime) {
                    RetrofitClient.apiService.obterCreditosSerieOuAnime(idSerie = idTmdb)
                } else {
                    RetrofitClient.apiService.obterCreditosFilme(idFilme = idTmdb)
                }
                _elencoMidia.value = creditos.elenco ?: emptyList()

                val videosResposta = if (ehSerieOuAnime) {
                    RetrofitClient.apiService.obterVideosSerieOuAnime(idSerie = idTmdb)
                } else {
                    RetrofitClient.apiService.obterVideosFilme(idFilme = idTmdb)
                }
                val trailer = videosResposta.videos?.firstOrNull {
                    it.sitePlataforma.equals("YouTube", ignoreCase = true) &&
                            (it.tipoVideo.equals("Trailer", ignoreCase = true) || it.tipoVideo.equals("Teaser", ignoreCase = true))
                }
                _chaveTrailerYoutube.value = trailer?.chaveYoutube

                val recomendacoesResposta = if (ehSerieOuAnime) {
                    RetrofitClient.apiService.obterRecomendacoesSerieOuAnime(idSerie = idTmdb)
                } else {
                    RetrofitClient.apiService.obterRecomendacoesFilme(idFilme = idTmdb)
                }
                _recomendacoesMidia.value = recomendacoesResposta.recomendacoes ?: emptyList()

            } catch (e: Exception) {
                e.printStackTrace()
                _detalhesEstendidosApi.value = null
                _elencoMidia.value = emptyList()
                _chaveTrailerYoutube.value = null
                _recomendacoesMidia.value = emptyList()
            }
        }
    }

    fun buscarOndeAssistir(idTmdb: Int, tipo: String) {
        if (idTmdb == 0) return
        viewModelScope.launch {
            try {
                val resposta = if (tipo.equals("Série", ignoreCase = true) || tipo.equals("Anime", ignoreCase = true)) {
                    RetrofitClient.apiService.obterProvedoresSerieOuAnime(idSerie = idTmdb)
                } else {
                    RetrofitClient.apiService.obterProvedoresFilme(idFilme = idTmdb)
                }
                val providersBr = resposta.resultados?.get("BR")?.streamingAssinatura ?: emptyList()
                _provedoresStreaming.value = providersBr
            } catch (e: Exception) {
                e.printStackTrace()
                _provedoresStreaming.value = emptyList()
            }
        }
    }

    fun limparDetalhesEstendidos() {
        _detalhesEstendidosApi.value = null
        _provedoresStreaming.value = emptyList()
        _elencoMidia.value = emptyList()
        _chaveTrailerYoutube.value = null
        _recomendacoesMidia.value = emptyList()
    }

    fun importarMidiasEmLote(novasMidias: List<Midia>) {
        viewModelScope.launch {
            novasMidias.forEach { midia ->
                repository.inserir(midia.copy(id = 0))
            }
        }
    }
}