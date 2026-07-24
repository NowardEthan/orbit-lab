package com.ethan.orbitlab.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethan.orbitlab.data.AuthRepository
import com.ethan.orbitlab.data.Mensagem
import com.ethan.orbitlab.data.lunaapi.LunaApiClient
import com.ethan.orbitlab.ui.theme.OrbitMetrics
import com.ethan.orbitlab.ui.theme.OrbitTokens
import com.ethan.orbitlab.ui.theme.orbitPressable
import kotlinx.coroutines.launch

/**
 * Busca dentro da conversa — o «pergunta à Luna» da busca. Uma tela própria que desliza
 * sobre o chat: você digita, ela lê as mensagens e devolve os pontos que casam pelo
 * SIGNIFICADO (não só a palavra literal). Tocar num resultado rola até a mensagem.
 */
@Composable
fun BuscaConversaOverlay(
    visivel: Boolean,
    mensagens: List<Mensagem>,
    onIrPara: (String) -> Unit,
    onFechar: () -> Unit,
) {
    AnimatedVisibility(
        visible = visivel,
        enter = fadeIn(tween(160)),
        exit = fadeOut(tween(140)),
    ) {
        BuscaConteudo(mensagens = mensagens, onIrPara = onIrPara, onFechar = onFechar)
    }
}

@Composable
private fun BuscaConteudo(
    mensagens: List<Mensagem>,
    onIrPara: (String) -> Unit,
    onFechar: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current
    val foco = remember { FocusRequester() }

    var termo by remember { mutableStateOf("") }
    var carregando by remember { mutableStateOf(false) }
    var erro by remember { mutableStateOf<String?>(null) }
    var resultados by remember { mutableStateOf<List<LunaApiClient.BuscaItem>>(emptyList()) }
    var buscou by remember { mutableStateOf(false) }

    // Abre com o teclado já pronto — é uma tela de digitar.
    LaunchedEffect(Unit) { runCatching { foco.requestFocus() } }

    fun disparar() {
        val q = termo.trim()
        if (q.isBlank() || carregando) return
        keyboard?.hide()
        carregando = true
        erro = null
        buscou = true
        scope.launch {
            val token = AuthRepository.getIdToken()
            val enxutas = mensagens
                .filterNot { it.erro }
                .filter { it.texto.isNotBlank() }
                .map {
                    LunaApiClient.MensagemBusca(
                        id = it.id,
                        papel = if (it.isLuna) "luna" else "user",
                        texto = it.texto,
                    )
                }
            val resp = LunaApiClient.buscarConversa(token, q, enxutas)
            carregando = false
            if (resp.error != null) {
                erro = resp.error
                resultados = emptyList()
            } else {
                erro = null
                resultados = resp.resultados
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(OrbitTokens.ink1)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        BuscaBarra(
            termo = termo,
            foco = foco,
            onTermo = { termo = it },
            onLimpar = { termo = ""; resultados = emptyList(); buscou = false; erro = null },
            onBuscar = { disparar() },
            onFechar = onFechar,
        )
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(OrbitTokens.borderSoft.copy(alpha = 0.65f)),
        )
        BuscaCorpo(
            carregando = carregando,
            erro = erro,
            buscou = buscou,
            resultados = resultados,
            onTentarDeNovo = { disparar() },
            onIrPara = onIrPara,
        )
    }
}

@Composable
private fun BuscaBarra(
    termo: String,
    foco: FocusRequester,
    onTermo: (String) -> Unit,
    onLimpar: () -> Unit,
    onBuscar: () -> Unit,
    onFechar: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = OrbitMetrics.pagePadding)
            .padding(top = 8.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(OrbitMetrics.iconBtn)
                .clip(CircleShape)
                .background(OrbitTokens.surface)
                .border(1.dp, OrbitTokens.borderSoft, CircleShape)
                .orbitPressable(onClick = onFechar),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.ArrowBackIosNew,
                contentDescription = "Fechar busca",
                tint = OrbitTokens.textHigh,
                modifier = Modifier.size(18.dp),
            )
        }

        Spacer(Modifier.width(12.dp))

        Row(
            modifier = Modifier
                .weight(1f)
                .height(OrbitMetrics.iconBtn)
                .clip(RoundedCornerShape(OrbitMetrics.radiusPill))
                .background(OrbitTokens.surface)
                .border(1.dp, OrbitTokens.borderSoft, RoundedCornerShape(OrbitMetrics.radiusPill))
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Rounded.Search,
                contentDescription = null,
                tint = OrbitTokens.textLow,
                modifier = Modifier.size(19.dp),
            )
            Spacer(Modifier.width(9.dp))
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (termo.isEmpty()) {
                    Text(
                        "Buscar com a Luna…",
                        color = OrbitTokens.textLow,
                        fontSize = 15.sp,
                    )
                }
                BasicTextField(
                    value = termo,
                    onValueChange = onTermo,
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = OrbitTokens.textHigh,
                        fontSize = 15.sp,
                    ),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(OrbitTokens.accent),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onBuscar() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(foco),
                )
            }
            if (termo.isNotEmpty()) {
                Spacer(Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .orbitPressable(onClick = onLimpar),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = "Limpar",
                        tint = OrbitTokens.textLow,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun BuscaCorpo(
    carregando: Boolean,
    erro: String?,
    buscou: Boolean,
    resultados: List<LunaApiClient.BuscaItem>,
    onTentarDeNovo: () -> Unit,
    onIrPara: (String) -> Unit,
) {
    when {
        carregando -> BuscaAviso(
            icone = { CircularProgressIndicator(color = OrbitTokens.accent, strokeWidth = 2.5.dp, modifier = Modifier.size(28.dp)) },
            titulo = "Procurando pelo significado…",
            subtitulo = "A Luna está lendo a conversa.",
        )
        erro != null -> BuscaAviso(
            titulo = "Não consegui buscar",
            subtitulo = erro,
            acao = "Tentar de novo" to onTentarDeNovo,
        )
        !buscou -> BuscaAviso(
            iconeVetor = Icons.Rounded.AutoAwesome,
            titulo = "Busque pelo que quis dizer",
            subtitulo = "A Luna entende o sentido, não só a palavra exata. Procure «orçamento» e ela acha onde vocês falaram de quanto dava pra gastar.",
        )
        resultados.isEmpty() -> BuscaAviso(
            iconeVetor = Icons.Rounded.Search,
            titulo = "Não achei nada sobre isso",
            subtitulo = "Tenta com outras palavras — ou fala do jeito que você lembra do assunto.",
        )
        else -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = OrbitMetrics.pagePadding,
                end = OrbitMetrics.pagePadding,
                top = 14.dp,
                bottom = 28.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Text(
                    "${resultados.size} ${if (resultados.size == 1) "ponto" else "pontos"} na conversa",
                    color = OrbitTokens.textLow,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 2.dp, start = 2.dp),
                )
            }
            items(resultados, key = { it.messageId + it.trecho.take(12) }) { r ->
                BuscaResultadoCard(item = r, onClick = { onIrPara(r.messageId) })
            }
        }
    }
}

@Composable
private fun BuscaResultadoCard(item: LunaApiClient.BuscaItem, onClick: () -> Unit) {
    val ehLuna = item.papel.equals("luna", ignoreCase = true) ||
        item.papel.equals("assistant", ignoreCase = true)
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(OrbitTokens.surface)
            .border(1.dp, OrbitTokens.borderSoft, shape)
            .orbitPressable(onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            if (ehLuna) "Luna" else "Você",
            color = if (ehLuna) OrbitTokens.accentText else OrbitTokens.textMid,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            "“${item.trecho}”",
            color = OrbitTokens.textHigh,
            fontSize = 14.sp,
            fontStyle = FontStyle.Italic,
            lineHeight = 20.sp,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Estado central (dica / carregando / erro / vazio) — ícone + título + subtítulo + ação. */
@Composable
private fun BuscaAviso(
    titulo: String,
    subtitulo: String,
    iconeVetor: androidx.compose.ui.graphics.vector.ImageVector? = null,
    icone: (@Composable () -> Unit)? = null,
    acao: Pair<String, () -> Unit>? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        when {
            icone != null -> icone()
            iconeVetor != null -> Icon(
                iconeVetor,
                contentDescription = null,
                tint = OrbitTokens.accentText,
                modifier = Modifier.size(30.dp),
            )
        }
        Spacer(Modifier.height(14.dp))
        Text(
            titulo,
            color = OrbitTokens.textHigh,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            subtitulo,
            color = OrbitTokens.textMid,
            fontSize = 13.sp,
            lineHeight = 19.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        if (acao != null) {
            Spacer(Modifier.height(18.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(OrbitMetrics.radiusPill))
                    .background(OrbitTokens.surfaceRaised)
                    .border(1.dp, OrbitTokens.borderSoft, RoundedCornerShape(OrbitMetrics.radiusPill))
                    .orbitPressable(onClick = acao.second)
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    acao.first,
                    color = OrbitTokens.accentText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}
