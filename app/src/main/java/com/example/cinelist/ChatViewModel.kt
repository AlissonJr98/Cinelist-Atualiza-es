package com.example.cinelist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel responsável por operações de chat (mensagens de grupo e entre amigos).
 */
@HiltViewModel
class ChatViewModel @Inject constructor(
    val socialRepository: SocialRepository
) : ViewModel() {

    fun enviarMensagemParaSala(grupoId: String, texto: String) {
        if (grupoId.isNotBlank()) {
            viewModelScope.launch {
                socialRepository.enviarMensagemGrupo(grupoId, texto)
            }
        }
    }

    fun editarMensagemSala(grupoId: String, mensagemId: String, novoTexto: String) {
        if (grupoId.isNotBlank()) {
            viewModelScope.launch {
                socialRepository.editarMensagemGrupo(grupoId, mensagemId, novoTexto)
            }
        }
    }

    fun deletarMensagemSala(grupoId: String, mensagemId: String) {
        if (grupoId.isNotBlank()) {
            viewModelScope.launch {
                socialRepository.deletarMensagemGrupo(grupoId, mensagemId)
            }
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

    fun enviarAnexoAmigo(amigoUid: String, uri: android.net.Uri, onConcluido: (Boolean) -> Unit) {
        viewModelScope.launch {
            val sucesso = socialRepository.enviarAnexoChat(amigoUid, uri)
            onConcluido(sucesso)
        }
    }
}
