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
}