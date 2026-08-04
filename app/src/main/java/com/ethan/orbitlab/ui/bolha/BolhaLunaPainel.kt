package com.ethan.orbitlab.ui.bolha

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Nightlight
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethan.orbitlab.data.ChatRepository
import com.ethan.orbitlab.data.Mensagem
import com.ethan.orbitlab.data.billing.PlanosNav
import com.ethan.orbitlab.data.billing.UsageRepository
import com.ethan.orbitlab.data.lunaMessageIdForUser
import com.ethan.orbitlab.data.lunaapi.LunaApiChat
import com.ethan.orbitlab.data.newUserMessageId
import com.ethan.orbitlab.ui.chat.LunaMarkdown
import com.ethan.orbitlab.ui.chat.LunaMarkdownVariante
import com.ethan.orbitlab.ui.chat.LunaStreamEstado
import com.ethan.orbitlab.ui.planos.LimiteAtingidoCard
import com.ethan.orbitlab.ui.theme.Bricolage
import com.ethan.orbitlab.ui.theme.OrbitTokens
import com.ethan.orbitlab.ui.theme.orbitPressable
import kotlin.coroutines.cancellation.CancellationException

/**
 * O painel de conversa da bolha flutuante — abre por cima de qualquer app.
 *
 * Continua a MESMA conversa principal da Luna ([ChatRepository.conversaPrincipal]); o que você
 * fala aqui aparece no chat quando abrir o Orbit. Composer enxuto (só texto): mic/câmera/anexo
 * dependem de uma Activity, que não existe num overlay de Service — pra isso, abre no app.
 */
@Composable
fun BolhaLunaPainel(
    onFechar: () -> Unit,
    onAbrirNoApp: () -> Unit,
) {
    val appContext = LocalContext.current.applicationContext as Application
    val conversaId = remember { ChatRepository.conversaPrincipal() }
    val conversa by ChatRepository.observarConversa(conversaId).collectAsState(initial = null)
    val mensagens = conversa?.mensagens.orEmpty()

    val streamRepo by ChatRepository.streamDaConversa(conversaId).collectAsState()
    val streamState = remember { mutableStateOf<LunaStreamEstado>(LunaStreamEstado.Idle) }
    LaunchedEffect(streamRepo) { streamState.value = streamRepo }
    var streamLunaMsgId by remember { mutableStateOf(ChatRepository.lunaMsgIdEmVoo(conversaId)) }
    val listState = rememberLazyListState()
    val ocioso = streamState.value is LunaStreamEstado.Idle &&
        !ChatRepository.turnoEmAndamento(conversaId)
    val cotaBloqueada by UsageRepository.bloqueado.collectAsState()
    val usageCota by UsageRepository.usage.collectAsState()
    val semSaldo = !usageCota.loading && !usageCota.ilimitado && !usageCota.temSaldoParaChat
    val paredeCota = cotaBloqueada || semSaldo

    LaunchedEffect(mensagens.size, streamState.value) {
        val ultimo = mensagens.lastIndex
        if (ultimo >= 0) listState.animateScrollToItem(ultimo)
    }

    fun enviar(texto: String) {
        val limpo = texto.trim()
        if (limpo.isEmpty() || !ocioso || paredeCota) return
        val historicoAntes = ChatRepository.getConversa(conversaId)?.mensagens.orEmpty()
        val userMsgId = newUserMessageId()
        val lunaMsgId = lunaMessageIdForUser(userMsgId)
        ChatRepository.enviarMensagem(
            conversaId = conversaId,
            texto = limpo,
            isLuna = false,
            messageId = userMsgId,
        )
        streamLunaMsgId = lunaMsgId
        streamState.value = LunaStreamEstado.Raciocinando("")
        val onEstado: (LunaStreamEstado) -> Unit = { e ->
            ChatRepository.publicarStream(conversaId, e)
            streamState.value = e
        }
        ChatRepository.launchTurno(conversaId, lunaMsgId) {
            try {
                val r = LunaApiChat.responder(
                    context = appContext,
                    conversaId = conversaId,
                    historico = historicoAntes,
                    textoUsuario = limpo,
                    anexos = emptyList(),
                    reference = null,
                    userMessageId = userMsgId,
                    lunaMessageId = lunaMsgId,
                    onEstado = onEstado,
                )
                if (r.cotaEsgotada) {
                    // Parede graciosa — sem balão de erro.
                    return@launchTurno
                }
                ChatRepository.enviarMensagem(
                    conversaId = conversaId,
                    texto = r.resposta,
                    isLuna = true,
                    reasoning = r.reasoning.takeIf { it.isNotBlank() && !r.erro },
                    reasoningDuracao = r.reasoningDuracao.takeIf { it.isNotBlank() && !r.erro },
                    actionRun = if (r.erro) null else r.actionRun,
                    messageId = lunaMsgId,
                    persistirNuvem = !r.erro,
                    erro = r.erro,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                ChatRepository.enviarMensagem(
                    conversaId = conversaId,
                    texto = "Erro ao falar com o servidor Luna: ${e.message ?: e.javaClass.simpleName}",
                    isLuna = true,
                    messageId = lunaMsgId,
                    persistirNuvem = false,
                    erro = true,
                )
            } finally {
                streamState.value = LunaStreamEstado.Idle
                streamLunaMsgId = null
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        // Scrim — toque fora fecha o painel (volta pra bolha).
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .orbitPressable(onClick = onFechar),
        )
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.82f)
                .statusBarsPadding()
                .imePadding()
                .navigationBarsPadding()
                .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                .background(OrbitTokens.graphiteSurf),
        ) {
            CabecalhoPainel(onFechar = onFechar, onAbrirNoApp = onAbrirNoApp)

            Box(Modifier.fillMaxWidth().weight(1f)) {
                if (mensagens.isEmpty() && ocioso) {
                    VazioPainel()
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(
                            mensagens.filter { it.id != streamLunaMsgId },
                            key = { it.id },
                        ) { msg -> BalaoPainel(msg) }
                        if (!ocioso) {
                            item(key = "stream") { StatusStreamPainel(streamState.value) }
                        }
                    }
                }
            }

            if (paredeCota) {
                LimiteAtingidoCard(
                    usage = usageCota,
                    onVerPlanos = {
                        PlanosNav.abrir()
                        onAbrirNoApp()
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                )
            } else {
                ComposerPainel(enabled = ocioso, onEnviar = { enviar(it) })
            }
        }
    }
}

@Composable
private fun CabecalhoPainel(onFechar: () -> Unit, onAbrirNoApp: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 14.dp, end = 8.dp, top = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(OrbitTokens.bluePastel.copy(alpha = 0.22f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.Nightlight,
                contentDescription = null,
                tint = OrbitTokens.bluePastel,
                modifier = Modifier.size(19.dp),
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "Luna",
                color = OrbitTokens.textHiN,
                fontSize = 16.sp,
                fontFamily = Bricolage,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "Flutuando · conversa principal",
                color = OrbitTokens.textLowN,
                fontSize = 11.sp,
            )
        }
        Icon(
            Icons.Rounded.OpenInFull,
            contentDescription = "Abrir no app",
            tint = OrbitTokens.textMidN,
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .orbitPressable(onClick = onAbrirNoApp)
                .padding(8.dp),
        )
        Icon(
            Icons.Rounded.Close,
            contentDescription = "Fechar",
            tint = OrbitTokens.textMidN,
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .orbitPressable(onClick = onFechar)
                .padding(8.dp),
        )
    }
}

@Composable
private fun VazioPainel() {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "Tô aqui 🌙",
            color = OrbitTokens.textHiN,
            fontSize = 18.sp,
            fontFamily = Bricolage,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Fala comigo sem sair do que você tá fazendo. Continua a mesma conversa do app.",
            color = OrbitTokens.textMidN,
            fontSize = 13.sp,
            lineHeight = 18.sp,
        )
    }
}

@Composable
private fun BalaoPainel(msg: Mensagem) {
    val luna = msg.isLuna
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = if (luna) Alignment.Start else Alignment.End,
    ) {
        Box(
            Modifier
                .widthIn(max = 300.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (luna) 4.dp else 16.dp,
                        bottomEnd = if (luna) 16.dp else 4.dp,
                    ),
                )
                .background(if (luna) OrbitTokens.bubbleLuna else OrbitTokens.bubbleUser)
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            if (luna && !msg.erro) {
                LunaMarkdown(content = msg.texto, variante = LunaMarkdownVariante.Chat)
            } else {
                Text(
                    msg.texto,
                    color = if (msg.erro) OrbitTokens.danger else OrbitTokens.textHiN,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                )
            }
        }
    }
}

@Composable
private fun StatusStreamPainel(estado: LunaStreamEstado) {
    val label = when (estado) {
        is LunaStreamEstado.Idle -> return
        is LunaStreamEstado.Pesquisando -> estado.liveLabel
        is LunaStreamEstado.Raciocinando -> estado.fase.ifBlank { "Pensando…" }
        is LunaStreamEstado.Respondendo -> "Escrevendo…"
    }
    val parcial = (estado as? LunaStreamEstado.Respondendo)?.respostaParcial.orEmpty()
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
        Text(
            label,
            color = OrbitTokens.bluePastel,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        if (parcial.isNotBlank()) {
            Box(
                Modifier
                    .widthIn(max = 300.dp)
                    .clip(RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp))
                    .background(OrbitTokens.bubbleLuna)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                LunaMarkdown(content = parcial, variante = LunaMarkdownVariante.Chat)
            }
        }
    }
}

@Composable
private fun ComposerPainel(enabled: Boolean, onEnviar: (String) -> Unit) {
    var texto by remember { mutableStateOf("") }
    val podeEnviar = enabled && texto.isNotBlank()

    fun disparar() {
        if (!podeEnviar) return
        onEnviar(texto)
        texto = ""
    }

    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextField(
            value = texto,
            onValueChange = { texto = it },
            modifier = Modifier.weight(1f),
            placeholder = { Text("Fala comigo…", color = OrbitTokens.textLowN) },
            maxLines = 4,
            shape = RoundedCornerShape(22.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { disparar() }),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = OrbitTokens.graphiteRaised,
                unfocusedContainerColor = OrbitTokens.graphiteRaised,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                cursorColor = OrbitTokens.bluePastel,
                focusedTextColor = OrbitTokens.textHiN,
                unfocusedTextColor = OrbitTokens.textHiN,
            ),
        )
        Spacer(Modifier.width(8.dp))
        Box(
            Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(
                    if (podeEnviar) OrbitTokens.bluePastel
                    else OrbitTokens.graphiteRaised,
                )
                .orbitPressable(onClick = { disparar() }),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.AutoMirrored.Rounded.Send,
                contentDescription = "Enviar",
                tint = if (podeEnviar) OrbitTokens.onBluePastel else OrbitTokens.textLowN,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
