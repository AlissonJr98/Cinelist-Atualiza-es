package com.example.cinelist

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaLogin(
    onLoginSucesso: () -> Unit,
    onNavegarParaCadastro: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }
    var erroMensagem by remember { mutableStateOf("") }
    var carregando by remember { mutableStateOf(false) }

    var mostrarDialogoRecuperacao by remember { mutableStateOf(false) }
    var emailRecuperacao by remember { mutableStateOf("") }
    var mensagemRecuperacao by remember { mutableStateOf("") }
    var carregandoRecuperacao by remember { mutableStateOf(false) }

    val firebaseAuth = FirebaseAuth.getInstance()
    val contexto = LocalContext.current

    // 1. CONFIGURAÇÃO DO GOOGLE SIGN-IN
    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken("336901227453-42ejfkk06rr8rifu7cs1lon7vvkjgkab.apps.googleusercontent.com")
        .requestEmail()
        .build()
    val googleSignInClient = GoogleSignIn.getClient(contexto, gso)

    // 2. LANÇADOR DA TELA DO GOOGLE
    val launcherGoogle = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { resultado ->
        if (resultado.resultCode == Activity.RESULT_OK) {
            val tarefa = GoogleSignIn.getSignedInAccountFromIntent(resultado.data)
            try {
                val conta = tarefa.getResult(ApiException::class.java)
                conta?.idToken?.let { token ->
                    val credencial = GoogleAuthProvider.getCredential(token, null)
                    firebaseAuth.signInWithCredential(credencial)
                        .addOnCompleteListener { tarefaFirebase ->
                            carregando = false
                            if (tarefaFirebase.isSuccessful) {
                                onLoginSucesso()
                            } else {
                                erroMensagem = "Erro ao autenticar com o Firebase via Google."
                            }
                        }
                }
            } catch (e: ApiException) {
                carregando = false
                erroMensagem = "Falha no login do Google: ${e.localizedMessage}"
            }
        } else {
            carregando = false
        }
    }

    // POP-UP DE RECUPERAÇÃO DE SENHA
    if (mostrarDialogoRecuperacao) {
        AlertDialog(
            onDismissRequest = {
                if (!carregandoRecuperacao) {
                    mostrarDialogoRecuperacao = false
                    mensagemRecuperacao = ""
                    emailRecuperacao = ""
                }
            },
            title = { Text("Recuperar Senha", color = Color.White, fontWeight = FontWeight.Bold) },
            containerColor = Color(0xFF1E1E1E),
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Insira o seu e-mail cadastrado. Enviaremos um link para você redefinir sua senha.", color = Color.Gray, fontSize = 14.sp)
                    OutlinedTextField(
                        value = emailRecuperacao,
                        onValueChange = { emailRecuperacao = it },
                        label = { Text("E-mail de Cadastro") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !carregandoRecuperacao,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedLabelColor = Color(0xFFFFD700)),
                        singleLine = true
                    )
                    if (mensagemRecuperacao.isNotEmpty()) {
                        Text(text = mensagemRecuperacao, color = if (mensagemRecuperacao.contains("sucesso")) Color(0xFF4CFF4C) else Color(0xFFFF4C4C), fontSize = 14.sp)
                    }
                }
            },
            confirmButton = {
                if (carregandoRecuperacao) {
                    CircularProgressIndicator(color = Color(0xFFFFD700), modifier = Modifier.size(24.dp))
                } else {
                    Button(
                        onClick = {
                            if (emailRecuperacao.isNotBlank()) {
                                carregandoRecuperacao = true
                                firebaseAuth.sendPasswordResetEmail(emailRecuperacao.trim())
                                    .addOnCompleteListener { t ->
                                        carregandoRecuperacao = false
                                        if (t.isSuccessful) {
                                            mensagemRecuperacao = "E-mail de recuperação enviado com sucesso! Verifique sua caixa de entrada."
                                        } else {
                                            val erro = t.exception?.message ?: ""
                                            mensagemRecuperacao = if (erro.contains("user-not-found")) "E-mail não encontrado." else "Erro ao enviar."
                                        }
                                    }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700))
                    ) { Text("Enviar", color = Color.Black, fontWeight = FontWeight.Bold) }
                }
            },
            dismissButton = {
                if (!carregandoRecuperacao) {
                    TextButton(onClick = { mostrarDialogoRecuperacao = false; mensagemRecuperacao = ""; emailRecuperacao = "" }) { Text("Cancelar", color = Color.Gray) }
                }
            }
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF121212)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "CineList", fontSize = 40.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFD700))
            Text(text = "Gerencie seus filmes, séries e animes", fontSize = 14.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 32.dp))

            // Campo E-mail
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("E-mail") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !carregando,
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedLabelColor = Color(0xFFFFD700), unfocusedLabelColor = Color.Gray, focusedBorderColor = Color(0xFFFFD700), unfocusedBorderColor = Color.DarkGray),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Campo Senha
            OutlinedTextField(
                value = senha,
                onValueChange = { senha = it },
                label = { Text("Senha") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !carregando,
                visualTransformation = PasswordVisualTransformation(),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedLabelColor = Color(0xFFFFD700), unfocusedLabelColor = Color.Gray, focusedBorderColor = Color(0xFFFFD700), unfocusedBorderColor = Color.DarkGray),
                singleLine = true
            )

            Box(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), contentAlignment = Alignment.CenterEnd) {
                Text(text = "Esqueci minha senha", color = Color.Gray, fontSize = 14.sp, modifier = Modifier.clickable(enabled = !carregando) { mostrarDialogoRecuperacao = true }.padding(4.dp))
            }

            if (erroMensagem.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = erroMensagem, color = Color(0xFFFF4C4C), fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (carregando) {
                CircularProgressIndicator(color = Color(0xFFFFD700))
            } else {
                // Botão de Entrar Tradicional
                Button(
                    onClick = {
                        if (email.isNotBlank() && senha.isNotBlank()) {
                            erroMensagem = ""
                            carregando = true
                            firebaseAuth.signInWithEmailAndPassword(email.trim(), senha.trim())
                                .addOnCompleteListener { t ->
                                    carregando = false
                                    if (t.isSuccessful) onLoginSucesso() else erroMensagem = "E-mail ou senha incorretos."
                                }
                        } else { erroMensagem = "Por favor, preencha todos os campos." }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700))
                ) { Text("ENTRAR", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp) }

                Spacer(modifier = Modifier.height(20.dp))

                Text(text = "ou entre com", color = Color.Gray, fontSize = 14.sp)

                Spacer(modifier = Modifier.height(12.dp))

                // BOTÃO DE LOGIN DO GOOGLE CIRCULAR COM ICONE DA LOGO
                Surface(
                    modifier = Modifier.size(56.dp),
                    shape = CircleShape,
                    color = Color(0xFF1E1E1E),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.DarkGray)
                ) {
                    IconButton(
                        onClick = {
                            erroMensagem = ""
                            carregando = true
                            val intentSelecaoOpc = googleSignInClient.signInIntent
                            launcherGoogle.launch(intentSelecaoOpc)
                        },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_google),
                            contentDescription = "Login com Google",
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(text = "Não tem uma conta? Cadastre-se", color = Color(0xFFFFD700), fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable(enabled = !carregando) { onNavegarParaCadastro() }.padding(8.dp))
        }
    }
}