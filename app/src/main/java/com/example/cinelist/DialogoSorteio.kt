package com.example.cinelist

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun DialogoSorteio(
    listaDeMidias: List<Midia>,
    onDispensar: () -> Unit,
    onSelecionarMidia: (Midia) -> Unit
) {
    var filtroStatus by remember { mutableStateOf("Quero Assistir") }
    var midiaSorteada by remember { mutableStateOf<Midia?>(null) }
    var sortearNovamente by remember { mutableStateOf(0) }
    var animandoRoleta by remember { mutableStateOf(false) }

    val listaCandidatos = remember(listaDeMidias, filtroStatus, sortearNovamente) {
        listaDeMidias.filter { midia ->
            if (filtroStatus == "Todos") true else midia.status.equals(filtroStatus, ignoreCase = true)
        }
    }

    LaunchedEffect(sortearNovamente) {
        if (listaCandidatos.isNotEmpty()) {
            animandoRoleta = true
            // Efeito visual de suspense na roleta
            repeat(8) {
                midiaSorteada = listaCandidatos[Random.nextInt(listaCandidatos.size)]
                delay(80)
            }
            animandoRoleta = false
        } else {
            midiaSorteada = null
        }
    }

    AlertDialog(
        onDismissRequest = onDispensar,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(imageVector = Icons.Default.Casino, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("O Que Assistir Hoje?", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Deixe o CineList sortear o próximo título da sua lista!",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.secondary,
                    textAlign = TextAlign.Center
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf("Quero Assistir", "Assistindo", "Todos").forEach { status ->
                        FilterChip(
                            selected = (filtroStatus == status),
                            onClick = {
                                filtroStatus = status
                                sortearNovamente++
                            },
                            label = { Text(status, fontSize = 11.sp) },
                            modifier = Modifier.padding(horizontal = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (listaCandidatos.isEmpty()) {
                            Text(
                                text = "Nenhum título encontrado com este status.",
                                color = MaterialTheme.colorScheme.secondary,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(16.dp)
                            )
                        } else if (midiaSorteada != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(90.dp)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surface),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (!midiaSorteada!!.imagemCapa.isNullOrBlank()) {
                                        SubcomposeAsyncImage(
                                            model = midiaSorteada!!.imagemCapa,
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Icon(imageVector = Icons.Default.Movie, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = midiaSorteada!!.tipo,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = midiaSorteada!!.titulo,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "🍿 ${midiaSorteada!!.plataforma.ifBlank { "Cinema" }}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
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
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { sortearNovamente++ },
                    modifier = Modifier.weight(1f),
                    enabled = !animandoRoleta && listaCandidatos.isNotEmpty(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Sortear Outro")
                }

                Button(
                    onClick = {
                        midiaSorteada?.let { onSelecionarMidia(it) }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !animandoRoleta && midiaSorteada != null,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Ver Detalhes", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDispensar, modifier = Modifier.fillMaxWidth()) {
                Text("Fechar", color = MaterialTheme.colorScheme.secondary)
            }
        }
    )
}