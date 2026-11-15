package com.example.cinelist

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun DialogoPerfilDublador(
    voz: VozDublador,
    onDispensar: () -> Unit,
    onBuscarNoApp: (String) -> Unit
) {
    val papeisIconicos = remember(voz.dublador) {
        obterPapeisIconicos(voz.dublador)
    }

    Dialog(onDismissRequest = onDispensar) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f)),
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Topo com botão fechar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF10B981).copy(alpha = 0.2f),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Dublador".t(),
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Text(
                            text = "Voz Brasileira".t(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981)
                        )
                    }

                    IconButton(
                        onClick = onDispensar,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fechar".t(),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Avatar / Holograma de Dublador
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF10B981), Color(0xFF00E5FF))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = voz.dublador.take(2).uppercase(),
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.Black
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = voz.dublador,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                if (voz.personagem.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.15f),
                        border = BorderStroke(0.6.dp, Color(0xFF10B981).copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = "${"Dá voz a:".t()} ${voz.personagem}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF10B981),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                if (voz.atorOriginal.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${"Ator/Atriz original:".t()} ${voz.atorOriginal}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.secondary,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                Spacer(modifier = Modifier.height(14.dp))

                // Trabalhos e papéis icônicos
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Outros Papéis Marcantes no Brasil:".t(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    papeisIconicos.forEach { papel ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "🎭 $papel",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Botão de Busca
                Button(
                    onClick = {
                        onDispensar()
                        onBuscarNoApp(voz.dublador)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Buscar Trabalhos no CineList".t(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

private fun obterPapeisIconicos(nomeDublador: String): List<String> {
    val n = nomeDublador.lowercase()
    return when {
        n.contains("guilherme briggs") -> listOf(
            "Buzz Lightyear (Toy Story)",
            "Superman / Clark Kent (DC)",
            "Cosmo (Os Padrinhos Mágicos)",
            "Freakazoid!",
            "Mickey Mouse (Disney)"
        )
        n.contains("wendell bezerra") -> listOf(
            "Goku (Dragon Ball Z / Super)",
            "Bob Esponja Calça Quadrada",
            "Robert Pattinson (The Batman / Crepúsculo)",
            "Edward Norton (Clube da Luta)"
        )
        n.contains("márcio seixas") || n.contains("marcio seixas") -> listOf(
            "Batman / Bruce Wayne (DC Animated)",
            "James Bond / Sean Connery",
            "Sr. Incrível (Os Incríveis)",
            "Spock (Jornada nas Estrelas)"
        )
        n.contains("manolo rey") -> listOf(
            "Sonic the Hedgehog",
            "Peter Parker / Homem-Aranha (Tobey Maguire)",
            "Michael J. Fox (De Volta Para o Futuro)",
            "Will Smith (Um Maluco no Pedaço)"
        )
        n.contains("mauro ramos") -> listOf(
            "Shrek (Shrek Terceiro e Para Sempre)",
            "Pumba (O Rei Leão)",
            "Sulley (Monstros S.A.)",
            "Gary Oldman (Harry Potter / Gordon)"
        )
        n.contains("mário jorge") || n.contains("mario jorge") -> listOf(
            "Eddie Murphy (Um Tira da Pesada / Dr. Dolittle)",
            "Burro (Shrek)",
            "Luke Skywalker (Star Wars)",
            "John Travolta (Pulp Fiction)"
        )
        n.contains("marco ribeiro") -> listOf(
            "Tony Stark / Homem de Ferro (MCU)",
            "O Máskara (Jim Carrey)",
            "Tom Hanks (Náufrago / Forrest Gump)",
            "Yusuke Urameshi (Yu Yu Hakusho)"
        )
        n.contains("alexandre moreno") -> listOf(
            "Gato de Botas (Shrek / Filme Solo)",
            "Marty (Madagascar)",
            "Adam Sandler (Click / Como Se Fosse a 1ª Vez)",
            "Pinky (Pinky e o Cérebro)"
        )
        n.contains("philippe maia") -> listOf(
            "Sam Winchester (Supernatural)",
            "Paul Walker / Brian O'Conner (Velozes & Furiosos)",
            "Sherlock Holmes (Robert Downey Jr.)",
            "Chris Rock (Todo Mundo Odeia o Chris)"
        )
        n.contains("mabel cezar") -> listOf(
            "Jessie (Toy Story 2, 3 e 4)",
            "Princesa Leia (Star Wars)",
            "Jay Kyle (Eu, a Patroa e as Crianças)",
            "Shannon Rutherford (Lost)"
        )
        n.contains("miriam ficher") -> listOf(
            "Drew Barrymore (Como Se Fosse a 1ª Vez)",
            "Sandra Bullock (Gravidade)",
            "Botan (Yu Yu Hakusho)",
            "Nicole Kidman (Moulin Rouge)"
        )
        n.contains("úrsula bezerra") || n.contains("ursula bezerra") -> listOf(
            "Naruto Uzumaki (Naruto / Shippuden)",
            "Goku criança (Dragon Ball clássico)",
            "Sailor Chibi Moon"
        )
        n.contains("tata guarnieri") || n.contains("tatá guarnieri") -> listOf(
            "Seiya de Pégaso (Cavaleiros do Zodíaco)",
            "Jackie Chan (As Aventuras de Jackie Chan)",
            "Kenshin Himura (Samurai X)"
        )
        n.contains("hermes baroli") -> listOf(
            "Seiya de Pégaso (Saga de Hades / Filmes)",
            "Ashton Kutcher (Efeito Borboleta)",
            "Roy Mustang (Fullmetal Alchemist)"
        )
        n.contains("letícia quinto") || n.contains("leticia quinto") -> listOf(
            "Saori Kido / Atena (Cavaleiros do Zodíaco)",
            "Sandy Bochechas (Bob Esponja)",
            "Riza Hawkeye (Fullmetal Alchemist)"
        )
        n.contains("garcia júnior") || n.contains("garcia junior") -> listOf(
            "He-Man (Mestres do Universo)",
            "Arnold Schwarzenegger (Exterminador do Futuro)",
            "Pica-Pau",
            "Capitão América / Steve Rogers (Animação)"
        )
        n.contains("isaac bardavid") -> listOf(
            "Wolverine / Logan (Hugh Jackman / X-Men)",
            "Esqueleto (He-Man)",
            "Tigrão (Ursinho Pooh)",
            "Odin (Anthony Hopkins)"
        )
        n.contains("orlando drummond") -> listOf(
            "Scooby-Doo",
            "Alf, o ETeimoso",
            "Popeye",
            "Vingador (Caverna do Dragão)"
        )
        n.contains("selton mello") -> listOf(
            "Kuzco (A Nova Onda do Imperador)",
            "Daniel LaRusso (Karatê Kid clássico)",
            "Charlie Brown"
        )
        n.contains("rodrigo santoro") -> listOf(
            "Stuart Little (Voz brasileira oficial)",
            "Klaus (A Lenda de Klaus)"
        )
        else -> listOf(
            "Voz consagrada na teledramaturgia e cinema brasileiro",
            "Trabalhos reconhecidos em séries, animes e animações"
        )
    }
}
