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
    val sugestoes: Map<String, Any> = emptyMap(),
    val vencedorId: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    fun obterSugestoesMapeadas(): List<SugestaoBlind> {
        val lista = mutableListOf<SugestaoBlind>()
        sugestoes.forEach { (chaveId, valor) ->
            try {
                if (valor is Map<*, *>) {
                    @Suppress("UNCHECKED_CAST")
                    val pistasLista = (valor["pistas"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
                    @Suppress("UNCHECKED_CAST")
                    val votosLista = (valor["votos"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()

                    val idFinal = (valor["id"] as? String)?.ifBlank { chaveId } ?: chaveId
                    lista.add(
                        SugestaoBlind(
                            id = idFinal,
                            autorUid = valor["autorUid"] as? String ?: "",
                            autorNome = valor["autorNome"] as? String ?: "Cinéfilo",
                            tituloReal = valor["tituloReal"] as? String ?: "",
                            posterReal = valor["posterReal"] as? String ?: "",
                            idTmdb = (valor["idTmdb"] as? Number)?.toInt() ?: 0,
                            genero = valor["genero"] as? String ?: "Cinema",
                            ano = valor["ano"] as? String ?: "",
                            pistas = pistasLista,
                            votos = votosLista
                        )
                    )
                }
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
