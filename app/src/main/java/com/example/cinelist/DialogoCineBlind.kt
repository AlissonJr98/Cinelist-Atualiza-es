package com.example.cinelist

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@Composable
fun DialogoCineBlind(
    viewModel: MidiaViewModel,
    grupoId: String,
    nomeGrupo: String,
    listaDeMidiasSala: List<Midia>,
    onIniciarCinePartyDoVencedor: (titulo: String, poster: String) -> Unit,
    onDispensar: () -> Unit
) {
    val contexto = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val escopo = rememberCoroutineScope()
    val meuUid = remember { FirebaseAuth.getInstance().currentUser?.uid ?: "" }

    val sessaoBlind by viewModel.sessaoCineBlindAtiva.collectAsState(initial = null)
    val gruposSalvos by viewModel.gruposSalvos.collectAsState(initial = emptyList())

    var mostrarModalAdicionarSugestao by remember { mutableStateOf(false) }
    var inputTitulo by remember { mutableStateOf("") }
    var inputGenero by remember { mutableStateOf("") }
    var inputPista1 by remember { mutableStateOf("") }
    var inputPista2 by remember { mutableStateOf("") }
    var inputPista3 by remember { mutableStateOf("") }
    var midiaSelecionadaIdTmdb by remember { mutableIntStateOf(0) }
    var midiaSelecionadaPoster by remember { mutableStateOf("") }

    val souCriadorDaSessao = sessaoBlind?.criadorUid.isNullOrBlank() || sessaoBlind?.criadorUid == meuUid
    val minhasugestaoJaEnviada = sessaoBlind?.sugestoes?.containsKey(meuUid) == true

    Dialog(
        onDismissRequest = onDispensar,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color(0xFF0F172A),
            border = BorderStroke(1.5.dp, Color(0xFFA855F7).copy(alpha = 0.7f)),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.90f)
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header do CineBlind
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
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFA855F7).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🕵️", fontSize = 20.sp)
                        }
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "CINEBLIND".t(),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFFA855F7),
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFA855F7).copy(alpha = 0.2f),
                                    border = BorderStroke(0.8.dp, Color(0xFFA855F7))
                                ) {
                                    Text(
                                        text = "AMIGO OCULTO".t(),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFFA855F7),
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Text(
                                text = "${"Sessão Secreta na Sala".t()} $nomeGrupo",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    IconButton(onClick = onDispensar, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar", tint = Color(0xFF94A3B8))
                    }
                }

                // CONTEÚDO BASEADO NO ESTADO DA SESSÃO
                if (grupoId.isBlank()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("👥", fontSize = 44.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Sala Compartilhada Necessária".t(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "O CineBlind é um amigo secreto de filmes jogado entre amigos na Sala Compartilhada. Escolha uma de suas salas abaixo:".t(),
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        if (gruposSalvos.isNotEmpty()) {
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(gruposSalvos, key = { it.grupoId }) { g ->
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color(0xFF1E293B),
                                        border = BorderStroke(1.dp, Color(0xFFA855F7).copy(alpha = 0.5f)),
                                        modifier = Modifier.fillMaxWidth().clickable {
                                            viewModel.selecionarGrupoAtivo(g.grupoId)
                                        }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column {
                                                Text(g.nomeGrupo.ifBlank { "Sala Compartilhada".t() }, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                                                Text("Toque para ativar esta sala".t(), fontSize = 11.sp, color = Color(0xFFA855F7))
                                            }
                                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFFA855F7))
                                        }
                                    }
                                }
                            }
                        } else {
                            Button(
                                onClick = onDispensar,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA855F7)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Text("Entendi / Criar Sala".t(), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                } else if (sessaoBlind == null) {
                    // Sem sessão iniciada: convite para abrir rodada
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("🎭", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Nenhuma Sessão Secreta Aberta".t(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "No CineBlind, cada membro da sala sugere um filme secreto com 3 pistas misteriosas. A galera vota nas pistas e o vencedor é desvendado para assistirem juntos!".t(),
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.iniciarCineBlind(grupoId) {
                                    Toast.makeText(contexto, "Rodada do CineBlind aberta!".t(), Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA855F7)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Text("Iniciar Rodada Secreta 🎲".t(), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                } else {
                    val sessao = sessaoBlind!!
                    val sugestoes = remember(sessao.sugestoes) { sessao.obterSugestoesMapeadas() }

                    when (sessao.estado) {
                        "INSCRICAO" -> {
                            // FASE 1: INSCRIÇÃO DE FILMES SECRETOS
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF1E293B),
                                    border = BorderStroke(1.dp, Color(0xFF334155)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = "FASE 1: CADASTRO DE PISTAS".t(),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFFA855F7),
                                            letterSpacing = 1.sp
                                        )
                                        Text(
                                            text = "Cadastre um filme com 3 pistas sem dar spoilers. Os amigos só verão as pistas!".t(),
                                            fontSize = 11.sp,
                                            color = Color(0xFF94A3B8)
                                        )
                                    }
                                }

                                Text(
                                    text = "${"Sugestões misteriosas enviadas".t()} (${sugestoes.size}):",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )

                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    itemsIndexed(sugestoes, key = { index, sug -> "${sug.id.ifBlank { sug.autorUid }}_${sug.idTmdb}_$index" }) { _, sug ->
                                        val souEu = sug.autorUid == meuUid
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (souEu) Color(0xFF2E1065) else Color(0xFF1E293B),
                                            border = BorderStroke(
                                                1.dp,
                                                if (souEu) Color(0xFFA855F7) else Color(0xFF334155)
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                ) {
                                                    Text("🔒", fontSize = 20.sp)
                                                    Column {
                                                        Text(
                                                            text = if (souEu) "${"Sua Sugestão".t()}: ${sug.tituloReal}" else "${"Sugestão Misteriosa de".t()} ${sug.autorNome}",
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 13.sp,
                                                            color = if (souEu) Color(0xFFE9D5FF) else Color.White
                                                        )
                                                        Text(
                                                            text = "${"Gênero:".t()} ${sug.genero} • ${sug.pistas.size} ${"pistas cadastradas".t()}",
                                                            fontSize = 11.sp,
                                                            color = Color(0xFF94A3B8)
                                                        )
                                                    }
                                                }

                                                if (souEu) {
                                                    Text("✅ " + "Pronto".t(), fontSize = 11.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }

                                if (!minhasugestaoJaEnviada) {
                                    Button(
                                        onClick = { mostrarModalAdicionarSugestao = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA855F7)),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Enviar Minha Sugestão Secreta".t(), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }

                                if (sugestoes.size >= 2) {
                                    Button(
                                        onClick = {
                                            viewModel.avancarParaVotacaoCineBlind(grupoId, sessao.id)
                                            Toast.makeText(contexto, "Votação aberta!".t(), Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.HowToVote, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Avançar para Votação das Pistas 🗳️".t(), fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 13.sp)
                                    }
                                }
                            }
                        }

                        "VOTACAO" -> {
                            // FASE 2: VOTAÇÃO PELAS PISTAS
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF1E293B),
                                    border = BorderStroke(1.dp, Color(0xFF334155)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = "FASE 2: VOTE NA MELHOR PISTA".t(),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFF00E5FF),
                                            letterSpacing = 1.sp
                                        )
                                        Text(
                                            text = "Leia as pistas misteriosas abaixo e vote na que mais te chamou atenção!".t(),
                                            fontSize = 11.sp,
                                            color = Color(0xFF94A3B8)
                                        )
                                    }
                                }

                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    itemsIndexed(sugestoes, key = { index, sug -> "${sug.id.ifBlank { sug.autorUid }}_${sug.idTmdb}_$index" }) { _, sug ->
                                        val jaVoteiNessa = sug.votos.contains(meuUid)
                                        Surface(
                                            shape = RoundedCornerShape(16.dp),
                                            color = if (jaVoteiNessa) Color(0xFF164E63) else Color(0xFF1E293B),
                                            border = BorderStroke(
                                                1.5.dp,
                                                if (jaVoteiNessa) Color(0xFF00E5FF) else Color(0xFF334155)
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                        Text("🎭", fontSize = 16.sp)
                                                        Text(
                                                            text = "${"Gênero:".t()} ${sug.genero}",
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 12.sp,
                                                            color = Color(0xFF00E5FF)
                                                        )
                                                    }

                                                    Surface(
                                                        shape = RoundedCornerShape(8.dp),
                                                        color = Color(0xFF0F172A),
                                                        border = BorderStroke(0.8.dp, Color(0xFF334155))
                                                    ) {
                                                        Text(
                                                            text = "${sug.totalVotos} votos",
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color.White,
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }

                                                // As 3 Pistas Enigmáticas
                                                sug.pistas.forEachIndexed { i, pista ->
                                                    Row(
                                                        verticalAlignment = Alignment.Top,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Text("🔍", fontSize = 12.sp)
                                                        Text(
                                                            text = pista,
                                                            fontSize = 12.sp,
                                                            color = Color(0xFFE2E8F0),
                                                            lineHeight = 16.sp
                                                        )
                                                    }
                                                }

                                                Button(
                                                    onClick = {
                                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                        viewModel.votarCineBlind(grupoId, sessao.id, sug.id)
                                                    },
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = if (jaVoteiNessa) Color(0xFF10B981) else Color(0xFF334155)
                                                    ),
                                                    shape = RoundedCornerShape(10.dp),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Icon(
                                                        imageVector = if (jaVoteiNessa) Icons.Default.Check else Icons.Default.HowToVote,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(if (jaVoteiNessa) "Votado! ⭐" else "Votar nesta Opção Secreta", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }

                                Button(
                                    onClick = {
                                        viewModel.revelarVencedorCineBlind(grupoId, sessao.id)
                                        Toast.makeText(contexto, "Filme campeão desvendado!", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA855F7)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Desvendar o Filme Vencedor 🎭", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }

                        "REVELADO" -> {
                            // FASE 3: REVELAÇÃO DO FILME VENCEDOR
                            val vencedora = sessao.vencedora
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .verticalScroll(rememberScrollState()),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Text("🎉 FILME ESCOLHIDO PELA SALA! 🎉", fontWeight = FontWeight.Black, fontSize = 13.sp, color = Color(0xFFF59E0B), letterSpacing = 1.sp)

                                if (vencedora != null) {
                                    // Pôster em Destaque
                                    Surface(
                                        shape = RoundedCornerShape(18.dp),
                                        color = Color(0xFF1E293B),
                                        border = BorderStroke(2.dp, Color(0xFFF59E0B)),
                                        modifier = Modifier.size(width = 160.dp, height = 230.dp)
                                    ) {
                                        if (vencedora.posterReal.isNotBlank()) {
                                            AsyncImage(
                                                model = if (vencedora.posterReal.startsWith("http")) vencedora.posterReal else "https://image.tmdb.org/t/p/w500${vencedora.posterReal}",
                                                contentDescription = vencedora.tituloReal,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text("🎬", fontSize = 48.sp)
                                            }
                                        }
                                    }

                                    Text(
                                        text = vencedora.tituloReal,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 18.sp,
                                        color = Color.White,
                                        textAlign = TextAlign.Center
                                    )

                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFF1E293B),
                                        border = BorderStroke(1.dp, Color(0xFF334155))
                                    ) {
                                        Text(
                                            text = "Sugerido por: ${vencedora.autorNome} • ${vencedora.totalVotos} votos",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFF00E5FF),
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }

                                    // Botão de Ir direto para o CineParty
                                    Button(
                                        onClick = {
                                            onIniciarCinePartyDoVencedor(vencedora.tituloReal, vencedora.posterReal)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth().height(48.dp)
                                    ) {
                                        Text("🍿", fontSize = 16.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Iniciar CineParty Deste Filme Agora!", fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 13.sp)
                                    }
                                } else {
                                    Text("Nenhuma sugestão computada para desvendar nesta rodada.", fontSize = 13.sp, color = Color(0xFF94A3B8), textAlign = TextAlign.Center)
                                }

                                OutlinedButton(
                                    onClick = {
                                        viewModel.encerrarCineBlind(grupoId, sessao.id)
                                        Toast.makeText(contexto, "Rodada encerrada", Toast.LENGTH_SHORT).show()
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Fechar Rodada / Iniciar Outra", fontSize = 12.sp, color = Color(0xFF94A3B8))
                                }
                            }
                        }
                        else -> {
                            Column(
                                modifier = Modifier.fillMaxWidth().weight(1f).padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text("🎭", fontSize = 48.sp)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Rodada Concluída ou Inativa", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Abra uma nova rodada do CineBlind para jogar com a sala!", fontSize = 12.sp, color = Color(0xFF94A3B8), textAlign = TextAlign.Center)
                                Spacer(modifier = Modifier.height(20.dp))
                                Button(
                                    onClick = {
                                        viewModel.iniciarCineBlind(grupoId) {
                                            Toast.makeText(contexto, "Nova rodada iniciada!", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA855F7)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().height(48.dp)
                                ) {
                                    Text("Abrir Nova Rodada 🎲", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal de Cadastro de Sugestão Secreta
    if (mostrarModalAdicionarSugestao && sessaoBlind != null) {
        val sessaoId = sessaoBlind!!.id
        AlertDialog(
            onDismissRequest = { mostrarModalAdicionarSugestao = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("🕵️", fontSize = 20.sp)
                    Text("Minha Sugestão Secreta", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            containerColor = Color(0xFF0F172A),
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Escolha ou digite um filme e dê 3 pistas misteriosas:", fontSize = 12.sp, color = Color(0xFF94A3B8))

                    // Atalho para escolher da lista da sala
                    if (listaDeMidiasSala.isNotEmpty()) {
                        Text("Ou selecione da lista da sala:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00E5FF))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listaDeMidiasSala.take(3).forEach { midia ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (inputTitulo == midia.titulo) Color(0xFFA855F7) else Color(0xFF1E293B),
                                    modifier = Modifier.clickable {
                                        inputTitulo = midia.titulo
                                        inputGenero = midia.genero
                                        midiaSelecionadaIdTmdb = midia.idTmdb
                                        midiaSelecionadaPoster = midia.imagemCapa
                                    }
                                ) {
                                    Text(
                                        text = midia.titulo,
                                        fontSize = 10.sp,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = inputTitulo,
                        onValueChange = { inputTitulo = it },
                        label = { Text("Nome do Filme (só revelado no final)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )

                    OutlinedTextField(
                        value = inputGenero,
                        onValueChange = { inputGenero = it },
                        label = { Text("Gênero (ex: Ficção Científica, Suspense)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )

                    OutlinedTextField(
                        value = inputPista1,
                        onValueChange = { inputPista1 = it },
                        label = { Text("Pista 1 (ex: Acontece no espaço)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )

                    OutlinedTextField(
                        value = inputPista2,
                        onValueChange = { inputPista2 = it },
                        label = { Text("Pista 2 (ex: O tempo passa diferente)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )

                    OutlinedTextField(
                        value = inputPista3,
                        onValueChange = { inputPista3 = it },
                        label = { Text("Pista 3 (ex: Um relógio que salva o mundo)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (inputTitulo.isBlank() || inputPista1.isBlank()) {
                            Toast.makeText(contexto, "Preencha o título e pelo menos uma pista!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val pistas = listOfNotNull(
                            inputPista1.trim().ifBlank { null },
                            inputPista2.trim().ifBlank { null },
                            inputPista3.trim().ifBlank { null }
                        )
                        viewModel.adicionarSugestaoBlind(
                            grupoId = grupoId,
                            sessaoId = sessaoId,
                            tituloReal = inputTitulo.trim(),
                            posterReal = midiaSelecionadaPoster,
                            idTmdb = midiaSelecionadaIdTmdb,
                            genero = inputGenero.ifBlank { "Cinema" },
                            ano = "",
                            pistas = pistas
                        ) { sucesso ->
                            if (sucesso) {
                                mostrarModalAdicionarSugestao = false
                                Toast.makeText(contexto, "Sugestão secreta cadastrada!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(contexto, "Erro ao registrar sugestão", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA855F7))
                ) {
                    Text("Salvar Secreto 🔒", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarModalAdicionarSugestao = false }) {
                    Text("Cancelar", color = Color(0xFF94A3B8))
                }
            }
        )
    }
}
