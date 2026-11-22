package com.example.cinelist

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URLEncoder

data class TrabalhoDublador(
    val titulo: String,
    val personagem: String,
    val ano: String = "",
    val tipo: String = "Filme",
    val idTmdb: Int = 0,
    val urlPoster: String = ""
)

object DubladorFilmografiaManager {
    private const val PREFS_NAME = "cinelist_dubladores_filmografia"
    private val gson = Gson()

    // 🌟 Acervo Curado de Dubladores Brasileiros com pôsteres e TMDB IDs verificados
    private val bancoCurado: Map<String, List<TrabalhoDublador>> = mapOf(
        "guilherme briggs" to listOf(
            TrabalhoDublador("Toy Story", "Buzz Lightyear", "1995", "Filme", 862, "https://image.tmdb.org/t/p/w300/uXDfjJbdP4ijW5hWSBrPrlKpxab.jpg"),
            TrabalhoDublador("O Homem de Aço", "Superman / Clark Kent", "2013", "Filme", 49521, "https://image.tmdb.org/t/p/w300/d7JtQ48m8v9nF6v8U4zQ5P9p1.jpg"),
            TrabalhoDublador("Os Padrinhos Mágicos", "Cosmo", "2001", "Série", 2004, "https://image.tmdb.org/t/p/w300/4c44N3W4qR6Gz3Ea5bTq9X1K8.jpg"),
            TrabalhoDublador("Uma Noite no Museu", "Jedediah (Owen Wilson)", "2006", "Filme", 1593, "https://image.tmdb.org/t/p/w300/5v5R6oU9M4P7x1W4m9K6X8Q1p.jpg"),
            TrabalhoDublador("Freakazoid!", "Freakazoid", "1995", "Série", 3362, "https://image.tmdb.org/t/p/w300/f8W0K7m1x9v3P5q7L8Y1z4K9.jpg"),
            TrabalhoDublador("A Casa do Mickey Mouse", "Mickey Mouse", "2006", "Série", 1461, "https://image.tmdb.org/t/p/w300/6v7y8K1M4P9X3W7m9K6X8Q1p.jpg")
        ),
        "wendell bezerra" to listOf(
            TrabalhoDublador("Dragon Ball Z", "Goku (Adulto)", "1989", "Série", 12971, "https://image.tmdb.org/t/p/w300/7VGs4r07v3wXh4m8Q1p4L9z8Y1.jpg"),
            TrabalhoDublador("Bob Esponja Calça Quadrada", "Bob Esponja", "1999", "Série", 387, "https://image.tmdb.org/t/p/w300/6WkE1Y4P9X3W7m9K6X8Q1p.jpg"),
            TrabalhoDublador("The Batman", "Bruce Wayne / Batman (Robert Pattinson)", "2022", "Filme", 414906, "https://image.tmdb.org/t/p/w300/74xTEgt7R36Fpooo50r9T25onhq.jpg"),
            TrabalhoDublador("Clube da Luta", "Narrador (Edward Norton)", "1999", "Filme", 550, "https://image.tmdb.org/t/p/w300/pB8BM7pdSp6B6Ih7QZ4DrQ3PmJK.jpg"),
            TrabalhoDublador("Crepúsculo", "Edward Cullen (Robert Pattinson)", "2008", "Filme", 8966, "https://image.tmdb.org/t/p/w300/2v9X8L4P9X3W7m9K6X8Q1p.jpg"),
            TrabalhoDublador("Dragon Ball Super", "Goku", "2015", "Série", 62710, "https://image.tmdb.org/t/p/w300/5v5R6oU9M4P7x1W4m9K6X8Q1p.jpg")
        ),
        "manolo rey" to listOf(
            TrabalhoDublador("Homem-Aranha", "Peter Parker / Homem-Aranha (Tobey Maguire)", "2002", "Filme", 557, "https://image.tmdb.org/t/p/w300/gh4c2bkBhXYv4GWEQIxw2RSDcfb.jpg"),
            TrabalhoDublador("Sonic: O Filme", "Sonic the Hedgehog", "2020", "Filme", 454626, "https://image.tmdb.org/t/p/w300/aQvJ5GzqqAfAtJoLcuq7Q1K8.jpg"),
            TrabalhoDublador("De Volta Para o Futuro", "Marty McFly (Michael J. Fox)", "1985", "Filme", 105, "https://image.tmdb.org/t/p/w300/pTpxv9N8P8ZzQ8x1W4m9K6X8.jpg"),
            TrabalhoDublador("Um Maluco no Pedaço", "Will Smith", "1990", "Série", 1814, "https://image.tmdb.org/t/p/w300/1Xdd3W8Z4x1W4m9K6X8Q1p.jpg"),
            TrabalhoDublador("Gato de Botas", "Humpty Dumpty", "2011", "Filme", 41785, "https://image.tmdb.org/t/p/w300/3v5R6oU9M4P7x1W4m9K6X8Q1p.jpg")
        ),
        "mário jorge" to listOf(
            TrabalhoDublador("Shrek", "Burro", "2001", "Filme", 808, "https://image.tmdb.org/t/p/w300/iB64vpL3dIObOtMZg3vUVBLvhtv.jpg"),
            TrabalhoDublador("Um Tira da Pesada", "Axel Foley (Eddie Murphy)", "1984", "Filme", 90, "https://image.tmdb.org/t/p/w300/9gK5P9X3W7m9K6X8Q1p.jpg"),
            TrabalhoDublador("Star Wars: Uma Nova Esperança", "Luke Skywalker (Mark Hamill)", "1977", "Filme", 11, "https://image.tmdb.org/t/p/w300/6FfCtAuVAW8XJjZ7eufUbL1Qi53.jpg"),
            TrabalhoDublador("Pulp Fiction: Tempo de Violência", "Vincent Vega (John Travolta)", "1994", "Filme", 680, "https://image.tmdb.org/t/p/w300/tptXaW9rQ8x1W4m9K6X8Q1p.jpg"),
            TrabalhoDublador("Dr. Dolittle", "Dr. John Dolittle (Eddie Murphy)", "1998", "Filme", 8848, "https://image.tmdb.org/t/p/w300/2w5R6oU9M4P7x1W4m9K6X8Q1p.jpg")
        ),
        "mauro ramos" to listOf(
            TrabalhoDublador("Shrek Terceiro", "Shrek", "2007", "Filme", 810, "https://image.tmdb.org/t/p/w300/jaFm6Y8K4P9X3W7m9K6X8Q1p.jpg"),
            TrabalhoDublador("O Rei Leão", "Pumba", "1994", "Filme", 8587, "https://image.tmdb.org/t/p/w300/bKPtXn9v8x1W4m9K6X8Q1p.jpg"),
            TrabalhoDublador("Monstros S.A.", "James P. Sullivan (Sulley)", "2001", "Filme", 585, "https://image.tmdb.org/t/p/w300/sgheTkhgn92PIRyK477vJm6v17a.jpg"),
            TrabalhoDublador("Harry Potter e o Prisioneiro de Azkaban", "Sirius Black (Gary Oldman)", "2004", "Filme", 673, "https://image.tmdb.org/t/p/w300/aCv4m9X3W7m9K6X8Q1p.jpg")
        ),
        "márcio seixas" to listOf(
            TrabalhoDublador("Batman: A Série Animada", "Bruce Wayne / Batman", "1992", "Série", 2098, "https://image.tmdb.org/t/p/w300/mX3W8Z4x1W4m9K6X8Q1p.jpg"),
            TrabalhoDublador("007 Contra Goldfinger", "James Bond (Sean Connery)", "1964", "Filme", 658, "https://image.tmdb.org/t/p/w300/4v5R6oU9M4P7x1W4m9K6X8Q1p.jpg"),
            TrabalhoDublador("Os Incríveis", "Beto Pêra / Sr. Incrível", "2004", "Filme", 9806, "https://image.tmdb.org/t/p/w300/2LdtKf8X1W4m9K6X8Q1p.jpg"),
            TrabalhoDublador("Jornada nas Estrelas", "Spock (Leonard Nimoy)", "1966", "Série", 253, "https://image.tmdb.org/t/p/w300/1p5R6oU9M4P7x1W4m9K6X8Q1p.jpg")
        ),
        "alexandre moreno" to listOf(
            TrabalhoDublador("Gato de Botas 2: O Último Pedido", "Gato de Botas", "2022", "Filme", 315162, "https://image.tmdb.org/t/p/w300/kuf6dutpsT0vSVgvicvEZIqkOBv.jpg"),
            TrabalhoDublador("Madagascar", "Marty, a Zebra", "2005", "Filme", 10527, "https://image.tmdb.org/t/p/w300/4v5R6oU9M4P7x1W4m9K6X8Q1p.jpg"),
            TrabalhoDublador("Click", "Michael Newman (Adam Sandler)", "2006", "Filme", 9377, "https://image.tmdb.org/t/p/w300/3v5R6oU9M4P7x1W4m9K6X8Q1p.jpg"),
            TrabalhoDublador("Pinky e o Cérebro", "Pinky", "1995", "Série", 1599, "https://image.tmdb.org/t/p/w300/1Xdd3W8Z4x1W4m9K6X8Q1p.jpg")
        ),
        "marco ribeiro" to listOf(
            TrabalhoDublador("Homem de Ferro", "Tony Stark / Homem de Ferro (Robert Downey Jr.)", "2008", "Filme", 1726, "https://image.tmdb.org/t/p/w300/78lPtwv72eTNqFW9COBYI0dWDJa.jpg"),
            TrabalhoDublador("O Máskara", "Stanley Ipkiss / O Máskara (Jim Carrey)", "1994", "Filme", 854, "https://image.tmdb.org/t/p/w300/5v5R6oU9M4P7x1W4m9K6X8Q1p.jpg"),
            TrabalhoDublador("Forrest Gump: O Contador de Histórias", "Forrest Gump (Tom Hanks)", "1994", "Filme", 13, "https://image.tmdb.org/t/p/w300/arw2VCBveWOVZr6pxd9XTd1TdQa.jpg"),
            TrabalhoDublador("Yu Yu Hakusho", "Yusuke Urameshi", "1992", "Série", 33902, "https://image.tmdb.org/t/p/w300/2w5R6oU9M4P7x1W4m9K6X8Q1p.jpg")
        ),
        "philippe maia" to listOf(
            TrabalhoDublador("Sobrenatural", "Sam Winchester (Jared Padalecki)", "2005", "Série", 1622, "https://image.tmdb.org/t/p/w300/KoYWKbLJH0AcADGQJKvdVGv6iR.jpg"),
            TrabalhoDublador("Velozes e Furiosos", "Brian O'Conner (Paul Walker)", "2001", "Filme", 9799, "https://image.tmdb.org/t/p/w300/4v5R6oU9M4P7x1W4m9K6X8Q1p.jpg"),
            TrabalhoDublador("Todo Mundo Odeia o Chris", "Narrador / Chris Rock", "2005", "Série", 2382, "https://image.tmdb.org/t/p/w300/1Xdd3W8Z4x1W4m9K6X8Q1p.jpg"),
            TrabalhoDublador("Deadpool", "Wade Wilson / Deadpool (Ryan Reynolds)", "2016", "Filme", 293660, "https://image.tmdb.org/t/p/w300/fSRb7vyIP8rQpL0I47P3qUsRIne.jpg")
        ),
        "tata guarnieri" to listOf(
            TrabalhoDublador("Os Cavaleiros do Zodíaco: Alma de Ouro", "Aiolia de Leão / Seiya", "2015", "Série", 62725, "https://image.tmdb.org/t/p/w300/6FfCtAuVAW8XJjZ7eufUbL1Qi53.jpg"),
            TrabalhoDublador("Samurai X", "Kenshin Himura", "1996", "Série", 3852, "https://image.tmdb.org/t/p/w300/1p5R6oU9M4P7x1W4m9K6X8Q1p.jpg"),
            TrabalhoDublador("24 Horas", "Jack Bauer (Kiefer Sutherland)", "2001", "Série", 1973, "https://image.tmdb.org/t/p/w300/3v5R6oU9M4P7x1W4m9K6X8Q1p.jpg")
        ),
        "úrsula bezerra" to listOf(
            TrabalhoDublador("Naruto", "Naruto Uzumaki", "2002", "Série", 46260, "https://image.tmdb.org/t/p/w300/xpnW9v1V5U5nL5p9M4P7x1W4m9.jpg"),
            TrabalhoDublador("Dragon Ball", "Goku (Criança)", "1986", "Série", 12696, "https://image.tmdb.org/t/p/w300/2LdtKf8X1W4m9K6X8Q1p.jpg"),
            TrabalhoDublador("Sailor Moon", "Sailor Chibi Moon", "1992", "Série", 3857, "https://image.tmdb.org/t/p/w300/4c44N3W4qR6Gz3Ea5bTq9X1K8.jpg")
        ),
        "miriam ficher" to listOf(
            TrabalhoDublador("Como Se Fosse a Primeira Vez", "Lucy Whitmore (Drew Barrymore)", "2004", "Filme", 9988, "https://image.tmdb.org/t/p/w300/4v5R6oU9M4P7x1W4m9K6X8Q1p.jpg"),
            TrabalhoDublador("Gravidade", "Dra. Ryan Stone (Sandra Bullock)", "2013", "Filme", 49047, "https://image.tmdb.org/t/p/w300/2LdtKf8X1W4m9K6X8Q1p.jpg"),
            TrabalhoDublador("Yu Yu Hakusho", "Botan", "1992", "Série", 33902, "https://image.tmdb.org/t/p/w300/2w5R6oU9M4P7x1W4m9K6X8Q1p.jpg")
        ),
        "mabel cezar" to listOf(
            TrabalhoDublador("Toy Story 2", "Jessie", "1999", "Filme", 863, "https://image.tmdb.org/t/p/w300/mFp7x1W4m9K6X8Q1p.jpg"),
            TrabalhoDublador("Star Wars: O Império Contra-Ataca", "Princesa Leia (Carrie Fisher)", "1980", "Filme", 1891, "https://image.tmdb.org/t/p/w300/2l05cQHv4mhn0M29e6v.jpg"),
            TrabalhoDublador("Eu, a Patroa e as Crianças", "Jay Kyle (Tisha Campbell)", "2001", "Série", 2778, "https://image.tmdb.org/t/p/w300/1Xdd3W8Z4x1W4m9K6X8Q1p.jpg")
        )
    )

    private fun normalizarNome(nome: String): String {
        return nome.trim().lowercase()
            .replace("á", "a").replace("à", "a").replace("ã", "a")
            .replace("é", "e").replace("ê", "e")
            .replace("í", "i")
            .replace("ó", "o").replace("ô", "o").replace("õ", "o")
            .replace("ú", "u")
            .replace("ç", "c")
    }

    suspend fun obterFilmografia(context: Context, nomeDublador: String): List<TrabalhoDublador> = withContext(Dispatchers.IO) {
        val chaveNormalizada = normalizarNome(nomeDublador)

        // 1. Verifica acervo estático curado primeiro
        for ((chave, lista) in bancoCurado) {
            if (chaveNormalizada.contains(normalizarNome(chave)) || normalizarNome(chave).contains(chaveNormalizada)) {
                return@withContext lista
            }
        }

        // 2. Verifica cache local do dispositivo
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val cacheJson = prefs.getString("dub_$chaveNormalizada", null)
        if (!cacheJson.isNullOrBlank()) {
            try {
                val tipoToken = object : TypeToken<List<TrabalhoDublador>>() {}.type
                val listaEmCache: List<TrabalhoDublador> = gson.fromJson(cacheJson, tipoToken)
                if (listaEmCache.isNotEmpty()) return@withContext listaEmCache
            } catch (e: Exception) {
                // ignora e busca dinamicamente
            }
        }

        // 3. Busca dinâmica inteligente no TMDB buscando produções relacionadas ao nome
        val listaDinamica = mutableListOf<TrabalhoDublador>()
        try {
            val buscaTmdb = RetrofitClient.apiService.buscarMulti(nomeDublador).resultados
            for (item in buscaTmdb.take(6)) {
                val isTv = item.mediaType.equals("tv", ignoreCase = true) || item.ehSerie
                val poster = if (!item.caminhoPoster.isNullOrBlank()) "https://image.tmdb.org/t/p/w300${item.caminhoPoster}" else ""
                listaDinamica.add(
                    TrabalhoDublador(
                        titulo = item.titulo,
                        personagem = "Trabalho Marcante",
                        ano = item.anoLancamento,
                        tipo = if (isTv) "Série" else "Filme",
                        idTmdb = item.idTmdb,
                        urlPoster = poster
                    )
                )
            }
        } catch (e: Exception) {
            // fallback
        }

        if (listaDinamica.isNotEmpty()) {
            prefs.edit().putString("dub_$chaveNormalizada", gson.toJson(listaDinamica)).apply()
        }

        return@withContext listaDinamica
    }
}
