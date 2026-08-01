package com.ethan.orbitlab.ui.estante

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethan.orbitlab.data.firebase.DocumentoUi
import com.ethan.orbitlab.data.firebase.FirestoreDocumentos
import com.ethan.orbitlab.ui.chat.DocumentoReaderSheet
import com.ethan.orbitlab.ui.theme.OrbitIconButton
import com.ethan.orbitlab.ui.theme.OrbitMetrics
import com.ethan.orbitlab.ui.theme.OrbitTokens
import com.ethan.orbitlab.ui.theme.orbitEnter
import com.ethan.orbitlab.ui.theme.orbitPressable
import com.google.firebase.auth.FirebaseAuth

/**
 * A Estante — a tela de TODOS os artefatos, de qualquer conversa. Resolve o «onde acho o que a
 * Luna criou»: antes cada artefato só existia na conversa onde nasceu. Aqui eles ficam juntos,
 * ordenados pelo mais mexido, com busca. Tocar num cartão abre o mesmo leitor tela cheia do chat.
 */
@Composable
fun EstanteScreen(onBack: (() -> Unit)? = null) {
    val uid = FirebaseAuth.getInstance().currentUser?.uid

    var artefatos by remember { mutableStateOf<List<DocumentoUi>>(emptyList()) }
    var carregando by remember { mutableStateOf(true) }
    var busca by remember { mutableStateOf("") }
    var selecionado by remember { mutableStateOf<DocumentoUi?>(null) }

    DisposableEffect(uid) {
        val registro = uid?.let {
            FirestoreDocumentos.subscribeTodos(
                uid = it,
                onChange = { lista -> artefatos = lista; carregando = false },
                onError = { carregando = false },
            )
        }
        onDispose { registro?.remove() }
    }

    val filtrados = remember(artefatos, busca) {
        val q = busca.trim()
        if (q.isBlank()) artefatos
        else artefatos.filter {
            it.titulo.contains(q, ignoreCase = true) || it.conteudo.contains(q, ignoreCase = true)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent),
    ) {
        AtmosferaEstante()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 2.dp),
        ) {
            HeaderEstante(total = artefatos.size)

            if (artefatos.isNotEmpty()) {
                CampoBusca(
                    valor = busca,
                    onValor = { busca = it },
                    modifier = Modifier
                        .padding(horizontal = OrbitMetrics.pagePadding)
                        .padding(top = 8.dp, bottom = 4.dp),
                )
            }

            when {
                carregando -> Unit // silêncio breve; o listener chega rápido
                artefatos.isEmpty() -> EstanteVazia(
                    titulo = "Nenhum artefato ainda",
                    linha = "Peça à Luna pra escrever algo — um plano, uma carta, uma nota. Ele nasce aqui.",
                )
                filtrados.isEmpty() -> EstanteVazia(
                    titulo = "Nada encontrado",
                    linha = "Nenhum artefato bate com «${busca.trim()}».",
                )
                else -> LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(
                        start = OrbitMetrics.pagePadding,
                        end = OrbitMetrics.pagePadding,
                        top = 6.dp,
                        bottom = 120.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(filtrados, key = { it.id }) { doc ->
                        CartaoEstante(doc = doc, onClick = { selecionado = doc })
                    }
                }
            }
        }
    }

    // O leitor tela cheia (o mesmo do chat) sobrepõe a estante quando um artefato é tocado.
    selecionado?.let { doc ->
        DocumentoReaderSheet(doc = doc, onDismiss = { selecionado = null })
    }
}

@Composable
private fun HeaderEstante(total: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = OrbitMetrics.pagePadding)
            .padding(top = 2.dp),
    ) {
        Text(
            "Estante",
            color = OrbitTokens.textHiN,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.3).sp,
        )
        Spacer(modifier = Modifier.height(1.dp))
        Text(
            text = when (total) {
                0 -> "O que a Luna escreveu pra você"
                1 -> "1 artefato no seu acervo"
                else -> "$total artefatos no seu acervo"
            },
            color = OrbitTokens.textMidN,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun CampoBusca(
    valor: String,
    onValor: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(OrbitMetrics.radiusChip + 4.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(OrbitTokens.graphiteSurf)
            .border(1.dp, OrbitTokens.graphiteHair, shape)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Rounded.Search,
            contentDescription = null,
            tint = OrbitTokens.textLowN,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(10.dp))
        BasicTextField(
            value = valor,
            onValueChange = onValor,
            textStyle = TextStyle(color = OrbitTokens.textHiN, fontSize = 15.sp),
            cursorBrush = SolidColor(OrbitTokens.bluePastel),
            singleLine = true,
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                if (valor.isEmpty()) {
                    Text("Buscar nos artefatos…", color = OrbitTokens.textLowN, fontSize = 15.sp)
                }
                inner()
            },
        )
    }
}

@Composable
private fun CartaoEstante(doc: DocumentoUi, onClick: () -> Unit) {
    // Espelha a linha da estante-no-fio (LinhaArtefatoEstante): cartão grafite quieto + ladrilho
    // NEUTRO com o ícone em azul pastel. O acento aparece só no ícone; contraste, não moldura.
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(OrbitTokens.graphiteSurf)
            .border(1.dp, OrbitTokens.graphiteHair, shape)
            .orbitPressable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(OrbitTokens.graphiteRaised),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.Description,
                contentDescription = null,
                tint = OrbitTokens.bluePastel,
                modifier = Modifier.size(21.dp),
            )
        }
        Spacer(Modifier.width(13.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = doc.titulo,
                color = OrbitTokens.textHiN,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.2).sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = metaEstante(doc),
                color = OrbitTokens.textMidN,
                fontSize = 12.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        Icon(
            Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = OrbitTokens.textLowN,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun EstanteVazia(titulo: String, linha: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(OrbitTokens.graphiteRaised),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Description,
                    contentDescription = null,
                    tint = OrbitTokens.bluePastel,
                    modifier = Modifier.size(28.dp),
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = titulo,
                color = OrbitTokens.textHiN,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = linha,
                color = OrbitTokens.textMidN,
                fontSize = 13.5.sp,
                lineHeight = 19.sp,
                modifier = Modifier.orbitEnter(),
            )
        }
    }
}

@Composable
private fun AtmosferaEstante() {
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(OrbitTokens.bluePastel.copy(alpha = 0.07f), Color.Transparent),
                        radius = 900f,
                    ),
                ),
        )
    }
}

/** "editado há 2 h · 340 palavras" — a linha discreta sob o título na estante. */
private fun metaEstante(doc: DocumentoUi): String {
    val editado = doc.updatedAtMs > doc.createdAtMs + 2000
    val verbo = if (editado) "editado" else "criado"
    val quando = tempoRelativoEstante(if (editado) doc.updatedAtMs else doc.createdAtMs)
    val palavras = contarPalavrasEstante(doc.conteudo)
    return buildList {
        add(listOf(verbo, quando).filter { it.isNotBlank() }.joinToString(" "))
        if (palavras > 0) add("$palavras ${if (palavras == 1) "palavra" else "palavras"}")
    }.joinToString("  ·  ")
}

private fun contarPalavrasEstante(texto: String): Int {
    val t = texto.trim()
    return if (t.isEmpty()) 0 else t.split(Regex("\\s+")).count { it.isNotBlank() }
}

private fun tempoRelativoEstante(ms: Long): String {
    if (ms <= 0L) return ""
    val diff = System.currentTimeMillis() - ms
    if (diff < 0) return "agora há pouco"
    val min = diff / 60_000
    val hora = diff / 3_600_000
    val dia = diff / 86_400_000
    return when {
        min < 1 -> "agora há pouco"
        min < 60 -> "há $min min"
        hora < 24 -> "há $hora h"
        dia == 1L -> "ontem"
        dia < 7 -> "há $dia dias"
        else -> "em " + java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale("pt", "BR"))
            .format(java.util.Date(ms))
    }
}
