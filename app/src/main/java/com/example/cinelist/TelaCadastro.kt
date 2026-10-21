package com.example.cinelist

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaCadastro(
    onCadastroSucesso: () -> Unit,
    onVoltarParaLogin: () -> Unit
) {
    var nome by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }
    var senhaVisivel by remember { mutableStateOf(false) }
    var erroMensagem by remember { mutableStateOf("") }
    var carregando by remember { mutableStateOf(false) }

    val firebaseAuth = FirebaseAuth.getInstance()

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

                // LOGÓTIPO OFICIAL COM ANEL HOLOGRÁFICO
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(90.dp)
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

                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(end = 4.dp, top = 4.dp)
                    ) {
                        PulsarPontoNeon(cor = Color(0xFF00E5FF), tamanho = 12.dp)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "NOVA IDENTIDADE",
                    fontSize = 28.sp,
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
                            text = "REGISTRO DE USUÁRIO // NÚCLEO CINELIST",
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
                        // Campo Nome
                        OutlinedTextField(
                            value = nome,
                            onValueChange = { nome = it },
                            label = { Text("Nome Completo") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !carregando,
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Person,
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

                        // Campo Senha
                        OutlinedTextField(
                            value = senha,
                            onValueChange = { senha = it },
                            label = { Text("Senha (mínimo 6 dígitos)") },
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

                        // Botão Criar Conta
                        if (carregando) {
                            CircularProgressIndicator(
                                color = Color(0xFF00E5FF),
                                modifier = Modifier.size(36.dp)
                            )
                        } else {
                            Button(
                                onClick = {
                                    if (nome.isNotBlank() && email.isNotBlank() && senha.isNotBlank()) {
                                        if (senha.length < 6) {
                                            erroMensagem = "A senha deve ter pelo menos 6 caracteres."
                                        } else {
                                            erroMensagem = ""
                                            carregando = true
                                            firebaseAuth.createUserWithEmailAndPassword(email.trim(), senha.trim())
                                                .addOnCompleteListener { tarefa ->
                                                    if (tarefa.isSuccessful) {
                                                        val usuario = firebaseAuth.currentUser
                                                        val nomeFinal = nome.trim()
                                                        val atualizacaoPerfil = userProfileChangeRequest {
                                                            displayName = nomeFinal
                                                        }
                                                        usuario?.updateProfile(atualizacaoPerfil)
                                                            ?.addOnCompleteListener {
                                                                val uid = usuario.uid
                                                                val dadosPublicos = mapOf(
                                                                    "uid" to uid,
                                                                    "nome" to nomeFinal,
                                                                    "nomeBusca" to nomeFinal.lowercase(),
                                                                    "email" to email.trim().lowercase(),
                                                                    "fotoUrl" to "",
                                                                    "online" to true,
                                                                    "vistoPorUltimo" to System.currentTimeMillis()
                                                                )
                                                                FirebaseFirestore.getInstance().collection("usuarios_publicos")
                                                                    .document(uid)
                                                                    .set(dadosPublicos, SetOptions.merge())
                                                                    .addOnCompleteListener {
                                                                        carregando = false
                                                                        onCadastroSucesso()
                                                                    }
                                                            }
                                                    } else {
                                                        carregando = false
                                                        val excecao = tarefa.exception?.message ?: ""
                                                        erroMensagem = when {
                                                            excecao.contains("already in use") -> "Este e-mail já está cadastrado."
                                                            excecao.contains("badly formatted") -> "O formato do e-mail é inválido."
                                                            else -> "Erro ao cadastrar: ${tarefa.exception?.localizedMessage}"
                                                        }
                                                    }
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
                                        if (nome.isNotBlank() && email.isNotBlank() && senha.isNotBlank()) {
                                            if (senha.length < 6) {
                                                erroMensagem = "A senha deve ter pelo menos 6 caracteres."
                                            } else {
                                                erroMensagem = ""
                                                carregando = true
                                                firebaseAuth.createUserWithEmailAndPassword(email.trim(), senha.trim())
                                                    .addOnCompleteListener { tarefa ->
                                                        if (tarefa.isSuccessful) {
                                                            val usuario = firebaseAuth.currentUser
                                                            val nomeFinal = nome.trim()
                                                            val atualizacaoPerfil = userProfileChangeRequest {
                                                                displayName = nomeFinal
                                                            }
                                                            usuario?.updateProfile(atualizacaoPerfil)
                                                                ?.addOnCompleteListener {
                                                                    val uid = usuario.uid
                                                                    val dadosPublicos = mapOf(
                                                                        "uid" to uid,
                                                                        "nome" to nomeFinal,
                                                                        "nomeBusca" to nomeFinal.lowercase(),
                                                                        "email" to email.trim().lowercase(),
                                                                        "fotoUrl" to "",
                                                                        "online" to true,
                                                                        "vistoPorUltimo" to System.currentTimeMillis()
                                                                    )
                                                                    FirebaseFirestore.getInstance().collection("usuarios_publicos")
                                                                        .document(uid)
                                                                        .set(dadosPublicos, SetOptions.merge())
                                                                        .addOnCompleteListener {
                                                                            carregando = false
                                                                            onCadastroSucesso()
                                                                        }
                                                                }
                                                        } else {
                                                            carregando = false
                                                            val excecao = tarefa.exception?.message ?: ""
                                                            erroMensagem = when {
                                                                excecao.contains("already in use") -> "Este e-mail já está cadastrado."
                                                                excecao.contains("badly formatted") -> "O formato do e-mail é inválido."
                                                                else -> "Erro ao cadastrar: ${tarefa.exception?.localizedMessage}"
                                                            }
                                                        }
                                                    }
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
                                            "FINALIZAR REGISTRO",
                                            color = Color.Black,
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = 0.5.sp,
                                            fontSize = 13.sp
                                        )
                                        Icon(
                                            Icons.Default.Check,
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

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(enabled = !carregando) { onVoltarParaLogin() }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Já possui uma conta? ",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 13.sp
                    )
                    Text(
                        text = "Entre aqui",
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
