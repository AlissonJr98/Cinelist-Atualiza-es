package com.example.cinelist

/**
 * Representa um país carimbado no Passaporte Cinéfilo.
 */
data class PaisCarimbo(
    val codigoIso: String,
    val nome: String,
    val bandeira: String,
    val totalObras: Int,
    val titulosExemplo: List<String> = emptyList()
)

/**
 * Resumo do status do Passaporte Cinéfilo do usuário.
 */
data class StatusPassaporte(
    val totalPaisesCarimbados: Int,
    val totalObrasGringasELocais: Int,
    val porcentagemMundial: Float,
    val patenteViajante: String,
    val paisesCarimbados: List<PaisCarimbo>,
    val paisMaisAssistido: PaisCarimbo?,
    val destinosSugeridos: List<DestinoSugerido>
)

data class DestinoSugerido(
    val pais: String,
    val bandeira: String,
    val recomendacaoTitulo: String,
    val motivo: String
)

object PassaporteCinefiloManager {

    // Mapeamento dos principais polos cinematográficos mundiais
    private val PAISES_CONHECIDOS = mapOf(
        "US" to Pair("Estados Unidos", "🇺🇸"),
        "BR" to Pair("Brasil", "🇧🇷"),
        "KR" to Pair("Coreia do Sul", "🇰🇷"),
        "JP" to Pair("Japão", "🇯🇵"),
        "GB" to Pair("Reino Unido", "🇬🇧"),
        "FR" to Pair("França", "🇫🇷"),
        "IT" to Pair("Itália", "🇮🇹"),
        "ES" to Pair("Espanha", "🇪🇸"),
        "DE" to Pair("Alemanha", "🇩🇪"),
        "MX" to Pair("México", "🇲🇽"),
        "CA" to Pair("Canadá", "🇨🇦"),
        "IN" to Pair("Índia", "🇮🇳"),
        "AU" to Pair("Austrália", "🇦🇺"),
        "DK" to Pair("Dinamarca", "🇩🇰"),
        "SE" to Pair("Suécia", "🇸🇪"),
        "AR" to Pair("Argentina", "🇦🇷"),
        "CN" to Pair("China", "🇨🇳"),
        "IE" to Pair("Irlanda", "🇮🇪"),
        "CL" to Pair("Chile", "🇨🇱"),
        "CO" to Pair("Colômbia", "🇨🇴")
    )

    private val RECOMENDACOES_POR_PAIS = listOf(
        DestinoSugerido("França", "🇫🇷", "O Fabuloso Destino de Amélie Poulain", "Um clássico poético imperdível"),
        DestinoSugerido("Coreia do Sul", "🇰🇷", "Parasita", "Ganhador do Oscar de Melhor Filme"),
        DestinoSugerido("Japão", "🇯🇵", "A Viagem de Chihiro", "Obra-prima do Studio Ghibli"),
        DestinoSugerido("Brasil", "🇧🇷", "Central do Brasil", "Um dos maiores marcos do cinema nacional"),
        DestinoSugerido("Itália", "🇮🇹", "Cinema Paradiso", "Uma declaração de amor ao cinema"),
        DestinoSugerido("Espanha", "🇪🇸", "O Labirinto do Fauno", "Fantasia sombria de Guillermo del Toro"),
        DestinoSugerido("Alemanha", "🇩🇪", "A Vida dos Outros", "Suspense histórico fascinante"),
        DestinoSugerido("Argentina", "🇦🇷", "O Segredo dos Seus Olhos", "Obra-prima argentina vencedora do Oscar"),
        DestinoSugerido("Dinamarca", "🇩🇰", "Druk: Mais Uma Rodada", "Comédia dramática aclamada internacionalmente"),
        DestinoSugerido("Índia", "🇮🇳", "RRR", "Ação épica e espetáculo visual de tirar o fôlego")
    )

    /**
     * Analisa a biblioteca de mídias concluídas e gera a telemetria do Passaporte Cinéfilo.
     */
    fun calcularPassaporte(midias: List<Midia>): StatusPassaporte {
        val concluidas = midias.filter { it.obterStatusEnum() == StatusMidia.CONCLUIDO }
        val contagemPorPais = mutableMapOf<String, MutableList<String>>()

        for (midia in concluidas) {
            val iso = inferirPais(midia)
            val lista = contagemPorPais.getOrPut(iso) { mutableListOf() }
            if (lista.size < 3) {
                lista.add(midia.titulo)
            }
        }

        // Se o usuário ainda não tiver concluído nada ou pouca coisa, garante um exemplo mínimo
        if (contagemPorPais.isEmpty() && midias.isNotEmpty()) {
            for (m in midias.take(3)) {
                val iso = inferirPais(m)
                contagemPorPais.getOrPut(iso) { mutableListOf() }.add(m.titulo)
            }
        }

        val listaCarimbos = contagemPorPais.map { (iso, titulos) ->
            val info = PAISES_CONHECIDOS[iso] ?: Pair("Internacional", "🌐")
            PaisCarimbo(
                codigoIso = iso,
                nome = info.first,
                bandeira = info.second,
                totalObras = titulos.size,
                titulosExemplo = titulos
            )
        }.sortedByDescending { it.totalObras }

        val totalPaises = listaCarimbos.size
        val totalMundoEstimado = 195 // Total de países reconhecidos
        val porcentagem = ((totalPaises.toFloat() / 25f) * 100f).coerceAtMost(100f)

        val patente = when {
            totalPaises >= 15 -> "🌟 Embaixador Global do Cinema"
            totalPaises >= 8 -> "✈️ Viajante Transcontinental"
            totalPaises >= 4 -> "🧭 Explorador Cosmopolita"
            totalPaises >= 2 -> "🎫 Mochileiro de Maratonas"
            else -> "🏠 Cinéfilo Local"
        }

        val paisMaisAssistido = listaCarimbos.firstOrNull()

        // Sugere países que o usuário AINDA NÃO carimbou
        val carimbadosNomes = listaCarimbos.map { it.nome }.toSet()
        val sugestoes = RECOMENDACOES_POR_PAIS.filter { it.pais !in carimbadosNomes }.take(3)
            .ifEmpty { RECOMENDACOES_POR_PAIS.take(3) }

        return StatusPassaporte(
            totalPaisesCarimbados = totalPaises,
            totalObrasGringasELocais = concluidas.size,
            porcentagemMundial = porcentagem,
            patenteViajante = patente,
            paisesCarimbados = listaCarimbos,
            paisMaisAssistido = paisMaisAssistido,
            destinosSugeridos = sugestoes
        )
    }

    /**
     * Infere o país com base nos metadados da mídia (tipo, tags, idioma ou título).
     */
    private fun inferirPais(midia: Midia): String {
        val tipo = midia.tipo.lowercase()
        val genero = midia.genero.lowercase()
        val sinopse = midia.sinopse.lowercase()
        val titulo = midia.titulo.lowercase()

        return when {
            tipo.contains("anime") || genero.contains("anime") || sinopse.contains("japão") || sinopse.contains("tóquio") -> "JP"
            tipo.contains("dorama") || sinopse.contains("coreia") || sinopse.contains("seul") -> "KR"
            tipo.contains("novela") || sinopse.contains("brasil") || sinopse.contains("globo") || sinopse.contains("rio de janeiro") || sinopse.contains("são paulo") -> "BR"
            sinopse.contains("frança") || sinopse.contains("paris") || sinopse.contains("francês") -> "FR"
            sinopse.contains("inglaterra") || sinopse.contains("londres") || sinopse.contains("britânico") -> "GB"
            sinopse.contains("itália") || sinopse.contains("roma") || sinopse.contains("italiano") -> "IT"
            sinopse.contains("espanha") || sinopse.contains("madri") || sinopse.contains("espanhol") -> "ES"
            sinopse.contains("alemanha") || sinopse.contains("berlim") || sinopse.contains("alemão") -> "DE"
            sinopse.contains("méxico") || sinopse.contains("mexicano") -> "MX"
            sinopse.contains("argentina") || sinopse.contains("buenos aires") -> "AR"
            // Por padrão, produções ocidentais de grande circuito caem em US
            else -> "US"
        }
    }
}
