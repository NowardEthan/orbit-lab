package com.ethan.orbitlab.ui.chat

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethan.orbitlab.R
import com.ethan.orbitlab.data.ChatRepository
import com.ethan.orbitlab.data.Mensagem
import com.ethan.orbitlab.ui.theme.OrbitTokens
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

enum class RecordState { Idle, Recording, Locked }

@Composable
fun ChatScreen(conversaId: String, onBack: () -> Unit) {
    val conversas by ChatRepository.conversas.collectAsState()
    val conversa = conversas.find { it.id == conversaId }
    val coroutineScope = rememberCoroutineScope()

    // Fallback caso a conversa suma
    if (conversa == null) {
        onBack()
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OrbitTokens.ink1)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        ChatHeader(onBack, conversa.titulo)

        // Área das mensagens
        Box(modifier = Modifier.weight(1f)) {
            ChatTimeline(conversa.mensagens)
        }

        ChatInputArea(
            onSend = { texto ->
                // Salva mensagem do usuário
                ChatRepository.enviarMensagem(conversaId, texto, isLuna = false)

                // Simula resposta da Luna
                coroutineScope.launch {
                    delay(1200) // 1.2s de "pensamento"
                    val resposta = "Que interessante! Analisando o que você disse sobre '$texto', me parece uma ótima linha de raciocínio. Quer se aprofundar mais nisso?"
                    ChatRepository.enviarMensagem(conversaId, resposta, isLuna = true)
                }
            }
        )
    }
}

@Composable
private fun ChatHeader(onBack: () -> Unit, titulo: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(OrbitTokens.surfaceRaised.copy(alpha = 0.5f)) // Fundo bem sutil
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .clickable { onBack() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.ArrowBackIosNew, contentDescription = "Voltar", tint = OrbitTokens.textHigh, modifier = Modifier.size(20.dp))
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Avatar da Luna (Imagem enviada pelo usuário)
        Image(
            painter = painterResource(id = R.drawable.luna_avatar),
            contentDescription = "Avatar Luna",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(if (titulo == "Nova Conversa") "Luna" else titulo, color = OrbitTokens.textHigh, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Text("Sempre aqui", color = OrbitTokens.textLow, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun ChatTimeline(historico: List<Mensagem>) {
    val listState = rememberLazyListState()

    // Rola para o final quando houver nova mensagem
    LaunchedEffect(historico.size) {
        if (historico.isNotEmpty()) {
            listState.animateScrollToItem(historico.size - 1)
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        items(historico) { msg ->
            MessageBubble(msg)
        }
    }
}

@Composable
private fun MessageBubble(msg: Mensagem) {
    val arranjo = if (msg.isLuna) Arrangement.Start else Arrangement.End

    val backgroundColor = if (msg.isLuna) OrbitTokens.surface else OrbitTokens.accent
    val textColor = if (msg.isLuna) OrbitTokens.textHigh else Color.White

    val shape = if (msg.isLuna) {
        RoundedCornerShape(topStart = 4.dp, topEnd = 24.dp, bottomEnd = 24.dp, bottomStart = 24.dp)
    } else {
        RoundedCornerShape(topStart = 24.dp, topEnd = 4.dp, bottomEnd = 24.dp, bottomStart = 24.dp)
    }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = arranjo) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(shape)
                .background(backgroundColor)
                .padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            Text(
                text = msg.texto,
                color = textColor,
                fontSize = 15.sp,
                lineHeight = 22.sp
            )
        }
    }
}

/** mm:ss a partir de um total de segundos. */
private fun formatTimer(totalSegundos: Int): String {
    val m = totalSegundos / 60
    val s = totalSegundos % 60
    return "%d:%02d".format(m, s)
}

@Composable
private fun ChatInputArea(onSend: (String) -> Unit) {
    var texto by remember { mutableStateOf("") }
    var recordState by remember { mutableStateOf(RecordState.Idle) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var segundos by remember { mutableStateOf(0) }

    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    // Quanto o dedo precisa arrastar pra disparar cada gesto (convertido de dp pra px real da tela).
    val cancelThresholdPx = with(density) { 120.dp.toPx() }
    val lockThresholdPx = with(density) { 90.dp.toPx() }

    // Cronômetro: conta 1 em 1 segundo enquanto grava (Recording OU Locked).
    LaunchedEffect(recordState) {
        if (recordState == RecordState.Recording || recordState == RecordState.Locked) {
            while (true) {
                delay(1000)
                segundos += 1
            }
        }
    }

    // Progresso 0→1 de cada gesto (só faz sentido enquanto o dedo está segurando: Recording).
    val cancelProgresso = if (recordState == RecordState.Recording)
        (-dragOffset.x / cancelThresholdPx).coerceIn(0f, 1f) else 0f
    val lockProgresso = if (recordState == RecordState.Recording)
        (-dragOffset.y / lockThresholdPx).coerceIn(0f, 1f) else 0f

    fun resetar() {
        recordState = RecordState.Idle
        dragOffset = Offset.Zero
        segundos = 0
    }

    fun enviarAudio() {
        onSend("Áudio (${formatTimer(segundos)})")
        resetar()
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(OrbitTokens.ink1)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "rec")
        // Ponto vermelho pulsando (usado nos dois estados de gravação).
        val dotAlpha by infiniteTransition.animateFloat(
            initialValue = 1f, targetValue = 0.2f,
            animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
            label = "dot"
        )

        // ÁREA ESQUERDA — texto (idle) OU barra de gravação.
        Box(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(OrbitTokens.surface)
                .padding(horizontal = 18.dp, vertical = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            when (recordState) {
                RecordState.Idle -> {
                    BasicTextField(
                        value = texto,
                        onValueChange = { texto = it },
                        textStyle = TextStyle(color = OrbitTokens.textHigh, fontSize = 15.sp),
                        modifier = Modifier.fillMaxWidth(),
                        cursorBrush = SolidColor(Color.White),
                        decorationBox = { inner ->
                            if (texto.isEmpty()) Text("Mensagem...", color = OrbitTokens.textLow, fontSize = 15.sp)
                            inner()
                        }
                    )
                }
                RecordState.Recording -> {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(9.dp).clip(CircleShape).background(OrbitTokens.danger.copy(alpha = dotAlpha)))
                        Spacer(Modifier.width(10.dp))
                        Text(formatTimer(segundos), color = OrbitTokens.textHigh, fontSize = 15.sp)
                        Spacer(Modifier.weight(1f))
                        // A dica segue o dedo um pouquinho e some conforme você chega perto do cancelar.
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .offset { IntOffset(dragOffset.x.coerceIn(-48f, 0f).roundToInt(), 0) }
                                .alpha(1f - cancelProgresso)
                        ) {
                            Icon(Icons.Rounded.ArrowBackIosNew, null, tint = OrbitTokens.textLow, modifier = Modifier.size(11.dp))
                            Spacer(Modifier.width(5.dp))
                            Text("arraste para cancelar", color = OrbitTokens.textLow, fontSize = 13.sp, maxLines = 1)
                        }
                    }
                }
                RecordState.Locked -> {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        // Lixeira: toque pra descartar.
                        Icon(
                            Icons.Rounded.Delete, contentDescription = "Descartar",
                            tint = OrbitTokens.danger,
                            modifier = Modifier.size(22.dp).clickable { resetar() }
                        )
                        Spacer(Modifier.width(16.dp))
                        Box(Modifier.size(9.dp).clip(CircleShape).background(OrbitTokens.danger.copy(alpha = dotAlpha)))
                        Spacer(Modifier.width(10.dp))
                        Text(formatTimer(segundos), color = OrbitTokens.textHigh, fontSize = 15.sp)
                        Spacer(Modifier.weight(1f))
                        // Cadeadinho fechado: sinaliza que travou (mãos livres).
                        Icon(Icons.Rounded.Lock, null, tint = OrbitTokens.accentText, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        Spacer(Modifier.width(12.dp))

        // ---- BOTÃO DA DIREITA ----
        val isTyping = texto.isNotBlank()
        val interactionSource = remember { MutableInteractionSource() }

        // Cresce + pulsa enquanto o dedo está gravando.
        val pulso by infiniteTransition.animateFloat(
            initialValue = 1f, targetValue = 1.12f,
            animationSpec = infiniteRepeatable(tween(700, easing = LinearOutSlowInEasing), RepeatMode.Reverse),
            label = "pulso"
        )
        val escalaBase by animateFloatAsState(
            if (recordState == RecordState.Recording) 1.3f else 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 300f),
            label = "escala"
        )
        val escalaFinal = escalaBase * if (recordState == RecordState.Recording) pulso else 1f

        val corBotao = if (recordState != RecordState.Idle) OrbitTokens.danger else OrbitTokens.accent
        val mostrarSend = isTyping || recordState == RecordState.Locked

        // O gesto de segurar-pra-gravar SÓ existe quando não estou digitando e não travei.
        val holdModifier = if (!isTyping && recordState != RecordState.Locked) {
            Modifier.pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    // Começa a gravar (vibra pra confirmar).
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    recordState = RecordState.Recording
                    dragOffset = Offset.Zero
                    segundos = 0
                    var finalizado = false
                    while (!finalizado) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break
                        if (change.pressed) {
                            dragOffset += change.positionChange()
                            when {
                                -dragOffset.x >= cancelThresholdPx -> {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    resetar()
                                    finalizado = true
                                }
                                -dragOffset.y >= lockThresholdPx -> {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    recordState = RecordState.Locked
                                    dragOffset = Offset.Zero
                                    finalizado = true
                                }
                            }
                        } else {
                            // Soltou o dedo sem travar nem cancelar → envia (se durou o suficiente).
                            if (segundos >= 1) enviarAudio() else resetar()
                            finalizado = true
                        }
                    }
                }
            }
        } else {
            Modifier.clickable(interactionSource = interactionSource, indication = null) {
                if (isTyping) {
                    onSend(texto)
                    texto = ""
                } else {
                    // Travado: o botão vira "enviar".
                    enviarAudio()
                }
            }
        }

        Box(
            modifier = Modifier.size(48.dp),
            contentAlignment = Alignment.Center
        ) {
            // Cadeado flutuante acima (só enquanto o dedo segura): enche conforme você sobe.
            if (recordState == RecordState.Recording) {
                Box(
                    modifier = Modifier
                        .offset(y = (-74 - lockProgresso * 10).dp)
                        .size(width = 40.dp, height = 58.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(OrbitTokens.surfaceRaised.copy(alpha = 0.9f))
                        .border(1.dp, OrbitTokens.borderSoft, RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Icon(
                            Icons.Rounded.Lock, contentDescription = null,
                            // Vai acendendo (cinza → accent) conforme lockProgresso sobe.
                            tint = androidx.compose.ui.graphics.lerp(OrbitTokens.textLow, OrbitTokens.accentText, lockProgresso),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.height(2.dp))
                        Icon(
                            Icons.Rounded.KeyboardArrowUp, contentDescription = null,
                            tint = OrbitTokens.textLow.copy(alpha = 1f - lockProgresso),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            // O botão em si — segue o dedo (offset) enquanto grava, escala com pulso.
            val offsetBotao = if (recordState == RecordState.Recording) {
                IntOffset(
                    dragOffset.x.coerceIn(-cancelThresholdPx, 0f).roundToInt(),
                    dragOffset.y.coerceIn(-lockThresholdPx, 0f).roundToInt()
                )
            } else IntOffset.Zero

            Box(
                modifier = Modifier
                    .offset { offsetBotao }
                    .scale(escalaFinal)
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(corBotao)
                    .then(holdModifier),
                contentAlignment = Alignment.Center
            ) {
                if (mostrarSend) {
                    Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Enviar", tint = Color.White, modifier = Modifier.size(22.dp).offset(x = 2.dp))
                } else {
                    Icon(Icons.Rounded.Mic, contentDescription = "Falar", tint = Color.White, modifier = Modifier.size(26.dp))
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0E1014, widthDp = 380, heightDp = 800)
@Composable
fun ChatScreenPreview() {
    ChatScreen(conversaId = "mock", onBack = {})
}
