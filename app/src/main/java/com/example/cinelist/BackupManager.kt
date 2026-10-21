package com.example.cinelist

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

object BackupManager {

    private val gson = Gson()

    fun exportarParaJson(context: Context, uri: Uri, midias: List<Midia>): Boolean {
        return try {
            val jsonString = gson.toJson(midias)
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream).use { writer ->
                    writer.write(jsonString)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun importarDeJson(context: Context, uri: Uri): List<Midia>? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    val tipoLista = object : TypeToken<List<Midia>>() {}.type
                    gson.fromJson<List<Midia>>(reader, tipoLista)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun exportarParaCsv(context: Context, uri: Uri, midias: List<Midia>): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream, Charsets.UTF_8).use { writer ->
                    writer.write("Título,Tipo,Status,Nota,Gênero,Plataforma,Favorito\n")
                    midias.forEach { midia ->
                        val tituloEscapado = "\"${midia.titulo.replace("\"", "\"\"")}\""
                        val generoEscapado = "\"${midia.genero.replace("\"", "\"\"")}\""
                        val plataformaEscapada = "\"${midia.plataforma.replace("\"", "\"\"")}\""
                        writer.write("$tituloEscapado,${midia.tipo},${midia.status},${midia.nota},$generoEscapado,$plataformaEscapada,${midia.favorito}\n")
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Importa filmes/séries a partir de arquivos CSV padrão do Letterboxd (watched.csv, watchlist.csv)
     * ou IMDb (ratings.csv, watchlist.csv) ou do próprio CineList CSV.
     */
    fun importarDeCsv(context: Context, uri: Uri): List<Midia>? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                    val linhas = reader.readLines()
                    if (linhas.isEmpty()) return null

                    val cabecalho = linhas.first().lowercase()
                    val resultado = mutableListOf<Midia>()

                    val ehLetterboxd = cabecalho.contains("letterboxd uri") || (cabecalho.contains("name") && cabecalho.contains("year"))
                    val ehImdb = cabecalho.contains("const") || cabecalho.contains("your rating") || cabecalho.contains("imdb")

                    linhas.drop(1).forEach { linha ->
                        if (linha.isBlank()) return@forEach

                        val colunas = parseCsvLine(linha)
                        if (colunas.isEmpty()) return@forEach

                        if (ehLetterboxd) {
                            // Letterboxd: Date, Name, Year, Letterboxd URI, Rating
                            val nome = colunas.getOrNull(1)?.ifBlank { colunas.getOrNull(0) } ?: ""
                            val notaStr = colunas.getOrNull(4) ?: colunas.getOrNull(3) ?: "0"
                            val notaFloat = notaStr.toFloatOrNull() ?: 0f

                            if (nome.isNotBlank()) {
                                resultado.add(
                                    Midia(
                                        titulo = nome.trim(),
                                        tipo = "Filme",
                                        status = if (notaFloat > 0f) StatusMidia.CONCLUIDO.valor else StatusMidia.QUERO_ASSISTIR.valor,
                                        nota = (notaFloat * 2).toInt().coerceIn(0, 10),
                                        jaEncerrou = notaFloat > 0f,
                                        listaCustomizada = "Importados Letterboxd"
                                    )
                                )
                            }
                        } else if (ehImdb) {
                            // IMDb: Const, Your Rating, Date Rated, Title, URL, Title Type, ...
                            val titulo = colunas.getOrNull(3)?.ifBlank { colunas.getOrNull(1) } ?: ""
                            val notaStr = colunas.getOrNull(1) ?: "0"
                            val tipoMidiaStr = colunas.getOrNull(5) ?: "movie"
                            val tipo = if (tipoMidiaStr.contains("tv", ignoreCase = true) || tipoMidiaStr.contains("series", ignoreCase = true)) "Série" else "Filme"
                            val notaInt = notaStr.toIntOrNull() ?: 0

                            if (titulo.isNotBlank()) {
                                resultado.add(
                                    Midia(
                                        titulo = titulo.trim(),
                                        tipo = tipo,
                                        status = if (notaInt > 0) StatusMidia.CONCLUIDO.valor else StatusMidia.QUERO_ASSISTIR.valor,
                                        nota = notaInt.coerceIn(0, 10),
                                        jaEncerrou = notaInt > 0,
                                        listaCustomizada = "Importados IMDb"
                                    )
                                )
                            }
                        } else {
                            // CSV padrão CineList: Título, Tipo, Status, Nota, Gênero, Plataforma, Favorito
                            val titulo = colunas.getOrNull(0) ?: ""
                            val tipo = colunas.getOrNull(1) ?: "Filme"
                            val status = colunas.getOrNull(2) ?: StatusMidia.QUERO_ASSISTIR.valor
                            val nota = colunas.getOrNull(3)?.toIntOrNull() ?: 0
                            val genero = colunas.getOrNull(4) ?: ""
                            val plataforma = colunas.getOrNull(5) ?: ""
                            val favorito = colunas.getOrNull(6)?.toBooleanStrictOrNull() ?: false

                            if (titulo.isNotBlank()) {
                                resultado.add(
                                    Midia(
                                        titulo = titulo.trim(),
                                        tipo = tipo,
                                        status = status,
                                        nota = nota,
                                        genero = genero,
                                        plataforma = plataforma,
                                        favorito = favorito,
                                        listaCustomizada = "Importados CSV"
                                    )
                                )
                            }
                        }
                    }

                    if (resultado.isNotEmpty()) resultado else null
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun parseCsvLine(linha: String): List<String> {
        val colunas = mutableListOf<String>()
        val sb = StringBuilder()
        var entreAspas = false

        for (ch in linha) {
            if (ch == '\"') {
                entreAspas = !entreAspas
            } else if (ch == ',' && !entreAspas) {
                colunas.add(sb.toString().trim())
                sb.clear()
            } else {
                sb.append(ch)
            }
        }
        colunas.add(sb.toString().trim())
        return colunas
    }
}
