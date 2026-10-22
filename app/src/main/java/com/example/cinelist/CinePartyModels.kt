package com.example.cinelist

/**
 * Representa um participante em uma sessão ativa de CineParty.
 * Inclui campos de presença e estado de voz (preparado para canais estilo Discord).
 */
data class CinePartyParticipante(
    val uid: String = "",
    val nome: String = "Cinéfilo",
    val fotoUrl: String = "",
    val online: Boolean = true,
    val isAnfitriao: Boolean = false,
    val statusVoz: String = "MUTADO", // "FALANDO", "MUTADO", "SURDO" -> Portas para Discord Voice
    val entrouEm: Long = System.currentTimeMillis()
)

/**
 * Representa uma sessão de CineParty sincronizada em tempo real via Firestore.
 * Suporta contagem regressiva ao vivo sincronizada, controle de play/pause coletivo,
 * cascata de reações compartilhadas e lista de membros presentes (estilo sala/Discord).
 */
data class CinePartySessao(
    val id: String = "",
    val salaId: String = "",
    val tituloMidia: String = "",
    val posterMidia: String = "",
    val anfitriaoUid: String = "",
    val anfitriaoNome: String = "",
    val emContagem: Boolean = false,
    val contagemNumero: Int = 3,
    val contagemIniciadaEm: Long = 0L,
    val estadoReproducao: String = "PARADO", // "PARADO", "CONTAGEM", "REPRODUZINDO", "PAUSADO"
    val tempoReproducaoSegundos: Int = 0,
    val ultimaAtualizacaoTimestamp: Long = 0L,
    val participantes: Map<String, Any> = emptyMap(),
    val ultimaReacaoEmoji: String = "",
    val ultimaReacaoAutor: String = "",
    val ultimaReacaoTimestamp: Long = 0L,
    val canalVozAtivo: Boolean = false // Porta para futuro canal de áudio WebRTC / Discord
) {
    fun obterParticipantesMapeados(): List<CinePartyParticipante> {
        val lista = mutableListOf<CinePartyParticipante>()
        participantes.forEach { (uid, dados) ->
            if (dados is Map<*, *>) {
                lista.add(
                    CinePartyParticipante(
                        uid = uid,
                        nome = dados["nome"] as? String ?: "Cinéfilo",
                        fotoUrl = dados["fotoUrl"] as? String ?: "",
                        online = dados["online"] as? Boolean ?: true,
                        isAnfitriao = (uid == anfitriaoUid) || (dados["isAnfitriao"] as? Boolean ?: false),
                        statusVoz = dados["statusVoz"] as? String ?: "MUTADO",
                        entrouEm = (dados["entrouEm"] as? Number)?.toLong() ?: 0L
                    )
                )
            }
        }
        return lista.sortedWith(
            compareByDescending<CinePartyParticipante> { it.isAnfitriao }
                .thenByDescending { it.online }
                .thenBy { it.nome }
        )
    }
}
