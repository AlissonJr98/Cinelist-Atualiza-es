package com.example.cinelist

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
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
            containerColor = Color(0xFF101826),
            shape = RoundedCornerShape(20.dp),
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Insira o seu e-mail cadastrado. Enviaremos um link para você redefinir sua senha.", color = Color.LightGray, fontSize = 14.sp)
                    OutlinedTextField(
                        value = emailRecuperacao,
                        onValueChange = { emailRecuperacao = it },
                        label = { Text("E-mail de Cadastro") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !carregandoRecuperacao,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedLabelColor = Color(0xFFFFD700),
                            unfocusedBorderColor = Color(0xFF1E293B),
                            focusedBorderColor = Color(0xFFFFD700)
                        ),
                        singleLine = true
                    )
                    if (mensagemRecuperacao.isNotEmpty()) {
                        Text(text = mensagemRecuperacao, color = if (mensagemRecuperacao.contains("sucesso")) Color(0xFF4CFF4C) else Color(0xFFFF4C4C), fontSize = 13.sp)
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
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700)),
                        shape = RoundedCornerShape(10.dp)
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
        color = Color(0xFF070B12)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0F1E36), // Azul profundo do logo
                            Color(0xFF0A101A), // Azul escuro intermediário
                            Color(0xFF070B12)  // Preto azulado profundo
                        ),
                        startY = 0f,
                        endY = 1900f
                    )
                )
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // LOGÓTIPO OFICIAL (Certifique-se de ter o arquivo logo_cinelist na pasta res/drawable)
                Image(
                    painter = painterResource(id = R.drawable.ic_logocinelist),
                    contentDescription = "Logótipo CineList",
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "CineList",
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Text(
                    text = "Gerencie seus filmes, séries e animes",
                    fontSize = 13.sp,
                    color = Color(0xFF94A3B8),
                    modifier = Modifier.padding(bottom = 32.dp),
                    textAlign = TextAlign.Center
                )

                // Card Central com efeito Glassmorphism escuro
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1624).copy(alpha = 0.9f)),
                    border = BorderStroke(1.dp, Color(0xFF1E293B)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Campo E-mail
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("E-mail") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !carregando,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedLabelColor = Color(0xFFFFD700),
                                unfocusedLabelColor = Color(0xFF94A3B8),
                                focusedBorderColor = Color(0xFFFFD700),
                                unfocusedBorderColor = Color(0xFF1E293B),
                                unfocusedContainerColor = Color(0xFF070B12).copy(alpha = 0.5f),
                                focusedContainerColor = Color(0xFF070B12).copy(alpha = 0.5f)
                            ),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Campo Senha
                        OutlinedTextField(
                            value = senha,
                            onValueChange = { senha = it },
                            label = { Text("Senha") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !carregando,
                            visualTransformation = PasswordVisualTransformation(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedLabelColor = Color(0xFFFFD700),
                                unfocusedLabelColor = Color(0xFF94A3B8),
                                focusedBorderColor = Color(0xFFFFD700),
                                unfocusedBorderColor = Color(0xFF1E293B),
                                unfocusedContainerColor = Color(0xFF070B12).copy(alpha = 0.5f),
                                focusedContainerColor = Color(0xFF070B12).copy(alpha = 0.5f)
                            ),
                            singleLine = true
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Text(
                                text = "Esqueci minha senha",
                                color = Color(0xFFFFD700),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier
                                    .clickable(enabled = !carregando) { mostrarDialogoRecuperacao = true }
                                    .padding(4.dp)
                            )
                        }

                        if (erroMensagem.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = erroMensagem,
                                color = Color(0xFFFF4C4C),
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        if (carregando) {
                            CircularProgressIndicator(
                                color = Color(0xFFFFD700),
                                modifier = Modifier.size(36.dp)
                            )
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
                                    } else {
                                        erroMensagem = "Por favor, preencha todos os campos."
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700))
                            ) {
                                Text(
                                    "ENTRAR",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFF1E293B))
                    Text(text = "ou continue com", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFF1E293B))
                }

                Spacer(modifier = Modifier.height(16.dp))

                // BOTÃO DE LOGIN DO GOOGLE CIRCULAR
                Surface(
                    modifier = Modifier.size(54.dp),
                    shape = CircleShape,
                    color = Color(0xFF0D1624),
                    border = BorderStroke(1.dp, Color(0xFF1E293B)),
                    shadowElevation = 4.dp
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
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    text = "Não tem uma conta? Cadastre-se",
                    color = Color(0xFFFFD700),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable(enabled = !carregando) { onNavegarParaCadastro() }
                        .padding(8.dp)
                )
            }
        }
    }
}