package com.example.cinelist

data class AvaliacaoMembro(
    val autorUid: String = "",
    val autorNome: String = "",
    val autorFoto: String = "",
    val nota: Int = 0,
    val comentario: String = "",
    val contemSpoiler: Boolean = false,
    val dataAtualizacao: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any> = mapOf(
        "autorUid" to autorUid,
        "autorNome" to autorNome,
        "autorFoto" to autorFoto,
        "nota" to nota,
        "comentario" to comentario,
        "contemSpoiler" to contemSpoiler,
        "dataAtualizacao" to dataAtualizacao
    )

    companion object {
        fun fromMap(map: Map<String, Any?>): AvaliacaoMembro {
            return AvaliacaoMembro(
                autorUid = map["autorUid"] as? String ?: "",
                autorNome = map["autorNome"] as? String ?: "",
                autorFoto = map["autorFoto"] as? String ?: "",
                nota = (map["nota"] as? Long)?.toInt() ?: (map["nota"] as? Int) ?: 0,
                comentario = map["comentario"] as? String ?: "",
                contemSpoiler = (map["contemSpoiler"] as? Boolean) ?: false,
                dataAtualizacao = (map["dataAtualizacao"] as? Long) ?: System.currentTimeMillis()
            )
        }
    }
}