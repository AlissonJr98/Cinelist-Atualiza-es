package com.example.cinelist

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.google.firebase.auth.FirebaseAuth

@Composable
fun DialogoQRCodeAmigo(
    viewModel: MidiaViewModel,
    onDispensar: () -> Unit
) {
    val contexto = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val usuarioAtual = FirebaseAuth.getInstance().currentUser
    val meuUid = remember { usuarioAtual?.uid ?: "" }
    val meuNome = remember { usuarioAtual?.displayName ?: "Usuário CineList" }

    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var codigoAmigoInput by remember { mutableStateOf("") }
    var enviandoSolicitacao by remember { mutableStateOf(false) }

    LaunchedEffect(meuUid) {
        if (meuUid.isNotBlank()) {
            qrBitmap = QrCodeCyberHelper.gerarQrCodeBitmap(
                conteudo = meuUid,
                tamanho = 512,
                corPontos = android.graphics.Color.parseColor("#00E5FF"),
                corFundo = android.graphics.Color.parseColor("#0B0E17")
            )
        }
    }

    Dialog(onDismissRequest = onDispensar) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF0F1420),
            border = BorderStroke(1.5.dp, Color(0xFF00E5FF).copy(alpha = 0.6f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Top Header com botão Fechar
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
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF00E5FF).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCode2,
                                contentDescription = null,
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = "QR CODE CONEXÃO",
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            color = Color(0xFF00E5FF)
                        )
                    }

                    IconButton(
                        onClick = onDispensar,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fechar",
                            tint = Color(0xFF94A3B8)
                        )
                    }
                }

                Text(
                    text = "Mostre este código para um amigo escanear ou copie seu ID para conectarem instantaneamente.",
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center
                )

                // Container do QR Code com Glow Holográfico
                Box(
                    modifier = Modifier
                        .size(230.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF0B0E17))
                        .border(2.dp, Brush.linearGradient(listOf(Color(0xFF00E5FF), Color(0xFF7C4DFF))), RoundedCornerShape(20.dp))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    qrBitmap?.let { bitmap ->
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Meu QR Code",
                            modifier = Modifier.fillMaxSize()
                        )
                    } ?: CircularProgressIndicator(color = Color(0xFF00E5FF), modifier = Modifier.size(36.dp))
                }

                // Nome do Usuário e Código Curto
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = meuNome,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                    Text(
                        text = "ID: ${if (meuUid.length > 12) meuUid.take(6) + "..." + meuUid.takeLast(6) else meuUid}",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                }

                // Botões de Ação Rápida: Copiar e Compartilhar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            val clipboard = contexto.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("ID CineList", meuUid))
                            Toast.makeText(contexto, "ID copiado para a área de transferência! 📋", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f).bounceOnClick(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copiar ID", fontSize = 12.sp, color = Color.White)
                    }

                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "Me adicione no CineList! Meu código de amigo é:\n$meuUid")
                            }
                            contexto.startActivity(Intent.createChooser(intent, "Compartilhar Código de Amigo"))
                        },
                        modifier = Modifier.weight(1f).bounceOnClick(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Black)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Compartilhar", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                }

                HorizontalDivider(color = Color(0xFF1E293B), thickness = 1.dp)

                // Campo para Conectar amigo colando o código
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Conectar Amigo por Código:",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = Color.White
                    )

                    OutlinedTextField(
                        value = codigoAmigoInput,
                        onValueChange = { codigoAmigoInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Cole o ID do seu amigo aqui...", fontSize = 12.sp, color = Color(0xFF64748B)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00E5FF),
                            unfocusedBorderColor = Color(0xFF1E293B),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color(0xFF0B0E17),
                            unfocusedContainerColor = Color(0xFF0B0E17)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Button(
                        onClick = {
                            val alvo = codigoAmigoInput.trim()
                            if (alvo.isBlank()) {
                                Toast.makeText(contexto, "Digite ou cole o ID do amigo.", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (alvo == meuUid) {
                                Toast.makeText(contexto, "Você não pode adicionar a si mesmo.", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            enviandoSolicitacao = true
                            viewModel.enviarSolicitacaoAmigo(alvo) { sucesso, erroMsg ->
                                enviandoSolicitacao = false
                                if (sucesso) {
                                    Toast.makeText(contexto, "Solicitação de amizade enviada com sucesso! ✨", Toast.LENGTH_SHORT).show()
                                    codigoAmigoInput = ""
                                } else {
                                    Toast.makeText(contexto, erroMsg ?: "Erro ao enviar solicitação.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().bounceOnClick(),
                        enabled = !enviandoSolicitacao && codigoAmigoInput.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C4DFF)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (enviandoSolicitacao) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Enviar Solicitação de Amizade", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
