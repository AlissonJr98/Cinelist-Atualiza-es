package com.example.cinelist

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

data class AmigoPerfil(
    val uid: String = "",
    val nome: String = "",
    val email: String = "",
    val bio: String = ""
)

@Singleton
class SocialRepository @Inject constructor() {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Atualiza o perfil público do usuário logado no Firestore para ser encontrado por amigos
    suspend fun atualizarPerfilPublico(nome: String, bio: String) {
        val usuario = auth.currentUser ?: return
        val dados = mapOf(
            "uid" to usuario.uid,
            "nome" to nome,
            "email" to (usuario.email ?: ""),
            "bio" to bio,
            "atualizadoEm" to System.currentTimeMillis()
        )
        firestore.collection("usuarios_publicos").document(usuario.uid).set(dados).await()
    }

    // Busca usuários públicos pelo nome ou e-mail
    suspend fun buscarUsuarios(termo: String): List<AmigoPerfil> {
        if (termo.isBlank()) return emptyList()
        val meuUid = auth.currentUser?.uid ?: ""
        try {
            val resultado = firestore.collection("usuarios_publicos")
                .orderBy("nome")
                .get()
                .await()

            return resultado.documents.mapNotNull { doc ->
                val perfil = doc.toObject(AmigoPerfil::class.java)
                if (perfil != null && perfil.uid != meuUid && (perfil.nome.contains(termo, ignoreCase = true) || perfil.email.contains(termo, ignoreCase = true))) {
                    perfil
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return emptyList()
        }
    }

    // Envia solicitação ou adiciona amigo
    suspend fun adicionarAmigo(amigoUid: String): Boolean {
        val meuUid = auth.currentUser?.uid ?: return false
        try {
            val relacao = mapOf("amigoUid" to amigoUid, "criadoEm" to System.currentTimeMillis())
            firestore.collection("usuarios").document(meuUid).collection("amigos").document(amigoUid).set(relacao).await()
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    // Retorna a lista de amigos conectados em tempo real
    fun observarAmigos(): Flow<List<AmigoPerfil>> = callbackFlow {
        val meuUid = auth.currentUser?.uid
        if (meuUid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection("usuarios").document(meuUid).collection("amigos")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val uidsAmigos = snapshot.documents.mapNotNull { it.getString("amigoUid") }
                if (uidsAmigos.isEmpty()) {
                    trySend(emptyList())
                } else {
                    firestore.collection("usuarios_publicos")
                        .whereIn("uid", uidsAmigos)
                        .get()
                        .addOnSuccessListener { pubDocs ->
                            val amigos = pubDocs.documents.mapNotNull { it.toObject(AmigoPerfil::class.java) }
                            trySend(amigos)
                        }
                        .addOnFailureListener {
                            trySend(emptyList())
                        }
                }
            }

        awaitClose { listener.remove() }
    }

    // Busca a lista de mídias públicas de um amigo específico
    suspend fun buscarListaAmigo(amigoUid: String): List<Midia> {
        try {
            val snapshot = firestore.collection("usuarios").document(amigoUid).collection("minha_lista").get().await()
            return snapshot.documents.mapNotNull { it.toObject(Midia::class.java) }
        } catch (e: Exception) {
            e.printStackTrace()
            return emptyList()
        }
    }
}