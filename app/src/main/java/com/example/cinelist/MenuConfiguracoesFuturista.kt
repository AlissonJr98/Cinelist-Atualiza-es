package com.example.cinelist

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * ====================================================================
 * MENU DE CONFIGURAÇÕES FUTURISTA & CYBERPUNK (CineList 2.0)
 * ====================================================================
 * Design de painel sci-fi com HUD holográfico, chips luminosos,
 * monitor de diagnóstico do sistema, controle avançado de FCM e
 * otimização de armazenamento em tempo real.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomSheetConfiguracoesFuturista(
    onDismiss: () -> Unit,
    viewModel: MidiaViewModel,
    usuarioAtual: FirebaseUser?,
    midiasContextoAtual: List<Midia>,
    onAbrirMeusStreamings: () -> Unit,
    onAbrirGenerosFavoritos: () -> Unit = {},
    onAbrirBloqueados: () -> Unit,
    onAbrirSuporte: () -> Unit,
    onConfirmarReset: () -> Unit,
    onConfirmarSair: () -> Unit,
    onConfirmarExclusaoConta: () -> Unit,
    exportarJsonLauncher: ActivityResultLauncher<String>,
    importarJsonLauncher: ActivityResultLauncher<Array<String>>,
    exportarCsvLauncher: ActivityResultLauncher<String>,
    importarCsvLauncher: ActivityResultLauncher<Array<String>>,
    pickerCsvLauncher: ActivityResultLauncher<String>,
    permissaoNotificacaoLauncher: ActivityResultLauncher<String>
) {
    val contexto = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    val sharedPreferences = remember {
        contexto.getSharedPreferences("ConfiguracoesPerfil", Context.MODE_PRIVATE)
    }

    // Estados de Tema & Preferências
    var modoEscuro by remember { mutableStateOf(sharedPreferences.getBoolean("modo_escuro", true)) }
    var idiomaSelecionado by remember { mutableStateOf(IdiomaAppManager.obterChaveConfigurada(contexto)) }
    var fundoAtual by remember { mutableStateOf(sharedPreferences.getString("cor_fundo", "Padrao") ?: "Padrao") }
    var temaAtual by remember { mutableStateOf(sharedPreferences.getString("cor_tema", "CineList") ?: "CineList") }
    var letraAtual by remember { mutableStateOf(sharedPreferences.getString("cor_texto", "Padrao") ?: "Padrao") }
    var receberNotificacoes by remember { mutableStateOf(sharedPreferences.getBoolean("notificacoes", true)) }
    var generoFavorito by remember { mutableStateOf(sharedPreferences.getString("genero", "Não definido") ?: "Não definido") }
    var menuGeneroExpandido by remember { mutableStateOf(false) }
    val listaGeneros = listOf("Ação", "Animes", "Comédia", "Drama", "Ficção Científica", "Terror", "Suspense", "Romance", "Novelas", "Doramas")

    // Efeitos Visuais & Animações
    var efeitosVisuaisAtivos by remember { mutableStateOf(sharedPreferences.getBoolean("efeitos_visuais", true)) }

    // Diagnóstico de Armazenamento de Cache
    var tamanhoCacheTexto by remember { mutableStateOf("Calculando...") }
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val tam = calcularTamanhoCache(contexto)
            tamanhoCacheTexto = tam
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0C101B),
        scrimColor = Color.Black.copy(alpha = 0.75f),
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .width(48.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFF00E5FF), Color(0xFFFFD700), Color(0xFFFF007F))
                        )
                    )
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentWidth(Alignment.CenterHorizontally)
                .widthIn(max = 640.dp)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ========================================================
            // 1. HUD SUPERIOR FUTURISTA (BARRA DE TÍTULO)
            // ========================================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PulsarPontoNeon(cor = Color(0xFF00FF7F))
                    Column {
                        Text(
                            text = "CONSOLE DO SISTEMA".t(),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            color = Color.White
                        )
                        Text(
                            text = "CINELINK OS • NÚCLEO ATIVO".t(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF00E5FF).copy(alpha = 0.8f)
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Fechar", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }

            // ========================================================
            // 2. CARD HOLOGRÁFICO DE IDENTIDADE DO USUÁRIO
            // ========================================================
            CardHolograficoUsuario(
                usuarioAtual = usuarioAtual,
                onCopiarUid = { uid ->
                    clipboardManager.setText(AnnotatedString(uid))
                    Toast.makeText(contexto, "ID copiado para a área de transferência! 📋", Toast.LENGTH_SHORT).show()
                }
            )

            // ========================================================
            // 3. SEÇÃO: INTERFACE & HOLOGRAFIA (PERSONALIZAÇÃO)
            // ========================================================
            CyberCardSecao(
                titulo = "INTERFACE & APARÊNCIA",
                subtitulo = "Paleta quântica, fundos OLED e tipografia",
                icone = Icons.Default.Palette,
                corDestaque = Color(0xFF00E5FF)
            ) {
                // Switch Modo Escuro
                CyberSwitchItem(
                    titulo = "Tema Escuro / Modo Noturno",
                    subtitulo = "Economia de energia e conforto visual",
                    icone = Icons.Default.NightsStay,
                    checado = modoEscuro,
                    onCheckedChange = { valor ->
                        modoEscuro = valor
                        sharedPreferences.edit().putBoolean("modo_escuro", valor).apply()
                    }
                )

                HorizontalDivider(color = Color.White.copy(alpha = 0.07f))

                // Seletor de Fundo do App
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Ambiente de Fundo (Backdrop)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )

                    val fundosDisponiveis = listOf(
                        "Padrao" to ("Slate Cósmico" to Color(0xFF0D111A)),
                        "Amoled" to ("AMOLED Void" to Color(0xFF000000)),
                        "AzulNoite" to ("Deep Space" to Color(0xFF070C18)),
                        "Grafite" to ("Titanium" to Color(0xFF121316)),
                        "Claro" to ("Estelar (Claro)" to Color(0xFFF8FAFC))
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        fundosDisponiveis.forEach { (chave, dados) ->
                            val (rotulo, corHex) = dados
                            val selecionado = fundoAtual == chave

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.White.copy(alpha = if (selecionado) 0.14f else 0.05f))
                                    .border(
                                        width = if (selecionado) 1.5.dp else 1.dp,
                                        color = if (selecionado) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        fundoAtual = chave
                                        sharedPreferences.edit().putString("cor_fundo", chave).apply()
                                    }
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(corHex)
                                            .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                                    )
                                    Text(
                                        text = rotulo,
                                        fontSize = 12.sp,
                                        fontWeight = if (selecionado) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selecionado) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.8f)
                                    )
                                    if (selecionado) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color(0xFF00E5FF),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.07f))

                // Seletor de Cor de Destaque Neon
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Identidade Visual & Destaques",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )

                    val temasDisponiveis = listOf(
                        "CineList" to ("Cine Gold" to Color(0xFFFFD700)),
                        "Netflix" to ("Cyber Red" to Color(0xFFE50914)),
                        "Max" to ("Nebula" to Color(0xFF8B5CF6)),
                        "Prime" to ("Holo Cyan" to Color(0xFF00E5FF)),
                        "Spotify" to ("Matrix Green" to Color(0xFF10B981)),
                        "Rose" to ("Neon Rose" to Color(0xFFF43F5E))
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        temasDisponiveis.forEach { (chave, dados) ->
                            val (rotulo, corHex) = dados
                            val selecionado = temaAtual == chave

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.White.copy(alpha = if (selecionado) 0.14f else 0.05f))
                                    .border(
                                        width = if (selecionado) 1.5.dp else 1.dp,
                                        color = if (selecionado) corHex else Color.White.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        temaAtual = chave
                                        sharedPreferences.edit().putString("cor_tema", chave).apply()
                                    }
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(corHex)
                                    )
                                    Text(
                                        text = rotulo,
                                        fontSize = 12.sp,
                                        fontWeight = if (selecionado) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selecionado) corHex else Color.White.copy(alpha = 0.8f)
                                    )
                                    if (selecionado) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = corHex,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.07f))

                // Seletor de Contraste e Legibilidade de Texto
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Legibilidade da Tipografia",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )

                    val letrasDisponiveis = listOf(
                        "Padrao" to ("Studio Suave" to Color(0xFFF1F5F9)),
                        "Branco" to ("Alto Contraste" to Color(0xFFFFFFFF)),
                        "Quente" to ("Solar Acolhedor" to Color(0xFFFBF7EE)),
                        "Titanio" to ("Titânio Gelo" to Color(0xFFE2E8F0))
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        letrasDisponiveis.forEach { (chave, dados) ->
                            val (rotulo, corHex) = dados
                            val selecionado = letraAtual == chave

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.White.copy(alpha = if (selecionado) 0.14f else 0.05f))
                                    .border(
                                        width = if (selecionado) 1.5.dp else 1.dp,
                                        color = if (selecionado) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        letraAtual = chave
                                        sharedPreferences.edit().putString("cor_texto", chave).apply()
                                    }
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clip(CircleShape)
                                            .background(corHex)
                                            .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                                    )
                                    Text(
                                        text = rotulo,
                                        fontSize = 12.sp,
                                        fontWeight = if (selecionado) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selecionado) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.9f)
                                    )
                                    if (selecionado) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color(0xFF00E5FF),
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.07f))

                // Gênero Favorito
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "Gênero Predileto", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                        Text(text = "Personaliza recomendações do CineBot IA", fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))
                    }

                    Box {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .border(1.dp, Color(0xFFFFD700).copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .clickable { menuGeneroExpandido = true }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = generoFavorito,
                                color = Color(0xFFFFD700),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(16.dp))
                        }

                        DropdownMenu(
                            expanded = menuGeneroExpandido,
                            onDismissRequest = { menuGeneroExpandido = false },
                            modifier = Modifier
                                .background(Color(0xFF141A29))
                                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        ) {
                            listaGeneros.forEach { item ->
                                DropdownMenuItem(
                                    text = { Text(item, color = Color.White, fontSize = 13.sp) },
                                    onClick = {
                                        generoFavorito = item
                                        sharedPreferences.edit().putString("genero", generoFavorito).apply()
                                        menuGeneroExpandido = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // ========================================================
            // SEÇÃO: IDIOMA & LOCALIZAÇÃO (RECONHECIMENTO AUTOMÁTICO DO DISPOSITIVO)
            // ========================================================
            CyberCardSecao(
                titulo = "IDIOMA & LOCALIZAÇÃO".t(),
                subtitulo = "Reconhecimento do celular e catálogo do TMDB".t(),
                icone = Icons.Default.Language,
                corDestaque = Color(0xFF00FF7F)
            ) {
                // Banner informativo com a língua detectada no celular
                val idiomaDetectadoNome = remember { IdiomaAppManager.obterNomeAmigavelIdiomaDispositivo() }
                val tagEfetiva = remember(idiomaSelecionado) { IdiomaAppManager.obterTagTmdbEfetiva(contexto) }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF00FF7F).copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, Color(0xFF00FF7F).copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF00FF7F).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("📱", fontSize = 18.sp)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Língua Padrão do Celular".t(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00FF7F)
                            )
                            Text(
                                text = idiomaDetectadoNome,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Text(
                                text = if (idiomaSelecionado == "SISTEMA") "Usando automaticamente a língua do dispositivo".t() else "${"Personalizado manualmente para".t()} $tagEfetiva",
                                fontSize = 10.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Selecione o idioma de exibição e busca:".t(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.85f)
                )

                var menuIdiomaExpandido by remember { mutableStateOf(false) }
                val opcaoSelecionada = remember(idiomaSelecionado) {
                    IdiomaAppManager.IDIOMAS_DISPONIVEIS.find { it.codigo == idiomaSelecionado }
                        ?: IdiomaAppManager.IDIOMAS_DISPONIVEIS.first()
                }

                // ========================================================
                // CAIXA DE SELEÇÃO DE IDIOMA (SPINNER / DROPDOWN FUTURISTA)
                // ========================================================
                Box(modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { menuIdiomaExpandido = true },
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF131B2E),
                        border = BorderStroke(
                            width = 1.5.dp,
                            color = if (menuIdiomaExpandido) Color(0xFF00FF7F) else Color(0xFF00FF7F).copy(alpha = 0.4f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(text = opcaoSelecionada.bandeiraEmoji, fontSize = 24.sp)
                                Column {
                                    Text(
                                        text = opcaoSelecionada.nome,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF00FF7F)
                                    )
                                    Text(
                                        text = if (opcaoSelecionada.codigo == "SISTEMA") "Automático ($idiomaDetectadoNome)" else "Código TMDB: ${opcaoSelecionada.tagTmdb}",
                                        fontSize = 11.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                            }
                            Icon(
                                imageVector = if (menuIdiomaExpandido) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                contentDescription = "Abrir caixa de seleção de idioma",
                                tint = Color(0xFF00FF7F),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = menuIdiomaExpandido,
                        onDismissRequest = { menuIdiomaExpandido = false },
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .background(Color(0xFF0C101B))
                            .border(1.dp, Color(0xFF00FF7F).copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                    ) {
                        IdiomaAppManager.IDIOMAS_DISPONIVEIS.forEach { opcao ->
                            val isAtivo = opcao.codigo == idiomaSelecionado
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(text = opcao.bandeiraEmoji, fontSize = 20.sp)
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = opcao.nome,
                                                fontSize = 13.sp,
                                                fontWeight = if (isAtivo) FontWeight.ExtraBold else FontWeight.Medium,
                                                color = if (isAtivo) Color(0xFF00FF7F) else Color.White
                                            )
                                            if (opcao.codigo == "SISTEMA") {
                                                Text(
                                                    text = "Automático ($idiomaDetectadoNome)",
                                                    fontSize = 10.sp,
                                                    color = Color(0xFF94A3B8)
                                                )
                                            } else {
                                                Text(
                                                    text = "Código TMDB: ${opcao.tagTmdb}",
                                                    fontSize = 10.sp,
                                                    color = Color(0xFF64748B)
                                                )
                                            }
                                        }
                                        if (isAtivo) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = "Selecionado",
                                                tint = Color(0xFF00FF7F),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    menuIdiomaExpandido = false
                                    if (idiomaSelecionado != opcao.codigo) {
                                        idiomaSelecionado = opcao.codigo
                                        IdiomaAppManager.salvarPreferenciaEAplicar(contexto, opcao.codigo)
                                        viewModel.carregarCarrosseisDescobrir(forcar = true)
                                        viewModel.carregarFilmesEmCartaz()
                                        viewModel.carregarNovidadesPorStreaming(contexto, forcar = true)
                                        viewModel.carregarCarrosseisGenerosFavoritos(contexto)
                                        Toast.makeText(
                                            contexto,
                                            "Idioma: ${opcao.bandeiraEmoji} ${opcao.nome}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        (contexto as? android.app.Activity)?.recreate()
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // ========================================================
            // 4. SEÇÃO: TRANSMISSÕES & NOTIFICAÇÕES (FCM & PUSH)
            // ========================================================
            CyberCardSecao(
                titulo = "TRANSMISSÕES & NOTIFICAÇÕES",
                subtitulo = "Entrega direta em alta prioridade (FCM High Priority)",
                icone = Icons.Default.NotificationsActive,
                corDestaque = Color(0xFFFFD700)
            ) {
                // Status da Conexão Push
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF00FF7F).copy(alpha = 0.1f))
                        .border(1.dp, Color(0xFF00FF7F).copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF00FF7F), modifier = Modifier.size(18.dp))
                    Text(
                        text = "FCM High Priority Ativo: Entrega garantida mesmo com o app encerrado.",
                        fontSize = 11.sp,
                        color = Color(0xFF00FF7F),
                        fontWeight = FontWeight.Medium
                    )
                }

                // Switch de Notificações
                CyberSwitchItem(
                    titulo = "Alertas de Salas & Amigos",
                    subtitulo = "Mensagens de chat, novas sugestões e enquetes",
                    icone = Icons.Default.Notifications,
                    checado = receberNotificacoes,
                    onCheckedChange = { valor ->
                        if (valor) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                val jaTemPermissao = ContextCompat.checkSelfPermission(contexto, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                                if (jaTemPermissao) {
                                    receberNotificacoes = true
                                    sharedPreferences.edit().putBoolean("notificacoes", true).apply()
                                    configurarLembretes(contexto, true)
                                } else {
                                    permissaoNotificacaoLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            } else {
                                receberNotificacoes = true
                                sharedPreferences.edit().putBoolean("notificacoes", true).apply()
                                configurarLembretes(contexto, true)
                            }
                        } else {
                            receberNotificacoes = false
                            sharedPreferences.edit().putBoolean("notificacoes", false).apply()
                            configurarLembretes(contexto, false)
                        }
                    }
                )

                // Botão de Disparo de Notificação de Teste
                Button(
                    onClick = {
                        NotificacaoHelper.dispararNotificacaoGrupo(
                            context = contexto,
                            titulo = "💬 Teste de Transmissão CineList",
                            autor = "CineBot",
                            midiaTitulo = "Notificação Sonora & Visual Operacional!",
                            idRef = 77777
                        )
                        Toast.makeText(contexto, "Notificação de teste enviada para a barra do sistema! 🚀", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 44.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.08f)),
                    border = BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.4f))
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "TESTAR NOTIFICAÇÃO IMEDIATAMENTE",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFD700)
                    )
                }
            }

            // ========================================================
            // 5. SEÇÃO: CONECTIVIDADE & SERVIÇOS DE STREAMING
            // ========================================================
            CyberCardSecao(
                titulo = "CONECTIVIDADE & SERVIÇOS",
                subtitulo = "Streamings configurados, moderação e canais",
                icone = Icons.Default.Tv,
                corDestaque = Color(0xFF38BDF8)
            ) {
                // Meus Streamings
                val totalStreamings = StreamingManager.obterMeusStreamingsIds(contexto).size
                CyberItemNavegacao(
                    titulo = "Meus Provedores de Streaming",
                    subtitulo = if (totalStreamings > 0) "$totalStreamings plataformas ativas (Netflix, Prime, Max...)" else "Nenhum configurado (toque para escolher)",
                    icone = Icons.Default.Tv,
                    corIcone = Color(0xFF38BDF8),
                    onClick = {
                        onDismiss()
                        onAbrirMeusStreamings()
                    }
                )

                HorizontalDivider(color = Color.White.copy(alpha = 0.07f))

                // Gêneros Favoritos
                val totalGeneros = GeneroPreferencesManager.obterGenerosFavoritosIds(contexto).size
                CyberItemNavegacao(
                    titulo = "Gêneros Favoritos (Explorar)",
                    subtitulo = if (totalGeneros > 0) "$totalGeneros gêneros selecionados" else "Personalize as vitrines da aba Descobrir",
                    icone = Icons.Default.Category,
                    corIcone = Color(0xFFFFD700),
                    onClick = {
                        onDismiss()
                        onAbrirGenerosFavoritos()
                    }
                )

                HorizontalDivider(color = Color.White.copy(alpha = 0.07f))

                // Contas Bloqueadas
                CyberItemNavegacao(
                    titulo = "Gerenciar Contas Bloqueadas",
                    subtitulo = "Privacidade e moderação de perfis",
                    icone = Icons.Default.Block,
                    corIcone = CineListTokens.CorErro,
                    onClick = {
                        onDismiss()
                        onAbrirBloqueados()
                    }
                )

                HorizontalDivider(color = Color.White.copy(alpha = 0.07f))

                // Suporte e Feedback
                CyberItemNavegacao(
                    titulo = "Canal de Suporte & Feedback",
                    subtitulo = "Reportar bugs ou sugerir recursos",
                    icone = Icons.Default.SupportAgent,
                    corIcone = Color(0xFF00FF7F),
                    onClick = {
                        onDismiss()
                        onAbrirSuporte()
                    }
                )
            }

            // ========================================================
            // 6. SEÇÃO: BANCO DE DADOS & IMPORTAÇÃO / EXPORTAÇÃO
            // ========================================================
            CyberCardSecao(
                titulo = "BANCO DE DADOS & BACKUP",
                subtitulo = "Importe do Letterboxd / IMDb e exporte seus dados",
                icone = Icons.Default.CloudSync,
                corDestaque = Color(0xFFB388FF)
            ) {
                // Status do Backup Automático
                val ultimoBackupMillis = sharedPreferences.getLong("ultimo_backup_automatico", 0L)
                val textoUltimoBackup = if (ultimoBackupMillis > 0L) {
                    "Sincronização: ${SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", Locale.getDefault()).format(Date(ultimoBackupMillis))}"
                } else {
                    "Backup semanal automático programado"
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.CloudSync, contentDescription = null, tint = Color(0xFFB388FF), modifier = Modifier.size(18.dp))
                    Text(
                        text = textoUltimoBackup,
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Importar Letterboxd / IMDb (.csv)
                Button(
                    onClick = {
                        onDismiss()
                        pickerCsvLauncher.launch("text/*")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 44.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB388FF).copy(alpha = 0.2f)),
                    border = BorderStroke(1.dp, Color(0xFFB388FF))
                ) {
                    Icon(Icons.Default.FileUpload, contentDescription = null, tint = Color(0xFFB388FF), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "IMPORTAR DO LETTERBOXD / IMDB (.CSV)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // Linha de Botões JSON
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { exportarJsonLauncher.launch("cinelist_backup_${System.currentTimeMillis()}.json") },
                        modifier = Modifier.weight(1f).defaultMinSize(minHeight = 40.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Exportar JSON", fontSize = 11.sp, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }

                    Button(
                        onClick = { importarJsonLauncher.launch(arrayOf("application/json")) },
                        modifier = Modifier.weight(1f).defaultMinSize(minHeight = 40.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF).copy(alpha = 0.25f)),
                        border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.5f))
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Restaurar JSON", fontSize = 11.sp, color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }

                // Linha de Botões CSV
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { exportarCsvLauncher.launch("cinelist_${System.currentTimeMillis()}.csv") },
                        modifier = Modifier.weight(1f).defaultMinSize(minHeight = 40.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                    ) {
                        Text("Exportar CSV", fontSize = 11.sp, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }

                    Button(
                        onClick = { importarCsvLauncher.launch(arrayOf("text/*", "application/*")) },
                        modifier = Modifier.weight(1f).defaultMinSize(minHeight = 40.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.08f)),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                    ) {
                        Text("Importar CSV", fontSize = 11.sp, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }

            // ========================================================
            // 7. SEÇÃO: DIAGNÓSTICO DO DISPOSITIVO & OTIMIZADOR
            // ========================================================
            CyberCardSecao(
                titulo = "DIAGNÓSTICO & OTIMIZAÇÃO",
                subtitulo = "Monitor de integridade, memória cache e OTA",
                icone = Icons.Default.Speed,
                corDestaque = Color(0xFF00FF7F)
            ) {
                // Verificador de Atualização OTA
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                        .clickable(enabled = !viewModel.verificandoAtualizacao && !viewModel.baixandoAtualizacao) {
                            viewModel.verificarAtualizacaoGitHub(manual = true) { disponivel ->
                                if (!disponivel) {
                                    Toast.makeText(
                                        contexto,
                                        "Você já está na versão mais recente (v${viewModel.versaoInstaladaNome})! 🚀",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Versão Instalada",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF00E5FF).copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "v${viewModel.versaoInstaladaNome} (${viewModel.versaoInstaladaCodigo})",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF00E5FF),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text(
                            text = "Toque para checar atualizações no GitHub",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }

                    if (viewModel.verificandoAtualizacao) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color(0xFF00E5FF))
                    } else {
                        Icon(Icons.Default.SystemUpdate, contentDescription = "Verificar", tint = Color(0xFF00E5FF), modifier = Modifier.size(20.dp))
                    }
                }

                // Limpeza e Otimização de Cache de Imagens
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Cache de Imagens & Posters",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Espaço ocupado: $tamanhoCacheTexto",
                            fontSize = 11.sp,
                            color = Color(0xFF00FF7F),
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                val liberado = withContext(Dispatchers.IO) { limparCacheImagens(contexto) }
                                val mb = liberado / (1024.0 * 1024.0)
                                tamanhoCacheTexto = "0 MB"
                                Toast.makeText(
                                    contexto,
                                    if (mb > 0.05) "Cache limpo! %.1f MB liberados com sucesso. ✨".format(Locale.US, mb) else "Cache do sistema já estava limpo!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF7F).copy(alpha = 0.15f)),
                        border = BorderStroke(1.dp, Color(0xFF00FF7F).copy(alpha = 0.4f)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = Color(0xFF00FF7F), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Limpar", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00FF7F))
                    }
                }
            }

            // ========================================================
            // 8. ZONA DE SEGURANÇA & SESSÃO (DANGER ZONE)
            // ========================================================
            CyberCardSecao(
                titulo = "ZONA DE SEGURANÇA & CONTA",
                subtitulo = "Limpeza de dados, encerramento de sessão e conta",
                icone = Icons.Default.Shield,
                corDestaque = CineListTokens.CorErro
            ) {
                // Limpar Todos os Dados Locais
                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onConfirmarReset()
                    },
                    modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 44.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CineListTokens.CorErro),
                    border = BorderStroke(1.dp, CineListTokens.CorErro.copy(alpha = 0.4f))
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, tint = CineListTokens.CorErro, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Limpar Todos os Dados Locais", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                // Sair da Conta (Logout)
                Button(
                    onClick = {
                        onDismiss()
                        onConfirmarSair()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CineListTokens.CorErro)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("DESCONECTAR DA CONTA", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                // Excluir Conta Permanentemente
                Text(
                    text = "Excluir permanentemente minha conta e dados",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 11.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onDismiss()
                            onConfirmarExclusaoConta()
                        }
                        .padding(vertical = 4.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * ====================================================================
 * COMPONENTES DE DESIGN SYSTEM FUTURISTA (CYBER UI)
 * ====================================================================
 */

@Composable
private fun CardHolograficoUsuario(
    usuarioAtual: FirebaseUser?,
    onCopiarUid: (String) -> Unit
) {
    val uid = usuarioAtual?.uid ?: ""
    val email = usuarioAtual?.email ?: "Não registrado"
    val nome = usuarioAtual?.displayName ?: "Usuário CineList"
    val provedor = usuarioAtual?.providerData?.lastOrNull()?.providerId?.uppercase() ?: "E-MAIL"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF162038),
                        Color(0xFF0F1526)
                    )
                )
            )
            .border(
                border = BorderStroke(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF00E5FF).copy(alpha = 0.5f),
                            Color(0xFFFFD700).copy(alpha = 0.3f),
                            Color.White.copy(alpha = 0.05f)
                        )
                    )
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Avatar com Anel Holográfico
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF00E5FF).copy(alpha = 0.2f))
                    .border(1.5.dp, Color(0xFF00E5FF), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (!usuarioAtual?.photoUrl?.toString().isNullOrBlank()) {
                    AsyncImage(
                        model = usuarioAtual?.photoUrl,
                        contentDescription = "Avatar",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                } else {
                    Text(
                        text = nome.take(2).uppercase(),
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = Color(0xFF00E5FF)
                    )
                }
            }

            // Metadados do Usuário
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = nome,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFFFD700).copy(alpha = 0.2f))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = provedor,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD700)
                        )
                    }
                }

                Text(
                    text = email,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Tag de UID com Botão de Copiar
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .clickable { onCopiarUid(uid) }
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "ID: ${uid.take(10)}...",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = Color(0xFF00E5FF)
                    )
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = "Copiar ID",
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(10.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CyberCardSecao(
    titulo: String,
    subtitulo: String,
    icone: ImageVector,
    corDestaque: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF131826))
            .border(
                BorderStroke(1.dp, corDestaque.copy(alpha = 0.25f)),
                RoundedCornerShape(16.dp)
            )
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(corDestaque.copy(alpha = 0.15f))
                    .border(1.dp, corDestaque.copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icone, contentDescription = null, tint = corDestaque, modifier = Modifier.size(18.dp))
            }

            Column {
                Text(
                    text = titulo.t(),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp,
                    color = Color.White
                )
                Text(
                    text = subtitulo.t(),
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        }

        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

        content()
    }
}

@Composable
private fun CyberSwitchItem(
    titulo: String,
    subtitulo: String,
    icone: ImageVector,
    checado: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f).padding(end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(icone, contentDescription = null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
            Column {
                Text(text = titulo.t(), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(text = subtitulo.t(), fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f), maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }

        Switch(
            checked = checado,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF00E5FF),
                checkedTrackColor = Color(0xFF00E5FF).copy(alpha = 0.3f),
                uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
            )
        )
    }
}

@Composable
private fun CyberItemNavegacao(
    titulo: String,
    subtitulo: String,
    icone: ImageVector,
    corIcone: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f).padding(end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(icone, contentDescription = null, tint = corIcone, modifier = Modifier.size(18.dp))
            Column {
                Text(text = titulo.t(), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(text = subtitulo.t(), fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f), maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.4f),
            modifier = Modifier.size(20.dp)
        )
    }
}



/**
 * Utilitários para medição e limpeza de cache
 */
private fun calcularTamanhoCache(context: Context): String {
    return try {
        val dir = context.cacheDir.resolve("image_cache_cinelist")
        var tamanho = 0L
        if (dir.exists()) {
            dir.walkTopDown().forEach { if (it.isFile) tamanho += it.length() }
        }
        val mb = tamanho / (1024.0 * 1024.0)
        if (mb < 0.1) "Menos de 1 MB" else "%.1f MB".format(Locale.US, mb)
    } catch (e: Exception) {
        "0 MB"
    }
}

private fun limparCacheImagens(context: Context): Long {
    return try {
        val dir = context.cacheDir.resolve("image_cache_cinelist")
        var liberado = 0L
        if (dir.exists()) {
            dir.walkTopDown().forEach { if (it.isFile) liberado += it.length() }
            dir.deleteRecursively()
            dir.mkdirs()
        }
        liberado
    } catch (e: Exception) {
        0L
    }
}
