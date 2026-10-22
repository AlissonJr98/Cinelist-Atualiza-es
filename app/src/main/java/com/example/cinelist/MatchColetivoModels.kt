package com.example.cinelist

import com.google.firebase.firestore.PropertyName

data class FilmeCandidatoMatch(
    @get:PropertyName("idTmdb") @set:PropertyName("idTmdb") var idTmdb: Int = 0,
    @get:PropertyName("titulo") @set:PropertyName("titulo") var titulo: String = "",
    @get:PropertyName("posterUrl") @set:PropertyName("posterUrl") var posterUrl: String = "",
    @get:PropertyName("ano") @set:PropertyName("ano") var ano: String = "",
    @get:PropertyName("genero") @set:PropertyName("genero") var genero: String = "",
    @get:PropertyName("sinopse") @set:PropertyName("sinopse") var sinopse: String = "",
    @get:PropertyName("nota") @set:PropertyName("nota") var nota: Double = 0.0
) {
    constructor() : this(0, "", "", "", "", "", 0.0)

    fun toMap(): Map<String, Any> {
        return mapOf(
            "idTmdb" to idTmdb,
            "titulo" to titulo,
            "posterUrl" to posterUrl,
            "ano" to ano,
            "genero" to genero,
            "sinopse" to sinopse,
            "nota" to nota
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any?>): FilmeCandidatoMatch {
            return FilmeCandidatoMatch(
                idTmdb = (map["idTmdb"] as? Number)?.toInt() ?: 0,
                titulo = map["titulo"] as? String ?: "",
                posterUrl = map["posterUrl"] as? String ?: "",
                ano = map["ano"] as? String ?: "",
                genero = map["genero"] as? String ?: "",
                sinopse = map["sinopse"] as? String ?: "",
                nota = (map["nota"] as? Number)?.toDouble() ?: 0.0
            )
        }
    }
}

data class MatchColetivoSessao(
    @get:PropertyName("id") @set:PropertyName("id") var id: String = "",
    @get:PropertyName("grupoId") @set:PropertyName("grupoId") var grupoId: String = "",
    @get:PropertyName("criadorUid") @set:PropertyName("criadorUid") var criadorUid: String = "",
    @get:PropertyName("criadorNome") @set:PropertyName("criadorNome") var criadorNome: String = "",
    @get:PropertyName("estado") @set:PropertyName("estado") var estado: String = "ATIVO", // "ATIVO", "MATCH", "FINALIZADO"
    @get:PropertyName("filmes") @set:PropertyName("filmes") var filmes: List<FilmeCandidatoMatch> = emptyList(),
    // Map de idTmdb (String) para lista de UIDs que votaram SIM
    @get:PropertyName("votosSim") @set:PropertyName("votosSim") var votosSim: Map<String, List<String>> = emptyMap(),
    @get:PropertyName("vencedorIdTmdb") @set:PropertyName("vencedorIdTmdb") var vencedorIdTmdb: Int = 0,
    @get:PropertyName("vencedorTitulo") @set:PropertyName("vencedorTitulo") var vencedorTitulo: String = "",
    @get:PropertyName("vencedorPoster") @set:PropertyName("vencedorPoster") var vencedorPoster: String = "",
    @get:PropertyName("timestamp") @set:PropertyName("timestamp") var timestamp: Long = System.currentTimeMillis()
) {
    constructor() : this("", "", "", "", "ATIVO")

    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "grupoId" to grupoId,
            "criadorUid" to criadorUid,
            "criadorNome" to criadorNome,
            "estado" to estado,
            "filmes" to filmes.map { it.toMap() },
            "votosSim" to votosSim,
            "vencedorIdTmdb" to vencedorIdTmdb,
            "vencedorTitulo" to vencedorTitulo,
            "vencedorPoster" to vencedorPoster,
            "timestamp" to timestamp
        )
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun fromMap(map: Map<String, Any?>): MatchColetivoSessao {
            val filmesList = (map["filmes"] as? List<Map<String, Any?>>)?.map {
                FilmeCandidatoMatch.fromMap(it)
            } ?: emptyList()

            val votosRaw = map["votosSim"] as? Map<String, List<String>> ?: emptyMap()

            return MatchColetivoSessao(
                id = map["id"] as? String ?: "",
                grupoId = map["grupoId"] as? String ?: "",
                criadorUid = map["criadorUid"] as? String ?: "",
                criadorNome = map["criadorNome"] as? String ?: "",
                estado = map["estado"] as? String ?: "ATIVO",
                filmes = filmesList,
                votosSim = votosRaw,
                vencedorIdTmdb = (map["vencedorIdTmdb"] as? Number)?.toInt() ?: 0,
                vencedorTitulo = map["vencedorTitulo"] as? String ?: "",
                vencedorPoster = map["vencedorPoster"] as? String ?: "",
                timestamp = (map["timestamp"] as? Number)?.toLong() ?: 0L
            )
        }
    }
}
