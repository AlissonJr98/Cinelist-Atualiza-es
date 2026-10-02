package com.example.cinelist

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await

/**
 * Worker de backup automático e silencioso: roda periodicamente em segundo plano
 * (agendado em agendarBackupAutomaticoSemanal, no MainActivity.kt) e envia a lista
 * pessoal local (Room) para o Firestore, sem exigir nenhuma ação do usuário.
 *
 * Isso complementa o backup manual em JSON que já existe na tela de Perfil —
 * aqui não é preciso escolher um arquivo, e o usuário nunca precisa lembrar de fazer.
 */
class BackupWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val CHAVE_ULTIMO_BACKUP = "ultimo_backup_automatico"
    }

    override suspend fun doWork(): Result {
        return try {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return Result.success()

            val database = AppDatabase.getDatabase(appContext)
            val midiaDao = database.midiaDao()
            val midiasPessoais = midiaDao.buscarMidiasPessoais().first()

            if (midiasPessoais.isEmpty()) {
                return Result.success()
            }

            val firestore = FirebaseFirestore.getInstance()
            val colecao = firestore.collection("usuarios").document(uid).collection("midias")

            midiasPessoais.forEach { midia ->
                val chaveDoc = if (midia.idTmdb != 0) "tmdb_${midia.idTmdb}" else "local_${midia.uuid}"
                colecao.document(chaveDoc).set(midia.toMap(), SetOptions.merge()).await()
            }

            val prefs = appContext.getSharedPreferences("ConfiguracoesPerfil", Context.MODE_PRIVATE)
            prefs.edit().putLong(CHAVE_ULTIMO_BACKUP, System.currentTimeMillis()).apply()

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}