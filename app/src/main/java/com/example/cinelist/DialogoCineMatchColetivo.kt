package com.example.cinelist

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.SubcomposeAsyncImage
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@Composable
fun DialogoCineMatchColetivo(
    grupoId: String,
    nomeGrupo: String,
    midiasGrupo: List<Midia> = emptyList(),
    membrosSala: List<MembroGrupo> = emptyList(),
    viewModel: MidiaViewModel,
    onIniciarCineParty: ((String, String) -> Unit)? = null,
    onDispensar: () -> Unit
) {
    val contexto = LocalContext.current
    val escopo = rememberCoroutineScope()
    val meuUid = remember { FirebaseAuth.getInstance().currentUser?.uid ?: "" }

    val sessaoAtiva by viewModel.sessaoMatchColetivoAtiva.collectAsState(initial = null)
    var indiceFilmeAtual by remember { mutableIntStateOf(0) }

    Dialog(onDismissRequest = onDispensar) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF0F172A),
            border = BorderStroke(2.dp, Color(0xFFEC4899).copy(alpha = 0.6f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEC4899).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("👥", fontSize = 18.sp)
                        }
                        Column {
                            Text(
                                text = "CINEMATCH COLETIVO".t(),
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp,
                                color = Color(0xFFEC4899)
                            )
                            Text(
                                text = "${"Sala".t()}: $nomeGrupo",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    IconButton(onClick = onDispensar) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Fechar".t(), tint = Color.White)
                    }
                }

                // Validação de Sala Selecionada
                if (grupoId.isBlank()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("👥", fontSize = 44.sp)
                        Text(
                            text = "Sala Compartilhada Necessária".t(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "O CineMatch Coletivo funciona exclusivamente em Salas Compartilhadas com amigos. Selecione ou crie uma sala primeiro!".t(),
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                        Button(
                            onClick = onDispensar,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEC4899)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                        ) {
                            Text("Entendi".t(), fontWeight = FontWeight.Bold)
                        }
                    }
                }
                // Estado 1: Nenhuma sessão ativa na sala
                else if (sessaoAtiva == null || sessaoAtiva?.estado == "FINALIZADO") {
                    val amigosOnline = remember(membrosSala) {
                        membrosSala.filter { it.uid != meuUid && it.estaRealmenteOnline }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "🍿",
                            fontSize = 44.sp
                        )
                        Text(
                            text = "Nenhuma rodada de CineMatch ativa no momento.".t(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Para iniciar o CineMatch Coletivo, pelo menos um amigo precisa estar online na sala com você!".t(),
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )

                        // Status dos amigos online
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (amigosOnline.isNotEmpty()) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFEF4444).copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, if (amigosOnline.isNotEmpty()) Color(0xFF10B981) else Color(0xFFEF4444).copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(if (amigosOnline.isNotEmpty()) "🟢" else "🔴", fontSize = 12.sp)
                                Text(
                                    text = if (amigosOnline.isNotEmpty())
                                        "${"Amigos online na sala".t()}: ${amigosOnline.joinToString { it.nome }}"
                                    else
                                        "Nenhum amigo está online nesta sala agora.".t(),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (amigosOnline.isNotEmpty()) Color(0xFF10B981) else Color(0xFFFCA5A5)
                                )
                            }
                        }

                        if (amigosOnline.isEmpty()) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.enviarMensagemParaSala("📢 " + "Galera, entrem no CineList para jogarmos CineMatch Coletivo!".t())
                                    android.widget.Toast.makeText(contexto, "Aviso enviado no chat da sala!".t(), android.widget.Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("📢 " + "Chamar Amigos no Chat da Sala".t())
                            }
                        }
                        Text(
                            text = "🍿",
                            fontSize = 44.sp
                        )
                        Text(
                            text = "Nenhuma rodada de CineMatch ativa no momento.".t(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Inicie uma rodada com os filmes da sala ou sugestões populares para todos darem match juntos!".t(),
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )

                        Button(
                            onClick = {
                                if (amigosOnline.isEmpty()) {
                                    android.widget.Toast.makeText(contexto, "⚠️ Pelo menos 1 amigo precisa estar online na sala para iniciar o CineMatch!", android.widget.Toast.LENGTH_LONG).show()
                                    return@Button
                                }
                                val candidatos = if (midiasGrupo.isNotEmpty()) {
                                    midiasGrupo.take(10).map {
                                        FilmeCandidatoMatch(
                                            idTmdb = it.idTmdb,
                                            titulo = it.titulo,
                                            posterUrl = it.imagemCapa,
                                            ano = it.dataLancamento.take(4),
                                            genero = it.genero,
                                            sinopse = it.sinopse,
                                            nota = it.nota.toDouble()
                                        )
                                    }
                                } else {
                                    listOf(
                                        FilmeCandidatoMatch(872585, "Oppenheimer", "https://image.tmdb.org/t/p/w500/8Gxv8gSFCU0XGDykEGv7zR1n2ua.jpg", "2023", "Drama/História", "A história do físico J. Robert Oppenheimer e o Projeto Manhattan.", 8.4),
                                        FilmeCandidatoMatch(693134, "Duna: Parte 2", "https://image.tmdb.org/t/p/w500/8b8R8l88Qje9dn9OE8PY05Nxl1X.jpg", "2024", "Sci-Fi/Aventura", "Paul Atreides se une a Chani e aos Fremen em busca de vingança.", 8.5),
                                        FilmeCandidatoMatch(157336, "Interestelar", "https://image.tmdb.org/t/p/w500/gEU2QniE6E77NI6lCU6MxlNBvIx.jpg", "2014", "Sci-Fi/Drama", "Um grupo de exploradores viaja através de um buraco de minhoca no espaço.", 8.7),
                                        FilmeCandidatoMatch(496243, "Parasita", "https://image.tmdb.org/t/p/w500/7IiTTgloJzvGI1TAYymCfbfl3vT.jpg", "2019", "Comédia/Suspense", "Toda a família de Ki-taek está desempregada, até o filho conseguir emprego como tutor.", 8.5)
                                    )
                                }
                                viewModel.iniciarMatchColetivo(grupoId, candidatos)
                                viewModel.enviarMensagemParaSala(
                                    texto = "👥 " + "Iniciei uma rodada de CineMatch Coletivo! Abra a sala para votar juntos!".t(),
                                    tipoMensagem = "CINEMATCH_CONVITE"
                                )
                                android.widget.Toast.makeText(contexto, "Rodada iniciada! Convite enviado para a sala.".t(), android.widget.Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEC4899)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                        ) {
                            Text("Iniciar Rodada de Swipe Coletivo 🚀".t(), fontWeight = FontWeight.Bold)
                        }
                    }
                }
                // Estado 2: MATCH ENCONTRADO! 🎉
                else if (sessaoAtiva?.estado == "MATCH") {
                    val vencedor = sessaoAtiva!!
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            color = Color(0xFF10B981).copy(alpha = 0.2f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFF10B981))
                        ) {
                            Text(
                                text = "🎉 " + "MATCH COLETIVO DA SALA!".t(),
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF10B981)
                            )
                        }

                        if (vencedor.vencedorPoster.isNotBlank()) {
                            SubcomposeAsyncImage(
                                model = vencedor.vencedorPoster,
                                contentDescription = vencedor.vencedorTitulo,
                                modifier = Modifier
                                    .width(150.dp)
                                    .height(220.dp)
                                    .clip(RoundedCornerShape(14.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }

                        Text(
                            text = vencedor.vencedorTitulo,
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "A maioria dos membros da sala aprovou este título! Bora dar o play?".t(),
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8),
                            textAlign = TextAlign.Center
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    onDispensar()
                                    onIniciarCineParty?.invoke(vencedor.vencedorTitulo, vencedor.vencedorPoster)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("CineParty!".t(), fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    viewModel.encerrarMatchColetivo(grupoId)
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Finalizar".t())
                            }
                        }
                    }
                }
                // Estado 3: ATIVO - Swipe dos filmes
                else {
                    val sessao = sessaoAtiva!!
                    val filmes = sessao.filmes

                    if (indiceFilmeAtual >= filmes.size) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("⏳", fontSize = 38.sp)
                            Text("Você avaliou todas as opções!", fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Aguardando os outros membros da sala votarem...", fontSize = 12.sp, color = Color(0xFF94A3B8))

                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = { indiceFilmeAtual = 0 },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155))
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Rever Filmes")
                            }
                        }
                    } else {
                        val filmeAtual = filmes[indiceFilmeAtual]
                        val votosDesteFilme = sessao.votosSim[filmeAtual.idTmdb.toString()] ?: emptyList()
                        val euVoteiSim = votosDesteFilme.contains(meuUid)

                        // Progresso
                        Text(
                            text = "Opção ${indiceFilmeAtual + 1} de ${filmes.size}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF94A3B8)
                        )

                        // Card do Filme com Poster
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                            border = BorderStroke(1.dp, if (euVoteiSim) Color(0xFF10B981) else Color(0xFF334155)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (filmeAtual.posterUrl.isNotBlank()) {
                                    SubcomposeAsyncImage(
                                        model = filmeAtual.posterUrl,
                                        contentDescription = filmeAtual.titulo,
                                        modifier = Modifier
                                            .width(140.dp)
                                            .height(200.dp)
                                            .clip(RoundedCornerShape(12.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                }

                                Text(
                                    text = filmeAtual.titulo,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 16.sp,
                                    color = Color.White,
                                    textAlign = TextAlign.Center
                                )

                                Text(
                                    text = "${filmeAtual.ano} • ${filmeAtual.genero} • ⭐ ${filmeAtual.nota}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF38BDF8)
                                )

                                if (filmeAtual.sinopse.isNotBlank()) {
                                    Text(
                                        text = filmeAtual.sinopse,
                                        fontSize = 11.sp,
                                        color = Color(0xFF94A3B8),
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center
                                    )
                                }

                                // Indicador de votos da galera
                                if (votosDesteFilme.isNotEmpty()) {
                                    Surface(
                                        color = Color(0xFFEC4899).copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "❤️ ${votosDesteFilme.size} amigo(s) votaram SIM",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFEC4899)
                                        )
                                    }
                                }
                            }
                        }

                        // Botões de Ação Swipe (Passo vs Quero Assistir!)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Botão Não / Passo
                            FilledIconButton(
                                onClick = {
                                    viewModel.votarMatchColetivo(grupoId, filmeAtual.idTmdb, false)
                                    indiceFilmeAtual++
                                },
                                modifier = Modifier.size(56.dp),
                                colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFFEF4444).copy(alpha = 0.2f))
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Passo", tint = Color(0xFFEF4444), modifier = Modifier.size(28.dp))
                            }

                            // Botão Sim / Quero Ver!
                            FilledIconButton(
                                onClick = {
                                    viewModel.votarMatchColetivo(grupoId, filmeAtual.idTmdb, true)
                                    indiceFilmeAtual++
                                },
                                modifier = Modifier.size(64.dp),
                                colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFF10B981))
                            ) {
                                Icon(Icons.Default.Favorite, contentDescription = "Quero Ver", tint = Color.White, modifier = Modifier.size(32.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
