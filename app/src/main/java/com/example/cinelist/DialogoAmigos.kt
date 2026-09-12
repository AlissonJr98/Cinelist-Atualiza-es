package com.example.cinelist

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogoAmigos(
    viewModel: MidiaViewModel,
    onDispensar: () -> Unit
) {
    val contexto = LocalContext.current
    val amigos by viewModel.amigosConectados.collectAsState(initial = emptyList())
    val resultadosBusca by viewModel.resultadosBuscaAmigos.collectAsState()
    val listaAmigoSelecionado by viewModel.listaAmigoSelecionado.collectAsState()
    val minhaLista by viewModel.todasAsMidias.collectAsState(initial = emptyList())

    var termoPesquisa by remember { mutableStateOf("") }
    var amigoVisualizando by remember { mutableStateOf<AmigoPerfil?>(null) }
    var mostrarDuelo by remember { mutableStateOf(false) }

    // Atualiza o perfil público do usuário atual ao abrir o diálogo para garantir que ele seja encontrado
    LaunchedEffect(Unit) {
        val prefs = contexto.getSharedPreferences("ConfiguracoesPerfil", android.content.Context.MODE_PRIVATE)
        val bio = prefs.getString("bio", "") ?: ""
        val nomeUsuario = FirebaseAuth.getInstance().currentUser?.displayName ?: "Usuário CineList"
        viewModel.atualizarMeuPerfilPublico(nomeUsuario, bio)
    }

    if (mostrarDuelo && amigoVisualizando != null) {
        DialogoComparacaoAmigo(
            amigo = amigoVisualizando!!,
            minhaLista = minhaLista,
            listaDoAmigo = listaAmigoSelecionado,
            onDispensar = { mostrarDuelo = false }
        )
    }

    if (amigoVisualizando != null) {
        AlertDialog(
            onDismissRequest = { amigoVisualizando = null },
            title = {
                Text(
                    text = "Lista de ${amigoVisualizando?.nome}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            },
            containerColor = MaterialTheme.colorScheme.surface,
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 350.dp)
                ) {
                    if (listaAmigoSelecionado.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                            Text("Este usuário ainda não adicionou mídias publicamente.", color = MaterialTheme.colorScheme.secondary, fontSize = 13.sp)
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(listaAmigoSelecionado, key = { it.id }) { midia ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(text = midia.titulo, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                                            Text(text = "${midia.tipo} • ${midia.status}", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { mostrarDuelo = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(imageVector = Icons.Default.CompareArrows, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Comparar Duelo", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    TextButton(onClick = { amigoVisualizando = null }) {
                        Text("Fechar")
                    }
                }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDispensar,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(imageVector = Icons.Default.Group, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Amigos & Listas Compartilhadas", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = termoPesquisa,
                    onValueChange = {
                        termoPesquisa = it
                        viewModel.pesquisarUsuarios(it)
                    },
                    label = { Text("Buscar amigo por nome ou e-mail") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (termoPesquisa.isNotBlank() && resultadosBusca.isNotEmpty()) {
                    Text("Resultados da Busca:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    LazyColumn(modifier = Modifier.heightIn(max = 120.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(resultadosBusca) { usuario ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = usuario.nome, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(text = usuario.email, fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                                }
                                IconButton(
                                    onClick = {
                                        viewModel.adicionarAmigo(usuario.uid) { sucesso ->
                                            if (sucesso) {
                                                Toast.makeText(contexto, "${usuario.nome} adicionado aos amigos!", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(contexto, "Erro ao adicionar amigo.", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Add, contentDescription = "Adicionar", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Text("Meus Amigos Conectados (${amigos.size}):", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                if (amigos.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        Text("Nenhum amigo adicionado ainda. Busque acima!", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                    }
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 180.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(amigos) { amigo ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp))
                                    .clickable {
                                        amigoVisualizando = amigo
                                        viewModel.carregarListaDoAmigo(amigo.uid)
                                    }
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = amigo.nome, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(text = amigo.bio.ifBlank { "Sem biografia" }, fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                Icon(imageVector = Icons.Default.Visibility, contentDescription = "Ver Lista", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDispensar) {
                Text("Fechar", fontWeight = FontWeight.Bold)
            }
        }
    )
}