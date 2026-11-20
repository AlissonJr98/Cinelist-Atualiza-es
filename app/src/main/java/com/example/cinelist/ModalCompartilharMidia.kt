package com.example.cinelist

import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModalCompartilharMidia(
    dados: DadosCompartilhamentoMidia,
    midiaOriginal: Midia?,
    viewModel: MidiaViewModel,
    onDispensar: () -> Unit
) {
    val contexto = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val casalIdAtivo by viewModel.casalIdAtivo.collectAsState()

    var gerandoCard by remember { mutableStateOf(false) }
    var posterBitmapCache by remember { mutableStateOf<Bitmap?>(null) }
    var uriCardCache by remember { mutableStateOf<Uri?>(null) }

    var incluirComentario by remember { mutableStateOf(dados.comentario.isNotBlank()) }

    val dadosEfetivos = remember(dados, incluirComentario) {
        if (incluirComentario) dados else dados.copy(comentario = "")
    }

    // Pré-carrega o pôster da mídia para geração instantânea do CineCard
    LaunchedEffect(dados.urlPoster) {
        if (dados.urlPoster.isNotBlank() && posterBitmapCache == null) {
            posterBitmapCache = MidiaShareHelper.carregarPosterBitmap(contexto, dados.urlPoster)
        }
    }

    suspend fun obterUriCard(): Uri {
        val existente = uriCardCache
        if (existente != null) return existente
        val bmp = posterBitmapCache ?: MidiaShareHelper.carregarPosterBitmap(contexto, dadosEfetivos.urlPoster)
        posterBitmapCache = bmp
        val cardBmp = MidiaShareHelper.gerarCineCardBitmap(contexto, dadosEfetivos, bmp)
        val uri = MidiaShareHelper.salvarBitmapEObterUri(contexto, cardBmp)
        uriCardCache = uri
        return uri
    }

    ModalBottomSheet(
        onDismissRequest = onDispensar,
        containerColor = Color(0xFF0F172A),
        contentColor = Color.White,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF334155))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: Preview da Mídia
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF1E293B),
                border = BorderStroke(1.dp, Color(0xFF334155)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(60.dp)
                            .height(90.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF0B132B)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (dados.urlPoster.isNotBlank()) {
                            AsyncImage(
                                model = ImageRequest.Builder(contexto)
                                    .data(dados.urlPoster)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = dados.titulo,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text("🎬", fontSize = 28.sp)
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF00E5FF).copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = dados.tipo.uppercase(),
                                    color = Color(0xFF00E5FF),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            if (dados.ano.isNotBlank()) {
                                Text(dados.ano, color = Color(0xFF94A3B8), fontSize = 11.sp)
                            }
                            if (dados.temDublagem == true) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFF10B981).copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "🎙️ PT-BR",
                                        color = Color(0xFF10B981),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = dados.titulo,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (dados.generos.isNotBlank()) {
                            Text(
                                text = dados.generos,
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (dados.nota > 0) {
                            val estrelas = "★".repeat(dados.nota.coerceIn(1, 5)) + "☆".repeat((5 - dados.nota).coerceAtLeast(0))
                            Text(
                                text = "⭐ $estrelas (${dados.nota}/5)",
                                color = Color(0xFFFFD700),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        } else if (dados.notaMediaTmdb > 0.0) {
                            Text(
                                text = "⭐ TMDB: ${String.format(java.util.Locale.US, "%.1f", dados.notaMediaTmdb)}/10",
                                color = Color(0xFFFFD700),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Opção de Personalização: Incluir Comentário
            if (dados.comentario.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF151D30),
                    border = BorderStroke(1.dp, Color(0xFF1E293B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                CineHaptics.tick(contexto)
                                incluirComentario = !incluirComentario
                                uriCardCache = null
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Incluir minha opinião/resenha no card".t(),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White
                            )
                            Text(
                                text = "“${if (dados.comentario.length > 40) dados.comentario.take(38) + "..." else dados.comentario}”",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                        Switch(
                            checked = incluirComentario,
                            onCheckedChange = {
                                CineHaptics.tick(contexto)
                                incluirComentario = it
                                uriCardCache = null
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF00E5FF),
                                checkedTrackColor = Color(0xFF00E5FF).copy(alpha = 0.3f)
                            )
                        )
                    }
                }
            }

            // Indicador de Carregamento
            AnimatedVisibility(visible = gerandoCard) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF00E5FF).copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = Color(0xFF00E5FF)
                        )
                        Text(
                            text = "Renderizando CineCard Holográfico em HD...".t(),
                            fontSize = 12.sp,
                            color = Color(0xFF00E5FF),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Text(
                text = "Escolha como deseja compartilhar:".t(),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF00E5FF)
            )

            // 1. Botão Principal: Compartilhamento Geral Universal (Chooser Android)
            Button(
                onClick = {
                    if (gerandoCard) return@Button
                    CineHaptics.click(contexto)
                    coroutineScope.launch {
                        gerandoCard = true
                        try {
                            val uri = obterUriCard()
                            val texto = MidiaShareHelper.gerarTextoFormatado(dadosEfetivos)
                            MidiaShareHelper.compartilharGeral(contexto, texto, uri)
                            onDispensar()
                        } catch (e: Exception) {
                            val texto = MidiaShareHelper.gerarTextoFormatado(dadosEfetivos)
                            MidiaShareHelper.compartilharGeral(contexto, texto, null)
                            onDispensar()
                        } finally {
                            gerandoCard = false
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .bounceOnClick(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00E5FF),
                    contentColor = Color.Black
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Compartilhar com Qualquer App 📱".t(),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // 2. Grid de Opções Específicas
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // WhatsApp
                CardOpcaoCompartilhamento(
                    modifier = Modifier.weight(1f),
                    titulo = "WhatsApp 💬".t(),
                    descricao = "Mensagem & Card".t(),
                    cor = Color(0xFF25D366),
                    icone = {
                        Icon(Icons.Default.Chat, contentDescription = null, tint = Color(0xFF25D366), modifier = Modifier.size(22.dp))
                    },
                    onClick = {
                        if (gerandoCard) return@CardOpcaoCompartilhamento
                        CineHaptics.click(contexto)
                        coroutineScope.launch {
                            gerandoCard = true
                            try {
                                val uri = obterUriCard()
                                val texto = MidiaShareHelper.gerarTextoFormatado(dadosEfetivos)
                                MidiaShareHelper.compartilharWhatsApp(contexto, texto, uri)
                                onDispensar()
                            } catch (e: Exception) {
                                val texto = MidiaShareHelper.gerarTextoFormatado(dadosEfetivos)
                                MidiaShareHelper.compartilharWhatsApp(contexto, texto, null)
                                onDispensar()
                            } finally {
                                gerandoCard = false
                            }
                        }
                    }
                )

                // Instagram Stories
                CardOpcaoCompartilhamento(
                    modifier = Modifier.weight(1f),
                    titulo = "Story 📸".t(),
                    descricao = "Instagram Stories".t(),
                    cor = Color(0xFFEC4899),
                    icone = {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color(0xFFEC4899), modifier = Modifier.size(22.dp))
                    },
                    onClick = {
                        if (gerandoCard) return@CardOpcaoCompartilhamento
                        CineHaptics.click(contexto)
                        coroutineScope.launch {
                            gerandoCard = true
                            try {
                                val uri = obterUriCard()
                                val texto = MidiaShareHelper.gerarTextoFormatado(dadosEfetivos)
                                MidiaShareHelper.compartilharInstagramStories(contexto, uri, texto)
                                onDispensar()
                            } catch (e: Exception) {
                                Toast.makeText(contexto, "Erro ao gerar imagem para o Story.".t(), Toast.LENGTH_SHORT).show()
                            } finally {
                                gerandoCard = false
                            }
                        }
                    }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Apenas Cartão Holográfico (Imagem HD para Status, feed, etc.)
                CardOpcaoCompartilhamento(
                    modifier = Modifier.weight(1f),
                    titulo = "CineCard HD 🎨".t(),
                    descricao = "Apenas Imagem".t(),
                    cor = Color(0xFF7C4DFF),
                    icone = {
                        Icon(Icons.Default.Image, contentDescription = null, tint = Color(0xFFA78BFA), modifier = Modifier.size(22.dp))
                    },
                    onClick = {
                        if (gerandoCard) return@CardOpcaoCompartilhamento
                        CineHaptics.click(contexto)
                        coroutineScope.launch {
                            gerandoCard = true
                            try {
                                val uri = obterUriCard()
                                MidiaShareHelper.compartilharGeral(contexto, "", uri)
                                onDispensar()
                            } catch (e: Exception) {
                                Toast.makeText(contexto, "Erro ao gerar CineCard.".t(), Toast.LENGTH_SHORT).show()
                            } finally {
                                gerandoCard = false
                            }
                        }
                    }
                )

                // Copiar Resumo / Ficha (Área de Transferência)
                CardOpcaoCompartilhamento(
                    modifier = Modifier.weight(1f),
                    titulo = "Copiar Ficha 📋".t(),
                    descricao = "Texto formatado".t(),
                    cor = Color(0xFFF59E0B),
                    icone = {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(22.dp))
                    },
                    onClick = {
                        CineHaptics.click(contexto)
                        val texto = MidiaShareHelper.gerarTextoFormatado(dadosEfetivos)
                        MidiaShareHelper.copiarTexto(contexto, texto)
                        onDispensar()
                    }
                )
            }

            // 3. Opção de Chat da Sala / Amigos do CineList (se conectado)
            if (casalIdAtivo.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF8B5CF6).copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            CineHaptics.click(contexto)
                            val midiaParaEnvio = midiaOriginal ?: Midia(
                                idTmdb = dados.idTmdb,
                                titulo = dados.titulo,
                                imagemCapa = dados.urlPoster,
                                tipo = dados.tipo,
                                genero = dados.generos,
                                dataLancamento = dados.ano
                            )
                            val titLocalizado = MidiaLocalizationManager.obterTituloLocalizado(midiaParaEnvio)
                            val payload = "${midiaParaEnvio.idTmdb}|||${midiaParaEnvio.titulo}|||${midiaParaEnvio.imagemCapa}|||${midiaParaEnvio.tipo}"
                            viewModel.enviarMensagemParaSala(
                                texto = "${"Recomendo assistirmos".t()} \"$titLocalizado\"! 🍿",
                                tipoMensagem = "INDICAÇÃO",
                                mediaUrl = payload
                            )
                            Toast.makeText(contexto, "Indicação enviada no Chat da Sala! 🍿".t(), Toast.LENGTH_SHORT).show()
                            onDispensar()
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF8B5CF6).copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = null,
                                tint = Color(0xFFA78BFA),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Indicar no Chat da Sala Compartilhada 🍿".t(),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Envia card interativo diretamente para os membros da sua sala.".t(),
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CardOpcaoCompartilhamento(
    modifier: Modifier = Modifier,
    titulo: String,
    descricao: String,
    cor: Color,
    icone: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF1E293B),
        border = BorderStroke(1.dp, cor.copy(alpha = 0.35f)),
        modifier = modifier
            .height(82.dp)
            .clickable(onClick = onClick)
            .bounceOnClick()
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(cor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                icone()
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = titulo,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = descricao,
                    fontSize = 10.sp,
                    color = Color(0xFF94A3B8),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
