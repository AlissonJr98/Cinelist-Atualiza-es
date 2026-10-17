package com.example.cinelist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel responsável por operações com notificações.
 */
@HiltViewModel
class NotificacaoViewModel @Inject constructor(
    private val notificacaoRepository: NotificacaoRepository
) : ViewModel() {

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

    fun limparTodasNotificacoes() {
        viewModelScope.launch { notificacaoRepository.limparTodas() }
    }
}
