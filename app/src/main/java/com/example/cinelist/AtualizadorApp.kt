package com.example.cinelist

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

data class InfoAtualizacao(
    val versaoCodigo: Int,
    val versaoNome: String,
    val notas: String,
    val apkUrl: String,
    val paginaUrl: String = "https://github.com/${AtualizadorGitHub.DONO_REPO}/${AtualizadorGitHub.NOME_REPO}"
) {
    // Compatibilidade com o código legado do CineList (UpdateManager, Workers, Notificações)
    val versaoCode: Int get() = versaoCodigo
    val urlApk: String get() = apkUrl
    val notasDaVersao: String get() = notas
}

data class ResultadoAtualizacao(
    val disponivel: Boolean,
    val info: InfoAtualizacao?,
    val versaoInstaladaNome: String,
    val versaoInstaladaCodigo: Int
)

object AtualizadorGitHub {
    const val DONO_REPO = "AlissonJr98"
    const val NOME_REPO = "Cinelist-Atualiza-es"
    const val URL_VERSAO_JSON = "https://raw.githubusercontent.com/$DONO_REPO/$NOME_REPO/main/atualizacao/versao.json"
    const val URL_VERSAO_JSON_LEGACY = "https://raw.githubusercontent.com/$DONO_REPO/$NOME_REPO/main/version.json"
    const val URL_RELEASES_API = "https://api.github.com/repos/$DONO_REPO/$NOME_REPO/releases/latest"
    const val URL_APK_PADRAO = "https://raw.githubusercontent.com/$DONO_REPO/$NOME_REPO/main/atualizacao/cinelist.apk"

    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    fun obterVersaoInstalada(context: Context): Pair<Int, String> {
        return try {
            val pInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            val codigo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                pInfo.versionCode
            }
            val nome = pInfo.versionName ?: "1.0"
            codigo to nome
        } catch (_: Throwable) {
            1 to "1.0"
        }
    }

    suspend fun verificar(context: Context): ResultadoAtualizacao = withContext(Dispatchers.IO) {
        val (codigoLocal, nomeLocal) = obterVersaoInstalada(context)
        val infoJson = runCatching { buscarPorVersaoJson() }.getOrNull()
        val infoRelease = runCatching { buscarPorGitHubRelease() }.getOrNull()

        val melhorInfo = when {
            infoJson != null && infoRelease != null -> {
                if (ehMaisNova(infoRelease.versaoCodigo, infoRelease.versaoNome, infoJson.versaoCodigo, infoJson.versaoNome)) {
                    infoRelease
                } else {
                    infoJson
                }
            }
            infoJson != null -> infoJson
            else -> infoRelease
        }

        val temNova = melhorInfo != null && ehMaisNova(
            melhorInfo.versaoCodigo,
            melhorInfo.versaoNome,
            codigoLocal,
            nomeLocal
        )

        ResultadoAtualizacao(
            disponivel = temNova,
            info = melhorInfo,
            versaoInstaladaNome = nomeLocal,
            versaoInstaladaCodigo = codigoLocal
        )
    }

    private fun buscarPorVersaoJson(): InfoAtualizacao? {
        // Tenta primeiro o endpoint em atualizacao/versao.json e faz fallback para version.json raiz
        val urls = listOf(
            "$URL_VERSAO_JSON?t=${System.currentTimeMillis()}",
            "$URL_VERSAO_JSON_LEGACY?t=${System.currentTimeMillis()}"
        )

        for (urlStr in urls) {
            try {
                val req = Request.Builder()
                    .url(urlStr)
                    .header("Cache-Control", "no-cache")
                    .header("User-Agent", "CineList-Android-Updater")
                    .build()

                http.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) return@use
                    val body = resp.body?.string() ?: return@use
                    val obj = JsonParser.parseString(body).asJsonObject

                    val codigo = obj.get("versaoCodigo")?.asInt
                        ?: obj.get("versionCode")?.asInt
                        ?: 1

                    val nome = (obj.get("versaoNome")?.asString ?: obj.get("versionName")?.asString)
                        ?.removePrefix("v")?.trim() ?: "1.0"

                    val notas = (obj.get("notas")?.asString ?: obj.get("changelog")?.asString ?: obj.get("notasDaVersao")?.asString)
                        ?.trim() ?: "Nova atualização disponível no GitHub."

                    val apkUrl = (obj.get("apkUrl")?.asString ?: obj.get("urlApk")?.asString)
                        ?.trim().takeUnless { it.isNullOrBlank() } ?: URL_APK_PADRAO

                    return InfoAtualizacao(
                        versaoCodigo = codigo,
                        versaoNome = nome,
                        notas = notas,
                        apkUrl = apkUrl
                    )
                }
            } catch (_: Throwable) {
                // Tenta próxima URL
            }
        }
        return null
    }

    private fun buscarPorGitHubRelease(): InfoAtualizacao? {
        val req = Request.Builder()
            .url(URL_RELEASES_API)
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "CineList-Android-Updater")
            .build()

        http.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return null
            val body = resp.body?.string() ?: return null
            val obj = JsonParser.parseString(body).asJsonObject
            val tag = obj.get("tag_name")?.asString?.removePrefix("v")?.trim() ?: return null
            val notas = obj.get("body")?.asString?.trim().takeUnless { it.isNullOrBlank() }
                ?: obj.get("name")?.asString?.trim()
                ?: "Nova versão publicada no GitHub."
            val htmlUrl = obj.get("html_url")?.asString ?: "https://github.com/$DONO_REPO/$NOME_REPO/releases"
            val assets = obj.getAsJsonArray("assets")
            var apkUrl: String? = null
            if (assets != null) {
                for (el in assets) {
                    val asset = el.asJsonObject
                    val nomeArquivo = asset.get("name")?.asString ?: ""
                    val url = asset.get("browser_download_url")?.asString
                    if (nomeArquivo.endsWith(".apk", ignoreCase = true) && !url.isNullOrBlank()) {
                        apkUrl = url
                        break
                    }
                }
            }
            if (apkUrl.isNullOrBlank()) return null
            val codigoExtraido = extrairCodigoDeTag(tag)
            return InfoAtualizacao(
                versaoCodigo = codigoExtraido,
                versaoNome = tag,
                notas = notas,
                apkUrl = apkUrl,
                paginaUrl = htmlUrl
            )
        }
    }

    private fun extrairCodigoDeTag(tag: String): Int {
        val limpo = tag.removePrefix("v").trim()
        val partes = limpo.split(".", "-", "_").mapNotNull { it.toIntOrNull() }
        if (partes.size >= 3) return partes[0] * 100 + partes[1] * 10 + partes[2]
        if (partes.size == 2 && partes[0] == 1) return partes[1] + 1
        return partes.lastOrNull() ?: 1
    }

    fun ehMaisNova(
        codigoRemoto: Int,
        nomeRemoto: String,
        codigoLocal: Int,
        nomeLocal: String
    ): Boolean {
        if (codigoRemoto > codigoLocal) return true
        val cmp = compararSemver(nomeRemoto, nomeLocal)
        return cmp > 0
    }

    private fun compararSemver(a: String, b: String): Int {
        val pa = a.removePrefix("v").trim().split(".", "-").map { it.toIntOrNull() ?: 0 }
        val pb = b.removePrefix("v").trim().split(".", "-").map { it.toIntOrNull() ?: 0 }
        val max = maxOf(pa.size, pb.size)
        for (i in 0 until max) {
            val va = pa.getOrElse(i) { 0 }
            val vb = pb.getOrElse(i) { 0 }
            if (va != vb) return va.compareTo(vb)
        }
        return 0
    }

    suspend fun baixarApk(
        context: Context,
        apkUrl: String,
        onProgresso: (Float) -> Unit
    ): File = withContext(Dispatchers.IO) {
        val pasta = File(context.cacheDir, "updates").apply { mkdirs() }
        val destino = File(pasta, "cinelist-update.apk")
        if (destino.exists()) destino.delete()

        val urlComCacheBust = if (apkUrl.contains("?")) "$apkUrl&t=${System.currentTimeMillis()}"
        else "$apkUrl?t=${System.currentTimeMillis()}"

        val req = Request.Builder()
            .url(urlComCacheBust)
            .header("User-Agent", "CineList-Android-Updater")
            .header("Accept", "application/octet-stream, application/vnd.android.package-archive, */*")
            .build()

        http.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) {
                throw IllegalStateException("Não foi possível baixar o APK no GitHub (HTTP ${resp.code}).")
            }
            val body = resp.body ?: throw IllegalStateException("Resposta vazia ao baixar o APK.")
            val total = body.contentLength()
            body.byteStream().use { input ->
                FileOutputStream(destino).use { output ->
                    val buffer = ByteArray(16 * 1024)
                    var baixado = 0L
                    var lidos: Int
                    while (input.read(buffer).also { lidos = it } != -1) {
                        output.write(buffer, 0, lidos)
                        baixado += lidos
                        if (total > 0) {
                            onProgresso((baixado.toFloat() / total.toFloat()).coerceIn(0f, 1f))
                        }
                    }
                    output.flush()
                }
            }
        }

        // Verifica se é realmente um arquivo APK (ZIP começa com 'P' 'K')
        if (destino.length() < 50_000L) {
            destino.delete()
            throw IllegalStateException("O arquivo baixado do GitHub é inválido ou ainda não foi publicado.")
        }
        val cabecalho = destino.inputStream().use {
            val b = ByteArray(2)
            it.read(b)
            b
        }
        if (cabecalho[0] != 'P'.code.toByte() || cabecalho[1] != 'K'.code.toByte()) {
            destino.delete()
            throw IllegalStateException("O arquivo baixado não é um pacote APK válido.")
        }

        onProgresso(1f)
        destino
    }

    fun podeInstalarPacotes(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun abrirConfiguracaoFontesDesconhecidas(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    fun instalarApk(context: Context, arquivoApk: File): Boolean {
        if (!podeInstalarPacotes(context)) {
            abrirConfiguracaoFontesDesconhecidas(context)
            return false
        }
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            arquivoApk
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return true
    }
}

@Composable
fun DialogoAtualizacaoGitHub(
    info: InfoAtualizacao,
    versaoInstalada: String,
    baixando: Boolean,
    progresso: Float,
    apkPronto: Boolean,
    erro: String?,
    onBaixarOuInstalar: () -> Unit,
    onFechar: () -> Unit
) {
    val linhasNovidades = info.notas
        .split("\n", ";")
        .map { it.trim() }
        .filter { it.isNotBlank() }

    Dialog(onDismissRequest = { if (!baixando) onFechar() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(22.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Cabeçalho com ícone e versões
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .background(
                                MaterialTheme.colorScheme.primaryContainer,
                                RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Atualização",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Atualização via GitHub",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Instalada: v$versaoInstalada → GitHub: v${info.versaoNome}",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Seção com novidades
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .padding(12.dp)
                            .heightIn(max = 200.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "NOVIDADES DA VERSÃO",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )

                        if (linhasNovidades.isNotEmpty()) {
                            linhasNovidades.forEach { item ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .size(16.dp)
                                            .padding(top = 2.dp)
                                    )
                                    Text(
                                        text = item.removePrefix("-").trim(),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = info.notas.ifBlank { "Melhorias gerais e correções de bugs." },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Progresso do download
                if (baixando) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        val pct = (progresso * 100).toInt()
                        Text(
                            text = "Baixando atualização do GitHub... $pct%",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium
                        )
                        LinearProgressIndicator(
                            progress = { progresso.coerceIn(0f, 1f) },
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                        )
                    }
                }

                // Mensagem de erro caso ocorra
                if (!erro.isNullOrBlank()) {
                    Text(
                        text = erro,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Botão de Download / Instalação
                Button(
                    onClick = onBaixarOuInstalar,
                    enabled = !baixando,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = when {
                            baixando -> "Baixando APK..."
                            apkPronto -> "📦 Instalar atualização agora"
                            else -> "⬇ Baixar e atualizar pelo GitHub"
                        },
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }

                // Botão fechar
                if (!baixando) {
                    TextButton(
                        onClick = onFechar,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(
                            text = "Agora não",
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CardAtualizacaoGitHub(
    versaoInstaladaNome: String,
    versaoInstaladaCodigo: Int,
    infoAtualizacao: InfoAtualizacao?,
    temNovaAtualizacao: Boolean,
    statusAtualizacao: String?,
    verificandoAtualizacao: Boolean,
    baixandoAtualizacao: Boolean,
    progressoAtualizacao: Float,
    apkAtualizacaoBaixado: File?,
    onVerificar: () -> Unit,
    onBaixarOuInstalar: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            1.dp,
            if (temNovaAtualizacao) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Atualização do App (GitHub)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Versão instalada: v$versaoInstaladaNome (build $versaoInstaladaCodigo)",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        text = "Repositório: github.com/${AtualizadorGitHub.DONO_REPO}/${AtualizadorGitHub.NOME_REPO}",
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                if (temNovaAtualizacao && infoAtualizacao != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Nova v${infoAtualizacao.versaoNome}",
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            statusAtualizacao?.let { msg ->
                Text(
                    text = msg,
                    color = if (temNovaAtualizacao) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (baixandoAtualizacao) {
                val pct = (progressoAtualizacao * 100).toInt()
                Text(
                    text = "Baixando APK: $pct%",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelSmall
                )
                LinearProgressIndicator(
                    progress = { progressoAtualizacao.coerceIn(0f, 1f) },
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onVerificar,
                    enabled = !verificandoAtualizacao && !baixandoAtualizacao,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    if (verificandoAtualizacao) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Buscando...", fontSize = 12.sp)
                    } else {
                        Text("🔄 Verificar no GitHub", fontSize = 12.sp)
                    }
                }

                if (infoAtualizacao != null) {
                    Button(
                        onClick = onBaixarOuInstalar,
                        enabled = !baixandoAtualizacao,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = when {
                                apkAtualizacaoBaixado != null && apkAtualizacaoBaixado.exists() -> "📦 Instalar"
                                temNovaAtualizacao -> "⬇ Atualizar"
                                else -> "⬇ Reinstalar"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}
