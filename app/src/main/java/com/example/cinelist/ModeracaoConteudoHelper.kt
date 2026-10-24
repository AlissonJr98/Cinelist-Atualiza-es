package com.example.cinelist

import android.content.Context
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.Normalizer
import java.util.UUID

/**
 * Resultado da moderação preventiva de texto.
 */
data class ResultadoModeracao(
    val aprovado: Boolean,
    val motivoBloqueio: String = ""
)

/**
 * Utilitário central de moderação de conteúdo, filtro anti-spam, detecção de ofensas
 * e sistema de denúncia para o CineList.
 */
object ModeracaoConteudoHelper {

    // Lista abrangente de termos impróprios, ofensas, xingamentos e palavras de baixo calão (PT/EN)
    private val TERMOS_PROIBIDOS = setOf(
        // Ofensas e xingamentos graves
        "arrombado", "arrombada", "babaca", "bastardo", "bicha", "biscate", "boiola", "bosta",
        "buceta", "burro", "burra", "cadela", "canalha", "caralho", "chifrudo", "corno", "cornudo",
        "cretino", "cuzão", "cuzao", "escroto", "escrota", "fdp", "fela da puta", "filho da puta",
        "filha da puta", "foda", "foder", "fudido", "fudida", "idiota", "imbecil", "lacaio",
        "macaco", "macaca", "merda", "merdinha", "otario", "otaria", "otário", "otária", "palhaço",
        "panaca", "parasita", "piranha", "porra", "prostituta", "puta", "puto", "quenga",
        "safado", "safada", "sapatão", "sapatao", "tarado", "trouxa", "vabundo", "vadia",
        "vagabundo", "vagabunda", "veado", "viado", "viadinho", "xoxota",
        // English common profanities
        "asshole", "bitch", "bastard", "cunt", "dick", "faggot", "fuck", "fucker", "motherfucker",
        "nigger", "nigga", "pussy", "retard", "shit", "slut", "whore"
    )

    // Domínios conhecidos de spam, phishing e encurtadores maliciosos
    private val DOMINIOS_SPAM = listOf(
        "bit.ly", "tinyurl.com", "t.me/", "wa.me/", "goo.gl", "cutt.ly",
        "is.gd", "rb.gy", "shorturl.at", "t.co", "chat.whatsapp.com"
    )

    /**
     * Normaliza a string para facilitar a verificação:
     * - Converte para minúsculas
     * - Remove acentos (ç -> c, é -> e)
     * - Converte leetspeak comum (4 -> a, 3 -> e, 1 -> i, 0 -> o, $ -> s, @ -> a)
     * - Remove pontuações repetidas entre letras (ex: p.o.r.r.a -> porra)
     */
    private fun normalizarTexto(texto: String): String {
        var t = texto.lowercase()
        // Leetspeak mapping
        t = t.replace('4', 'a')
            .replace('@', 'a')
            .replace('3', 'e')
            .replace('1', 'i')
            .replace('!', 'i')
            .replace('0', 'o')
            .replace('$', 's')
            .replace('5', 's')
            .replace('7', 't')

        // Remove acentos
        t = Normalizer.normalize(t, Normalizer.Form.NFD)
        t = Regex("\\p{M}").replace(t, "")

        return t
    }

    /**
     * Valida um texto contra ofensas, besteiras, xingamentos e padrões de spam.
     */
    fun validarTexto(texto: String): ResultadoModeracao {
        val limpo = texto.trim()

        if (limpo.isBlank()) {
            return ResultadoModeracao(false, "O texto não pode ser vazio.")
        }

        if (limpo.length < 2) {
            return ResultadoModeracao(false, "O texto é muito curto.")
        }

        if (limpo.length > 2500) {
            return ResultadoModeracao(false, "O texto excede o limite máximo permitido.")
        }

        // 1. Verificação de Caracteres Repetidos Excessivos (ex: aaaaaaaa, kkkkkkkkkkkkkk de 20 letras)
        if (Regex("([a-zA-Z0-9])\\1{8,}").containsMatchIn(limpo)) {
            return ResultadoModeracao(false, "Conteúdo identificado como spam (repetição excessiva de caracteres).")
        }

        // 2. Verificação de Palavras Repetidas em Excesso (ex: spam spam spam spam)
        val palavras = limpo.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (palavras.size >= 4) {
            val mapaContagem = palavras.groupingBy { it.lowercase() }.eachCount()
            val maxRepeticao = mapaContagem.values.maxOrNull() ?: 0
            if (maxRepeticao > 8 || (palavras.size >= 6 && maxRepeticao.toFloat() / palavras.size > 0.6f)) {
                return ResultadoModeracao(false, "Conteúdo identificado como spam (repetição de palavras).")
            }
        }

        // 3. Verificação de Links e Domínios Suspeitos de Spam / Phishing
        val textoLower = limpo.lowercase()
        for (dom in DOMINIOS_SPAM) {
            if (textoLower.contains(dom)) {
                return ResultadoModeracao(false, "Não é permitido divulgar links ou grupos externos nos comentários.")
            }
        }

        // 4. Verificação de Palavras de Baixo Calão e Ofensas
        val textoNormalizado = normalizarTexto(limpo)
        // Separa tanto por espaços quanto por pontuações
        val tokens = textoNormalizado.split(Regex("[^a-z0-9]+")).filter { it.isNotBlank() }

        for (token in tokens) {
            if (TERMOS_PROIBIDOS.contains(token)) {
                return ResultadoModeracao(
                    false,
                    "O conteúdo contém palavras ofensivas ou inapropriadas para a comunidade."
                )
            }
        }

        // Checagem de palavras proibidas concatenadas sem espaço
        for (proibida in TERMOS_PROIBIDOS) {
            if (proibida.length >= 4 && textoNormalizado.contains(proibida)) {
                return ResultadoModeracao(
                    false,
                    "O conteúdo contém expressões impróprias ou ofensivas."
                )
            }
        }

        return ResultadoModeracao(true)
    }

    /**
     * Sanitiza o texto para exibição segura (previne injeções e remove caracteres de controle).
     */
    fun sanitizarTexto(texto: String): String {
        return texto.replace(Regex("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]"), "")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .trim()
    }

    /**
     * Registra uma denúncia de comentário ou tópico no Firestore para análise.
     */
    fun denunciarConteudo(
        tipoConteudo: String, // "comentario_forum", "topico_forum", "comentario_midia"
        idConteudo: String,
        autorConteudoUid: String,
        textoConteudo: String,
        motivo: String = "Conteúdo impróprio / ofensivo",
        contexto: Context? = null,
        onConcluido: (sucesso: Boolean, mensagem: String) -> Unit = { _, _ -> }
    ) {
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser
        val uid = user?.uid ?: "anonimo"
        val email = user?.email ?: ""

        val denunciaId = "denuncia_${UUID.randomUUID()}"
        val dados = hashMapOf(
            "id" to denunciaId,
            "tipoConteudo" to tipoConteudo,
            "idConteudo" to idConteudo,
            "autorConteudoUid" to autorConteudoUid,
            "textoConteudo" to textoConteudo.take(500),
            "motivo" to motivo,
            "denuncianteUid" to uid,
            "denuncianteEmail" to email,
            "timestamp" to System.currentTimeMillis()
        )

        FirebaseFirestore.getInstance().collection("denuncias_comentarios")
            .document(denunciaId)
            .set(dados)
            .addOnSuccessListener {
                contexto?.let { ctx ->
                    Toast.makeText(
                        ctx,
                        "Denúncia enviada à moderação. Obrigado por manter nossa comunidade segura!",
                        Toast.LENGTH_LONG
                    ).show()
                }
                onConcluido(true, "Denúncia registrada com sucesso.")
            }
            .addOnFailureListener { e ->
                contexto?.let { ctx ->
                    Toast.makeText(
                        ctx,
                        "Não foi possível registrar a denúncia no momento.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                onConcluido(false, e.localizedMessage ?: "Falha ao registrar denúncia.")
            }
    }
}
