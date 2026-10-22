package com.example.cinelist

data class FilmeDiretor(
    val titulo: String,
    val ano: Int,
    val idTmdb: Int,
    val genero: String
)

data class DiretorMestre(
    val id: String,
    val nome: String,
    val fotoEmoji: String,
    val estilo: String,
    val bio: String,
    val badgeRecompensa: String,
    val filmes: List<FilmeDiretor>
)

data class StatusDiretor(
    val diretor: DiretorMestre,
    val totalAssistidos: Int,
    val porcentagem: Float,
    val completo: Boolean
)

object MestresCinemaManager {

    val DIRETORES = listOf(
        DiretorMestre(
            id = "nolan",
            nome = "Christopher Nolan",
            fotoEmoji = "⏳",
            estilo = "Ficção não-linear & Práticos Grandiosos",
            bio = "Mestre da manipulação do tempo, reviravoltas complexas e efeitos práticos monumentais.",
            badgeRecompensa = "Guardião do Tempo (Nolan)",
            filmes = listOf(
                FilmeDiretor("Oppenheimer", 2023, 872585, "Drama/História"),
                FilmeDiretor("Tenet", 2020, 577922, "Ação/Sci-Fi"),
                FilmeDiretor("Dunkirk", 2017, 374720, "Guerra/História"),
                FilmeDiretor("Interestelar", 2014, 157336, "Sci-Fi/Drama"),
                FilmeDiretor("Batman: O Cavaleiro das Trevas Ressurge", 2012, 49026, "Ação/Drama"),
                FilmeDiretor("A Origem", 2010, 27205, "Sci-Fi/Ação"),
                FilmeDiretor("Batman: O Cavaleiro das Trevas", 2008, 155, "Ação/Crime"),
                FilmeDiretor("O Grande Truque", 2006, 1124, "Mistério/Drama"),
                FilmeDiretor("Batman Begins", 2005, 272, "Ação/Aventura"),
                FilmeDiretor("Amnésia (Memento)", 2000, 77, "Suspense/Mistério")
            )
        ),
        DiretorMestre(
            id = "tarantino",
            nome = "Quentin Tarantino",
            fotoEmoji = "🩸",
            estilo = "Diálogos Afiados & Violência Estilizada",
            bio = "Cineasta apaixonado pela cultura pop, trilhas sonoras icônicas e homenagens ao cinema cult.",
            badgeRecompensa = "Cinéfilo Pulp (Tarantino)",
            filmes = listOf(
                FilmeDiretor("Era Uma Vez em... Hollywood", 2019, 466272, "Comédia/Drama"),
                FilmeDiretor("Os Oito Odiados", 2015, 273248, "Faroeste/Mistério"),
                FilmeDiretor("Django Livre", 2012, 68718, "Faroeste/Drama"),
                FilmeDiretor("Bastardos Inglórios", 2009, 16869, "Guerra/Drama"),
                FilmeDiretor("Kill Bill: Volume 2", 2004, 393, "Ação/Crime"),
                FilmeDiretor("Kill Bill: Volume 1", 2003, 24, "Ação/Crime"),
                FilmeDiretor("Jackie Brown", 1997, 184, "Crime/Drama"),
                FilmeDiretor("Pulp Fiction: Tempo de Violência", 1994, 680, "Crime/Suspense"),
                FilmeDiretor("Cães de Aluguel", 1992, 500, "Crime/Suspense")
            )
        ),
        DiretorMestre(
            id = "villeneuve",
            nome = "Denis Villeneuve",
            fotoEmoji = "🪐",
            estilo = "Ficção Científica Épica & Tensão Visual",
            bio = "Criador de atmosferas hipnóticas, fotografia imersiva e histórias comoventes de grande escala.",
            badgeRecompensa = "Navegador de Arrakis (Villeneuve)",
            filmes = listOf(
                FilmeDiretor("Duna: Parte 2", 2024, 693134, "Sci-Fi/Aventura"),
                FilmeDiretor("Duna", 2021, 438631, "Sci-Fi/Aventura"),
                FilmeDiretor("Blade Runner 2049", 2017, 335984, "Sci-Fi/Mistério"),
                FilmeDiretor("A Chegada", 2016, 329865, "Sci-Fi/Drama"),
                FilmeDiretor("Sicario: Terra de Ninguém", 2015, 273481, "Ação/Crime"),
                FilmeDiretor("O Homem Duplicado", 2013, 157827, "Suspense/Mistério"),
                FilmeDiretor("Os Suspeitos (Prisoners)", 2013, 146233, "Suspense/Drama"),
                FilmeDiretor("Incêndios", 2010, 46146, "Drama/Mistério")
            )
        ),
        DiretorMestre(
            id = "scorsese",
            nome = "Martin Scorsese",
            fotoEmoji = "🗽",
            estilo = "Cinema de Máfia & Culpa Moral",
            bio = "Uma das maiores lendas vivas de Hollywood, narrando ambição, crime e redenção humana.",
            badgeRecompensa = "Lenda de Manhattan (Scorsese)",
            filmes = listOf(
                FilmeDiretor("Assassinos da Lua das Flores", 2023, 466420, "Crime/Drama"),
                FilmeDiretor("O Irlandês", 2019, 398978, "Crime/Drama"),
                FilmeDiretor("O Lobo de Wall Street", 2013, 106646, "Comédia/Drama"),
                FilmeDiretor("A Invenção de Hugo Cabret", 2011, 44826, "Aventura/Família"),
                FilmeDiretor("Ilha do Medo", 2010, 11324, "Suspense/Mistério"),
                FilmeDiretor("Os Infiltrados", 2006, 1422, "Crime/Drama"),
                FilmeDiretor("Gangues de Nova York", 2002, 3131, "Drama/História"),
                FilmeDiretor("Cassino", 1995, 235, "Crime/Drama"),
                FilmeDiretor("Os Bons Companheiros", 1990, 769, "Crime/Drama"),
                FilmeDiretor("Taxi Driver", 1976, 103, "Crime/Drama")
            )
        ),
        DiretorMestre(
            id = "miyazaki",
            nome = "Hayao Miyazaki",
            fotoEmoji = "🍃",
            estilo = "Animação Poética & Natureza Viva",
            bio = "Cofundador do Studio Ghibli e mestre indiscutível da animação mundial e fantasia humanista.",
            badgeRecompensa = "Espírito da Floresta (Miyazaki)",
            filmes = listOf(
                FilmeDiretor("O Menino e a Garça", 2023, 508883, "Animação/Fantasia"),
                FilmeDiretor("Vidas ao Vento", 2013, 149870, "Animação/Drama"),
                FilmeDiretor("Ponyo: Uma Amizade que Veio do Mar", 2008, 12429, "Animação/Família"),
                FilmeDiretor("O Castelo Animado", 2004, 4935, "Animação/Fantasia"),
                FilmeDiretor("A Viagem de Chihiro", 2001, 129, "Animação/Aventura"),
                FilmeDiretor("Princesa Mononoke", 1997, 128, "Animação/Fantasia"),
                FilmeDiretor("Porco Rosso: O Porquinho Voador", 1992, 11621, "Animação/Aventura"),
                FilmeDiretor("O Serviço de Entregas da Kiki", 1989, 16859, "Animação/Família"),
                FilmeDiretor("Meu Amigo Totoro", 1988, 8392, "Animação/Fantasia"),
                FilmeDiretor("Nausicaä do Vale do Vento", 1984, 81, "Animação/Aventura")
            )
        ),
        DiretorMestre(
            id = "gerwig",
            nome = "Greta Gerwig",
            fotoEmoji = "🎀",
            estilo = "Feminilidade, Amadurecimento & Cores Vivas",
            bio = "Voz contemporânea poderosa, explorando identidade, liberdade feminina e laços familiares.",
            badgeRecompensa = "Autora Brilhante (Gerwig)",
            filmes = listOf(
                FilmeDiretor("Barbie", 2023, 346698, "Comédia/Aventura"),
                FilmeDiretor("Adoráveis Mulheres", 2019, 331482, "Drama/Romance"),
                FilmeDiretor("Lady Bird: A Hora de Voar", 2017, 391713, "Comédia/Drama")
            )
        )
    )

    fun calcularProgresso(midiasUsuario: List<Midia>): List<StatusDiretor> {
        val titulosConcluidos = midiasUsuario.filter { it.obterStatusEnum() == StatusMidia.CONCLUIDO }
            .map { it.titulo.lowercase().trim() }
            .toSet()

        val idsTmdbConcluidos = midiasUsuario.filter { it.obterStatusEnum() == StatusMidia.CONCLUIDO && it.idTmdb > 0 }
            .map { it.idTmdb }
            .toSet()

        return DIRETORES.map { diretor ->
            val assistidos = diretor.filmes.count { filme ->
                filme.idTmdb in idsTmdbConcluidos || filme.titulo.lowercase().trim() in titulosConcluidos
            }
            val total = diretor.filmes.size
            val perc = if (total > 0) (assistidos.toFloat() / total.toFloat()) * 100f else 0f

            StatusDiretor(
                diretor = diretor,
                totalAssistidos = assistidos,
                porcentagem = perc,
                completo = assistidos == total
            )
        }
    }
}
