package com.example.cinelist

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel responsável por operações com grupos/salas compartilhadas.
 */
@HiltViewModel
class GrupoViewModel @Inject constructor(
    private val repository: MidiaRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val prefsMute = context.getSharedPreferences("CineListMutePrefs", Context.MODE_PRIVATE)

    private val _isAdministradorSala = MutableStateFlow(false)
    val isAdministradorSala: StateFlow<Boolean> = _isAdministradorSala.asStateFlow()

    private val _salaMutada = MutableStateFlow(false)
    val salaMutada: StateFlow<Boolean> = _salaMutada.asStateFlow()

    fun carregarConfiguracaoMute(grupoId: String) {
        if (grupoId.isBlank()) return
        val mutado = prefsMute.getBoolean("mute_$grupoId", false)
        _salaMutada.value = mutado

        if (!mutado) {
            FirebaseMessaging.getInstance().subscribeToTopic("grupo_$grupoId")
        } else {
            FirebaseMessaging.getInstance().unsubscribeFromTopic("grupo_$grupoId")
        }
    }

    fun alternarMuteSala(grupoId: String) {
        if (grupoId.isNotBlank()) {
            val novoEstado = !_salaMutada.value
            prefsMute.edit().putBoolean("mute_$grupoId", novoEstado).apply()
            _salaMutada.value = novoEstado

            if (novoEstado) {
                FirebaseMessaging.getInstance().unsubscribeFromTopic("grupo_$grupoId")
            } else {
                FirebaseMessaging.getInstance().subscribeToTopic("grupo_$grupoId")
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

    fun carregarGrupoAtivoInicial(onCarregado: (String?) -> Unit) {
        viewModelScope.launch {
            val grupoAtivo = repository.obterGrupoAtivoLocal()
            onCarregado(grupoAtivo?.grupoId)
        }
    }

    fun selecionarGrupo(
        grupoId: String,
        onConfigurado: () -> Unit
    ) {
        viewModelScope.launch {
            if (grupoId.isNotBlank()) {
                repository.ativarGrupoLocal(grupoId)
                onConfigurado()
            }
        }
    }

    fun entrarEmGrupoExistente(
        grupoId: String,
        nomeGrupo: String,
        tipo: String,
        senhaDigitada: String,
        onResultado: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            val idFormatado = if (grupoId.startsWith("CINE-", ignoreCase = true)) {
                grupoId.trim().uppercase()
            } else {
                "CINE-${grupoId.trim().uppercase().removePrefix("CINE-")}"
            }
            val resultado = repository.verificarEEntrarNoGrupo(idFormatado, nomeGrupo, tipo, senhaDigitada)
            if (resultado.isSuccess) {
                onResultado(true, null)
            } else {
                onResultado(false, resultado.exceptionOrNull()?.localizedMessage ?: "Erro ao entrar no grupo.")
            }
        }
    }

    fun criarGrupoComSenha(
        grupoId: String,
        nomeGrupo: String,
        tipo: String,
        senha: String,
        onResultado: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val idFormatado = if (grupoId.startsWith("CINE-", ignoreCase = true)) {
                grupoId.trim().uppercase()
            } else {
                "CINE-${grupoId.trim().uppercase().removePrefix("CINE-")}"
            }
            val sucesso = repository.criarNovoGrupoNaNuvem(idFormatado, nomeGrupo, tipo, senha)
            if (sucesso) {
                repository.ativarGrupoLocal(idFormatado)
            }
            onResultado(sucesso)
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

    fun excluirGrupoSalvo(grupo: GrupoEntity) {
        viewModelScope.launch {
            repository.deletarGrupoLocal(grupo)
        }
    }

    fun gerarNovoCodigoGrupo(): String = repository.gerarCodigoAleatorio()
}
