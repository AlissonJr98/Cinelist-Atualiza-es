package com.example.cinelist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaNotificacoes(
    viewModel: MidiaViewModel,
    onVoltar: () -> Unit
) {
    val notificacoes by viewModel.todasNotificacoes.collectAsState(initial = emptyList())
    var notificacaoParaExcluir by remember { mutableStateOf<NotificacaoEntity?>(null) }
    var mostrarConfirmacaoLimparTudo by remember { mutableStateOf(false) }

    // Diálogo de confirmação para exclusão única
    notificacaoParaExcluir?.let { notif ->
        AlertDialog(
            onDismissRequest = { notificacaoParaExcluir = null },
            title = { Text("Excluir Notificação", fontWeight = FontWeight.Bold) },
            text = { Text("Deseja realmente remover esta notificação do histórico?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deletarNotificacao(notif)
                        notificacaoParaExcluir = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Excluir", color = MaterialTheme.colorScheme.onError)
                }
            },
            dismissButton = {
                TextButton(onClick = { notificacaoParaExcluir = null }) {
                    Text("Cancelar")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    // Diálogo de confirmação para limpar tudo
    if (mostrarConfirmacaoLimparTudo) {
        AlertDialog(
            onDismissRequest = { mostrarConfirmacaoLimparTudo = false },
            title = { Text("Limpar Histórico", fontWeight = FontWeight.Bold) },
            text = { Text("Todas as notificações de atualizações e avisos serão apagadas.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.limparTodasNotificacoes()
                        mostrarConfirmacaoLimparTudo = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Limpar Tudo", color = MaterialTheme.colorScheme.onError)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarConfirmacaoLimparTudo = false }) {
                    Text("Cancelar")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notificações e Novidades", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onVoltar) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    if (notificacoes.isNotEmpty()) {
                        IconButton(onClick = { mostrarConfirmacaoLimparTudo = true }) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Limpar Todas",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                    navigationIconContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        if (notificacoes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Nenhuma notificação registrada",
                        color = MaterialTheme.colorScheme.secondary,
                        fontSize = 15.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(items = notificacoes, key = { it.id }) { notif ->
                    ItemNotificacaoCard(
                        notificacao = notif,
                        onMarcarLida = { viewModel.marcarNotificacaoComoLida(notif.id) },
                        onExcluir = { notificacaoParaExcluir = notif }
                    )
                }
            }
        }
    }
}

@Composable
fun ItemNotificacaoCard(
    notificacao: NotificacaoEntity,
    onMarcarLida: () -> Unit,
    onExcluir: () -> Unit
) {
    val dataFormatada = remember(notificacao.dataCriacao) {
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(notificacao.dataCriacao))
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (notificacao.lida) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onMarcarLida() }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (notificacao.tipo == "ATUALIZACAO") Icons.Default.SystemUpdate else Icons.Default.Notifications,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = notificacao.titulo,
                        fontWeight = if (notificacao.lida) FontWeight.SemiBold else FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(onClick = onExcluir, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Excluir",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = notificacao.mensagem,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = dataFormatada,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}