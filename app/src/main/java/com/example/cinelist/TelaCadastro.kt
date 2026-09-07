package com.example.cinelist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaCadastro(
    onCadastroSucesso: () -> Unit,
    onVoltarParaLogin: () -> Unit
) {
    var nome by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }
    var erroMensagem by remember { mutableStateOf("") }

    // Estado para mostrar uma barrinha de carregamento enquanto o Firebase trabalha
    var carregando by remember { mutableStateOf(false) }

    // Pegamos o "gerente" de autenticação do Firebase
    val firebaseAuth = FirebaseAuth.getInstance()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF121212)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Criar Conta",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFD700)
            )

            Text(
                text = "Comece a organizar sua lista de mídias agora",
                fontSize = 14.sp,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            // Campo de Nome
            OutlinedTextField(
                value = nome,
                onValueChange = { nome = it },
                label = { Text("Nome Completo") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !carregando,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedLabelColor = Color(0xFFFFD700),
                    unfocusedLabelColor = Color.Gray,
                    focusedBorderColor = Color(0xFFFFD700),
                    unfocusedBorderColor = Color.DarkGray
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Campo de E-mail
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("E-mail") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !carregando,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedLabelColor = Color(0xFFFFD700),
                    unfocusedLabelColor = Color.Gray,
                    focusedBorderColor = Color(0xFFFFD700),
                    unfocusedBorderColor = Color.DarkGray
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Campo de Senha
            OutlinedTextField(
                value = senha,
                onValueChange = { senha = it },
                label = { Text("Senha") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !carregando,
                visualTransformation = PasswordVisualTransformation(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedLabelColor = Color(0xFFFFD700),
                    unfocusedLabelColor = Color.Gray,
                    focusedBorderColor = Color(0xFFFFD700),
                    unfocusedBorderColor = Color.DarkGray
                ),
                singleLine = true
            )

            if (erroMensagem.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = erroMensagem, color = Color(0xFFFF4C4C), fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Se estiver carregando, mostra o indicador redondo, se não, mostra o botão
            if (carregando) {
                CircularProgressIndicator(color = Color(0xFFFFD700))
            } else {
                Button(
                    onClick = {
                        if (nome.isNotBlank() && email.isNotBlank() && senha.isNotBlank()) {
                            if (senha.length < 6) {
                                erroMensagem = "A senha deve ter pelo menos 6 caracteres."
                            } else {
                                erroMensagem = ""
                                carregando = true

                                // COMANDO DO FIREBASE: Cria o usuário com e-mail e senha
                                firebaseAuth.createUserWithEmailAndPassword(email.trim(), senha.trim())
                                    .addOnCompleteListener { tarefa ->
                                        if (tarefa.isSuccessful) {
                                            // Se criou a conta, vamos salvar o Nome do usuário no perfil do Firebase
                                            val usuario = firebaseAuth.currentUser
                                            val atualizacaoPerfil = userProfileChangeRequest {
                                                displayName = nome.trim()
                                            }

                                            usuario?.updateProfile(atualizacaoPerfil)
                                                ?.addOnCompleteListener {
                                                    carregando = false
                                                    onCadastroSucesso() // Avança para a Home
                                                }
                                        } else {
                                            carregando = false
                                            // Traduz os erros mais comuns do Firebase para o usuário
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
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700))
                ) {
                    Text(
                        text = "CADASTRAR",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Já tem uma conta? Entre aqui",
                color = Color(0xFFFFD700),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clickable(enabled = !carregando) { onVoltarParaLogin() }
                    .padding(8.dp)
            )
        }
    }
}