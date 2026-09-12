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
    val socialRepository: SocialRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val todasAsMidias: Flow<List<Midia>> = repository.todasAsMidias
    val midiasPessoais: Flow<List<Midia>> = repository.midiasPessoais
    val gruposSalvos: Flow<List<GrupoEntity>> = repository.gruposSalvos

    // Estado do ID do grupo/sala ativo no ViewModel
    private val _casalIdAtivo = MutableStateFlow("")
    val casalIdAtivo: StateFlow<String> = _casalIdAtivo.asStateFlow()

    // Fluxo de mídias dinâmico baseado no grupo ativo selecionado
    @OptIn(ExperimentalCoroutinesApi::class)
    val midiasGrupoAtivo: Flow<List<Midia>> = _casalIdAtivo.flatMapLatest { grupoId ->
        if (grupoId.isBlank()) {
            repository.midiasPessoais
        } else {
            repository.buscarMidiasPorGrupo(grupoId)
        }
    }

    init {
        carregarGrupoAtivoInicial()
    }

    private fun carregarGrupoAtivoInicial() {
        viewModelScope.launch {
            val grupoAtivo = repository.obterGrupoAtivoLocal()
            if (grupoAtivo != null) {
                _casalIdAtivo.value = grupoAtivo.grupoId
                observarGrupoFirestore(grupoAtivo.grupoId)
            }
        }
    }

    fun selecionarGrupoAtivo(grupoId: String) {
        viewModelScope.launch {
            repository.ativarGrupoLocal(grupoId)
            _casalIdAtivo.value = grupoId
            if (grupoId.isNotBlank()) {
                observarGrupoFirestore(grupoId)
            }
        }
    }

    fun criarOuEntrarNoGrupo(grupoId: String, nomeGrupo: String, tipo: String) {
        viewModelScope.launch {
            repository.salvarOuEntrarNoGrupo(grupoId, nomeGrupo, tipo)
            _casalIdAtivo.value = grupoId
            observarGrupoFirestore(grupoId)
        }
    }

    fun excluirGrupoSalvo(grupo: GrupoEntity) {
        viewModelScope.launch {
            if (_casalIdAtivo.value == grupo.grupoId) {
                selecionarGrupoAtivo("") // Volta para o perfil pessoal se apagar o ativo
            }
            repository.deletarGrupoLocal(grupo)
        }
    }

    private fun observarGrupoFirestore(grupoId: String) {
        viewModelScope.launch {
            repository.observarMidiasDoGrupoFirestore(grupoId).collect {
                // Sincronizado automaticamente via repository
            }
        }
    }

    val todasNotificacoes: Flow<List<NotificacaoEntity>> = notificacaoRepository.todasNotificacoes
    val quantidadeNaoLidas: Flow<Int> = notificacaoRepository.quantidadeNaoLidas

    // --- MÓDULO SOCIAL / AMIGOS ---
    val amigosConectados: Flow<List<AmigoPerfil>> = socialRepository.observarAmigos()

    private val _resultadosBuscaAmigos = MutableStateFlow<List<AmigoPerfil>>(emptyList())
    val resultadosBuscaAmigos: StateFlow<List<AmigoPerfil>> = _resultadosBuscaAmigos

    private val _listaAmigoSelecionado = MutableStateFlow<List<Midia>>(emptyList())
    val listaAmigoSelecionado: StateFlow<List<Midia>> = _listaAmigoSelecionado

    fun atualizarMeuPerfilPublico(nome: String, bio: String) {
        viewModelScope.launch {
            socialRepository.atualizarPerfilPublico(nome, bio)
        }
    }

    fun pesquisarUsuarios(termo: String) {
        viewModelScope.launch {
            _resultadosBuscaAmigos.value = socialRepository.buscarUsuarios(termo)
        }
    }

    fun adicionarAmigo(amigoUid: String, onResultado: (Boolean) -> Unit) {
        viewModelScope.launch {
            val sucesso = socialRepository.adicionarAmigo(amigoUid)
            onResultado(sucesso)
        }
    }

    fun carregarListaDoAmigo(amigoUid: String) {
        viewModelScope.launch {
            _listaAmigoSelecionado.value = socialRepository.buscarListaAmigo(amigoUid)
        }
    }

    fun marcarNotificacaoComoLida(id: Int) {
        viewModelScope.launch { notificacaoRepository.marcarComoLida(id) }
    }

    fun marcarTodasNotificacoesComoLidas() {
        viewModelScope.launch { notificacaoRepository.marcarTodasComoLidas() }
    }

    fun deletarNotificacao(notificacao: NotificacaoEntity) {
        viewModelScope.launch { notificacaoRepository.deletar(notificacao) }
    }

    fun limparTodasNotificacoes() {
        viewModelScope.launch { notificacaoRepository.limparTodas() }
    }

    private val _updatePendente = MutableStateFlow<InfoAtualizacao?>(null)
    val updatePendente: StateFlow<InfoAtualizacao?> = _updatePendente.asStateFlow()

    init {
        verificarAtualizacaoSilenciosa()
        iniciarSincronizacaoSilenciosaNuvem()
    }

    fun iniciarSincronizacaoSilenciosaNuvem() {
        viewModelScope.launch {
            repository.sincronizacaoAutomaticaSilenciosa()
        }
    }

    fun verificarAtualizacaoSilenciosa() {
        viewModelScope.launch {
            val update = UpdateManager.checarAtualizacaoSilenciosa()
            if (update != null) {
                _updatePendente.value = update
                UpdateManager.exibirNotificacaoAtualizacao(context, update)
                salvarNotificacaoInterna(update)
            }
        }
    }

    private suspend fun salvarNotificacaoInterna(update: InfoAtualizacao) {
        val jaRegistrada = notificacaoRepository.contarNotificacaoRecente(
            idRef = update.versaoCode,
            tipo = "ATUALIZACAO",
            desde = 0L
        ) > 0

        if (!jaRegistrada) {
            val corpo = if (update.notasDaVersao.isNotBlank()) {
                "Novidades da versão ${update.versaoNome}:\n${update.notasDaVersao}"
            } else {
                "Uma nova versão (${update.versaoNome}) com melhorias e correções está disponível para instalação."
            }

            val novaNotificacao = NotificacaoEntity(
                tipo = "ATUALIZACAO",
                titulo = "Nova Versão v${update.versaoNome} Disponível",
                mensagem = corpo,
                dataCriacao = System.currentTimeMillis(),
                lida = false,
                idReferencia = update.versaoCode
            )

            notificacaoRepository.inserir(novaNotificacao)
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

    private val _tipoPaginado = MutableStateFlow("Todos")
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
        _tipoPaginado.value = "Todos"
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

    fun alternarFavorito(midia: Midia) {
        viewModelScope.launch {
            val midiaAtualizada = midia.copy(favorito = !midia.favorito)
            repository.atualizar(midiaAtualizada)
        }
    }

    fun moverParaListaCustomizada(midia: Midia, novaLista: String) {
        viewModelScope.launch {
            val midiaAtualizada = midia.copy(listaCustomizada = novaLista)
            repository.atualizar(midiaAtualizada)
        }
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

    private val _episodiosTemporada = MutableStateFlow<List<TmdbEpisodioItem>>(emptyList())
    val episodiosTemporada: StateFlow<List<TmdbEpisodioItem>> = _episodiosTemporada

    private val _carregandoEpisodios = MutableStateFlow(false)
    val carregandoEpisodios: StateFlow<Boolean> = _carregandoEpisodios

    private val _galeriaImagens = MutableStateFlow<List<TmdbImagemItem>>(emptyList())
    val galeriaImagens: StateFlow<List<TmdbImagemItem>> = _galeriaImagens

    private fun verificarSeEhSerie(tipo: String): Boolean {
        return tipo.equals("Série", ignoreCase = true) ||
                tipo.equals("Anime", ignoreCase = true) ||
                tipo.equals("Novela", ignoreCase = true) ||
                tipo.equals("Dorama", ignoreCase = true) ||
                tipo.equals("tv", ignoreCase = true)
    }

    fun buscarEpisodiosTemporada(idTmdb: Int, numeroTemporada: Int) {
        if (idTmdb == 0) return
        viewModelScope.launch {
            _carregandoEpisodios.value = true
            _episodiosTemporada.value = emptyList()
            try {
                val resultado = RetrofitClient.apiService.obterEpisodiosTemporada(idTmdb, numeroTemporada)
                _episodiosTemporada.value = resultado.episodios
            } catch (e: Exception) {
                e.printStackTrace()
                _episodiosTemporada.value = emptyList()
            } finally {
                _carregandoEpisodios.value = false
            }
        }
    }

    fun buscarDetalhesEstendidos(idTmdb: Int, tipo: String) {
        if (idTmdb == 0) return

        viewModelScope.launch {
            limparDetalhesEstendidos()
            try {
                val ehSerieOuAnime = verificarSeEhSerie(tipo)

                var detalhes: TmdbDetalhesEstendidos? = null
                var creditos: TmdbCreditosResposta? = null
                var videosResposta: TmdbVideosResposta? = null
                var recomendacoesResposta: TmdbRecomendacoesResposta? = null
                var imagensResposta: TmdbImagensResposta? = null
                var ehRealmenteSerie = ehSerieOuAnime

                try {
                    if (ehSerieOuAnime) {
                        detalhes = RetrofitClient.apiService.obterDetalhesSerieOuAnime(idSerie = idTmdb)
                        creditos = RetrofitClient.apiService.obterCreditosSerieOuAnime(idSerie = idTmdb)
                        videosResposta = RetrofitClient.apiService.obterVideosSerieOuAnime(idSerie = idTmdb)
                        recomendacoesResposta = RetrofitClient.apiService.obterRecomendacoesSerieOuAnime(idSerie = idTmdb)
                        imagensResposta = RetrofitClient.apiService.obterImagensSerieOuAnime(idSerie = idTmdb)
                    } else {
                        detalhes = RetrofitClient.apiService.obterDetalhesFilme(idFilme = idTmdb)
                        creditos = RetrofitClient.apiService.obterCreditosFilme(idFilme = idTmdb)
                        videosResposta = RetrofitClient.apiService.obterVideosFilme(idFilme = idTmdb)
                        recomendacoesResposta = RetrofitClient.apiService.obterRecomendacoesFilme(idFilme = idTmdb)
                        imagensResposta = RetrofitClient.apiService.obterImagensFilme(idFilme = idTmdb)
                    }
                } catch (e: Exception) {
                    try {
                        if (!ehSerieOuAnime) {
                            detalhes = RetrofitClient.apiService.obterDetalhesSerieOuAnime(idSerie = idTmdb)
                            creditos = RetrofitClient.apiService.obterCreditosSerieOuAnime(idSerie = idTmdb)
                            videosResposta = RetrofitClient.apiService.obterVideosSerieOuAnime(idSerie = idTmdb)
                            recomendacoesResposta = RetrofitClient.apiService.obterRecomendacoesSerieOuAnime(idSerie = idTmdb)
                            imagensResposta = RetrofitClient.apiService.obterImagensSerieOuAnime(idSerie = idTmdb)
                            ehRealmenteSerie = true
                        } else {
                            detalhes = RetrofitClient.apiService.obterDetalhesFilme(idFilme = idTmdb)
                            creditos = RetrofitClient.apiService.obterCreditosFilme(idFilme = idTmdb)
                            videosResposta = RetrofitClient.apiService.obterVideosFilme(idFilme = idTmdb)
                            recomendacoesResposta = RetrofitClient.apiService.obterRecomendacoesFilme(idFilme = idTmdb)
                            imagensResposta = RetrofitClient.apiService.obterImagensFilme(idFilme = idTmdb)
                            ehRealmenteSerie = false
                        }
                    } catch (e2: Exception) {
                        e2.printStackTrace()
                    }
                }

                _detalhesEstendidosApi.value = detalhes
                _elencoMidia.value = creditos?.elenco ?: emptyList()

                val trailer = videosResposta?.videos?.firstOrNull {
                    it.sitePlataforma.equals("YouTube", ignoreCase = true) &&
                            (it.tipoVideo.equals("Trailer", ignoreCase = true) || it.tipoVideo.equals("Teaser", ignoreCase = true))
                }
                _chaveTrailerYoutube.value = trailer?.chaveYoutube
                _recomendacoesMidia.value = recomendacoesResposta?.recomendacoes ?: emptyList()

                val todasImagens = (imagensResposta?.backdrops ?: emptyList()) + (imagensResposta?.posters ?: emptyList())
                _galeriaImagens.value = todasImagens.distinctBy { it.caminhoArquivo }

                buscarOndeAssistir(idTmdb, if (ehRealmenteSerie) "Série" else "Filme")

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun buscarOndeAssistir(idTmdb: Int, tipo: String) {
        if (idTmdb == 0) return
        viewModelScope.launch {
            try {
                val ehSerieOuAnime = verificarSeEhSerie(tipo)
                val resposta = if (ehSerieOuAnime) {
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
        _episodiosTemporada.value = emptyList()
        _galeriaImagens.value = emptyList()
    }

    fun importarMidiasEmLote(novasMidias: List<Midia>) {
        viewModelScope.launch {
            novasMidias.forEach { midia ->
                repository.inserir(midia.copy(id = 0))
            }
        }
    }

    fun incrementarEpisodioRapido(midia: Midia) {
        viewModelScope.launch {
            repository.incrementarEpisodio(midia.id)
        }
    }

    fun definirProgressoEpisodio(idMidia: Int, temporada: Int, episodio: Int) {
        viewModelScope.launch {
            repository.atualizarProgressoEpisodio(idMidia, temporada, episodio)
        }
    }

    fun sincronizarNuvemManual(onResultado: (Int) -> Unit) {
        viewModelScope.launch {
            val totalRestaurado = repository.restaurarDoFirestore()
            onResultado(totalRestaurado)
        }
    }

    fun fazerBackupCompletoNuvem(onResultado: (Boolean) -> Unit) {
        viewModelScope.launch {
            val sucesso = repository.backupCompletoParaFirestore()
            onResultado(sucesso)
        }
    }

    fun limparTodaALista() {
        viewModelScope.launch {
            repository.limparTudoCompleto()
        }
    }
}