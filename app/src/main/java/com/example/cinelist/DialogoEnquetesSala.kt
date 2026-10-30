package com.example.cinelist

import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogoEnquetesSala(
    viewModel: MidiaViewModel,
    listaDeMidiasSala: List<Midia>,
    onDispensar: () -> Unit
) {
    val contexto = LocalContext.current
    val enquetes by viewModel.enquetesSalaAtiva.collectAsState(initial = emptyList())
    val meuUid = remember { FirebaseAuth.getInstance().currentUser?.uid ?: "" }
    var modoCriacao by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDispensar,
        containerColor = MaterialTheme.colorScheme.surface,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .padding(horizontal = 16.dp, vertical = 8.dp)
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
                    Icon(
                        imageVector = Icons.Default.Poll,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (modoCriacao) "Nova Votação".t() else "Votações da Sala".t(),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (!modoCriacao) {
                        FilledTonalButton(
                            onClick = { modoCriacao = true },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Criar".t(), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        TextButton(onClick = { modoCriacao = false }) {
                            Text("Voltar".t(), fontSize = 13.sp)
                        }
                    }
                    IconButton(onClick = onDispensar) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar".t())
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            if (modoCriacao) {
                SecaoCriarEnquete(
                    listaDeMidiasSala = listaDeMidiasSala,
                    onCriar = { titulo, opcoes, duracao ->
                        viewModel.criarEnqueteNaSala(
                            titulo = titulo,
                            descricao = "",
                            opcoes = opcoes,
                            duracaoMinutos = duracao
                        )
                        modoCriacao = false
                        Toast.makeText(contexto, "Votação criada com sucesso!".t(), Toast.LENGTH_SHORT).show()
                    }
                )
            } else {
                if (enquetes.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Poll,
                                contentDescription = null,
                                modifier = Modifier.size(60.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Nenhuma votação ativa na sala".t(),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Crie uma enquete rápida com 2 ou mais títulos para decidirem juntos o que assistir hoje!".t(),
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.secondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(18.dp))
                            Button(
                                onClick = { modoCriacao = true },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Lançar Votação Agora".t())
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(bottom = 20.dp)
                    ) {
                        items(enquetes, key = { it.id }) { enquete ->
                            CardItemEnquete(
                                enquete = enquete,
                                meuUid = meuUid,
                                onVotar = { opcaoId ->
                                    viewModel.votarEnqueteNaSala(enquete.id, opcaoId)
                                },
                                onEncerrar = {
                                    viewModel.encerrarEnqueteNaSala(enquete.id)
                                },
                                onDeletar = {
                                    viewModel.deletarEnqueteNaSala(enquete.id)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CardItemEnquete(
    enquete: EnqueteGrupo,
    meuUid: String,
    onVotar: (String) -> Unit,
    onEncerrar: () -> Unit,
    onDeletar: () -> Unit
) {
    val totalVotos = enquete.votos.size
    val meuVotoOpcaoId = enquete.votos[meuUid]
    val souCriador = enquete.criadorUid == meuUid

    // Conta votos por opção
    val contagemPorOpcao = remember(enquete.votos) {
        val contagem = mutableMapOf<String, Int>()
        enquete.votos.values.forEach { opId ->
            contagem[opId] = (contagem[opId] ?: 0) + 1
        }
        contagem
    }

    val opcaoVencedoraId = remember(contagemPorOpcao) {
        contagemPorOpcao.maxByOrNull { it.value }?.key
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Cabeçalho da Enquete
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        if (enquete.criadorFotoUrl.isNotBlank()) {
                            AsyncImage(
                                model = enquete.criadorFotoUrl,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(
                                text = enquete.criadorNome.take(1).uppercase(),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontSize = 14.sp
                            )
                        }
                    }

                    Column {
                        Text(
                            text = enquete.criadorNome,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (enquete.encerrada) "Votação Encerrada".t() else "${"Votação Aberta".t()} • $totalVotos ${"voto(s)".t()}",
                            fontSize = 11.sp,
                            color = if (enquete.encerrada) Color(0xFFFF5252) else Color(0xFF22C55E),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                if (souCriador) {
                    Row {
                        if (!enquete.encerrada) {
                            TextButton(onClick = onEncerrar) {
                                Text("Encerrar".t(), fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                            }
                        }
                        IconButton(onClick = onDeletar, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Excluir".t(), tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Título
            val tituloEnquete = if (enquete.titulo == "O que vamos assistir hoje?" || enquete.titulo.isBlank()) {
                "O que vamos assistir hoje?".t()
            } else {
                enquete.titulo.t()
            }
            Text(
                text = tituloEnquete,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Opções
                        enquete.opcoes.forEach { opMap ->
                val opId = opMap["id"] as? String ?: ""
                val opTmdbId = (opMap["tmdbId"] as? Number)?.toInt() ?: 0
                val opTituloRaw = opMap["titulo"] as? String ?: "Opção".t()
                val opTitulo = if (opTmdbId > 0) MidiaLocalizationManager.obterTituloLocalizadoPorId(opTmdbId, opTituloRaw) else opTituloRaw.t()
                val posterPathRaw = opMap["posterPath"] as? String ?: ""
                val posterPath = if (opTmdbId > 0) MidiaLocalizationManager.obterCapaLocalizada(opTmdbId, posterPathRaw) else posterPathRaw
                val votosOpcao = contagemPorOpcao[opId] ?: 0
                val percentual = if (totalVotos > 0) (votosOpcao.toFloat() / totalVotos.toFloat()) else 0f
                val votadoPorMim = meuVotoOpcaoId == opId
                val ehVencedor = enquete.encerrada && opId == opcaoVencedoraId && votosOpcao > 0

                val progressoAnimado by animateFloatAsState(targetValue = percentual, label = "progress")

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(enabled = !enquete.encerrada) { onVotar(opId) },
                    color = if (votadoPorMim) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    else MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                    border = if (votadoPorMim || ehVencedor) androidx.compose.foundation.BorderStroke(
                        1.5.dp,
                        if (ehVencedor) Color(0xFFFFB300) else MaterialTheme.colorScheme.primary
                    ) else null
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (posterPath.isNotBlank()) {
                                val url = if (posterPath.startsWith("http")) posterPath else "https://image.tmdb.org/t/p/w200$posterPath"
                                AsyncImage(
                                    model = url,
                                    contentDescription = opTitulo,
                                    modifier = Modifier
                                        .size(width = 36.dp, height = 50.dp)
                                        .clip(RoundedCornerShape(6.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = opTitulo,
                                        fontWeight = if (votadoPorMim || ehVencedor) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (ehVencedor) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("🏆 " + "Vencedor!".t(), fontSize = 11.sp, color = Color(0xFFFFB300), fontWeight = FontWeight.Bold)
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "$votosOpcao ${"voto(s)".t()} • ${(percentual * 100).toInt()}%",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }

                            if (votadoPorMim) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Seu voto".t(),
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Barra de Progresso do Voto
                        LinearProgressIndicator(
                            progress = { progressoAnimado },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = if (ehVencedor) Color(0xFFFFB300) else MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SecaoCriarEnquete(
    listaDeMidiasSala: List<Midia>,
    onCriar: (titulo: String, opcoes: List<Map<String, Any>>, duracaoMinutos: Int) -> Unit
) {
    var tituloInput by remember { mutableStateOf("O que vamos assistir hoje?".t()) }
    var duracaoSelecionada by remember { mutableIntStateOf(60) } // 60 min default
    val opcoesSelecionadas = remember { mutableStateListOf<Map<String, Any>>() }
    var textoOpcaoCustomizada by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 16.dp)
    ) {
        OutlinedTextField(
            value = tituloInput,
            onValueChange = { tituloInput = it },
            label = { Text("Pergunta da Votação".t()) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Adicionar Títulos da Sala:".t(),
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Carrossel de seleção rápida de mídias da sala
        if (listaDeMidiasSala.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(listaDeMidiasSala.take(15)) { midia ->
                    val jaSelecionada = opcoesSelecionadas.any { it["tmdbId"] == midia.idTmdb }
                    val titMidia = MidiaLocalizationManager.obterTituloLocalizado(midia)
                    val capaMidia = MidiaLocalizationManager.obterCapaLocalizada(midia)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (jaSelecionada) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = if (jaSelecionada) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
                        modifier = Modifier.clickable {
                            if (jaSelecionada) {
                                opcoesSelecionadas.removeAll { it["tmdbId"] == midia.idTmdb }
                            } else {
                                if (opcoesSelecionadas.size < 6) {
                                    opcoesSelecionadas.add(
                                        mapOf(
                                            "id" to UUID.randomUUID().toString(),
                                            "titulo" to titMidia,
                                            "posterPath" to capaMidia,
                                            "tmdbId" to midia.idTmdb
                                        )
                                    )
                                }
                            }
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (jaSelecionada) Icons.Default.Check else Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = if (jaSelecionada) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = titMidia,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Adicionar opção manual
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = textoOpcaoCustomizada,
                onValueChange = { textoOpcaoCustomizada = it },
                label = { Text("Ou digite uma opção personalizada".t()) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = RoundedCornerShape(10.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (textoOpcaoCustomizada.isNotBlank() && opcoesSelecionadas.size < 6) {
                        opcoesSelecionadas.add(
                            mapOf(
                                "id" to UUID.randomUUID().toString(),
                                "titulo" to textoOpcaoCustomizada.trim(),
                                "posterPath" to "",
                                "tmdbId" to 0
                            )
                        )
                        textoOpcaoCustomizada = ""
                    }
                },
                enabled = textoOpcaoCustomizada.isNotBlank() && opcoesSelecionadas.size < 6
            ) {
                Icon(Icons.Default.AddCircle, contentDescription = "Adicionar".t(), tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "${"Opções Selecionadas".t()} (${opcoesSelecionadas.size}/6, ${"mínimo 2".t()}):",
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(6.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(opcoesSelecionadas) { opMap ->
                val rawTitulo = opMap["titulo"] as? String ?: ""
                val tmdbId = (opMap["tmdbId"] as? Number)?.toInt() ?: 0
                val titulo = if (tmdbId > 0) MidiaLocalizationManager.obterTituloLocalizadoPorId(tmdbId, rawTitulo) else rawTitulo.t()
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = titulo, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        IconButton(
                            onClick = { opcoesSelecionadas.remove(opMap) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Remover".t(), tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = {
                if (tituloInput.isNotBlank() && opcoesSelecionadas.size >= 2) {
                    onCriar(tituloInput.trim(), opcoesSelecionadas.toList(), duracaoSelecionada)
                }
            },
            enabled = tituloInput.isNotBlank() && opcoesSelecionadas.size >= 2,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Poll, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Publicar Votação para a Sala".t(), fontWeight = FontWeight.Bold)
        }
    }
}
