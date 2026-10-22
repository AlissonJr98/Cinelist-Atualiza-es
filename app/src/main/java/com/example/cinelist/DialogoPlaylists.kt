package com.example.cinelist

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun DialogoPlaylists(
    listaDeMidias: List<Midia> = emptyList(),
    midiaIdTmdbParaAdicionar: Int? = null,
    onMidiaClique: ((Int, String) -> Unit)? = null,
    onDispensar: () -> Unit
) {
    val contexto = LocalContext.current
    var playlists by remember { mutableStateOf(PlaylistsManager.obterPlaylists(contexto)) }
    var criandoNova by remember { mutableStateOf(false) }
    var nomeNova by remember { mutableStateOf("") }
    var descNova by remember { mutableStateOf("") }
    var iconeNova by remember { mutableStateOf("🍿") }

    var playlistSelecionadaId by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDispensar) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF0F172A),
            border = BorderStroke(2.dp, Color(0xFF8B5CF6).copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF8B5CF6).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🏷️", fontSize = 22.sp)
                        }
                        Column {
                            Text(
                                text = "PLAYLISTS CINÉFILAS",
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = Color(0xFFA78BFA)
                            )
                            Text(
                                text = if (midiaIdTmdbParaAdicionar != null) "Selecione para salvar a obra" else "Suas coleções temáticas",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    IconButton(onClick = onDispensar) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Fechar", tint = Color.White)
                    }
                }

                // Botão de Criar Nova Playlist
                if (!criandoNova) {
                    Button(
                        onClick = { criandoNova = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C4DFF)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Criar Nova Playlist Temática", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        border = BorderStroke(1.dp, Color(0xFF8B5CF6))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("Nova Playlist", fontWeight = FontWeight.Bold, color = Color.White)

                            OutlinedTextField(
                                value = nomeNova,
                                onValueChange = { nomeNova = it },
                                label = { Text("Nome da Playlist (ex: Terror Noturno)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = descNova,
                                onValueChange = { descNova = it },
                                label = { Text("Descrição rápida") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("🍿", "🤯", "😭", "🚀", "⚡", "❤️", "👻").forEach { emoji ->
                                    Surface(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .clickable { iconeNova = emoji },
                                        color = if (iconeNova == emoji) Color(0xFF7C4DFF) else Color(0xFF334155)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(emoji, fontSize = 18.sp)
                                        }
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = { criandoNova = false }) {
                                    Text("Cancelar", color = Color(0xFF94A3B8))
                                }
                                Button(
                                    onClick = {
                                        if (nomeNova.isNotBlank()) {
                                            PlaylistsManager.criarPlaylist(contexto, nomeNova, descNova, iconeNova, "#8B5CF6")
                                            playlists = PlaylistsManager.obterPlaylists(contexto)
                                            criandoNova = false
                                            nomeNova = ""
                                            descNova = ""
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                                ) {
                                    Text("Salvar")
                                }
                            }
                        }
                    }
                }

                // Lista de Playlists
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    playlists.forEach { pl ->
                        val contemMidia = midiaIdTmdbParaAdicionar != null && pl.midiaIdsTmdb.contains(midiaIdTmdbParaAdicionar)
                        val cor = try { Color(android.graphics.Color.parseColor(pl.corHex)) } catch (e: Exception) { Color(0xFF7C4DFF) }

                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                            border = BorderStroke(1.dp, if (contemMidia) Color(0xFF10B981) else Color(0xFF334155)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (midiaIdTmdbParaAdicionar != null) {
                                        PlaylistsManager.alternarMidiaNaPlaylist(contexto, pl.id, midiaIdTmdbParaAdicionar)
                                        playlists = PlaylistsManager.obterPlaylists(contexto)
                                    } else {
                                        playlistSelecionadaId = if (playlistSelecionadaId == pl.id) null else pl.id
                                    }
                                }
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(pl.icone, fontSize = 26.sp)
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = pl.nome,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = Color.White
                                        )
                                        if (pl.descricao.isNotBlank()) {
                                            Text(
                                                text = pl.descricao,
                                                fontSize = 11.sp,
                                                color = Color(0xFF94A3B8),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    if (midiaIdTmdbParaAdicionar != null) {
                                        IconButton(
                                            onClick = {
                                                PlaylistsManager.alternarMidiaNaPlaylist(contexto, pl.id, midiaIdTmdbParaAdicionar)
                                                playlists = PlaylistsManager.obterPlaylists(contexto)
                                            }
                                        ) {
                                            if (contemMidia) {
                                                Icon(imageVector = Icons.Default.Check, contentDescription = "Salvo", tint = Color(0xFF10B981))
                                            } else {
                                                Icon(imageVector = Icons.Default.Add, contentDescription = "Adicionar", tint = Color(0xFF94A3B8))
                                            }
                                        }
                                    } else {
                                        Surface(
                                            color = cor.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = "${pl.midiaIdsTmdb.size} obras",
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = cor
                                            )
                                        }
                                    }
                                }

                                // Obras na playlist selecionada
                                if (playlistSelecionadaId == pl.id && midiaIdTmdbParaAdicionar == null) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    val midiasDestaque = listaDeMidias.filter { it.idTmdb in pl.midiaIdsTmdb }
                                    if (midiasDestaque.isEmpty()) {
                                        Text(
                                            text = "Nenhuma obra adicionada ainda nesta playlist.",
                                            fontSize = 11.sp,
                                            color = Color(0xFF64748B)
                                        )
                                    } else {
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            midiasDestaque.forEach { m ->
                                                Text(
                                                    text = "• ${m.titulo} (${m.tipo})",
                                                    fontSize = 12.sp,
                                                    color = Color(0xFFCBD5E1),
                                                    modifier = Modifier.clickable {
                                                        onMidiaClique?.invoke(m.idTmdb, m.tipo)
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
