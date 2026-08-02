package com.ethan.orbitlab.ui.financas

import android.app.Application
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Nightlight
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethan.orbitlab.data.ChatRepository
import com.ethan.orbitlab.data.Mensagem
import com.ethan.orbitlab.data.lunaMessageIdForUser
import com.ethan.orbitlab.data.lunaapi.LunaApiChat
import com.ethan.orbitlab.data.newUserMessageId
import com.ethan.orbitlab.ui.chat.ChatInputArea
import com.ethan.orbitlab.ui.chat.ComposerAttachment
import com.ethan.orbitlab.ui.chat.LunaMarkdown
import com.ethan.orbitlab.ui.chat.LunaMarkdownVariante
import com.ethan.orbitlab.ui.chat.LunaStreamEstado
import com.ethan.orbitlab.ui.chat.ThreadReference
import com.ethan.orbitlab.ui.theme.Bricolage
import com.ethan.orbitlab.ui.theme.OrbitTokens
import com.ethan.orbitlab.ui.theme.orbitPressable
import kotlin.coroutines.cancellation.CancellationException

/**
 * Widget flutuante interativo da Luna para Finanças.
 * Emerge diretamente sobre o FAB circular com animação de escala e transparência.
 */
@Composable
fun FinancasLunaWidget(
    conversaId: String,
    visivel: Boolean,
    onFechar: () -> Unit,
    onAbrirFullscreen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val appContext = LocalContext.current.applicationContext as Application
    val keyboardController = LocalSoftwareKeyboardController.current
    val conversa by ChatRepository.observarConversa(conversaId).collectAsState(initial = null)
    val mensagens = conversa?.mensagens.orEmpty()

    var streamState by remember { mutableStateOf<LunaStreamEstado>(LunaStreamEstado.Idle) }
    var inputTexto by remember { mutableStateOf("") }
    val ocioso = streamState is LunaStreamEstado.Idle

    val ultimasLunaMsg = remember(mensagens) {
        mensagens.lastOrNull { it.isLuna && it.texto.isNotBlank() }
    }

    fun dispararResposta(
        textoEnvio: String,
        anexos: List<ComposerAttachment>,
        reference: ThreadReference?,
        historicoAntes: List<Mensagem>,
        userMsgId: String,
        lunaMsgId: String,
    ) {
        streamState = LunaStreamEstado.Raciocinando("")
        ChatRepository.launch {
            try {
                val resultado = LunaApiChat.responder(
                    context = appContext,
                    conversaId = conversaId,
                    historico = historicoAntes,
                    textoUsuario = textoEnvio,
                    anexos = anexos,
                    reference = reference,
                    userMessageId = userMsgId,
                    lunaMessageId = lunaMsgId,
                    onEstado = { streamState = it },
                )
                ChatRepository.enviarMensagem(
                    conversaId = conversaId,
                    texto = resultado.resposta,
                    isLuna = true,
                    reasoning = resultado.reasoning.takeIf { it.isNotBlank() && !resultado.erro },
                    reasoningDuracao = resultado.reasoningDuracao
                        .takeIf { it.isNotBlank() && !resultado.erro },
                    actionRun = if (resultado.erro) null else resultado.actionRun,
                    messageId = lunaMsgId,
                    persistirNuvem = !resultado.erro,
                    erro = resultado.erro,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                ChatRepository.enviarMensagem(
                    conversaId = conversaId,
                    texto = "Erro no servidor Luna: ${e.message ?: e.javaClass.simpleName}",
                    isLuna = true,
                    messageId = lunaMsgId,
                    persistirNuvem = false,
                    erro = true,
                )
            } finally {
                streamState = LunaStreamEstado.Idle
            }
        }
    }

    fun enviar(
        texto: String,
        anexos: List<ComposerAttachment> = emptyList(),
        reference: ThreadReference? = null,
    ) {
        val textoEnvio = texto.trim()
        if (textoEnvio.isEmpty() && anexos.isEmpty() && reference == null) return
        if (!ocioso) return
        val historicoAntes = ChatRepository.getConversa(conversaId)?.mensagens.orEmpty()
        val userMsgId = newUserMessageId()
        val lunaMsgId = lunaMessageIdForUser(userMsgId)
        ChatRepository.enviarMensagem(
            conversaId = conversaId,
            texto = textoEnvio,
            isLuna = false,
            messageId = userMsgId,
            attachments = anexos,
            reference = reference,
        )
        dispararResposta(textoEnvio, anexos, reference, historicoAntes, userMsgId, lunaMsgId)
    }

    val sugPrompts = listOf(
        "💰 Quanto gastei este mês?",
        "📊 Resumo dos cartões",
        "✍️ Registrar gasto de R$ 50",
        "🔄 Próximas recorrentes",
    )

    AnimatedVisibility(
        visible = visivel,
        enter = scaleIn(
            animationSpec = tween(280),
            transformOrigin = TransformOrigin(0.05f, 0.95f),
        ) + fadeIn(tween(200)),
        exit = scaleOut(
            animationSpec = tween(200),
            transformOrigin = TransformOrigin(0.05f, 0.95f),
        ) + fadeOut(tween(160)),
        modifier = modifier,
    ) {
        val shapeCard = RoundedCornerShape(22.dp)
        val bordaCard = Brush.verticalGradient(
            listOf(
                OrbitTokens.bluePastel.copy(alpha = 0.35f),
                OrbitTokens.graphiteHair,
            ),
        )

        Column(
            modifier = Modifier
                .widthIn(max = 340.dp)
                .navigationBarsPadding()
                .imePadding()
                .shadow(
                    elevation = 16.dp,
                    shape = shapeCard,
                    ambientColor = OrbitTokens.bluePastel.copy(alpha = 0.30f),
                    spotColor = Color.Black.copy(alpha = 0.50f),
                )
                .clip(shapeCard)
                .background(OrbitTokens.graphiteSurf)
                .border(BorderStroke(1.dp, bordaCard), shapeCard)
                .padding(14.dp),
        ) {
            // ── Cabeçalho do Widget ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(OrbitTokens.bluePastel.copy(alpha = 0.18f))
                        .border(1.dp, OrbitTokens.bluePastel.copy(alpha = 0.30f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Nightlight,
                        contentDescription = null,
                        tint = OrbitTokens.bluePastel,
                        modifier = Modifier.size(15.dp),
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Luna",
                    color = OrbitTokens.textHiN,
                    fontSize = 15.sp,
                    fontFamily = Bricolage,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.2).sp,
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = OrbitTokens.bluePastel,
                    modifier = Modifier.size(12.dp),
                )
                Spacer(Modifier.weight(1f))

                // Botão de expandir para tela cheia
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(OrbitTokens.graphiteRaised)
                        .border(1.dp, OrbitTokens.graphiteHair, CircleShape)
                        .orbitPressable(onClick = onAbrirFullscreen),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.OpenInFull,
                        contentDescription = "Abrir no chat",
                        tint = OrbitTokens.textMidN,
                        modifier = Modifier.size(13.dp),
                    )
                }
                Spacer(Modifier.width(6.dp))

                // Botão de fechar widget
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(OrbitTokens.graphiteRaised)
                        .border(1.dp, OrbitTokens.graphiteHair, CircleShape)
                        .orbitPressable(onClick = onFechar),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Fechar",
                        tint = OrbitTokens.textMidN,
                        modifier = Modifier.size(15.dp),
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // ── Sugestões Rápidas (Chips Roláveis) ──
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(horizontal = 2.dp),
            ) {
                items(sugPrompts) { prompt ->
                    val shapeChip = RoundedCornerShape(99.dp)
                    Box(
                        modifier = Modifier
                            .clip(shapeChip)
                            .background(OrbitTokens.graphiteRaised)
                            .border(1.dp, OrbitTokens.graphiteHair, shapeChip)
                            .orbitPressable { enviar(prompt) }
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                    ) {
                        Text(
                            text = prompt,
                            color = OrbitTokens.textHiN,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // ── Área de Resposta da Luna / Streaming ──
            val stream = streamState
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 60.dp, max = 160.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(OrbitTokens.graphiteRaised)
                    .border(1.dp, OrbitTokens.graphiteHair, RoundedCornerShape(14.dp))
                    .padding(10.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                ) {
                    when {
                        stream is LunaStreamEstado.Respondendo -> {
                            Text(
                                text = "Luna respondendo…",
                                color = OrbitTokens.bluePastel,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Spacer(Modifier.height(4.dp))
                            LunaMarkdown(
                                content = stream.respostaParcial,
                                variante = LunaMarkdownVariante.Chat,
                            )
                        }
                        stream is LunaStreamEstado.Raciocinando -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Rounded.AutoAwesome,
                                    contentDescription = null,
                                    tint = OrbitTokens.bluePastel,
                                    modifier = Modifier.size(13.dp),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = if (stream.parcial.isNotBlank()) {
                                        stream.parcial.takeLast(60)
                                    } else {
                                        "Analisando suas finanças…"
                                    },
                                    color = OrbitTokens.bluePastel,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }
                        ultimasLunaMsg != null -> {
                            LunaMarkdown(
                                content = ultimasLunaMsg.texto,
                                variante = LunaMarkdownVariante.Chat,
                            )
                        }
                        else -> {
                            Text(
                                text = "Como posso te ajudar com suas finanças hoje? Digite ou escolha um atalho acima.",
                                color = OrbitTokens.textLowN,
                                fontSize = 12.sp,
                                lineHeight = 17.sp,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // ── Composer Oficial do Orbit (Áudio, Anexos, Câmera, Modos e Referências) ──
            val streamStateState = remember { mutableStateOf(streamState) }
            LaunchedEffect(streamState) {
                streamStateState.value = streamState
            }

            ChatInputArea(
                onSend = { texto, anexos, ref ->
                    enviar(texto, anexos, ref)
                },
                streamState = streamStateState,
                containerColor = OrbitTokens.graphiteRaised,
                exibirSeletorModo = false,
            )
        }
    }
}
