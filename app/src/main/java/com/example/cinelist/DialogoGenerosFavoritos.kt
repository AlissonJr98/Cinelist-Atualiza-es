package com.example.cinelist

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

/**
 * Diálogo para alterar os Gêneros Favoritos do usuário que personalizam o Explorar.
 */
@Composable
fun DialogoGenerosFavoritos(
    onDispensar: () -> Unit,
    onSalvo: (Set<Int>) -> Unit
) {
    val contexto = LocalContext.current
    var selecionados by remember {
        mutableStateOf(
            GeneroPreferencesManager.obterGenerosFavoritosIds(contexto).ifEmpty {
                setOf(28, 878, 35)
            }
        )
    }

    Dialog(onDismissRequest = onDispensar) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF0F1526),
            border = BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.35f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Gêneros Favoritos 🎬",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            text = "${selecionados.size} selecionados para o Explorar",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }

                    IconButton(onClick = onDispensar) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar", tint = Color.LightGray)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(GeneroPreferencesManager.GENEROS, key = { it.id }) { genero ->
                        val estaSelecionado = genero.id in selecionados

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .clickable {
                                    selecionados = if (estaSelecionado) {
                                        selecionados - genero.id
                                    } else {
                                        selecionados + genero.id
                                    }
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = if (estaSelecionado) Color(0xFFFFD700).copy(alpha = 0.18f) else Color(0xFF131B2E),
                            border = BorderStroke(
                                width = if (estaSelecionado) 1.5.dp else 1.dp,
                                color = if (estaSelecionado) Color(0xFFFFD700) else Color.White.copy(alpha = 0.08f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(genero.iconeEmoji, fontSize = 18.sp)
                                    Text(
                                        text = genero.nome,
                                        fontSize = 12.sp,
                                        fontWeight = if (estaSelecionado) FontWeight.Bold else FontWeight.Medium,
                                        color = if (estaSelecionado) Color.White else Color(0xFF94A3B8)
                                    )
                                }

                                if (estaSelecionado) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFFFFD700),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        GeneroPreferencesManager.salvarGenerosFavoritosIds(contexto, selecionados)
                        onSalvo(selecionados)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Salvar Preferências",
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.Black,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
