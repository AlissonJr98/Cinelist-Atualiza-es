package com.example.cinelist

/**
 * Utilitário global para normalizar e traduzir gêneros cinematográficos e de TV
 * vindos do TMDB em qualquer idioma de origem (inglês, português, etc.) para o idioma
 * atualmente selecionado no aplicativo CineList.
 */
object GeneroLocalizationHelper {

    // Mapeamento de termos conhecidos (em inglês, português e variantes) para a chave canônica em inglês
    private val mapaTermoParaChave = mapOf(
        // Action
        "action" to "Action",
        "ação" to "Action",
        "acao" to "Action",
        "acción" to "Action",
        "accion" to "Action",
        "azione" to "Action",
        "アクション" to "Action",
        "액션" to "Action",
        "格闘" to "Action",
        "martial arts" to "Action",

        // Adventure
        "adventure" to "Adventure",
        "aventura" to "Adventure",
        "aventure" to "Adventure",
        "abenteuer" to "Adventure",
        "avventura" to "Adventure",
        "アドベンチャー" to "Adventure",
        "冒険" to "Adventure",
        "모험" to "Adventure",

        // Action & Adventure
        "action & adventure" to "Action & Adventure",
        "action and adventure" to "Action & Adventure",
        "ação & aventura" to "Action & Adventure",
        "ação e aventura" to "Action & Adventure",
        "acao & aventura" to "Action & Adventure",
        "acao e aventura" to "Action & Adventure",
        "acción y aventura" to "Action & Adventure",
        "action & aventure" to "Action & Adventure",
        "action & abenteuer" to "Action & Adventure",
        "azione e avventura" to "Action & Adventure",
        "アクション＆アドベンチャー" to "Action & Adventure",
        "アクション & アドベンチャー" to "Action & Adventure",
        "アクション/アドベンチャー" to "Action & Adventure",
        "액션 및 모험" to "Action & Adventure",
        "액션 & 모험" to "Action & Adventure",

        // Animation
        "animation" to "Animation",
        "animação" to "Animation",
        "animacao" to "Animation",
        "animación" to "Animation",
        "animacion" to "Animation",
        "animazione" to "Animation",
        "anime" to "Animation",
        "アニメーション" to "Animation",
        "アニメ" to "Animation",
        "애니메이션" to "Animation",
        "애니" to "Animation",

        // Comedy
        "comedy" to "Comedy",
        "comédia" to "Comedy",
        "comedia" to "Comedy",
        "comédie" to "Comedy",
        "comedie" to "Comedy",
        "komödie" to "Comedy",
        "komodie" to "Comedy",
        "commedia" to "Comedy",
        "コメディ" to "Comedy",
        "코미디" to "Comedy",

        // Crime
        "crime" to "Crime",
        "crimen" to "Crime",
        "krimi" to "Crime",
        "crimine" to "Crime",
        "犯罪" to "Crime",
        "범죄" to "Crime",

        // Documentary
        "documentary" to "Documentary",
        "documentário" to "Documentary",
        "documentario" to "Documentary",
        "documental" to "Documentary",
        "documentaire" to "Documentary",
        "dokumentarfilm" to "Documentary",
        "ドキュメンタリー" to "Documentary",
        "다큐멘터리" to "Documentary",

        // Drama
        "drama" to "Drama",
        "drame" to "Drama",
        "dramma" to "Drama",
        "ドラマ" to "Drama",
        "드라마" to "Drama",

        // Family
        "family" to "Family",
        "família" to "Family",
        "familia" to "Family",
        "famille" to "Family",
        "familie" to "Family",
        "famiglia" to "Family",
        "ファミリー" to "Family",
        "家族" to "Family",
        "가족" to "Family",

        // Fantasy
        "fantasy" to "Fantasy",
        "fantasia" to "Fantasy",
        "fantasía" to "Fantasy",
        "fantastique" to "Fantasy",
        "ファンタジー" to "Fantasy",
        "판타지" to "Fantasy",
        "異世界" to "Fantasy",
        "isekai" to "Fantasy",

        // History
        "history" to "History",
        "história" to "History",
        "historia" to "History",
        "histoire" to "History",
        "historie" to "History",
        "storia" to "History",
        "歴史" to "History",
        "역사" to "History",

        // Horror
        "horror" to "Horror",
        "terror" to "Horror",
        "horreur" to "Horror",
        "ホラー" to "Horror",
        "공포" to "Horror",

        // Kids
        "kids" to "Kids",
        "infantil" to "Kids",
        "enfants" to "Kids",
        "kinder" to "Kids",
        "bambini" to "Kids",
        "キッズ" to "Kids",
        "子供" to "Kids",
        "키즈" to "Kids",

        // Music
        "music" to "Music",
        "música" to "Music",
        "musica" to "Music",
        "musique" to "Music",
        "musik" to "Music",
        "音楽" to "Music",
        "음악" to "Music",

        // Mystery
        "mystery" to "Mystery",
        "mistério" to "Mystery",
        "misterio" to "Mystery",
        "mystère" to "Mystery",
        "mystere" to "Mystery",
        "ミステリー" to "Mystery",
        "謎" to "Mystery",
        "미스터리" to "Mystery",

        // News
        "news" to "News",
        "notícias" to "News",
        "noticias" to "News",
        "actualités" to "News",
        "nachrichten" to "News",
        "notizie" to "News",
        "ニュース" to "News",
        "뉴스" to "News",

        // Reality
        "reality" to "Reality",
        "reality show" to "Reality",
        "reality-tv" to "Reality",
        "télé-réalité" to "Reality",
        "リアリティ番組" to "Reality",
        "リアリティ" to "Reality",
        "리얼리티" to "Reality",

        // Romance
        "romance" to "Romance",
        "romântico" to "Romance",
        "romantico" to "Romance",
        "liebesfilm" to "Romance",
        "ロマンス" to "Romance",
        "恋愛" to "Romance",
        "로맨스" to "Romance",

        // Sci-Fi & Fantasy
        "sci-fi & fantasy" to "Sci-Fi & Fantasy",
        "sci-fi and fantasy" to "Sci-Fi & Fantasy",
        "ficção científica & fantasia" to "Sci-Fi & Fantasy",
        "ficção científica e fantasia" to "Sci-Fi & Fantasy",
        "ficcao cientifica & fantasia" to "Sci-Fi & Fantasy",
        "ficcao cientifica e fantasia" to "Sci-Fi & Fantasy",
        "ciencia ficción y fantasía" to "Sci-Fi & Fantasy",
        "science-fiction & fantastique" to "Sci-Fi & Fantasy",
        "fantascienza e fantasy" to "Sci-Fi & Fantasy",
        "sci-fi" to "Sci-Fi & Fantasy",
        "sf＆ファンタジー" to "Sci-Fi & Fantasy",
        "sf & ファンタジー" to "Sci-Fi & Fantasy",
        "sf 및 판타지" to "Sci-Fi & Fantasy",
        "sf & 판타지" to "Sci-Fi & Fantasy",
        "ficção científica" to "Science Fiction",
        "ficcao cientifica" to "Science Fiction",
        "science fiction" to "Science Fiction",
        "ciencia ficción" to "Science Fiction",
        "science-fiction" to "Science Fiction",
        "fantascienza" to "Science Fiction",
        "sf" to "Science Fiction",
        "サイエンスフィクション" to "Science Fiction",

        // Soap
        "soap" to "Soap",
        "novela" to "Soap",
        "novelas" to "Soap",
        "telenovela" to "Soap",
        "ソープオペラ" to "Soap",
        "드라마/연속극" to "Soap",

        // Talk
        "talk" to "Talk",
        "talk show" to "Talk",
        "talk-show" to "Talk",
        "talkshow" to "Talk",
        "トーク番組" to "Talk",
        "토크쇼" to "Talk",

        // Thriller
        "thriller" to "Thriller",
        "suspense" to "Thriller",
        "スリラー" to "Thriller",
        "스릴러" to "Thriller",

        // TV Movie
        "tv movie" to "TV Movie",
        "cinema tv" to "TV Movie",
        "filme de tv" to "TV Movie",
        "filme para tv" to "TV Movie",
        "película de tv" to "TV Movie",
        "téléfilm" to "TV Movie",
        "tv-film" to "TV Movie",
        "film tv" to "TV Movie",
        "テレビ映画" to "TV Movie",
        "tv 영화" to "TV Movie",

        // War
        "war" to "War",
        "guerra" to "War",
        "guerre" to "War",
        "kriegsfilm" to "War",
        "戦争" to "War",
        "전쟁" to "War",

        // War & Politics
        "war & politics" to "War & Politics",
        "war and politics" to "War & Politics",
        "guerra & política" to "War & Politics",
        "guerra e política" to "War & Politics",
        "guerra e politica" to "War & Politics",
        "guerra y política" to "War & Politics",
        "guerre & politique" to "War & Politics",
        "krieg & politik" to "War & Politics",
        "戦争・政治" to "War & Politics",
        "戦争 & 政治" to "War & Politics",
        "전쟁 및 정치" to "War & Politics",
        "전쟁 & 정치" to "War & Politics",

        // Western
        "western" to "Western",
        "faroeste" to "Western",
        "西部劇" to "Western",
        "西洋" to "Western",
        "서부" to "Western"
    )

    // Tabela completa de traduções por idioma
    private val traducoesPorIdioma = mapOf(
        "pt-BR" to mapOf(
            "Action" to "Ação",
            "Adventure" to "Aventura",
            "Action & Adventure" to "Ação e Aventura",
            "Animation" to "Animação",
            "Comedy" to "Comédia",
            "Crime" to "Crime",
            "Documentary" to "Documentário",
            "Drama" to "Drama",
            "Family" to "Família",
            "Fantasy" to "Fantasia",
            "History" to "História",
            "Horror" to "Terror",
            "Kids" to "Infantil",
            "Music" to "Música",
            "Mystery" to "Mistério",
            "News" to "Notícias",
            "Reality" to "Reality Show",
            "Romance" to "Romance",
            "Sci-Fi & Fantasy" to "Ficção Científica e Fantasia",
            "Science Fiction" to "Ficção Científica",
            "Soap" to "Novela",
            "Talk" to "Talk Show",
            "Thriller" to "Suspense",
            "TV Movie" to "Cinema TV",
            "War" to "Guerra",
            "War & Politics" to "Guerra e Política",
            "Western" to "Faroeste"
        ),
        "en-US" to mapOf(
            "Action" to "Action",
            "Adventure" to "Adventure",
            "Action & Adventure" to "Action & Adventure",
            "Animation" to "Animation",
            "Comedy" to "Comedy",
            "Crime" to "Crime",
            "Documentary" to "Documentary",
            "Drama" to "Drama",
            "Family" to "Family",
            "Fantasy" to "Fantasy",
            "History" to "History",
            "Horror" to "Horror",
            "Kids" to "Kids",
            "Music" to "Music",
            "Mystery" to "Mystery",
            "News" to "News",
            "Reality" to "Reality",
            "Romance" to "Romance",
            "Sci-Fi & Fantasy" to "Sci-Fi & Fantasy",
            "Science Fiction" to "Science Fiction",
            "Soap" to "Soap",
            "Talk" to "Talk",
            "Thriller" to "Thriller",
            "TV Movie" to "TV Movie",
            "War" to "War",
            "War & Politics" to "War & Politics",
            "Western" to "Western"
        ),
        "es-ES" to mapOf(
            "Action" to "Acción",
            "Adventure" to "Aventura",
            "Action & Adventure" to "Acción y Aventura",
            "Animation" to "Animación",
            "Comedy" to "Comedia",
            "Crime" to "Crimen",
            "Documentary" to "Documental",
            "Drama" to "Drama",
            "Family" to "Familia",
            "Fantasy" to "Fantasía",
            "History" to "Historia",
            "Horror" to "Terror",
            "Kids" to "Infantil",
            "Music" to "Música",
            "Mystery" to "Misterio",
            "News" to "Noticias",
            "Reality" to "Reality",
            "Romance" to "Romance",
            "Sci-Fi & Fantasy" to "Ciencia Ficción y Fantasía",
            "Science Fiction" to "Ciencia Ficción",
            "Soap" to "Telenovela",
            "Talk" to "Talk Show",
            "Thriller" to "Suspense",
            "TV Movie" to "Película de TV",
            "War" to "Guerra",
            "War & Politics" to "Guerra y Política",
            "Western" to "Western"
        ),
        "fr-FR" to mapOf(
            "Action" to "Action",
            "Adventure" to "Aventure",
            "Action & Adventure" to "Action & Aventure",
            "Animation" to "Animation",
            "Comedy" to "Comédie",
            "Crime" to "Crime",
            "Documentary" to "Documentaire",
            "Drama" to "Drame",
            "Family" to "Famille",
            "Fantasy" to "Fantastique",
            "History" to "Histoire",
            "Horror" to "Horreur",
            "Kids" to "Enfants",
            "Music" to "Musique",
            "Mystery" to "Mystère",
            "News" to "Actualités",
            "Reality" to "Télé-réalité",
            "Romance" to "Romance",
            "Sci-Fi & Fantasy" to "Science-Fiction & Fantastique",
            "Science Fiction" to "Science-Fiction",
            "Soap" to "Soap",
            "Talk" to "Talk-show",
            "Thriller" to "Thriller",
            "TV Movie" to "Téléfilm",
            "War" to "Guerre",
            "War & Politics" to "Guerre & Politique",
            "Western" to "Western"
        ),
        "de-DE" to mapOf(
            "Action" to "Action",
            "Adventure" to "Abenteuer",
            "Action & Adventure" to "Action & Abenteuer",
            "Animation" to "Animation",
            "Comedy" to "Komödie",
            "Crime" to "Krimi",
            "Documentary" to "Dokumentarfilm",
            "Drama" to "Drama",
            "Family" to "Familie",
            "Fantasy" to "Fantasy",
            "History" to "Historie",
            "Horror" to "Horror",
            "Kids" to "Kinder",
            "Music" to "Musik",
            "Mystery" to "Mystery",
            "News" to "Nachrichten",
            "Reality" to "Reality-TV",
            "Romance" to "Liebesfilm",
            "Sci-Fi & Fantasy" to "Sci-Fi & Fantasy",
            "Science Fiction" to "Science Fiction",
            "Soap" to "Soap",
            "Talk" to "Talkshow",
            "Thriller" to "Thriller",
            "TV Movie" to "TV-Film",
            "War" to "Kriegsfilm",
            "War & Politics" to "Krieg & Politik",
            "Western" to "Western"
        ),
        "it-IT" to mapOf(
            "Action" to "Azione",
            "Adventure" to "Avventura",
            "Action & Adventure" to "Azione e avventura",
            "Animation" to "Animazione",
            "Comedy" to "Commedia",
            "Crime" to "Crimine",
            "Documentary" to "Documentario",
            "Drama" to "Dramma",
            "Family" to "Famiglia",
            "Fantasy" to "Fantasy",
            "History" to "Storia",
            "Horror" to "Horror",
            "Kids" to "Bambini",
            "Music" to "Musica",
            "Mystery" to "Mistero",
            "News" to "Notizie",
            "Reality" to "Reality",
            "Romance" to "Romance",
            "Sci-Fi & Fantasy" to "Fantascienza e Fantasy",
            "Science Fiction" to "Fantascienza",
            "Soap" to "Telenovela",
            "Talk" to "Talk Show",
            "Thriller" to "Thriller",
            "TV Movie" to "Film TV",
            "War" to "Guerra",
            "War & Politics" to "Guerra e politica",
            "Western" to "Western"
        ),
        "ja-JP" to mapOf(
            "Action" to "アクション",
            "Adventure" to "アドベンチャー",
            "Action & Adventure" to "アクション＆アドベンチャー",
            "Animation" to "アニメーション",
            "Comedy" to "コメディ",
            "Crime" to "犯罪",
            "Documentary" to "ドキュメンタリー",
            "Drama" to "ドラマ",
            "Family" to "ファミリー",
            "Fantasy" to "ファンタジー",
            "History" to "歴史",
            "Horror" to "ホラー",
            "Kids" to "キッズ",
            "Music" to "音楽",
            "Mystery" to "ミステリー",
            "News" to "ニュース",
            "Reality" to "リアリティ番組",
            "Romance" to "ロマンス",
            "Sci-Fi & Fantasy" to "SF＆ファンタジー",
            "Science Fiction" to "SF",
            "Soap" to "ソープオペラ",
            "Talk" to "トーク番組",
            "Thriller" to "スリラー",
            "TV Movie" to "テレビ映画",
            "War" to "戦争",
            "War & Politics" to "戦争・政治",
            "Western" to "西部劇"
        ),
        "ko-KR" to mapOf(
            "Action" to "액션",
            "Adventure" to "모험",
            "Action & Adventure" to "액션 및 모험",
            "Animation" to "애니메이션",
            "Comedy" to "코미디",
            "Crime" to "범죄",
            "Documentary" to "다큐멘터리",
            "Drama" to "드라마",
            "Family" to "가족",
            "Fantasy" to "판타지",
            "History" to "역사",
            "Horror" to "공포",
            "Kids" to "키즈",
            "Music" to "음악",
            "Mystery" to "미스터리",
            "News" to "뉴스",
            "Reality" to "리얼리티",
            "Romance" to "로맨스",
            "Sci-Fi & Fantasy" to "SF 및 판타지",
            "Science Fiction" to "SF",
            "Soap" to "드라마/연속극",
            "Talk" to "토크쇼",
            "Thriller" to "스릴러",
            "TV Movie" to "TV 영화",
            "War" to "전쟁",
            "War & Politics" to "전쟁 및 정치",
            "Western" to "서부"
        )
    )

    fun traduzirGeneroIndividual(generoBruto: String, tagIdioma: String = I18n.idiomaAtivo): String {
        val limpo = generoBruto.trim()
        if (limpo.isBlank()) return ""

        val chaveCanonica = mapaTermoParaChave[limpo.lowercase()]
        if (chaveCanonica != null) {
            val tabelaIdioma = traducoesPorIdioma[tagIdioma]
                ?: traducoesPorIdioma.entries.firstOrNull { it.key.startsWith(tagIdioma.take(2), ignoreCase = true) }?.value
                ?: traducoesPorIdioma["pt-BR"]
                ?: emptyMap()
            val traducao = tabelaIdioma[chaveCanonica]
            if (!traducao.isNullOrBlank()) return traducao
        }

        // Se o idioma ativo NÃO for asiático e o termo possui caracteres japoneses, chineses ou coreanos não mapeados
        val contemCaracteresAsiaticos = limpo.any { ch ->
            ch in '\u3040'..'\u30ff' || ch in '\u4e00'..'\u9fff' || ch in '\uac00'..'\ud7af'
        }
        val idiomaAlvoEhAsiatico = tagIdioma.startsWith("ja", ignoreCase = true) || tagIdioma.startsWith("ko", ignoreCase = true)

        if (contemCaracteresAsiaticos && !idiomaAlvoEhAsiatico) {
            val l = limpo.lowercase()
            return when {
                l.contains("アニメ") || l.contains("애니") -> "Animação".t()
                l.contains("アクション") || l.contains("액션") || l.contains("格闘") -> "Ação".t()
                l.contains("冒険") || l.contains("モ험") -> "Aventura".t()
                l.contains("コメディ") || l.contains("코미디") -> "Comédia".t()
                l.contains("ドラマ") || l.contains("드라마") -> "Drama".t()
                l.contains("ファンタジー") || l.contains("판타지") || l.contains("異世界") -> "Fantasia".t()
                l.contains("sf") || l.contains("サイエンス") -> "Ficção Científica".t()
                else -> "" // Remove termo asiático isolado para nunca contaminar texto em português ou outros idiomas
            }
        }

        // Fallback: tenta .t() ou retorna o próprio termo
        return limpo.t()
    }

    fun formatarGeneros(generosBrutos: String?, tagIdioma: String = I18n.idiomaAtivo): String {
        if (generosBrutos.isNullOrBlank()) return ""
        val tokens = generosBrutos.split(",", ";", "/", "•", "|").map { it.trim() }.filter { it.isNotBlank() }
        if (tokens.isEmpty()) return ""
        return tokens.map { traduzirGeneroIndividual(it, tagIdioma) }
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString(", ")
    }
}
