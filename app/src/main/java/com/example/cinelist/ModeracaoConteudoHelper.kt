package com.example.cinelist

import android.content.Context
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.Normalizer
import java.util.UUID

/**
 * Resultado da moderação preventiva de conteúdo familiar e comunitário.
 */
data class ResultadoModeracao(
    val aprovado: Boolean,
    val motivoBloqueio: String = ""
)

/**
 * Utilitário central de moderação de conteúdo, proteção anti-spoiler, filtro anti-spam,
 * detecção de ofensas, política, religião e sistema de denúncia para o CineList (App para Família).
 */
object ModeracaoConteudoHelper {

    // Palavras e ofensas proibidas (match exato em tokens isolados)
    private val TERMOS_PROIBIDOS_TOKENS = setOf(
        // Ofensas e xingamentos graves em Português
        "arrombado", "arrombada", "arrombados", "arrombadas",
        "babaca", "babacas", "bastardo", "bastarda", "biscate", "biscates",
        "boiola", "boiolas", "bosta", "bostas", "buceta", "bucetas", "bucetinha",
        "canalha", "canalhas", "caralho", "caralhos", "caralhada",
        "chifrudo", "chifruda", "corno", "cornos", "cornudo", "cretino", "cretina",
        "cuzao", "cuzaos", "cuzudo", "escroto", "escrota", "escrotos", "escrotas",
        "fdp", "foda", "foder", "fodeu", "fudido", "fudida", "fudidos", "fudidas",
        "fodase", "fodasse", "fodam-se", "foda-se",
        "imbecil", "imbecis", "merda", "merdas", "merdinha", "merdinhas",
        "otario", "otaria", "otarios", "otarias",
        "panaca", "panacas", "parasita", "piranha", "piranhas",
        "porra", "porras", "prostituta", "prostitutas",
        "puta", "putas", "putaria", "putarias", "puto", "putos",
        "quenga", "quengas", "safado", "safada", "safados", "safadas",
        "tarado", "tarada", "tarados", "taradas",
        "trouxa", "trouxas", "vagabundo", "vagabunda", "vagabundos", "vagabundas",
        "viado", "viados", "viadinho", "viadinhos", "xoxota", "xoxotas",
        "desgracado", "desgracada", "desgracados", "desgracadas",
        "vsf", "vtnc",
        // English common profanities
        "asshole", "bitch", "bitches", "bastard", "cunt", "cunts", "dick", "dicks",
        "faggot", "fuck", "fucker", "fucking", "motherfucker", "nigger", "nigga",
        "pussy", "retard", "shit", "slut", "sluts", "whore", "whores", "cocksucker"
    )

    // Expressões proibidas compostas ou concatenadas
    private val EXPRESSOES_PROIBIDAS_COMPOSTAS = listOf(
        "filho da puta", "filha da puta", "filhodaputa", "filhadaputa", "fela da puta",
        "vai se foder", "vaisefoder", "vai tomar no cu", "vaitomarnocu",
        "vai se fuder", "vaisefuder", "pra puta que pariu", "puta que pariu", "pqp"
    )

    // Termos e figuras de disputa política partidária para ambiente familiar
    private val TERMOS_POLITICA_TOKENS = setOf(
        "bolsonaro", "bolsonarista", "bolsonaristas", "bolsonarismo",
        "lula", "lulista", "lulistas", "lulismo", "lulopetismo",
        "petista", "petistas", "petralha", "petralhas",
        "bolsominion", "bolsominions",
        "esquerdalha", "direitralha",
        "marcal", "tarcisio", "haddad", "boulos",
        "comunista", "comunismo", "fascista", "fascismo", "nazista", "nazismo"
    )

    // Expressões políticas compostas
    private val EXPRESSOES_POLITICA_COMPOSTAS = listOf(
        "faz o l", "faz o 22", "mito 22", "lula livre",
        "urna eletronica", "voto impresso", "fraude nas urnas", "fraude na urna",
        "intervencao militar", "patriota do quartel", "stf vergonha",
        "eleicoes 2026", "eleicao 2026", "partido dos trabalhadores"
    )

    // Expressões de intolerância ou disputa religiosa (evita banir filmes como Cidade de Deus ou O Auto da Compadecida)
    private val EXPRESSOES_RELIGIAO_OFENSIVAS = listOf(
        "crente safado", "crente lixo", "ateu de merda", "religiao lixo", "seita maldita",
        "chuta que e macumba", "padre pedofilo", "pastor ladrao", "bancada evangelica",
        "intolerancia religiosa", "vai pro inferno pecador", "convertei-vos", "aceite jesus ou queime",
        "malditos cristaos", "malditos evangelicos", "malditos catolicos", "malditos ateus"
    )

    // Padrões de spoiler em filmes e séries
    private val PADROES_SPOILER = listOf(
        "morre no final", "morreu no final", "quem morre", "quem morreu", "acaba morrendo", "termina morrendo",
        "o assassino e", "o assassino era", "a assassina e", "a assassina era", "o culpado e", "o traidor e",
        "o vilao e", "a vila e", "o monstro e na verdade",
        "final do filme", "no final do filme", "no fim do filme", "o final e que", "o desfecho do filme",
        "no final ele morre", "no final ela morre", "no final eles morrem",
        "plot twist", "plottwist", "reviravolta no final", "cena pos creditos", "pos creditos", "pos-creditos",
        "revela que ele e", "revela que ela e", "revelou que o pai", "descobre que e filho",
        "descobre que o pai", "descobre que a mae", "sobrevive no final", "ninguem sobrevive",
        "todos morrem no final", "todo mundo morre"
    )

    // Domínios conhecidos de spam, phishing e links externos
    private val DOMINIOS_SPAM = listOf(
        "bit.ly", "tinyurl.com", "t.me/", "wa.me/", "goo.gl", "cutt.ly",
        "is.gd", "rb.gy", "shorturl.at", "t.co", "chat.whatsapp.com"
    )

    /**
     * Normaliza a string para verificação:
     * - Converte para minúsculas
     * - Remove acentos
     * - Converte leetspeak comum (4/@ -> a, 3 -> e, 1/! -> i, 0 -> o, $/5 -> s, 7 -> t)
     */
    fun normalizarTexto(texto: String): String {
        var t = texto.lowercase()
        // Substituição inteligente de leetspeak
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
     * Identifica automaticamente se o texto contém frases clássicas de spoilers sobre enredo ou final da obra.
     */
    fun detectarPossivelSpoiler(texto: String): Boolean {
        if (texto.isBlank()) return false
        val norm = normalizarTexto(texto)
        for (padrao in PADROES_SPOILER) {
            if (norm.contains(padrao)) return true
        }
        return false
    }

    /**
     * Valida um texto para comunidade familiar:
     * - Filtra ofensas, palavrões e xingamentos
     * - Filtra debates partidários e figuras políticas
     * - Filtra intolerância religiosa
     * - Filtra spam e links externos
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
            return ResultadoModeracao(false, "O texto excede o limite máximo de 2.500 caracteres.")
        }

        // 1. Verificação de Caracteres Repetidos Excessivos (anti-spam)
        if (Regex("([a-zA-Z0-9])\\1{8,}").containsMatchIn(limpo)) {
            return ResultadoModeracao(false, "Conteúdo identificado como spam (repetição excessiva de caracteres).")
        }

        // 2. Verificação de Palavras Repetidas em Excesso
        val palavrasOriginais = limpo.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (palavrasOriginais.size >= 4) {
            val mapaContagem = palavrasOriginais.groupingBy { it.lowercase() }.eachCount()
            val maxRepeticao = mapaContagem.values.maxOrNull() ?: 0
            if (maxRepeticao > 8 || (palavrasOriginais.size >= 6 && maxRepeticao.toFloat() / palavrasOriginais.size > 0.6f)) {
                return ResultadoModeracao(false, "Conteúdo identificado como spam (repetição de palavras).")
            }
        }

        // 3. Verificação de Links e Phishing
        val textoLower = limpo.lowercase()
        for (dom in DOMINIOS_SPAM) {
            if (textoLower.contains(dom)) {
                return ResultadoModeracao(false, "Não é permitido divulgar links ou grupos externos no fórum.")
            }
        }

        val textoNormalizado = normalizarTexto(limpo)

        // 4. Verificação de Expressões Ofensivas Compostas
        for (expr in EXPRESSOES_PROIBIDAS_COMPOSTAS) {
            if (textoNormalizado.contains(expr)) {
                return ResultadoModeracao(
                    false,
                    "Para manter o CineList seguro para toda a família, palavras de baixo calão e ofensas não são permitidas."
                )
            }
        }

        // 5. Verificação de Expressões Políticas Compostas
        for (expr in EXPRESSOES_POLITICA_COMPOSTAS) {
            if (textoNormalizado.contains(expr)) {
                return ResultadoModeracao(
                    false,
                    "Para manter o foco em filmes e preservar o ambiente familiar, discussões e termos políticos não são permitidos."
                )
            }
        }

        // 6. Verificação de Ataques Religiosos Compostos
        for (expr in EXPRESSOES_RELIGIAO_OFENSIVAS) {
            if (textoNormalizado.contains(expr)) {
                return ResultadoModeracao(
                    false,
                    "Para manter a harmonia familiar, discussões ou intolerância religiosa não são permitidas."
                )
            }
        }

        // 7. Verificação de Palavras Proibidas e Política via Tokens Isolados (evita falsos positivos como computador/disputa/Cidade de Deus)
        val tokens = textoNormalizado.split(Regex("[^a-z0-9]+")).filter { it.isNotBlank() }
        for (token in tokens) {
            // Colapsa repetição de letras no token (ex: puuuuutaaaa -> puta)
            val tokenColapsado = Regex("(.)\\1{2,}").replace(token, "$1")

            if (TERMOS_PROIBIDOS_TOKENS.contains(token) || TERMOS_PROIBIDOS_TOKENS.contains(tokenColapsado)) {
                return ResultadoModeracao(
                    false,
                    "Para manter o CineList seguro para toda a família, palavras de baixo calão e ofensas não são permitidas."
                )
            }

            if (TERMOS_POLITICA_TOKENS.contains(token) || TERMOS_POLITICA_TOKENS.contains(tokenColapsado)) {
                return ResultadoModeracao(
                    false,
                    "Para manter o foco em filmes e preservar o ambiente familiar, discussões e termos políticos não são permitidos."
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
