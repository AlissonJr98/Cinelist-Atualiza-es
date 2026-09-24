package com.example.cinelist

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CineWrappedData(
    val totalTitulosConcluidos: Int,
    val horasTotaisAssistidas: Int,
    val generoFavorito: String,
    val plataformaMaisUtilizada: String,
    val totalEpisodiosMaratonados: Int,
    val mediaNotasAtribuidas: String,
    val maiorNotaDada: Int,
    val filmeOuSerieDestaque: String,
    val frasePersonalizada: String
)

@HiltViewModel
class MidiaViewModel @Inject constructor(
    private val repository: MidiaRepository,
    private val notificacaoRepository: NotificacaoRepository,
    val socialRepository: SocialRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()

    val todasAsMidias: Flow<List<Midia>> = repository.todasAsMidias
    val midiasPessoais: Flow<List<Midia>> = repository.midiasPessoais
    val gruposSalvos: Flow<List<GrupoEntity>> = repository.gruposSalvos

    private val _casalIdAtivo = MutableStateFlow("")
    val casalIdAtivo: StateFlow<String> = _casalIdAtivo.asStateFlow()

    private val _isAdministradorSala = MutableStateFlow(false)
    val isAdministradorSala: StateFlow<Boolean> = _isAdministradorSala.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val midiasGrupoAtivo: Flow<List<Midia>> = _casalIdAtivo.flatMapLatest { grupoId ->
        if (grupoId.isBlank()) {
            repository.midiasPessoais
        } else {
            repository.buscarMidiasPorGrupo(grupoId)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val membrosGrupoAtivo: Flow<List<MembroGrupo>> = _casalIdAtivo.flatMapLatest { grupoId ->
        if (grupoId.isBlank()) {
            kotlinx.coroutines.flow.flowOf(emptyList())
        } else {
            repository.observarMembrosDoGrupo(grupoId).map { lista ->
                lista.sortedWith(
                    compareByDescending<MembroGrupo> { it.estaRealmenteOnline }
                        .thenByDescending { it.vistoPorUltimo }
                        .thenBy { it.nome.lowercase() }
                )
            }
        }
    }

    private val _filmesEmCartaz = MutableStateFlow<List<TmdbFilme>>(emptyList())
    val filmesEmCartaz: StateFlow<List<TmdbFilme>> = _filmesEmCartaz

    init {
        carregarGrupoAtivoInicial()
        verificarAtualizacaoSilenciosa()
        iniciarSincronizacaoSilenciosaNuvem()
        carregarMaisPopularesMatch()
        carregarFilmesEmCartaz()
        monitorarExpulsaoDaSala()
    }

    fun carregarFilmesEmCartaz() {
        viewModelScope.launch {
            try {
                val resposta = RetrofitClient.apiService.obterFilmesEmCartaz()
                _filmesEmCartaz.value = resposta.resultados
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun iniciarAssistirMidiaAgora(midia: Midia) {
        val grupo = _casalIdAtivo.value
        if (grupo.isNotBlank()) {
            viewModelScope.launch {
                val epTexto = if (midia.tipo.equals("Filme", ignoreCase = true)) "" else "T${midia.temporadaAtual} • Ep ${midia.episodioAtual}"
                repository.definirAssistindoAgora(grupo, midia.titulo, epTexto)
            }
        }
    }

    fun pararAssistirAgora() {
        val grupo = _casalIdAtivo.value
        if (grupo.isNotBlank()) {
            viewModelScope.launch {
                repository.limparAssistindoAgora(grupo)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        pararAssistirAgora()
    }

    fun avaliarMidiaNaSala(midia: Midia, nota: Int, comentario: String) {
        val grupoId = _casalIdAtivo.value
        if (grupoId.isNotBlank()) {
            viewModelScope.launch {
                repository.salvarAvaliacaoMembro(grupoId, midia, nota, comentario)
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val matchesDoGrupo: Flow<List<MatchMidia>> = _casalIdAtivo.flatMapLatest { grupoId ->
        if (grupoId.isBlank()) {
            kotlinx.coroutines.flow.flowOf(emptyList())
        } else {
            repository.observarMatchesDoGrupo(grupoId)
        }
    }

    private val _popularesTmdbMatch = MutableStateFlow<List<Midia>>(emptyList())
    val popularesTmdbMatch: StateFlow<List<Midia>> = _popularesTmdbMatch.asStateFlow()

    private var paginaTmdbAtual = 1
    private var carregandoPopulares = false

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
                val existentes = repository.buscarMidiasPorGrupo(grupo).firstOrNull() ?: emptyList()
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
                        adicionadoPor = "Modo Match ❤️"
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

    fun atualizarStatusPresenca(online: Boolean) {
        val grupoId = _casalIdAtivo.value
        if (grupoId.isNotBlank()) {
            viewModelScope.launch {
                repository.atualizarPresencaNoGrupo(grupoId, online)
            }
        }
    }

    val historicoPessoal: Flow<List<Midia>> = repository.midiasPessoais.map { lista ->
        lista.filter { (!it.isCasal || it.casalId.isBlank()) && (it.status.equals("Concluído", ignoreCase = true) || it.status.equals("Concluido", ignoreCase = true)) }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val historicoGrupoAtivo: Flow<List<Midia>> = _casalIdAtivo.flatMapLatest { grupoId ->
        if (grupoId.isBlank()) {
            kotlinx.coroutines.flow.flowOf(emptyList())
        } else {
            repository.buscarMidiasPorGrupo(grupoId).map { lista ->
                lista.filter { it.isCasal && it.casalId == grupoId && (it.status.equals("Concluído", ignoreCase = true) || it.status.equals("Concluido", ignoreCase = true)) }
            }
        }
    }

    fun carregarGrupoAtivoInicial() {
        viewModelScope.launch {
            val grupoAtivo = repository.obterGrupoAtivoLocal()
            if (grupoAtivo != null) {
                _casalIdAtivo.value = grupoAtivo.grupoId
                if (grupoAtivo.grupoId.isNotBlank()) {
                    observarGrupoFirestore(grupoAtivo.grupoId)
                    repository.atualizarPresencaNoGrupo(grupoAtivo.grupoId, true)
                    verificarSeSouAdministrador(grupoAtivo.grupoId)
                }
            }
        }
    }

    fun selecionarGrupoAtivo(grupoId: String) {
        viewModelScope.launch {
            val grupoAnterior = _casalIdAtivo.value
            if (grupoAnterior.isNotBlank() && grupoAnterior != grupoId) {
                repository.atualizarPresencaNoGrupo(grupoAnterior, false)
            }

            repository.ativarGrupoLocal(grupoId)
            _casalIdAtivo.value = grupoId
            if (grupoId.isNotBlank()) {
                observarGrupoFirestore(grupoId)
                repository.atualizarPresencaNoGrupo(grupoId, true)
                verificarSeSouAdministrador(grupoId)
            } else {
                _isAdministradorSala.value = false
            }
        }
    }

    fun verificarSeSouAdministrador(grupoId: String) {
        val uidAtual = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        if (grupoId.isBlank() || uidAtual.isBlank()) {
            _isAdministradorSala.value = false
            return
        }
        viewModelScope.launch {
            val souAdm = repository.verificarSeUsuarioEhAdmin(grupoId, uidAtual)
            _isAdministradorSala.value = souAdm
        }
    }

    fun excluirMembroDaSala(grupoId: String, membroUid: String, onResultado: (Boolean) -> Unit) {
        viewModelScope.launch {
            val sucesso = repository.removerMembroDoGrupo(grupoId, membroUid)
            onResultado(sucesso)
        }
    }

    fun atualizarSenhaDaSala(grupoId: String, novaSenha: String, onResultado: (Boolean) -> Unit) {
        viewModelScope.launch {
            val sucesso = repository.atualizarSenhaDoGrupo(grupoId, novaSenha)
            onResultado(sucesso)
        }
    }

    fun criarGrupoComSenha(grupoId: String, nomeGrupo: String, tipo: String, senha: String, onResultado: (Boolean) -> Unit) {
        viewModelScope.launch {
            val idFormatado = if (grupoId.startsWith("CINE-", ignoreCase = true)) grupoId.trim().uppercase() else "CINE-${grupoId.trim().uppercase().removePrefix("CINE-")}"
            val sucesso = repository.criarNovoGrupoNaNuvem(idFormatado, nomeGrupo, tipo, senha)
            if (sucesso) {
                _casalIdAtivo.value = idFormatado
                observarGrupoFirestore(idFormatado)
                verificarSeSouAdministrador(idFormatado)
            }
            onResultado(sucesso)
        }
    }

    fun entrarEmGrupoExistente(grupoId: String, nomeGrupo: String, tipo: String, senhaDigitada: String, onResultado: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val idFormatado = if (grupoId.startsWith("CINE-", ignoreCase = true)) grupoId.trim().uppercase() else "CINE-${grupoId.trim().uppercase().removePrefix("CINE-")}"
            val resultado = repository.verificarEEntrarNoGrupo(idFormatado, nomeGrupo, tipo, senhaDigitada)
            if (resultado.isSuccess) {
                _casalIdAtivo.value = idFormatado
                observarGrupoFirestore(idFormatado)
                verificarSeSouAdministrador(idFormatado)
                onResultado(true, null)
            } else {
                onResultado(false, resultado.exceptionOrNull()?.localizedMessage ?: "Erro ao entrar no grupo.")
            }
        }
    }

    fun excluirGrupoSalvo(grupo: GrupoEntity) {
        viewModelScope.launch {
            if (_casalIdAtivo.value == grupo.grupoId) {
                selecionarGrupoAtivo("")
            }
            repository.deletarGrupoLocal(grupo)
        }
    }

    private fun observarGrupoFirestore(grupoId: String) {
        viewModelScope.launch {
            repository.observarMidiasDoGrupoFirestore(grupoId).collect {
            }
        }
    }

    val todasNotificacoes: Flow<List<NotificacaoEntity>> = notificacaoRepository.todasNotificacoes
    val quantidadeNaoLidas: Flow<Int> = notificacaoRepository.quantidadeNaoLidas
    val amigosConectados: Flow<List<AmigoPerfil>> = socialRepository.observarAmigos()

    val solicitacoesRecebidas: Flow<List<SolicitacaoAmizadeRecebida>> = socialRepository.observarSolicitacoesRecebidas()
    val solicitacoesPendentesEnviadas: Flow<List<String>> = socialRepository.observarUidsStatusPendente()
    val usuariosBloqueados: Flow<List<AmigoPerfil>> = socialRepository.observarUsuariosBloqueados()

    private val _resultadosBuscaAmigos = MutableStateFlow<List<AmigoPerfil>>(emptyList())
    val resultadosBuscaAmigos: StateFlow<List<AmigoPerfil>> = _resultadosBuscaAmigos

    private val _listaAmigoSelecionado = MutableStateFlow<List<Midia>>(emptyList())
    val listaAmigoSelecionado: StateFlow<List<Midia>> = _listaAmigoSelecionado

    fun atualizarMeuPerfilPublico(nome: String, bio: String) {
        viewModelScope.launch { socialRepository.atualizarPerfilPublico(nome, bio) }
    }

    private var jobBuscaUsuarios: Job? = null

    fun pesquisarUsuarios(termo: String) {
        jobBuscaUsuarios?.cancel()
        val termoLimpo = termo.trim()
        if (termoLimpo.isEmpty()) {
            _resultadosBuscaAmigos.value = emptyList()
            return
        }
        jobBuscaUsuarios = viewModelScope.launch {
            _resultadosBuscaAmigos.value = socialRepository.buscarUsuarios(termoLimpo)
        }
    }

    fun monitorarExpulsaoDaSala() {
        viewModelScope.launch {
            _casalIdAtivo.collect { grupoId ->
                if (grupoId.isNotBlank()) {
                    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@collect
                    firestore.collection("grupos").document(grupoId)
                        .collection("membros").document(uid)
                        .addSnapshotListener { snapshot, _ ->
                            if (snapshot != null && !snapshot.exists()) {
                                viewModelScope.launch {
                                    val grupoLocal = repository.obterGrupoAtivoLocal()
                                    if (grupoLocal != null) {
                                        repository.deletarGrupoLocal(grupoLocal)
                                    }
                                    _casalIdAtivo.value = ""
                                    NotificacaoHelper.dispararNotificacaoExpulsao(context)
                                }
                            }
                        }
                }
            }
        }
    }

    fun enviarSolicitacaoAmigo(amigoUid: String, onResultado: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val resultado = socialRepository.enviarSolicitacaoAmizade(amigoUid)
            resultado.onSuccess {
                onResultado(true, null)
            }.onFailure { erro ->
                onResultado(false, erro.message)
            }
        }
    }

    fun cancelarSolicitacaoAmigo(amigoUid: String, onResultado: (Boolean) -> Unit) {
        viewModelScope.launch {
            val sucesso = socialRepository.cancelarSolicitacaoEnviada(amigoUid)
            onResultado(sucesso)
        }
    }

    fun atualizarPresencaGlobal(online: Boolean) {
        viewModelScope.launch {
            socialRepository.atualizarPresencaGlobal(online)
        }
    }

    fun aceitarSolicitacaoAmizade(remetenteUid: String, onResultado: (Boolean) -> Unit) {
        viewModelScope.launch {
            val sucesso = socialRepository.aceitarSolicitacao(remetenteUid)
            onResultado(sucesso)
        }
    }

    fun recusarSolicitacaoAmizade(remetenteUid: String, onResultado: (Boolean) -> Unit) {
        viewModelScope.launch {
            val sucesso = socialRepository.recusarSolicitacao(remetenteUid)
            onResultado(sucesso)
        }
    }

    fun removerAmigo(amigoUid: String, onResultado: (Boolean) -> Unit) {
        viewModelScope.launch {
            val sucesso = socialRepository.removerAmigo(amigoUid)
            onResultado(sucesso)
        }
    }

    fun bloquearUsuario(alvoUid: String, onResultado: (Boolean) -> Unit) {
        viewModelScope.launch {
            val sucesso = socialRepository.bloquearUsuario(alvoUid)
            onResultado(sucesso)
        }
    }

    fun desbloquearUsuario(alvoUid: String, onResultado: (Boolean) -> Unit) {
        viewModelScope.launch {
            val sucesso = socialRepository.desbloquearUsuario(alvoUid)
            onResultado(sucesso)
        }
    }

    fun carregarListaDoAmigo(amigoUid: String) {
        viewModelScope.launch {
            _listaAmigoSelecionado.value = socialRepository.buscarListaAmigo(amigoUid)
        }
    }

    fun enviarMensagemAmigo(amigoUid: String, texto: String) {
        viewModelScope.launch {
            socialRepository.enviarMensagemChat(amigoUid, texto)
        }
    }

    fun observarMensagensAmigo(amigoUid: String): Flow<List<MensagemChat>> {
        return socialRepository.observarMensagensChat(amigoUid)
    }

    fun calcularMidiasEmComum(minhasMidias: List<Midia>, midiasAmigo: List<Midia>): List<Midia> {
        return minhasMidias.filter { minhaMidia ->
            midiasAmigo.any { midiaAmigoItem ->
                val tipo1 = minhaMidia.tipo.trim().lowercase()
                val tipo2 = midiaAmigoItem.tipo.trim().lowercase()

                val mesmoTipo = tipo1 == tipo2 ||
                        ((tipo1 in listOf("série", "anime", "novela", "dorama")) &&
                                (tipo2 in listOf("série", "anime", "novela", "dorama")))

                if (!mesmoTipo) return@any false

                if (minhaMidia.idTmdb != 0 && midiaAmigoItem.idTmdb != 0) {
                    minhaMidia.idTmdb == midiaAmigoItem.idTmdb
                } else {
                    val titulo1 = minhaMidia.titulo.trim().lowercase()
                    val titulo2 = midiaAmigoItem.titulo.trim().lowercase()
                    titulo1.isNotBlank() && titulo1 == titulo2
                }
            }
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

    fun iniciarSincronizacaoSilenciosaNuvem() {
        viewModelScope.launch {
            repository.sincronizacaoAutomaticaSilenciosa()
            carregarGrupoAtivoInicial()
        }
    }

    fun forcarSincronizacaoManual() {
        viewModelScope.launch {
            repository.forcarSincronizacaoManual()
            carregarGrupoAtivoInicial()
        }
    }

    fun limparEstadoSincronizacao() {
        repository.limparEstadoSincronizacao()
        _casalIdAtivo.value = ""
        _isAdministradorSala.value = false
    }

    fun verificarAtualizacaoSilenciosa() {
        viewModelScope.launch {
            val update = UpdateManager.checarAtualizacaoSilenciosa()
            if (update != null) {
                _updatePendente.value = update
                UpdateManager.exibirNotificacaoAtualizacao(context, update)
            }
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
        _queryPaginada, _tipoPaginado, _provedorSelecionadoId, _generoSelecionadoId, _ordenacaoSelecionada
    ) { query, tipo, provedor, genero, ordenacao ->
        repository.buscarNoTmdbPaginado(query, tipo, provedor, genero, ordenacao)
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

    fun selecionarTipo(tipo: String) { _tipoPaginado.value = tipo }
    fun selecionarOrdenacao(ordenacao: String) { _ordenacaoSelecionada.value = ordenacao }

    fun limparBuscaApi() {
        _resultadosBuscaApi.value = emptyList()
        _queryPaginada.value = ""
        _tipoPaginado.value = "Todos"
        _provedorSelecionadoId.value = null
        _generoSelecionadoId.value = null
        _ordenacaoSelecionada.value = "popularity.desc"
    }

    fun inserir(midia: Midia) {
        viewModelScope.launch {
            val usuarioLogado = FirebaseAuth.getInstance().currentUser
            val uid = usuarioLogado?.uid ?: ""
            val nomeAExibir = usuarioLogado?.displayName?.takeIf { it.isNotBlank() } ?: usuarioLogado?.email ?: "Parceiro(a)"
            val identificadorAdicao = "${uid}_${nomeAExibir}"

            val midiaProcessada = if (midia.isCasal && midia.casalId.isNotBlank()) {
                midia.copy(
                    status = "Pendente",
                    adicionadoPor = identificadorAdicao
                )
            } else {
                midia.copy(
                    adicionadoPor = identificadorAdicao
                )
            }

            repository.inserir(midiaProcessada)
        }
    }

    fun aceitarMidiaPendente(midia: Midia) {
        viewModelScope.launch {
            val midiaAprovada = midia.copy(status = "Quero Assistir")
            repository.atualizar(midiaAprovada)
        }
    }

    fun recusarMidiaPendente(midia: Midia) {
        viewModelScope.launch {
            val midiaRecusada = midia.copy(status = "Recusado")
            repository.atualizar(midiaRecusada)
        }
    }

    fun processarMidiaRecusadaPeloParceiro(midia: Midia, moverParaListaPessoal: Boolean) {
        viewModelScope.launch {
            repository.deletar(midia)
            if (moverParaListaPessoal) {
                val midiaPessoal = midia.copy(
                    id = 0,
                    isCasal = false,
                    casalId = "",
                    status = "Quero Assistir"
                )
                repository.inserir(midiaPessoal)
            }
        }
    }

    fun atualizar(midia: Midia) { viewModelScope.launch { repository.atualizar(midia) } }

    fun concluirMidia(midia: Midia) {
        viewModelScope.launch {
            val usuarioLogado = FirebaseAuth.getInstance().currentUser
            val nomeUsuario = usuarioLogado?.displayName?.takeIf { it.isNotBlank() } ?: usuarioLogado?.email ?: "Parceiro(a)"
            val nomeAExibir = nomeUsuario.substringBefore("@")

            val midiaConcluida = midia.copy(
                status = "Concluído",
                jaEncerrou = true,
                dataConclusao = System.currentTimeMillis(),
                concluidoPor = if (midia.isCasal) nomeAExibir else ""
            )
            repository.atualizar(midiaConcluida)

            if (midia.isCasal && midia.casalId.isNotBlank()) {
                repository.limparAssistindoAgora(midia.casalId)
            }
        }
    }

    fun deletar(midia: Midia) { viewModelScope.launch { repository.deletar(midia) } }
    fun alternarFavorito(midia: Midia) { viewModelScope.launch { repository.atualizar(midia.copy(favorito = !midia.favorito)) } }
    fun moverParaListaCustomizada(midia: Midia, novaLista: String) { viewModelScope.launch { repository.atualizar(midia.copy(listaCustomizada = novaLista)) } }

    fun buscarFilmeNoTmdb(nome: String, tipo: String) {
        if (nome.isBlank()) { limparBuscaApi(); return }
        viewModelScope.launch {
            _carregandoApi.value = true
            try { _resultadosBuscaApi.value = repository.buscarNoTmdb(nome, tipo) }
            catch (e: Exception) { e.printStackTrace(); limparBuscaApi() }
            finally { _carregandoApi.value = false }
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
        return tipo.equals("Série", ignoreCase = true) || tipo.equals("Anime", ignoreCase = true) ||
                tipo.equals("Novela", ignoreCase = true) || tipo.equals("Dorama", ignoreCase = true) || tipo.equals("tv", ignoreCase = true)
    }

    fun buscarEpisodiosTemporada(idTmdb: Int, numeroTemporada: Int) {
        if (idTmdb == 0) return
        viewModelScope.launch {
            _carregandoEpisodios.value = true
            try {
                val resultado = RetrofitClient.apiService.obterEpisodiosTemporada(idTmdb, numeroTemporada)
                _episodiosTemporada.value = resultado.episodios
            } catch (e: Exception) { e.printStackTrace(); _episodiosTemporada.value = emptyList() }
            finally { _carregandoEpisodios.value = false }
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
                    } catch (e2: Exception) { e2.printStackTrace() }
                }

                _detalhesEstendidosApi.value = detalhes
                _elencoMidia.value = creditos?.elenco ?: emptyList()
                val trailer = videosResposta?.videos?.firstOrNull { it.sitePlataforma.equals("YouTube", ignoreCase = true) && (it.tipoVideo.equals("Trailer", ignoreCase = true) || it.tipoVideo.equals("Teaser", ignoreCase = true)) }
                _chaveTrailerYoutube.value = trailer?.chaveYoutube
                _recomendacoesMidia.value = recomendacoesResposta?.recomendacoes ?: emptyList()
                _galeriaImagens.value = ((imagensResposta?.backdrops ?: emptyList()) + (imagensResposta?.posters ?: emptyList())).distinctBy { it.caminhoArquivo }
                buscarOndeAssistir(idTmdb, if (ehRealmenteSerie) "Série" else "Filme")
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun buscarOndeAssistir(idTmdb: Int, tipo: String) {
        if (idTmdb == 0) return
        viewModelScope.launch {
            try {
                val ehSerieOuAnime = verificarSeEhSerie(tipo)
                val resposta = if (ehSerieOuAnime) RetrofitClient.apiService.obterProvedoresSerieOuAnime(idSerie = idTmdb) else RetrofitClient.apiService.obterProvedoresFilme(idFilme = idTmdb)
                _provedoresStreaming.value = resposta.resultados?.get("BR")?.streamingAssinatura ?: emptyList()
            } catch (e: Exception) { e.printStackTrace(); _provedoresStreaming.value = emptyList() }
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
        viewModelScope.launch { novasMidias.forEach { repository.inserir(it.copy(id = 0)) } }
    }

    fun incrementarEpisodioRapido(midia: Midia) { viewModelScope.launch { repository.incrementarEpisodio(midia.id) } }
    fun definirProgressoEpisodio(idMidia: Int, temporada: Int, episodio: Int) { viewModelScope.launch { repository.atualizarProgressoEpisodio(idMidia, temporada, episodio) } }
    fun sincronizarNuvemManual(onResultado: (Int) -> Unit) { viewModelScope.launch { onResultado(repository.restaurarDoFirestore()) } }
    fun fazerBackupCompletoNuvem(onResultado: (Boolean) -> Unit) { viewModelScope.launch { onResultado(repository.backupCompletoParaFirestore()) } }
    fun limparTodaALista() { viewModelScope.launch { repository.limparTodaALista() } }
    fun gerarNovoCodigoGrupo(): String = repository.gerarCodigoAleatorio()

    fun calcularDadosWrapped(midias: List<Midia>): CineWrappedData {
        val concluidos = midias.filter { it.status.equals("Concluído", ignoreCase = true) || it.status.equals("Concluido", ignoreCase = true) }
        val totalFilmes = concluidos.count { it.tipo.equals("Filme", ignoreCase = true) }
        val totalEpisodios = concluidos.filter { !it.tipo.equals("Filme", ignoreCase = true) }.sumOf { if (it.episodioAtual > 0) it.episodioAtual - 1 else 0 }

        val minutosTotais = (totalFilmes * 115) + (totalEpisodios * 45)
        val horas = minutosTotais / 60

        val generoTop = midias.filter { it.genero.isNotBlank() && it.genero != "Geral" }
            .groupingBy { it.genero }.eachCount().maxByOrNull { it.value }?.key ?: "Geral"

        val plataformaTop = midias.filter { it.plataforma.isNotBlank() && it.plataforma != "Não Informado" }
            .groupingBy { it.plataforma }.eachCount().maxByOrNull { it.value }?.key ?: "Diversas"

        val midiasComNota = midias.filter { it.nota > 0 }
        val mediaNotas = midiasComNota.map { it.nota }.average()
        val mediaFormatada = if (!mediaNotas.isNaN()) String.format(java.util.Locale.US, "%.1f", mediaNotas) else "0.0"
        val maiorNota = midiasComNota.maxOfOrNull { it.nota } ?: 0

        val destaque = concluidos.maxByOrNull { it.nota }?.titulo ?: midias.firstOrNull()?.titulo ?: "Nenhum título ainda"

        val frase = when {
            horas > 100 -> "Você é uma verdadeira lenda das maratonas! 🍿🔥"
            horas > 50 -> "Sua lista está recheada de ótimas histórias! 🎬"
            else -> "Sua jornada cinéfila está apenas começando! ✨"
        }

        return CineWrappedData(
            totalTitulosConcluidos = concluidos.size,
            horasTotaisAssistidas = horas,
            generoFavorito = generoTop,
            plataformaMaisUtilizada = plataformaTop,
            totalEpisodiosMaratonados = totalEpisodios,
            mediaNotasAtribuidas = mediaFormatada,
            maiorNotaDada = maiorNota,
            filmeOuSerieDestaque = destaque,
            frasePersonalizada = frase
        )
    }
}