package com.example.cinelist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel responsável por operações de perfil e amigos.
 */
@HiltViewModel
class PerfilViewModel @Inject constructor(
    val socialRepository: SocialRepository
) : ViewModel() {

    val amigosConectados: Flow<List<AmigoPerfil>> = socialRepository.observarAmigos()
    val solicitacoesRecebidas: Flow<List<SolicitacaoAmizadeRecebida>> = socialRepository.observarSolicitacoesRecebidas()
    val solicitacoesPendentesEnviadas: Flow<List<String>> = socialRepository.observarUidsStatusPendente()
    val usuariosBloqueados: Flow<List<AmigoPerfil>> = socialRepository.observarUsuariosBloqueados()

    private val _resultadosBuscaAmigos = MutableStateFlow<List<AmigoPerfil>>(emptyList())
    val resultadosBuscaAmigos: StateFlow<List<AmigoPerfil>> = _resultadosBuscaAmigos.asStateFlow()

    private val _listaAmigoSelecionado = MutableStateFlow<List<Midia>>(emptyList())
    val listaAmigoSelecionado: StateFlow<List<Midia>> = _listaAmigoSelecionado.asStateFlow()

    private val _comentariosPublicosAtivos = MutableStateFlow<List<ComentarioSocial>>(emptyList())
    val comentariosPublicosAtivos: StateFlow<List<ComentarioSocial>> = _comentariosPublicosAtivos.asStateFlow()

    private var jobBuscaUsuarios: Job? = null

    fun atualizarMeuPerfilPublico(nome: String, bio: String, instagram: String = "", twitter: String = "", letterboxd: String = "") {
        viewModelScope.launch { socialRepository.atualizarPerfilPublico(nome, bio, instagram, twitter, letterboxd) }
    }

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

    fun atualizarPresencaGlobal(online: Boolean) {
        viewModelScope.launch {
            socialRepository.atualizarPresencaGlobal(online)
        }
    }

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
}
