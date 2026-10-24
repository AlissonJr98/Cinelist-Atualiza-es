package com.example.cinelist

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import android.widget.Toast
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
    var senhaVisivel by remember { mutableStateOf(false) }
    var erroMensagem by remember { mutableStateOf("") }
    var carregando by remember { mutableStateOf(false) }
    var mostrarBotaoReenviarVerificacao by remember { mutableStateOf(false) }
    var reenviandoEmailLogin by remember { mutableStateOf(false) }

    var mostrarDialogoRecuperacao by remember { mutableStateOf(false) }
    var emailRecuperacao by remember { mutableStateOf("") }
    var mensagemRecuperacao by remember { mutableStateOf("") }
    var carregandoRecuperacao by remember { mutableStateOf(false) }

    val firebaseAuth = FirebaseAuth.getInstance()
    val contexto = LocalContext.current

    // 1. CONFIGURAÇÃO DO GOOGLE SIGN-IN
    val googleSignInClient = remember(contexto) {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("336901227453-42ejfkk06rr8rifu7cs1lon7vvkjgkab.apps.googleusercontent.com")
            .requestEmail()
            .build()
        GoogleSignIn.getClient(contexto, gso)
    }

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

    // POP-UP DE RECUPERAÇÃO DE SENHA SCI-FI
    if (mostrarDialogoRecuperacao) {
        AlertDialog(
            onDismissRequest = {
                if (!carregandoRecuperacao) {
                    mostrarDialogoRecuperacao = false
                    mensagemRecuperacao = ""
                    emailRecuperacao = ""
                }
            },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PulsarPontoNeon(cor = Color(0xFF00E5FF), tamanho = 8.dp)
                    Text("Recuperar Acesso", color = Color.White, fontWeight = FontWeight.Black, fontSize = 17.sp)
                }
            },
            containerColor = Color(0xFF0C101B),
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier.border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    listOf(
                        Color(0xFF00E5FF).copy(alpha = 0.5f),
                        Color(0xFFFFD700).copy(alpha = 0.2f),
                        Color.White.copy(alpha = 0.05f)
                    )
                ),
                shape = RoundedCornerShape(22.dp)
            ),
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Insira o seu e-mail cadastrado. Enviaremos um link de recuperação para você redefinir sua credencial.",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 13.sp
                    )
                    OutlinedTextField(
                        value = emailRecuperacao,
                        onValueChange = { emailRecuperacao = it },
                        label = { Text("E-mail de Cadastro") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !carregandoRecuperacao,
                        shape = RoundedCornerShape(14.dp),
                        leadingIcon = {
                            Icon(
                                Icons.Default.Email,
                                contentDescription = null,
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedLabelColor = Color(0xFF00E5FF),
                            unfocusedLabelColor = Color.White.copy(alpha = 0.6f),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                            focusedBorderColor = Color(0xFF00E5FF),
                            unfocusedContainerColor = Color(0xFF070B12).copy(alpha = 0.6f),
                            focusedContainerColor = Color(0xFF070B12).copy(alpha = 0.6f)
                        ),
                        singleLine = true
                    )
                    if (mensagemRecuperacao.isNotEmpty()) {
                        Text(
                            text = mensagemRecuperacao,
                            color = if (mensagemRecuperacao.contains("sucesso")) Color(0xFF00FF7F) else Color(0xFFFF5252),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            },
            confirmButton = {
                if (carregandoRecuperacao) {
                    CircularProgressIndicator(color = Color(0xFF00E5FF), modifier = Modifier.size(24.dp))
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
                                            mensagemRecuperacao = if (erro.contains("user-not-found")) "E-mail não encontrado." else "Erro ao enviar link."
                                        }
                                    }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Enviar Link", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                if (!carregandoRecuperacao) {
                    TextButton(onClick = {
                        mostrarDialogoRecuperacao = false
                        mensagemRecuperacao = ""
                        emailRecuperacao = ""
                    }) {
                        Text("Cancelar", color = Color.White.copy(alpha = 0.6f))
                    }
                }
            }
        )
    }

    // TELA PRINCIPAL DE LOGIN COM FUNDO MESH 3D
    MeshBackground3D {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // LOGÓTIPO OFICIAL COM ANEL HOLOGRÁFICO E BEACON NEON
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        Color(0xFF00E5FF).copy(alpha = 0.25f),
                                        Color.Transparent
                                    )
                                )
                            )
                            .border(
                                width = 1.5.dp,
                                brush = Brush.linearGradient(
                                    listOf(
                                        Color(0xFF00E5FF),
                                        Color(0xFFFFD700),
                                        Color(0xFF00E5FF).copy(alpha = 0.2f)
                                    )
                                ),
                                shape = CircleShape
                            )
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_logocinelist),
                            contentDescription = "Logótipo CineList",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    }

                    // Ponto neon pulsante no canto
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(end = 4.dp, top = 4.dp)
                    ) {
                        PulsarPontoNeon(cor = Color(0xFF00FF7F), tamanho = 12.dp)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // TÍTULO E SUBTÍTULO HUD SCI-FI
                Text(
                    text = "CINELIST",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    color = Color.White
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF00E5FF).copy(alpha = 0.15f))
                            .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "PORTAL DE ACESSO // V2.0",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00E5FF)
                        )
                    }
                }

                // CARD CENTRAL COM EFEITO CYBER GLASSMORPHISM
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color(0xFF0C101B).copy(alpha = 0.90f))
                        .border(
                            width = 1.dp,
                            brush = Brush.linearGradient(
                                listOf(
                                    Color(0xFF00E5FF).copy(alpha = 0.45f),
                                    Color(0xFFFFD700).copy(alpha = 0.25f),
                                    Color.White.copy(alpha = 0.06f)
                                )
                            ),
                            shape = RoundedCornerShape(22.dp)
                        )
                        .padding(20.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Campo E-mail
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("E-mail") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !carregando,
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Email,
                                    contentDescription = null,
                                    tint = Color(0xFF00E5FF).copy(alpha = 0.8f),
                                    modifier = Modifier.size(19.dp)
                                )
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedLabelColor = Color(0xFF00E5FF),
                                unfocusedLabelColor = Color.White.copy(alpha = 0.6f),
                                focusedBorderColor = Color(0xFF00E5FF),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                                unfocusedContainerColor = Color(0xFF070B12).copy(alpha = 0.6f),
                                focusedContainerColor = Color(0xFF070B12).copy(alpha = 0.6f)
                            ),
                            singleLine = true
                        )

                        // Campo Senha com Alternância de Visibilidade
                        OutlinedTextField(
                            value = senha,
                            onValueChange = { senha = it },
                            label = { Text("Senha") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !carregando,
                            visualTransformation = if (senhaVisivel) VisualTransformation.None else PasswordVisualTransformation(),
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = Color(0xFF00E5FF).copy(alpha = 0.8f),
                                    modifier = Modifier.size(19.dp)
                                )
                            },
                            trailingIcon = {
                                IconButton(onClick = { senhaVisivel = !senhaVisivel }) {
                                    Icon(
                                        imageVector = if (senhaVisivel) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = if (senhaVisivel) "Ocultar senha" else "Mostrar senha",
                                        tint = Color.White.copy(alpha = 0.6f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedLabelColor = Color(0xFF00E5FF),
                                unfocusedLabelColor = Color.White.copy(alpha = 0.6f),
                                focusedBorderColor = Color(0xFF00E5FF),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                                unfocusedContainerColor = Color(0xFF070B12).copy(alpha = 0.6f),
                                focusedContainerColor = Color(0xFF070B12).copy(alpha = 0.6f)
                            ),
                            singleLine = true
                        )

                        // Link "Esqueci minha senha"
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 2.dp),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Text(
                                text = "Esqueceu sua senha?",
                                color = Color(0xFF00E5FF),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .clickable(enabled = !carregando) { mostrarDialogoRecuperacao = true }
                                    .padding(vertical = 4.dp, horizontal = 2.dp)
                            )
                        }

                        // Mensagem de Erro
                        AnimatedVisibility(
                            visible = erroMensagem.isNotEmpty(),
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Text(
                                text = erroMensagem,
                                color = Color(0xFFFF5252),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Botão Conectar (Entrar)
                        if (carregando) {
                            CircularProgressIndicator(
                                color = Color(0xFF00E5FF),
                                modifier = Modifier.size(36.dp)
                            )
                        } else {
                            Button(
                                onClick = {
                                    if (email.isNotBlank() && senha.isNotBlank()) {
                                        erroMensagem = ""
                                        carregando = true
                                        firebaseAuth.signInWithEmailAndPassword(email.trim(), senha.trim())
                                            .addOnCompleteListener { t ->
                                                carregando = false
                                                if (t.isSuccessful) {
                                                    val u = firebaseAuth.currentUser
                                                    if (u != null && !u.isEmailVerified) {
                                                        erroMensagem = "Seu e-mail ainda não foi confirmado. Por favor, confirme o link enviado para ${u.email} antes de acessar."
                                                        mostrarBotaoReenviarVerificacao = true
                                                    } else {
                                                        onLoginSucesso()
                                                    }
                                                } else {
                                                    erroMensagem = "E-mail ou senha incorretos."
                                                    mostrarBotaoReenviarVerificacao = false
                                                }
                                            }
                                    } else {
                                        erroMensagem = "Por favor, preencha todos os campos."
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .bounceOnClick {
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
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                contentPadding = PaddingValues()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(
                                                    Color(0xFF00E5FF),
                                                    Color(0xFF0088FF)
                                                )
                                            ),
                                            shape = RoundedCornerShape(14.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            "ENTRAR NA CONTA",
                                            color = Color.Black,
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = 0.5.sp,
                                            fontSize = 13.sp
                                        )
                                        Icon(
                                            Icons.AutoMirrored.Filled.Login,
                                            contentDescription = null,
                                            tint = Color.Black,
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // DIVISOR HOLOGRÁFICO "OU CONTINUE COM"
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = Color.White.copy(alpha = 0.12f)
                    )
                    Text(
                        text = "ou autentique com",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = Color.White.copy(alpha = 0.12f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // BOTÃO DE LOGIN COM O GOOGLE EM CÁPSULA SCI-FI
                OutlinedButton(
                    onClick = {
                        erroMensagem = ""
                        carregando = true
                        val intentSelecaoOpc = googleSignInClient.signInIntent
                        launcherGoogle.launch(intentSelecaoOpc)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .bounceOnClick {
                            erroMensagem = ""
                            carregando = true
                            val intentSelecaoOpc = googleSignInClient.signInIntent
                            launcherGoogle.launch(intentSelecaoOpc)
                        },
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.White.copy(alpha = 0.05f)
                    )
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_google),
                        contentDescription = "Login com Google",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Continuar com Google",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // LINK PARA A TELA DE CADASTRO
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(enabled = !carregando) { onNavegarParaCadastro() }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Não tem uma conta? ",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 13.sp
                    )
                    Text(
                        text = "Cadastre-se",
                        color = Color(0xFF00E5FF),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
