package com.example.cinelist

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions

/**
 * Configuração do canal de suporte.
 *
 * Enquanto não existir um servidor próprio, os relatórios são enviados
 * por e-mail através da app de e-mail do utilizador.
 */
object SuporteConfig {
    /** E-mail que recebe os contactos. */
    const val EMAIL_DESTINO = "alissonjunior41@gmail.com"

    /** Prefixo do assunto, para facilitar a triagem. */
    const val ASSUNTO_PREFIXO = "[CineList]"

    /** Categorias disponíveis no formulário de feedback. */
    val categorias = listOf(
        CategoriaFeedback(
            rotulo = "Erro/Bug",
            icone = Icons.Default.BugReport,
            prefixo = "ERRO"
        ),
        CategoriaFeedback(
            rotulo = "Sugestão",
            icone = Icons.Default.Lightbulb,
            prefixo = "SUGESTAO"
        ),
        CategoriaFeedback(
            rotulo = "Dúvida",
            icone = Icons.Default.Info,
            prefixo = "DUVIDA"
        )
    )
}

/**
 * Categoria de um contacto de suporte.
 */
data class CategoriaFeedback(
    val rotulo: String,
    val icone: androidx.compose.ui.graphics.vector.ImageVector,
    val prefixo: String
)

/**
 * Constrói o corpo do e-mail com o diagnóstico do dispositivo.
 *
 * Incluir estas informações acelera muito a reprodução do problema.
 */
private fun construirRelatorio(
    categoria: CategoriaFeedback,
    descricao: String,
    incluirDiagnostico: Boolean
): String {
    val builder = StringBuilder()

    builder.appendLine(categoria.rotulo.uppercase())
    builder.appendLine("=".repeat(34))
    builder.appendLine()
    builder.appendLine(descricao.trim().ifBlank { "(sem descrição)" })
    builder.appendLine()

    if (incluirDiagnostico) {
        builder.appendLine()
        builder.appendLine("--- Diagnóstico automático ---")
        builder.appendLine("Versão da app: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
        builder.appendLine("Android: ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})")
        builder.appendLine("Fabricante: ${android.os.Build.MANUFACTURER}")
        builder.appendLine("Modelo: ${android.os.Build.MODEL}")
    }

    return builder.toString()
}

/**
 * Abre a app de e-mail com o relatório preenchido.
 *
 * Se não existir nenhuma app de e-mail, devolve false para que a UI
 * possa oferecer a alternativa de copiar o texto.
 */
private fun abrirAppEmail(
    context: Context,
    categoria: CategoriaFeedback,
    corpo: String
): Boolean {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "message/rfc822"
        putExtra(Intent.EXTRA_EMAIL, arrayOf(SuporteConfig.EMAIL_DESTINO))
        putExtra(Intent.EXTRA_SUBJECT, "${SuporteConfig.ASSUNTO_PREFIXO} ${categoria.prefixo}")
        putExtra(Intent.EXTRA_TEXT, corpo)
    }

    // Verifica se existe alguma app capaz de tratar o intent
    val podeEnviar = runCatching {
        context.packageManager
            .queryIntentActivities(intent, 0)
            .isNotEmpty()
    }.getOrDefault(false)

    if (!podeEnviar) return false

    return try {
        context.startActivity(
            Intent.createChooser(intent, "Enviar feedback por e-mail")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        true
    } catch (e: ActivityNotFoundException) {
        false
    }
}

/** Copia texto para a área de transferência do sistema. */
private fun copiarParaClipboard(context: Context, texto: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    clipboard?.setPrimaryClip(ClipData.newPlainText("CineList - Feedback", texto))
}

/**
 * Diálogo de feedback / contacto com o suporte.
 *
 * Recolhe o tipo de contacto e a descrição, e envia por e-mail.
 * Se o dispositivo não tiver app de e-mail, oferece copiar o relatório.
 */
@Composable
fun DialogoFeedbackSuporte(
    onDispensar: () -> Unit
) {
    val contexto = androidx.compose.ui.platform.LocalContext.current

    var categoria by remember { mutableStateOf(SuporteConfig.categorias.first()) }
    var descricao by remember { mutableStateOf("") }
    var incluirDiagnostico by remember { mutableStateOf(true) }

    var erroSemAppEmail by remember { mutableStateOf(false) }
    var relatorioGerado by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDispensar,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Feedback e Suporte",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Ajuda a melhorar o CineList. Vamos abrir a tua app de e-mail com a mensagem já escrita.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Categorias
                Text(
                    text = "Qual é o assunto?",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SuporteConfig.categorias.forEach { cat ->
                        AssistChip(
                            onClick = { categoria = cat },
                            label = { Text(cat.rotulo, fontSize = 12.sp) },
                            leadingIcon = {
                                Icon(
                                    imageVector = cat.icone,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                }

                // Descrição
                OutlinedTextField(
                    value = descricao,
                    onValueChange = { if (it.length <= 2000) descricao = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Descreve o que aconteceu") },
                    placeholder = {
                        Text(
                            if (categoria.rotulo == "Erro/Bug") {
                                "Ex.: Ao mudar de grupo, a lista fica vazia e não volta ao normal."
                            } else {
                                "Ex.: Seria ótimo poder ordenar por ano de lançamento."
                            },
                            fontSize = 12.sp
                        )
                    },
                    minLines = 4,
                    maxLines = 7,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                    supportingText = {
                        Text(
                            text = "${descricao.length}/2000",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )

                // Toggle de diagnóstico
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { incluirDiagnostico = !incluirDiagnostico }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .background(
                                if (incluirDiagnostico) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline,
                                RoundedCornerShape(6.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (incluirDiagnostico) {
                            Text(
                                text = "✓",
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Incluir dados do dispositivo",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Modelo, versão do Android e versão da app. Acelera a resolução.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Alternativa quando não há app de e-mail
                if (erroSemAppEmail) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Não foi encontrada nenhuma app de e-mail neste dispositivo.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            OutlinedButton(
                                onClick = {
                                    copiarParaClipboard(
                                        contexto,
                                        relatorioGerado ?: construirRelatorio(
                                            categoria, descricao, incluirDiagnostico
                                        )
                                    )
                                    Toast.makeText(
                                        contexto,
                                        "Relatório copiado. Cola-o num e-mail para ${SuporteConfig.EMAIL_DESTINO}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    erroSemAppEmail = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    Icons.Default.ContentCopy,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Copiar relatório", fontSize = 13.sp)
                            }
                        }
                    }
                }

                Text(
                    text = "Destino: ${SuporteConfig.EMAIL_DESTINO}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val corpo = construirRelatorio(categoria, descricao, incluirDiagnostico)
                    relatorioGerado = corpo
                    val enviou = abrirAppEmail(contexto, categoria, corpo)
                    if (enviou) {
                        onDispensar()
                    } else {
                        erroSemAppEmail = true
                    }
                },
                enabled = descricao.isNotBlank(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Enviar", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDispensar) {
                Text("Cancelar", color = MaterialTheme.colorScheme.secondary)
            }
        }
    )
}
