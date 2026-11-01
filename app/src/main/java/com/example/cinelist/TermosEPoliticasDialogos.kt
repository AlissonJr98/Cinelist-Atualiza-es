package com.example.cinelist

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

/**
 * Diálogo Futurista com os Termos de Uso do aplicativo CineList.
 * Estruturado com cláusulas claras para uso pessoal, propriedade intelectual
 * e preparado para futuras comercializações e planos de assinatura.
 */
@Composable
fun DialogoTermosUso(onDispensar: () -> Unit) {
    Dialog(onDismissRequest = onDispensar) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1424)),
            border = BorderStroke(
                1.5.dp,
                Brush.linearGradient(
                    listOf(
                        Color(0xFF00E5FF).copy(alpha = 0.8f),
                        Color(0xFF8B5CF6).copy(alpha = 0.6f),
                        Color(0xFF00E5FF).copy(alpha = 0.8f)
                    )
                )
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Top Bar do Diálogo
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
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF00E5FF).copy(alpha = 0.15f))
                                .border(1.dp, Color(0xFF00E5FF), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Gavel,
                                contentDescription = null,
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "TERMOS DE USO".t(),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            Text(
                                text = "CineList Mobile Platform".t(),
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    IconButton(onClick = onDispensar, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar".t(), tint = Color.White.copy(alpha = 0.7f))
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.12f))

                // Conteúdo legal rolável
                Surface(
                    color = Color(0xFF151D30),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0xFF26324D)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .padding(14.dp)
                            .heightIn(max = 380.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SecaoLegalItem(
                            numero = "1",
                            titulo = "Aceitação dos Termos".t(),
                            corpo = "Ao criar uma conta, acessar ou utilizar o aplicativo CineList, você concorda expressamente com todos os termos e diretrizes aqui estabelecidos. Caso discorde de qualquer disposição, solicitamos que interrompa imediatamente o uso do serviço.".t()
                        )

                        SecaoLegalItem(
                            numero = "2",
                            titulo = "Finalidade e Natureza do Serviço".t(),
                            corpo = "O CineList é um catálogo pessoal inteligente, organizador de filmes, séries e animes e rede social para cinéfilos. O aplicativo NÃO realiza streaming, cópia ou hospedagem de arquivos de vídeo protegidos por direitos autorais, servindo estritamente como organizador e agregador informativo de onde assistir.".t()
                        )

                        SecaoLegalItem(
                            numero = "3",
                            titulo = "Comercialização e Planos Futuros".t(),
                            corpo = "O usuário reconhece expressamente que o aplicativo CineList poderá, a qualquer momento e a exclusivo critério de seus desenvolvedores, introduzir modelos de monetização, assinaturas pagas (CineList Pro/Premium), anúncios publicitários ou compras no aplicativo. Tais planos futuros serão transparentemente comunicados, garantindo a preservação da conta e dados já cadastrados.".t()
                        )

                        SecaoLegalItem(
                            numero = "4",
                            titulo = "Propriedade Intelectual e APIs de Terceiros".t(),
                            corpo = "O software, sua identidade visual, códigos-fonte, design futurista e funcionalidades são de titularidade de seus criadores. As informações e metadados de filmes e séries são obtidos mediante a API pública do The Movie Database (TMDB). Marcas de serviços de streaming (Netflix, Prime Video, Disney+, Max, etc.) pertencem a seus respectivos titulares.".t()
                        )

                        SecaoLegalItem(
                            numero = "5",
                            titulo = "Conta e Conduta Comunitária".t(),
                            corpo = "O usuário é responsável por manter o sigilo de suas credenciais de acesso. É estritamente vedada a utilização do CineList para fins ilícitos, spam, ofensas, discriminação ou propagação de conteúdo inadequado nas salas sociais (CineMatch e CineParty), sujeitando o infrator ao banimento imediato da conta.".t()
                        )

                        SecaoLegalItem(
                            numero = "6",
                            titulo = "Contato e Suporte Oficial".t(),
                            corpo = "Para esclarecimento de dúvidas, sugestões comerciais ou solicitações formais relativas aos Termos de Uso, utilize nosso canal oficial de comunicação através do e-mail: suportecinelist@gmail.com.".t()
                        )
                    }
                }

                // Botão de Entendido
                Button(
                    onClick = onDispensar,
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
                ) {
                    Text(
                        text = "ENTENDIDO E DE ACORDO".t(),
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0A0F1D),
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

/**
 * Diálogo Futurista com a Política de Privacidade (LGPD / GDPR).
 */
@Composable
fun DialogoPoliticaPrivacidade(onDispensar: () -> Unit) {
    Dialog(onDismissRequest = onDispensar) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1424)),
            border = BorderStroke(
                1.5.dp,
                Brush.linearGradient(
                    listOf(
                        Color(0xFF10B981).copy(alpha = 0.8f),
                        Color(0xFF00E5FF).copy(alpha = 0.6f),
                        Color(0xFF10B981).copy(alpha = 0.8f)
                    )
                )
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Top Bar
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
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF10B981).copy(alpha = 0.15f))
                                .border(1.dp, Color(0xFF10B981), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.PrivacyTip,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "POLÍTICA DE PRIVACIDADE".t(),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            Text(
                                text = "Conformidade LGPD & GDPR".t(),
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    IconButton(onClick = onDispensar, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar".t(), tint = Color.White.copy(alpha = 0.7f))
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.12f))

                Surface(
                    color = Color(0xFF151D30),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0xFF26324D)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .padding(14.dp)
                            .heightIn(max = 380.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SecaoLegalItem(
                            numero = "1",
                            titulo = "Dados que Coletamos".t(),
                            corpo = "Coletamos o e-mail cadastrado, nome de exibição público, foto de perfil voluntária e as listas de mídias adicionadas (favoritos, assistidos, quero ver, notas e progresso de episódios). Não solicitamos dados sensíveis como número de documento, dados bancários ou localização em tempo real.".t()
                        )

                        SecaoLegalItem(
                            numero = "2",
                            titulo = "Uso e Processamento das Informações".t(),
                            corpo = "Seus dados são utilizados única e exclusivamente para sincronizar seu histórico entre dispositivos via Google Firebase, gerar recomendações personalizadas com a IA (CineAI) e viabilizar interações nas salas do CineParty e CineMatch.".t()
                        )

                        SecaoLegalItem(
                            numero = "3",
                            titulo = "Não Venda de Dados a Terceiros".t(),
                            corpo = "O CineList adota o compromisso rígido de NUNCA vender, comercializar ou repassar suas informações pessoais ou históricos de visualização para terceiros ou redes de publicidade invasiva.".t()
                        )

                        SecaoLegalItem(
                            numero = "4",
                            titulo = "Armazenamento e Criptografia".t(),
                            corpo = "Os dados são transmitidos via conexão segura e criptografada (TLS/HTTPS). As credenciais de acesso são criptografadas com segurança de nível bancário pelos servidores certificados do Google Cloud Firebase.".t()
                        )

                        SecaoLegalItem(
                            numero = "5",
                            titulo = "Direito de Acesso, Exportação e Exclusão Total".t(),
                            corpo = "Você tem pleno controle sobre seus dados: pode exportar todo o seu catálogo a qualquer momento em arquivos abertos (.JSON e .CSV) e pode excluir permanentemente sua conta e todos os dados associados diretamente no menu 'Zona de Segurança & Conta' das Configurações.".t()
                        )

                        SecaoLegalItem(
                            numero = "6",
                            titulo = "Canal de Privacidade (DPO / Suporte)".t(),
                            corpo = "Para exercer seus direitos de titular ou sanar qualquer dúvida sobre tratamento de dados, envie um e-mail para: suportecinelist@gmail.com.".t()
                        )
                    }
                }

                Button(
                    onClick = onDispensar,
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Text(
                        text = "COMPREENDIDO E CIENTE".t(),
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

/**
 * Diálogo interativo para contato oficial de Suporte, Dúvidas e Feedback via suportecinelist@gmail.com
 */
@Composable
fun DialogoCanalSuporteCineList(
    contexto: Context,
    onDispensar: () -> Unit,
    onAbrirFeedback: () -> Unit
) {
    Dialog(onDismissRequest = onDispensar) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1424)),
            border = BorderStroke(1.5.dp, Color(0xFFFFD700).copy(alpha = 0.8f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
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
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFFFD700).copy(alpha = 0.15f))
                                .border(1.dp, Color(0xFFFFD700), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.SupportAgent, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(22.dp))
                        }
                        Column {
                            Text(
                                text = "SUPORTE & CONTATO".t(),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            Text(
                                text = "Canal Oficial CineList".t(),
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    IconButton(onClick = onDispensar, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar".t(), tint = Color.White.copy(alpha = 0.7f))
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.12f))

                Text(
                    text = "Dúvidas, sugestões de melhorias, propostas comerciais ou reporte de problemas: nossa equipe está pronta para atendê-lo!".t(),
                    fontSize = 12.sp,
                    color = Color(0xFFE2E8F0),
                    lineHeight = 18.sp
                )

                // Card de E-mail
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF151D30),
                    border = BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "E-MAIL OFICIAL DE ATENDIMENTO".t(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD700),
                            letterSpacing = 0.5.sp
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "suportecinelist@gmail.com",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontFamily = FontFamily.Monospace
                            )
                            IconButton(
                                onClick = {
                                    val clipboard = contexto.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("E-mail Suporte CineList", "suportecinelist@gmail.com"))
                                    Toast.makeText(contexto, "E-mail copiado para a área de transferência! 📋".t(), Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copiar".t(), tint = Color(0xFFFFD700), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                // Botão de envio de e-mail direto
                Button(
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:suportecinelist@gmail.com")
                                putExtra(Intent.EXTRA_SUBJECT, "[Suporte CineList] Dúvida ou Feedback")
                            }
                            contexto.startActivity(intent)
                            onDispensar()
                        } catch (e: Exception) {
                            Toast.makeText(contexto, "suportecinelist@gmail.com", Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700))
                ) {
                    Icon(Icons.Default.Email, contentDescription = null, tint = Color(0xFF0A0F1D), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ENVIAR E-MAIL AGORA".t(),
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF0A0F1D),
                        fontSize = 12.sp
                    )
                }

                // Botão para formulário com diagnóstico
                OutlinedButton(
                    onClick = {
                        onDispensar()
                        onAbrirFeedback()
                    },
                    modifier = Modifier.fillMaxWidth().height(42.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f))
                ) {
                    Text(
                        text = "Formulário Interno com Diagnóstico".t(),
                        color = Color.White,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

/**
 * Card Oficial de Atribuição da API The Movie Database (TMDB) conforme termos de serviço
 * https://www.themoviedb.org
 */
@Composable
fun CardAtribuicaoTmdb(contexto: Context) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF10192A),
        border = BorderStroke(1.dp, Color(0xFF01B4E4).copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Emblema TMDB em estilo Cyber UI
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF90CEA1), Color(0xFF01B4E4))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "TMDB",
                        fontWeight = FontWeight.Black,
                        fontSize = 10.sp,
                        color = Color(0xFF0D253F)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "The Movie Database (TMDB)".t(),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Catálogo oficial e metadados".t(),
                        fontSize = 10.sp,
                        color = Color(0xFF01B4E4)
                    )
                }
            }

            Text(
                text = "Este produto usa a API do TMDB, mas não é endossado nem certificado pelo TMDB. Todas as imagens e metadados pertencem aos seus respectivos donos e colaboradores do TMDB.".t(),
                fontSize = 11.sp,
                color = Color(0xFF94A3B8),
                lineHeight = 16.sp
            )

            Button(
                onClick = {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.themoviedb.org"))
                        contexto.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(contexto, "https://www.themoviedb.org", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(38.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF01B4E4).copy(alpha = 0.2f)),
                border = BorderStroke(1.dp, Color(0xFF01B4E4).copy(alpha = 0.5f)),
                contentPadding = PaddingValues(horizontal = 10.dp)
            ) {
                Icon(Icons.Default.OpenInBrowser, contentDescription = null, tint = Color(0xFF01B4E4), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "ACESSAR THEMOVIEDB.ORG".t(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF01B4E4)
                )
            }
        }
    }
}

@Composable
private fun SecaoLegalItem(numero: String, titulo: String, corpo: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF00E5FF).copy(alpha = 0.2f))
                    .border(1.dp, Color(0xFF00E5FF), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = numero,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00E5FF)
                )
            }
            Text(
                text = titulo,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        Text(
            text = corpo,
            fontSize = 11.sp,
            color = Color(0xFFCBD5E1),
            lineHeight = 16.sp,
            modifier = Modifier.padding(start = 28.dp)
        )
    }
}


@Composable
fun DialogoConsentimentoInicialTermosEPolitica(
    onAceitar: () -> Unit
) {
    var exibirTermosDetalhe by remember { mutableStateOf(false) }
    var exibirPoliticaDetalhe by remember { mutableStateOf(false) }

    if (exibirTermosDetalhe) {
        DialogoTermosUso(onDispensar = { exibirTermosDetalhe = false })
    }
    if (exibirPoliticaDetalhe) {
        DialogoPoliticaPrivacidade(onDispensar = { exibirPoliticaDetalhe = false })
    }

    Dialog(
        onDismissRequest = { /* Bloqueante no primeiro acesso */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        val config = androidx.compose.ui.platform.LocalConfiguration.current
        val maxDialogHeight = (config.screenHeightDp * 0.88f).dp

        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .heightIn(max = maxDialogHeight)
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            border = BorderStroke(
                1.8.dp,
                Brush.linearGradient(listOf(Color(0xFF00E5FF), Color(0xFF8B5CF6), Color(0xFF10B981)))
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(Color(0xFF00E5FF).copy(alpha = 0.35f), Color(0xFF00E5FF).copy(alpha = 0.08f))
                            )
                        )
                        .border(1.5.dp, Color(0xFF00E5FF), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🛡️", fontSize = 28.sp)
                }

                Text(
                    text = "BEM-VINDO AO CINELIST".t(),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 0.8.sp,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Para sua total segurança e transparência, nosso aplicativo opera em estrita conformidade com a LGPD e GDPR. Antes de continuar, solicitamos que leia e aceite nossos Termos de Uso e Política de Privacidade.".t(),
                    fontSize = 13.sp,
                    color = Color(0xFFCBD5E1),
                    lineHeight = 18.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Botões de consulta aos documentos completos
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { exibirTermosDetalhe = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.2.dp, Color(0xFF00E5FF)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00E5FF)),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Gavel, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Termos de Uso".t(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    OutlinedButton(
                        onClick = { exibirPoliticaDetalhe = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.2.dp, Color(0xFF10B981)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF10B981)),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(imageVector = Icons.Default.PrivacyTip, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Privacidade".t(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1E293B),
                    border = BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "✓ Dados 100% criptografados e confidenciais".t(),
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8),
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "✓ Direito garantido de exclusão integral de conta e dados".t(),
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8),
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "✓ Sem venda ou repasse a intermediários de anúncios".t(),
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Button(
                    onClick = onAceitar,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .bounceOnClick(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                ) {
                    Text(
                        text = "LI E CONCORDO COM OS TERMOS".t(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.Black,
                        letterSpacing = 0.5.sp,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
