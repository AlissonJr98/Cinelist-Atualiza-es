package com.example.cinelist

/**
 * Sugestão misteriosa adicionada por um participante na rodada do CineBlind.
 * Durante a votação, apenas as pistas e o gênero ficam visíveis para a sala!
 */
data class SugestaoBlind(
    val id: String = "",
    val autorUid: String = "",
    val autorNome: String = "Cinéfilo Misterioso",
    val tituloReal: String = "",
    val posterReal: String = "",
    val idTmdb: Int = 0,
    val genero: String = "Não Informado",
    val ano: String = "",
    val pistas: List<String> = emptyList(),
    val votos: List<String> = emptyList() // Lista de UIDs que votaram nesta sugestão
) {
    val totalVotos: Int
        get() = votos.size
}

/**
 * Sessão de CineBlind (Amigo Oculto / Sessão Secreta) sincronizada na Sala Compartilhada via Firestore.
 * Estados:
 * - "INSCRICAO": Cada participante adiciona secretamente um filme com pistas.
 * - "VOTACAO": Os membros leem as pistas misteriosas e votam na melhor.
 * - "REVELADO": O filme campeão é desvendado com pôster, título e botão de iniciar CineParty!
 */
data class CineBlindSessao(
    val id: String = "",
    val grupoId: String = "",
    val criadorUid: String = "",
    val criadorNome: String = "",
    val estado: String = "INSCRICAO", // "INSCRICAO", "VOTACAO", "REVELADO"
    val sugestoes: Map<String, Map<String, Any>> = emptyMap(),
    val vencedorId: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    fun obterSugestoesMapeadas(): List<SugestaoBlind> {
        val lista = mutableListOf<SugestaoBlind>()
        sugestoes.forEach { (id, mapa) ->
            try {
                @Suppress("UNCHECKED_CAST")
                val pistasLista = (mapa["pistas"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
                @Suppress("UNCHECKED_CAST")
                val votosLista = (mapa["votos"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()

                lista.add(
                    SugestaoBlind(
                        id = id,
                        autorUid = mapa["autorUid"] as? String ?: "",
                        autorNome = mapa["autorNome"] as? String ?: "Cinéfilo",
                        tituloReal = mapa["tituloReal"] as? String ?: "",
                        posterReal = mapa["posterReal"] as? String ?: "",
                        idTmdb = (mapa["idTmdb"] as? Number)?.toInt() ?: 0,
                        genero = mapa["genero"] as? String ?: "Cinema",
                        ano = mapa["ano"] as? String ?: "",
                        pistas = pistasLista,
                        votos = votosLista
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return lista.sortedByDescending { it.totalVotos }
    }

    val vencedora: SugestaoBlind?
        get() = obterSugestoesMapeadas().firstOrNull { it.id == vencedorId }
            ?: obterSugestoesMapeadas().firstOrNull()
}
