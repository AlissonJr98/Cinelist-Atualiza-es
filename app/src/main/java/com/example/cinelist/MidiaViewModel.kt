package com.example.cinelist







import android.content.Context



import androidx.compose.runtime.getValue



import androidx.compose.runtime.mutableStateOf



import androidx.compose.runtime.setValue



import androidx.lifecycle.ViewModel



import androidx.lifecycle.viewModelScope



import androidx.paging.PagingData



import androidx.paging.cachedIn



import com.google.firebase.auth.FirebaseAuth



import com.google.firebase.firestore.FirebaseFirestore



import com.google.firebase.messaging.FirebaseMessaging



import dagger.hilt.android.lifecycle.HiltViewModel



import dagger.hilt.android.qualifiers.ApplicationContext



import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.ExperimentalCoroutinesApi



import kotlinx.coroutines.Job



import kotlinx.coroutines.flow.Flow



import kotlinx.coroutines.flow.MutableStateFlow



import kotlinx.coroutines.flow.StateFlow



import kotlinx.coroutines.flow.asStateFlow



import kotlinx.coroutines.flow.combine



import kotlinx.coroutines.flow.flatMapLatest



import kotlinx.coroutines.flow.map



import kotlinx.coroutines.launch



import javax.inject.Inject



import kotlinx.coroutines.async



import java.util.UUID







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







/**



 * ViewModel principal do aplicativo.



 * 



 * Gerencia mídias, grupos, notificações, chat e funcionalidades sociais.



 * 



 * NOTA: Este ViewModel está sendo gradualmente dividido em ViewModels menores



 * (GrupoViewModel, ChatViewModel, NotificacaoViewModel, MatchViewModel, PerfilViewModel)



 * para melhor organização do código.



 */



@HiltViewModel



class MidiaViewModel @Inject constructor(



    private val repository: MidiaRepository,



    private val notificacaoRepository: NotificacaoRepository,



    val socialRepository: SocialRepository,



    @ApplicationContext private val context: Context



) : ViewModel() {







    private val firestore = FirebaseFirestore.getInstance()



    private val prefsMute = context.getSharedPreferences("CineListMutePrefs", Context.MODE_PRIVATE)







    // ---- Atualização do aplicativo via GitHub (estilo LivennIA) ----



    private val versaoInicial = AtualizadorGitHub.obterVersaoInstalada(context)







    var versaoInstaladaCodigo by mutableStateOf(versaoInicial.first)



        private set







    var versaoInstaladaNome by mutableStateOf(versaoInicial.second)



        private set







    var infoAtualizacao by mutableStateOf<InfoAtualizacao?>(null)



        private set







    var temNovaAtualizacao by mutableStateOf(false)



        private set







    var mostrarDialogoAtualizacao by mutableStateOf(false)



        private set







    var verificandoAtualizacao by mutableStateOf(false)



        private set







    var baixandoAtualizacao by mutableStateOf(false)



        private set







    var progressoAtualizacao by mutableStateOf(0f)



        private set







    var statusAtualizacao by mutableStateOf<String?>(null)



        private set







    var erroAtualizacao by mutableStateOf<String?>(null)



        private set







    var apkAtualizacaoBaixado by mutableStateOf<java.io.File?>(null)



        private set







    private val _updatePendente = MutableStateFlow<InfoAtualizacao?>(null)



    val updatePendente: StateFlow<InfoAtualizacao?> = _updatePendente.asStateFlow()







    // Estado global de operação para feedback ao usuário



    private val _estadoOperacao = MutableStateFlow<Resultado<Unit>?>(null)



    val estadoOperacao: StateFlow<Resultado<Unit>?> = _estadoOperacao.asStateFlow()







    fun limparEstadoOperacao() {



        _estadoOperacao.value = null



    }







    val todasAsMidias: Flow<List<Midia>> = repository.todasAsMidias



    val midiasPessoais: Flow<List<Midia>> = repository.midiasPessoais



    val gruposSalvos: Flow<List<GrupoEntity>> = repository.gruposSalvos







    private val _casalIdAtivo = MutableStateFlow("")



    val casalIdAtivo: StateFlow<String> = _casalIdAtivo.asStateFlow()







    private val _isAdministradorSala = MutableStateFlow(false)



    val isAdministradorSala: StateFlow<Boolean> = _isAdministradorSala.asStateFlow()







    private val _salaMutada = MutableStateFlow(false)



    val salaMutada: StateFlow<Boolean> = _salaMutada.asStateFlow()







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







    @OptIn(ExperimentalCoroutinesApi::class)



    val mensagensGrupoAtivo: Flow<List<MensagemGrupo>> = _casalIdAtivo.flatMapLatest { grupoId ->



        if (grupoId.isBlank()) {



            kotlinx.coroutines.flow.flowOf(emptyList())



        } else {



            socialRepository.observarMensagensGrupo(grupoId)



        }



    }







    fun enviarMensagemParaSala(texto: String, tipoMensagem: String = "TEXTO", mediaUrl: String = "") {



        val grupoId = _casalIdAtivo.value



        if (grupoId.isNotBlank()) {



            viewModelScope.launch {



                socialRepository.enviarMensagemGrupo(grupoId, texto, tipoMensagem, mediaUrl)



            }



        }



    }







    fun editarMensagemSala(mensagemId: String, novoTexto: String) {



        val grupoId = _casalIdAtivo.value



        if (grupoId.isNotBlank()) {



            viewModelScope.launch {



                socialRepository.editarMensagemGrupo(grupoId, mensagemId, novoTexto)



            }



        }



    }







    fun deletarMensagemSala(mensagemId: String) {



        val grupoId = _casalIdAtivo.value



        if (grupoId.isNotBlank()) {



            viewModelScope.launch {



                socialRepository.deletarMensagemGrupo(grupoId, mensagemId)



            }



        }



    }







    private fun carregarConfiguracaoMute(grupoId: String) {



        if (grupoId.isBlank()) return



        val mutado = prefsMute.getBoolean("mute_$grupoId", false)



        _salaMutada.value = mutado







        try {



            if (!mutado) {



                FirebaseMessaging.getInstance().subscribeToTopic("grupo_$grupoId")



            } else {



                FirebaseMessaging.getInstance().unsubscribeFromTopic("grupo_$grupoId")



            }



        } catch (e: Throwable) {



            e.printStackTrace()



        }



    }







    fun alternarMuteSala() {



        val grupoId = _casalIdAtivo.value



        if (grupoId.isNotBlank()) {



            val novoEstado = !_salaMutada.value



            prefsMute.edit().putBoolean("mute_$grupoId", novoEstado).apply()



            _salaMutada.value = novoEstado







            try {



                if (novoEstado) {



                    FirebaseMessaging.getInstance().unsubscribeFromTopic("grupo_$grupoId")



                } else {



                    FirebaseMessaging.getInstance().subscribeToTopic("grupo_$grupoId")



                }



            } catch (e: Throwable) {



                e.printStackTrace()



            }



        }



    }







    // Carrosseis



    private val _filmesEmCartaz = MutableStateFlow<List<TmdbFilme>>(emptyList())



    val filmesEmCartaz: StateFlow<List<TmdbFilme>> = _filmesEmCartaz







    private val _carrosselPopulares = MutableStateFlow<List<TmdbFilme>>(emptyList())



    val carrosselPopulares: StateFlow<List<TmdbFilme>> = _carrosselPopulares







    private val _carrosselSeriesAlta = MutableStateFlow<List<TmdbFilme>>(emptyList())



    val carrosselSeriesAlta: StateFlow<List<TmdbFilme>> = _carrosselSeriesAlta







    private val _carrosselComedias = MutableStateFlow<List<TmdbFilme>>(emptyList())



    val carrosselComedias: StateFlow<List<TmdbFilme>> = _carrosselComedias







    private val _carrosselAcao = MutableStateFlow<List<TmdbFilme>>(emptyList())



    val carrosselAcao: StateFlow<List<TmdbFilme>> = _carrosselAcao







    private val _secoesLancamentosStreaming = MutableStateFlow<List<StreamingLancamentosSecao>>(emptyList())
    val secoesLancamentosStreaming: StateFlow<List<StreamingLancamentosSecao>> = _secoesLancamentosStreaming

    private val _carrosseisGenerosFavoritos = MutableStateFlow<List<Pair<GeneroInfo, List<TmdbFilme>>>>(emptyList())
    val carrosseisGenerosFavoritos: StateFlow<List<Pair<GeneroInfo, List<TmdbFilme>>>> = _carrosseisGenerosFavoritos

    private val _carregandoCarrosseis = MutableStateFlow(false)



    val carregandoCarrosseis: StateFlow<Boolean> = _carregandoCarrosseis







    init {



        carregarGrupoAtivoInicial()



        verificarAtualizacaoGitHub(manual = false)



        iniciarSincronizacaoSilenciosaNuvem()



        carregarMaisPopularesMatch()



        carregarFilmesEmCartaz()



        carregarCarrosseisDescobrir()



        monitorarExpulsaoDaSala()



    }







    /**



     * Carrega os carrosséis da aba Descobrir.



     *



     * Cada secção é carregada de forma INDEPENDENTE: se uma falhar (ex.: 429 do



     * TMDB), as restantes continuam a aparecer, em vez de a aba inteira ficar vazia.



     */



    fun carregarCarrosseisDescobrir(forcar: Boolean = false) {



        if (!forcar && _carrosselPopulares.value.isNotEmpty()) return







        viewModelScope.launch {



            _carregandoCarrosseis.value = true



            try {



                // Helpers locais: agregam páginas sem falhar a secção inteira







                suspend fun filmes(vararg paginas: Int, genero: String? = null): List<TmdbFilme> {



                    val acumulado = mutableListOf<TmdbFilme>()



                    for (p in paginas) {



                        runCatching {



                            RetrofitClient.apiService.descobrirFilmes(



                                pagina = p, sortBy = "popularity.desc",



                                provedores = null, generos = genero



                            ).resultados



                        }.onSuccess { acumulado.addAll(it) }



                    }



                    return acumulado.map { it.copy(mediaType = "movie") }



                }







                suspend fun series(vararg paginas: Int): List<TmdbFilme> {



                    val acumulado = mutableListOf<TmdbFilme>()



                    for (p in paginas) {



                        runCatching {



                            RetrofitClient.apiService.descobrirSeries(



                                pagina = p, sortBy = "popularity.desc",



                                provedores = null, generos = null



                            ).resultados



                        }.onSuccess { acumulado.addAll(it) }



                    }



                    return acumulado.map { it.copy(mediaType = "tv") }



                }







                // Populares e Séries em paralelo (as duas hero lists)



                val popularesDeferred = async { filmes(1, 2, 3) }



                val seriesDeferred = async { series(1, 2, 3) }







                _carrosselPopulares.value = popularesDeferred.await()



                _carrosselSeriesAlta.value = seriesDeferred.await()







                // Géneros específicos depois, para não competir pelo rate limit



                _carrosselComedias.value = filmes(1, 2, genero = "35")



                _carrosselAcao.value = filmes(1, 2, genero = "28")



            } catch (e: Exception) {



                android.util.Log.e("MidiaViewModel", "Falha ao carregar carrosséis", e)



            } finally {



                _carregandoCarrosseis.value = false



            }



        }



    }







    fun carregarNovidadesPorStreaming(context: Context, forcar: Boolean = false) {
        if (!forcar && _secoesLancamentosStreaming.value.isNotEmpty()) return

        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val meusIds = StreamingManager.obterMeusStreamingsIds(context)
                    val servicosAlvo = if (meusIds.isNotEmpty()) {
                        StreamingManager.SERVICOS.filter { it.id in meusIds }
                    } else {
                        StreamingManager.SERVICOS.filter { it.id in listOf(8, 119, 337, 1899) }
                    }

                    val resultadoSecoes = mutableListOf<StreamingLancamentosSecao>()

                    for (servico in servicosAlvo.take(6)) {
                        try {
                            val seriesResp = runCatching {
                                RetrofitClient.apiService.descobrirSeries(
                                    pagina = 1,
                                    provedores = servico.id.toString(),
                                    regiao = "BR",
                                    sortBy = "first_air_date.desc"
                                ).resultados.map { it.copy(mediaType = "tv", plataformaDetectada = servico.nome) }
                            }.getOrDefault(emptyList())

                            val filmesResp = runCatching {
                                RetrofitClient.apiService.descobrirFilmes(
                                    pagina = 1,
                                    provedores = servico.id.toString(),
                                    regiao = "BR",
                                    sortBy = "popularity.desc"
                                ).resultados.map { it.copy(mediaType = "movie", plataformaDetectada = servico.nome) }
                            }.getOrDefault(emptyList())

                            val combinados = (seriesResp.take(8) + filmesResp.take(8))
                                .filter { !it.caminhoPoster.isNullOrBlank() }
                                .shuffled()

                            if (combinados.isNotEmpty()) {
                                resultadoSecoes.add(
                                    StreamingLancamentosSecao(
                                        servico = servico,
                                        titulos = combinados,
                                        totalTitulos = combinados.size
                                    )
                                )
                            }
                        } catch (e: Exception) {
                            // Silencioso por plataforma
                        }
                    }

                    _secoesLancamentosStreaming.value = resultadoSecoes
                }
            } catch (e: Exception) {
                // Silencioso
            }
        }
    }

    fun carregarCarrosseisGenerosFavoritos(context: Context) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val idsGeneros = GeneroPreferencesManager.obterGenerosFavoritosIds(context)
                    if (idsGeneros.isEmpty()) return@withContext

                    val generosAlvo = GeneroPreferencesManager.GENEROS.filter { it.id in idsGeneros }
                    val listaPares = mutableListOf<Pair<GeneroInfo, List<TmdbFilme>>>()

                    for (gen in generosAlvo.take(4)) {
                        val filmesGen = runCatching {
                            RetrofitClient.apiService.descobrirFilmes(
                                pagina = 1,
                                generos = gen.id.toString(),
                                sortBy = "popularity.desc"
                            ).resultados.map { it.copy(mediaType = "movie", generoTexto = gen.nome) }
                        }.getOrDefault(emptyList())

                        if (filmesGen.isNotEmpty()) {
                            listaPares.add(Pair(gen, filmesGen.filter { !it.caminhoPoster.isNullOrBlank() }))
                        }
                    }

                    _carrosseisGenerosFavoritos.value = listaPares
                }
            } catch (e: Exception) {
                // Silencioso
            }
        }
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







    fun avaliarMidiaNaSala(midia: Midia, nota: Int, comentario: String, contemSpoiler: Boolean = false) {



        val grupoId = _casalIdAtivo.value



        if (grupoId.isNotBlank()) {



            viewModelScope.launch {



                repository.salvarAvaliacaoMembro(grupoId, midia, nota, comentario, contemSpoiler)



                try {



                    socialRepository.publicarAtividade(



                        tipo = "AVALIOU",



                        tmdbId = midia.idTmdb,



                        tituloMidia = midia.titulo,



                        posterPath = midia.imagemCapa,



                        tipoMidia = midia.tipo,



                        nota = nota.toFloat(),



                        comentario = comentario



                    )



                } catch (ePub: Exception) {



                    ePub.printStackTrace()



                }



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



                    carregarConfiguracaoMute(grupoAtivo.grupoId)



                }



            }



        }



    }







    fun selecionarGrupoAtivo(grupoId: String) {



        viewModelScope.launch {



            val grupoAnterior = _casalIdAtivo.value



            if (grupoAnterior.isNotBlank() && grupoAnterior != grupoId) {



                repository.atualizarPresencaNoGrupo(grupoAnterior, false)



                try {

                    FirebaseMessaging.getInstance().unsubscribeFromTopic("grupo_$grupoAnterior")

                } catch (e: Throwable) {

                    e.printStackTrace()

                }



            }







            repository.ativarGrupoLocal(grupoId)



            _casalIdAtivo.value = grupoId



            if (grupoId.isNotBlank()) {



                observarGrupoFirestore(grupoId)



                repository.atualizarPresencaNoGrupo(grupoId, true)



                verificarSeSouAdministrador(grupoId)



                carregarConfiguracaoMute(grupoId)



            } else {



                _isAdministradorSala.value = false



                _salaMutada.value = false



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



                carregarConfiguracaoMute(idFormatado)



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



                carregarConfiguracaoMute(idFormatado)



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



            repository.observarMidiasDoGrupoFirestore(grupoId).collect {}



        }



    }







    val todasNotificacoes: Flow<List<NotificacaoEntity>> = notificacaoRepository.todasNotificacoes



    val quantidadeNaoLidas: Flow<Int> = notificacaoRepository.quantidadeNaoLidas



    val amigosConectados: Flow<List<AmigoPerfil>> = socialRepository.observarAmigos()







    val solicitacoesRecebidas: Flow<List<SolicitacaoAmizadeRecebida>> = socialRepository.observarSolicitacoesRecebidas()



    val solicitacoesPendentesEnviadas: Flow<List<String>> = socialRepository.observarUidsStatusPendente()



    val usuariosBloqueados: Flow<List<AmigoPerfil>> = socialRepository.observarUsuariosBloqueados()



    val feedAtividades: Flow<List<AtividadeSocial>> = socialRepository.observarFeedAtividades()







    fun publicarAtividade(



        tipo: String,



        tmdbId: Int,



        tituloMidia: String,



        posterPath: String,



        tipoMidia: String,



        nota: Float = 0f,



        comentario: String = ""



    ) {



        viewModelScope.launch {



            socialRepository.publicarAtividade(



                tipo = tipo,



                tmdbId = tmdbId,



                tituloMidia = tituloMidia,



                posterPath = posterPath,



                tipoMidia = tipoMidia,



                nota = nota,



                comentario = comentario



            )



        }



    }







    fun alternarReacaoAtividade(atividadeId: String, emoji: String) {



        viewModelScope.launch {



            socialRepository.alternarReacaoAtividade(atividadeId, emoji)



        }



    }







    fun deletarAtividade(atividadeId: String) {
        viewModelScope.launch {
            socialRepository.deletarAtividade(atividadeId)
        }
    }

    fun alternarLikeAtividade(atividadeId: String) {
        viewModelScope.launch {
            socialRepository.alternarLikeAtividade(atividadeId)
        }
    }

    fun publicarTopicoForum(
        topicoTitulo: String,
        categoria: String,
        texto: String,
        tituloMidia: String = "",
        tmdbId: Int = 0,
        posterPath: String = "",
        tipoMidia: String = "Filme",
        contemSpoiler: Boolean = false,
        onConcluido: ((Boolean, String) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val resultado = socialRepository.publicarTopicoForum(
                topicoTitulo = topicoTitulo,
                categoria = categoria,
                texto = texto,
                tituloMidia = tituloMidia,
                tmdbId = tmdbId,
                posterPath = posterPath,
                tipoMidia = tipoMidia,
                contemSpoiler = contemSpoiler
            )
            onConcluido?.invoke(resultado.first, resultado.second)
        }
    }

    fun observarComentariosAtividade(atividadeId: String): Flow<List<ComentarioForum>> {
        return socialRepository.observarComentariosAtividade(atividadeId)
    }

    fun adicionarComentarioAtividade(
        atividadeId: String,
        texto: String,
        onConcluido: ((Boolean, String) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val resultado = socialRepository.adicionarComentarioAtividade(atividadeId, texto)
            onConcluido?.invoke(resultado.first, resultado.second)
        }
    }

    fun alternarCurtidaComentarioAtividade(atividadeId: String, comentarioId: String) {
        viewModelScope.launch {
            socialRepository.alternarCurtidaComentarioAtividade(atividadeId, comentarioId)
        }
    }

    fun deletarComentarioAtividade(atividadeId: String, comentarioId: String) {
        viewModelScope.launch {
            socialRepository.deletarComentarioAtividade(atividadeId, comentarioId)
        }
    }







    // =========================================================================



    // ENQUETES RÁPIDAS NA SALA



    // =========================================================================







    @OptIn(ExperimentalCoroutinesApi::class)



    val enquetesSalaAtiva: Flow<List<EnqueteGrupo>> = _casalIdAtivo.flatMapLatest { grupoId ->



        if (grupoId.isBlank()) kotlinx.coroutines.flow.flowOf(emptyList())



        else socialRepository.observarEnquetesGrupo(grupoId)



    }







    fun criarEnqueteNaSala(



        titulo: String,



        descricao: String,



        opcoes: List<Map<String, Any>>,



        duracaoMinutos: Int = 60



    ) {



        val grupoId = _casalIdAtivo.value



        if (grupoId.isNotBlank()) {



            viewModelScope.launch {



                socialRepository.criarEnqueteGrupo(grupoId, titulo, descricao, opcoes, duracaoMinutos)



            }



        }



    }







    fun votarEnqueteNaSala(enqueteId: String, opcaoId: String) {



        val grupoId = _casalIdAtivo.value



        if (grupoId.isNotBlank()) {



            viewModelScope.launch {



                socialRepository.votarEnqueteGrupo(grupoId, enqueteId, opcaoId)



            }



        }



    }







    fun encerrarEnqueteNaSala(enqueteId: String) {



        val grupoId = _casalIdAtivo.value



        if (grupoId.isNotBlank()) {



            viewModelScope.launch {



                socialRepository.encerrarEnqueteGrupo(grupoId, enqueteId)



            }



        }



    }







    fun deletarEnqueteNaSala(enqueteId: String) {



        val grupoId = _casalIdAtivo.value



        if (grupoId.isNotBlank()) {



            viewModelScope.launch {



                socialRepository.deletarEnqueteGrupo(grupoId, enqueteId)



            }



        }



    }







    private val _resultadosBuscaAmigos = MutableStateFlow<List<AmigoPerfil>>(emptyList())



    val resultadosBuscaAmigos: StateFlow<List<AmigoPerfil>> = _resultadosBuscaAmigos







    private val _listaAmigoSelecionado = MutableStateFlow<List<Midia>>(emptyList())



    val listaAmigoSelecionado: StateFlow<List<Midia>> = _listaAmigoSelecionado







    fun atualizarMeuPerfilPublico(nome: String, bio: String, instagram: String = "", twitter: String = "", letterboxd: String = "") {



        viewModelScope.launch { socialRepository.atualizarPerfilPublico(nome, bio, instagram, twitter, letterboxd) }



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











    fun fecharDialogoAtualizacao() {



        if (!baixandoAtualizacao) {



            mostrarDialogoAtualizacao = false



        }



    }







    fun abrirDialogoAtualizacao() {



        if (infoAtualizacao != null && temNovaAtualizacao) {



            mostrarDialogoAtualizacao = true



        }



    }







    fun verificarAtualizacaoGitHub(manual: Boolean = false, onConcluido: ((Boolean) -> Unit)? = null) {



        if (verificandoAtualizacao || baixandoAtualizacao) return



        verificandoAtualizacao = true



        erroAtualizacao = null



        if (manual) statusAtualizacao = "Buscando atualizações..."



        viewModelScope.launch {



            try {



                val res = AtualizadorGitHub.verificar(context)



                versaoInstaladaCodigo = res.versaoInstaladaCodigo



                versaoInstaladaNome = res.versaoInstaladaNome



                infoAtualizacao = res.info



                temNovaAtualizacao = res.disponivel



                if (res.disponivel && res.info != null) {



                    statusAtualizacao = "Nova versão v${res.info.versaoNome} disponível! ${res.info.notas}"



                    mostrarDialogoAtualizacao = true



                    _updatePendente.value = res.info



                    onConcluido?.invoke(true)



                } else if (res.info != null) {



                    statusAtualizacao = "Você já está na versão mais recente (v${res.info.versaoNome})."



                    _updatePendente.value = null



                    onConcluido?.invoke(false)



                } else {



                    statusAtualizacao = "Nenhuma atualização encontrada no momento."



                    _updatePendente.value = null



                    onConcluido?.invoke(false)



                }



            } catch (e: kotlinx.coroutines.CancellationException) {



                throw e



            } catch (e: Throwable) {



                statusAtualizacao = "Não foi possível verificar atualizações no momento."



                erroAtualizacao = e.message ?: "Falha ao verificar atualização"



                onConcluido?.invoke(false)



            } finally {



                verificandoAtualizacao = false



            }



        }



    }







    fun iniciarDownloadOuInstalacaoGitHub() {



        val arquivoExistente = apkAtualizacaoBaixado



        if (arquivoExistente != null && arquivoExistente.exists()) {



            val abriuInstalador = AtualizadorGitHub.instalarApk(context, arquivoExistente)



            if (!abriuInstalador) {



                erroAtualizacao = "Permita 'Instalar apps desconhecidos' para o CineList e toque em Instalar novamente."



            }



            return



        }







        val info = infoAtualizacao ?: return



        if (baixandoAtualizacao) return



        baixandoAtualizacao = true



        progressoAtualizacao = 0f



        erroAtualizacao = null



        statusAtualizacao = "Baixando atualização v${info.versaoNome}..."



        viewModelScope.launch {



            try {



                val arquivo = AtualizadorGitHub.baixarApk(context, info.apkUrl) { prog ->



                    progressoAtualizacao = prog



                }



                apkAtualizacaoBaixado = arquivo



                statusAtualizacao = "Download concluído! Abrindo instalador..."



                val abriuInstalador = AtualizadorGitHub.instalarApk(context, arquivo)



                if (!abriuInstalador) {



                    erroAtualizacao = "Ative a permissão 'Instalar apps desconhecidos' na tela que abriu e depois toque em 'Instalar atualização agora'."



                    mostrarDialogoAtualizacao = true



                }



            } catch (e: kotlinx.coroutines.CancellationException) {



                throw e



            } catch (e: Throwable) {



                val msg = e.message ?: "Erro ao baixar a atualização."



                erroAtualizacao = msg



                statusAtualizacao = msg



            } finally {



                baixandoAtualizacao = false



            }



        }



    }







    fun verificarAtualizacaoSilenciosa() {



        verificarAtualizacaoGitHub(manual = false)



    }







    fun dispensarUpdate() {



        _updatePendente.value = null



        fecharDialogoAtualizacao()



    }







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
        _salaMutada.value = false
    }

    /**
     * Efetua o logout seguro da conta:
     * 1. Limpa todas as tabelas Room locais (mídias, grupos) para evitar que a próxima conta herde dados antigos.
     * 2. Desconecta o Firebase Auth.
     * 3. Reseta estados da ViewModel.
     */
    fun deslogar(contexto: android.content.Context? = null, onConcluido: () -> Unit = {}) {
        viewModelScope.launch {
            limparEstadoSincronizacao()
            withContext(Dispatchers.IO) {
                repository.limparDadosLocaisAoDeslogar()
            }
            try {
                com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            _casalIdAtivo.value = ""
            _isAdministradorSala.value = false
            _salaMutada.value = false
            withContext(Dispatchers.Main) {
                onConcluido()
            }
        }
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







    private val _provedorSelecionadoQuery = MutableStateFlow<String?>(null)



    val provedorSelecionadoQuery: StateFlow<String?> = _provedorSelecionadoQuery







    private val _filtroMeusStreamingsAtivo = MutableStateFlow(false)



    val filtroMeusStreamingsAtivo: StateFlow<Boolean> = _filtroMeusStreamingsAtivo







    private val _generoSelecionadoId = MutableStateFlow<Int?>(null)



    val generoSelecionadoId: StateFlow<Int?> = _generoSelecionadoId







    private val _ordenacaoSelecionada = MutableStateFlow("popularity.desc")



    val ordenacaoSelecionada: StateFlow<String> = _ordenacaoSelecionada







    @OptIn(ExperimentalCoroutinesApi::class)



    val resultadosBuscaPaginadaApi: Flow<PagingData<TmdbFilme>> = combine(



        _queryPaginada, _tipoPaginado, _provedorSelecionadoQuery, _generoSelecionadoId, _ordenacaoSelecionada



    ) { query, tipo, provedor, genero, ordenacao ->



        repository.buscarNoTmdbPaginado(query, tipo, provedor, genero, ordenacao)



    }.flatMapLatest { flow -> flow }.cachedIn(viewModelScope)







    fun atualizarQueryEFiltrarPaginado(novaQuery: String, novoTipo: String) {



        _queryPaginada.value = novaQuery



        _tipoPaginado.value = novoTipo



    }







    fun selecionarProvedorStreaming(provedorId: Int?) {



        _filtroMeusStreamingsAtivo.value = false



        if (_provedorSelecionadoId.value == provedorId) {



            _provedorSelecionadoId.value = null



        _provedorSelecionadoQuery.value = null



        _filtroMeusStreamingsAtivo.value = false



            _provedorSelecionadoQuery.value = null



        } else {



            _provedorSelecionadoId.value = provedorId



            _provedorSelecionadoQuery.value = provedorId?.toString()



        }



    }







    fun alternarFiltroMeusStreamings(queryPipe: String?) {



        if (_filtroMeusStreamingsAtivo.value) {



            _filtroMeusStreamingsAtivo.value = false



            _provedorSelecionadoId.value = null



        _provedorSelecionadoQuery.value = null



        _filtroMeusStreamingsAtivo.value = false



            _provedorSelecionadoQuery.value = null



        } else {



            _filtroMeusStreamingsAtivo.value = true



            _provedorSelecionadoId.value = null



        _provedorSelecionadoQuery.value = null



        _filtroMeusStreamingsAtivo.value = false



            _provedorSelecionadoQuery.value = queryPipe



        }



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



        _provedorSelecionadoQuery.value = null



        _filtroMeusStreamingsAtivo.value = false



        _generoSelecionadoId.value = null



        _ordenacaoSelecionada.value = "popularity.desc"



    }







    fun inserir(midia: Midia) {



        viewModelScope.launch {



            _estadoOperacao.value = Resultado.Carregando



            try {



                val usuarioLogado = FirebaseAuth.getInstance().currentUser



                val uid = usuarioLogado?.uid ?: ""



                val nomeAExibir = usuarioLogado?.displayName?.takeIf { it.isNotBlank() } ?: usuarioLogado?.email ?: "Parceiro(a)"



                val identificadorAdicao = "${uid}_${nomeAExibir}"







                val midiaProcessada = if (midia.isCasal && midia.casalId.isNotBlank()) {



                    midia.copy(



                        adicionadoPor = identificadorAdicao,



                        statusSugestao = "PENDENTE"



                    )



                } else {



                    midia.copy(



                        adicionadoPor = identificadorAdicao,



                        statusSugestao = "APROVADO"



                    )



                }







                repository.inserir(midiaProcessada)



                _estadoOperacao.value = Resultado.Sucesso(Unit)



            } catch (e: Exception) {



                _estadoOperacao.value = Resultado.Erro(



                    mensagem = "Erro ao adicionar mídia: ${e.message}",



                    causa = e



                )



            }



        }



    }







    fun aceitarMidiaPendente(midia: Midia, totalMembrosNaSala: Int) {



        viewModelScope.launch {



            val meuUid = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch







            val listaAprovados = midia.uidsAprovados.split(",").filter { it.isNotBlank() }.toMutableSet()



            listaAprovados.add(meuUid)







            val novoUidsAprovados = listaAprovados.joinToString(",")



            val totalDeAprovacoesAtuais = listaAprovados.size + 1







            if (totalDeAprovacoesAtuais >= totalMembrosNaSala) {



                val midiaAprovada = midia.copy(statusSugestao = "APROVADO", uidsAprovados = novoUidsAprovados)



                repository.atualizar(midiaAprovada)



            } else {



                val midiaAtualizada = midia.copy(uidsAprovados = novoUidsAprovados)



                repository.atualizar(midiaAtualizada)



            }



        }



    }







    fun recusarMidiaPendente(midia: Midia) {



        viewModelScope.launch {



            val midiaRecusada = midia.copy(statusSugestao = "RECUSADO")



            repository.atualizar(midiaRecusada)



        }



    }







    fun processarMidiaRecusadaPeloParceiro(midia: Midia, moverParaListaPessoal: Boolean) {



        viewModelScope.launch {



            repository.deletar(midia)



            if (moverParaListaPessoal) {



                val midiaPessoal = midia.copy(



                    id = 0,



                    uuid = UUID.randomUUID().toString(),



                    isCasal = false,



                    casalId = "",



                    statusSugestao = "APROVADO"



                )



                repository.inserir(midiaPessoal)



            }



        }



    }







    fun forcarAprovacaoAdmin(midia: Midia) {



        viewModelScope.launch {



            val midiaAprovada = midia.copy(statusSugestao = "APROVADO")



            repository.atualizar(midiaAprovada)



        }



    }







    fun retirarSugestaoPendente(midia: Midia) {



        viewModelScope.launch {



            repository.deletar(midia)



            val meuUid = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch



            val midiaPessoal = midia.copy(



                id = 0,



                uuid = java.util.UUID.randomUUID().toString(),



                isCasal = false,



                casalId = "",



                statusSugestao = "APROVADO",



                status = StatusMidia.QUERO_ASSISTIR.valor



            )



            firestore.collection("usuarios").document(meuUid)



                .collection("midias").document()



                .set(midiaPessoal)



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







            try {



                socialRepository.publicarAtividade(



                    tipo = "ASSISTIU",



                    tmdbId = midia.idTmdb,



                    tituloMidia = midia.titulo,



                    posterPath = midia.imagemCapa,



                    tipoMidia = midia.tipo,



                    nota = midia.nota.toFloat(),



                    comentario = ""



                )



            } catch (ePub: Exception) {



                ePub.printStackTrace()



            }



        }



    }







    fun enviarAnexoAmigo(amigoUid: String, uri: android.net.Uri, onConcluido: (Boolean) -> Unit) {



        viewModelScope.launch {



            val sucesso = socialRepository.enviarAnexoChat(amigoUid, uri)



            onConcluido(sucesso)



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



            horas > 100 -> "Você é uma verdadeira lenda das maratonas!"



            horas > 50 -> "Sua lista está recheada de ótimas histórias!"



            else -> "Sua jornada cinéfila está apenas começando!"



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







    private val _comentariosPublicosAtivos = MutableStateFlow<List<ComentarioSocial>>(emptyList())



    val comentariosPublicosAtivos = _comentariosPublicosAtivos.asStateFlow()







    fun carregarComentariosMidia(tmdbId: Int) {



        viewModelScope.launch {



            socialRepository.observarComentariosMidia(tmdbId).collect { lista ->



                _comentariosPublicosAtivos.value = lista



            }



        }



    }







    fun enviarComentarioPublico(tmdbId: Int, texto: String) {



        viewModelScope.launch {



            socialRepository.adicionarComentarioPublico(tmdbId, texto)



        }



    }







    fun curtirComentarioPublico(tmdbId: Int, comentarioId: String, curtiu: Boolean) {



        viewModelScope.launch {



            socialRepository.curtirComentario(tmdbId, comentarioId, curtiu)



        }



    }







    fun responderComentarioPublico(tmdbId: Int, comentarioId: String, resposta: String) {



        viewModelScope.launch {



            socialRepository.adicionarRespostaComentario(tmdbId, comentarioId, resposta)



        }



    }







    fun deletarComentarioPublico(tmdbId: Int, comentarioId: String) {



        viewModelScope.launch {



            socialRepository.deletarComentarioPublico(tmdbId, comentarioId)



        }



    }







    fun editarComentarioPublico(tmdbId: Int, comentarioId: String, novoTexto: String) {



        viewModelScope.launch {



            socialRepository.editarComentarioPublico(tmdbId, comentarioId, novoTexto)



        }



    }







    // =========================================================================



    // CINEPARTY SINCRONIZADO EM TEMPO REAL (ESTILO SALA / DISCORD)



    // =========================================================================







    private val _sessaoCinePartyAtiva = MutableStateFlow<CinePartySessao?>(null)



    val sessaoCinePartyAtiva: StateFlow<CinePartySessao?> = _sessaoCinePartyAtiva.asStateFlow()







    private var jobObservarCineParty: Job? = null







    fun conectarCineParty(salaId: String) {



        if (salaId.isBlank()) return



        jobObservarCineParty?.cancel()



        jobObservarCineParty = viewModelScope.launch {



            socialRepository.observarSessaoCineParty(salaId).collect { sessao ->



                _sessaoCinePartyAtiva.value = sessao



            }



        }



    }







    fun desconectarCineParty(salaId: String) {



        jobObservarCineParty?.cancel()



        jobObservarCineParty = null



        viewModelScope.launch {



            socialRepository.sairCineParty(salaId)



            _sessaoCinePartyAtiva.value = null



        }



    }







    fun iniciarOuEntrarCineParty(salaId: String, tituloMidia: String, posterMidia: String, onConcluido: ((Boolean) -> Unit)? = null) {



        viewModelScope.launch {



            val res = socialRepository.iniciarOuEntrarCineParty(salaId, tituloMidia, posterMidia)



            if (res.isSuccess) {



                conectarCineParty(salaId)



                onConcluido?.invoke(true)



            } else {



                onConcluido?.invoke(false)



            }



        }



    }







    fun dispararContagemCineParty(salaId: String) {



        viewModelScope.launch {



            socialRepository.dispararContagemCineParty(salaId)



        }



    }







    fun atualizarContagemPassoCineParty(salaId: String, numero: Int) {



        viewModelScope.launch {



            socialRepository.atualizarContagemPassoCineParty(salaId, numero)



        }



    }







    fun alternarPauseCineParty(salaId: String, pausar: Boolean, segundosAtuais: Int) {



        viewModelScope.launch {



            socialRepository.alternarPauseCineParty(salaId, pausar, segundosAtuais)



        }



    }







    fun enviarReacaoCineParty(salaId: String, emoji: String) {



        viewModelScope.launch {



            socialRepository.enviarReacaoCineParty(salaId, emoji)



        }



    }







    // ==========================================



    // 🏆 RANKING SEMANAL DE AMIGOS



    // ==========================================



    private val _rankingSemanal = MutableStateFlow<List<ItemLeaderboard>>(emptyList())



    val rankingSemanal: StateFlow<List<ItemLeaderboard>> = _rankingSemanal.asStateFlow()



    private val _carregandoRanking = MutableStateFlow(false)



    val carregandoRanking: StateFlow<Boolean> = _carregandoRanking.asStateFlow()







    fun carregarRankingSemanal(minhasMidias: List<Midia>) {



        viewModelScope.launch {



            _carregandoRanking.value = true



            try {



                _rankingSemanal.value = socialRepository.obterRankingSemanal(minhasMidias)



            } finally {



                _carregandoRanking.value = false



            }



        }



    }







    // ==========================================



    // 🕵️ CINEBLIND (AMIGO OCULTO DE FILMES NA SALA)



    // ==========================================



    @OptIn(ExperimentalCoroutinesApi::class)



    val sessaoCineBlindAtiva: Flow<CineBlindSessao?> = _casalIdAtivo.flatMapLatest { grupoId ->



        if (grupoId.isBlank()) kotlinx.coroutines.flow.flowOf(null)



        else socialRepository.observarCineBlindSala(grupoId)



    }







    fun iniciarCineBlind(grupoId: String, onConcluido: ((String) -> Unit)? = null) {



        viewModelScope.launch {



            val id = socialRepository.iniciarCineBlind(grupoId)



            onConcluido?.invoke(id)



        }



    }







    fun adicionarSugestaoBlind(



        grupoId: String,



        sessaoId: String,



        tituloReal: String,



        posterReal: String,



        idTmdb: Int,



        genero: String,



        ano: String,



        pistas: List<String>,



        onConcluido: ((Boolean) -> Unit)? = null



    ) {



        viewModelScope.launch {



            val res = socialRepository.adicionarSugestaoBlind(



                grupoId, sessaoId, tituloReal, posterReal, idTmdb, genero, ano, pistas



            )



            onConcluido?.invoke(res)



        }



    }







    fun avancarParaVotacaoCineBlind(grupoId: String, sessaoId: String) {



        viewModelScope.launch {



            socialRepository.avancarParaVotacaoCineBlind(grupoId, sessaoId)



        }



    }







    fun votarCineBlind(grupoId: String, sessaoId: String, sugestaoId: String) {



        viewModelScope.launch {



            socialRepository.votarCineBlind(grupoId, sessaoId, sugestaoId)



        }



    }







    fun revelarVencedorCineBlind(grupoId: String, sessaoId: String) {



        viewModelScope.launch {



            socialRepository.revelarVencedorCineBlind(grupoId, sessaoId)



        }



    }







    fun encerrarCineBlind(grupoId: String, sessaoId: String) {



        viewModelScope.launch {



            socialRepository.encerrarCineBlind(grupoId, sessaoId)



        }



    }







    // ==========================================



    // 🔔 RADAR DE ESTREIAS NOS STREAMINGS



    // ==========================================



    private val _alertasRadarStreaming = MutableStateFlow<List<AlertaRadarStreaming>>(emptyList())



    val alertasRadarStreaming: StateFlow<List<AlertaRadarStreaming>> = _alertasRadarStreaming.asStateFlow()







    fun carregarAlertasRadar(context: Context) {



        _alertasRadarStreaming.value = RadarStreamingManager.obterAlertas(context)



    }







    fun alternarAlertaRadar(context: Context, alerta: AlertaRadarStreaming, onResultado: (Boolean) -> Unit) {



        val adicionou = RadarStreamingManager.alternarAlerta(context, alerta)



        _alertasRadarStreaming.value = RadarStreamingManager.obterAlertas(context)



        onResultado(adicionou)



    }







    fun sincronizarAlertasRadar(context: Context) {



        viewModelScope.launch {



            _alertasRadarStreaming.value = RadarStreamingManager.sincronizarTodosAlertas(context)



        }



    }







    // ==========================================



    // 👥 CINEMATCH COLETIVO (SWIPE EM GRUPO NA SALA)



    // ==========================================



    @OptIn(ExperimentalCoroutinesApi::class)



    val sessaoMatchColetivoAtiva: Flow<MatchColetivoSessao?> = _casalIdAtivo.flatMapLatest { grupoId ->



        if (grupoId.isBlank()) kotlinx.coroutines.flow.flowOf(null)



        else socialRepository.observarMatchColetivoSala(grupoId)



    }







    fun iniciarMatchColetivo(grupoId: String, filmes: List<FilmeCandidatoMatch>, onConcluido: ((Boolean) -> Unit)? = null) {



        viewModelScope.launch {



            val ok = socialRepository.iniciarMatchColetivo(grupoId, filmes)



            onConcluido?.invoke(ok)



        }



    }







    fun votarMatchColetivo(grupoId: String, idTmdb: Int, deuMatch: Boolean) {



        viewModelScope.launch {



            socialRepository.votarMatchColetivo(grupoId, idTmdb, deuMatch)



        }



    }







    fun encerrarMatchColetivo(grupoId: String, onConcluido: (() -> Unit)? = null) {



        viewModelScope.launch {



            socialRepository.encerrarMatchColetivo(grupoId)



            onConcluido?.invoke()



        }



    }



}



