package com.ethan.orbitlab.data.openrouter

import android.content.Context
import com.ethan.orbitlab.data.Mensagem
import com.ethan.orbitlab.data.latencia.LatenciaProbe
import com.ethan.orbitlab.ui.chat.AttachmentKind
import com.ethan.orbitlab.ui.chat.ComposerAttachment
import com.ethan.orbitlab.ui.chat.LunaActionRun
import com.ethan.orbitlab.ui.chat.LunaActionRunStatus
import com.ethan.orbitlab.ui.chat.LunaActionStep
import com.ethan.orbitlab.ui.chat.LunaActionStepStatus
import com.ethan.orbitlab.ui.chat.LunaStreamEstado
import com.ethan.orbitlab.ui.chat.LunaStreamResultado
import com.ethan.orbitlab.ui.chat.ThreadReference
import com.ethan.orbitlab.ui.chat.WireToolStep
import com.ethan.orbitlab.ui.chat.buildActionRunFromWire
import com.ethan.orbitlab.ui.chat.formatMessageWithReference
import com.ethan.orbitlab.ui.chat.toolMeta
import kotlin.math.roundToInt

/** Mensagem amigável — não joga "software caused connection abort" cru na cara do usuário. */
private fun mensagemErro(erro: String): String =
    if (erroTransitorio(erro)) {
        "A conexão caiu no meio da resposta. Toca em «Tentar de novo» — se insistir, é a rede oscilando."
    } else {
        "Não consegui responder: $erro"
    }

/** Falha transitória (rede/DNS/timeout/5xx) que vale re-tentar; 4xx (auth/limite) não. */
private fun erroTransitorio(msg: String?): Boolean {
    if (msg == null) return true
    val m = msg.lowercase()
    return listOf(
        "unable to resolve host", "failed to connect", "timeout", "timed out",
        "econnreset", "connection reset", "connection closed", "connection abort",
        "unexpected end", "network", "http 5", " 500", " 502", " 503", " 504",
    ).any { m.contains(it) }
}

/**
 * Resposta da Luna no lab — OpenRouter direto (sem agentico / luna-core).
 *
 * - Texto → `deepseek/deepseek-v4-flash` (stream)
 * - Imagem → modelo de visão; depois flash com o laudo
 * - Vídeo → modelo de vídeo; depois flash
 */
object LunaDirectChat {

    suspend fun responder(
        context: Context,
        historico: List<Mensagem>,
        textoUsuario: String,
        anexos: List<ComposerAttachment>,
        reference: ThreadReference?,
        onEstado: (LunaStreamEstado) -> Unit,
    ): LunaStreamResultado {
        if (!OpenRouterConfig.isConfigured()) {
            return LunaStreamResultado(
                reasoning = "",
                reasoningDuracao = "",
                resposta = "Falta a chave OpenRouter. Coloca `OPENROUTER_API_KEY` no " +
                    "`core/src/luna-core/.env` (ou `openrouter.api.key` no local.properties) e recompila.",
                erro = true,
            )
        }

        val pergunta = when {
            reference != null -> formatMessageWithReference(textoUsuario, reference)
            else -> textoUsuario.trim()
        }

        val wireTools = mutableListOf<WireToolStep>()
        val laudos = mutableListOf<String>()
        val midias = anexos.filter {
            it.kind == AttachmentKind.IMAGE || it.kind == AttachmentKind.VIDEO
        }

        if (midias.isNotEmpty()) {
            val stepsLive = midias.mapIndexed { i, att ->
                val ferramenta = if (att.kind == AttachmentKind.VIDEO) "ver_video" else "ver_imagem"
                val meta = toolMeta(ferramenta)
                LunaActionStep(
                    id = "t-$i",
                    label = meta.live(att.name),
                    status = LunaActionStepStatus.RUNNING,
                    kind = meta.kind,
                    ferramenta = ferramenta,
                )
            }
            onEstado(
                LunaStreamEstado.Pesquisando(
                    run = LunaActionRun(
                        id = "tools-live",
                        title = "Ferramentas",
                        status = LunaActionRunStatus.RUNNING,
                        steps = stepsLive,
                        profile = com.ethan.orbitlab.ui.chat.LunaActionProfile.TASK,
                    ),
                    liveLabel = "Olhando anexos",
                ),
            )

            midias.forEach { att ->
                val ehVideo = att.kind == AttachmentKind.VIDEO
                val ferramenta = if (ehVideo) "ver_video" else "ver_imagem"
                val uri = att.uri
                val fonte = when {
                    uri == null -> null
                    uri.scheme.equals("http", true) || uri.scheme.equals("https", true) ->
                        uri.toString()
                    else -> runCatching {
                        OpenRouterClient.uriToDataUri(
                            context,
                            uri,
                            att.mime.ifBlank {
                                if (ehVideo) "video/mp4" else "image/jpeg"
                            },
                        )
                    }.getOrNull()
                }
                if (fonte == null) {
                    laudos += "(Não consegui ler «${att.name}».)"
                    wireTools += WireToolStep(ferramenta, att.name, sucesso = false)
                    return@forEach
                }
                val laudo = runCatching {
                    OpenRouterClient.analisarMidia(
                        midiaUrl = fonte,
                        mime = att.mime,
                        pergunta = pergunta.takeIf { it.isNotBlank() },
                        ehVideo = ehVideo,
                    )
                }.getOrElse { e ->
                    wireTools += WireToolStep(ferramenta, att.name, sucesso = false)
                    laudos += "Falha ao olhar «${att.name}»: ${e.message}"
                    return@forEach
                }
                wireTools += WireToolStep(ferramenta, att.name, sucesso = true)
                laudos += "Sobre «${att.name}»:\n$laudo"
            }
        }

        val docs = anexos.filter { it.kind == AttachmentKind.FILE }
        if (docs.isNotEmpty()) {
            laudos += "Anexos de arquivo neste turno: " +
                docs.joinToString(", ") { it.name } +
                " (lab direto ainda não extrai texto de documentos)."
        }

        val actionRun = if (wireTools.isNotEmpty()) {
            buildActionRunFromWire(
                id = "tools-done",
                title = "Ferramentas",
                wireSteps = wireTools,
                status = LunaActionRunStatus.DONE,
            )
        } else {
            null
        }

        val userContent = buildString {
            if (laudos.isNotEmpty()) {
                appendLine("[Contexto visual — o que a Luna viu]")
                laudos.forEach { appendLine(it); appendLine() }
            }
            if (pergunta.isNotBlank()) {
                append(pergunta)
            } else if (laudos.isNotEmpty()) {
                append("Comenta o que você viu nos anexos.")
            } else {
                append("Oi")
            }
        }

        val apiMessages = mutableListOf(
            ChatMessage(role = "system", content = OpenRouterConfig.systemPrompt),
        )
        // Histórico curto (últimas 12 mensagens antes do turno actual). Balões de erro são
        // avisos locais, não fala real da Luna — ficam de fora pra não confundi-la.
        historico.filterNot { it.erro }.takeLast(12).forEach { msg ->
            apiMessages += ChatMessage(
                role = if (msg.isLuna) "assistant" else "user",
                content = msg.texto.ifBlank {
                    if (msg.attachments.isNotEmpty()) {
                        msg.attachments.joinToString(", ") { it.name }
                    } else {
                        "…"
                    }
                },
            )
        }
        apiMessages += ChatMessage(role = "user", content = userContent)

        val t0 = System.currentTimeMillis()
        onEstado(LunaStreamEstado.Raciocinando(parcial = "", actionRun = actionRun))

        // STREAM de verdade: pinta o texto conforme chega (caret animado) em vez de esperar
        // a resposta inteira bloqueada. Mata o "Pensando…" estático e o timeout percebido.
        val respostaBuf = StringBuilder()
        val reasoningBuf = StringBuilder()
        var ttfbMs: Long? = null
        var ok = true
        var erro: String? = null

        fun durLabel(): String =
            "${((System.currentTimeMillis() - t0) / 1000.0).roundToInt().coerceAtLeast(1)}s"

        val maxTentativas = 3
        var tentativa = 0
        while (true) {
            tentativa++
            // Só repetimos se NADA foi transmitido ainda → limpar o buffer é seguro.
            respostaBuf.setLength(0)
            reasoningBuf.setLength(0)
            ttfbMs = null
            ok = true
            erro = null
            try {
                OpenRouterClient.streamChat(apiMessages).collect { ev ->
                    when (ev) {
                        is OpenRouterClient.StreamEvent.Delta -> {
                            if (ttfbMs == null) ttfbMs = System.currentTimeMillis() - t0
                            respostaBuf.append(ev.text)
                            onEstado(
                                LunaStreamEstado.Respondendo(
                                    reasoning = reasoningBuf.toString(),
                                    reasoningDuracao = durLabel(),
                                    respostaParcial = respostaBuf.toString(),
                                    actionRun = actionRun,
                                ),
                            )
                        }
                        is OpenRouterClient.StreamEvent.Reasoning -> {
                            reasoningBuf.append(ev.text)
                            if (respostaBuf.isEmpty()) {
                                onEstado(
                                    LunaStreamEstado.Raciocinando(
                                        parcial = reasoningBuf.toString(),
                                        actionRun = actionRun,
                                    ),
                                )
                            }
                        }
                        is OpenRouterClient.StreamEvent.Done -> {
                            if (respostaBuf.isEmpty() && ev.fullText.isNotBlank()) {
                                respostaBuf.append(ev.fullText)
                            }
                        }
                        is OpenRouterClient.StreamEvent.Error -> {
                            ok = false
                            erro = ev.message
                        }
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                ok = false
                erro = e.message
            }

            // Retry só em falha de REDE quando nada apareceu (DNS/timeout/5xx). Nunca
            // re-tenta se já streamou texto (senão duplicaria) nem em 4xx (auth/limite).
            val podeRepetir = !ok && respostaBuf.isEmpty() && erroTransitorio(erro)
            if (podeRepetir && tentativa < maxTentativas) {
                // Conexão abortada costuma ser um socket morto no pool — descarta antes de repetir.
                OpenRouterClient.evictConnections()
                onEstado(LunaStreamEstado.Raciocinando(parcial = "", actionRun = actionRun))
                kotlinx.coroutines.delay(tentativa * 1200L)
                continue
            }
            break
        }

        val totalMs = System.currentTimeMillis() - t0
        val dur = (totalMs / 1000.0).roundToInt().coerceAtLeast(1)
        val texto = when {
            respostaBuf.isNotBlank() -> respostaBuf.toString()
            erro != null -> mensagemErro(erro)
            else -> "…"
        }
        LatenciaProbe.record(
            caminho = "openrouter_direct",
            totalMs = totalMs,
            ttfbMs = ttfbMs ?: totalMs,
            chars = texto.length,
            ok = ok,
            detalhe = if (wireTools.isNotEmpty()) "com_midia" else null,
        )

        return LunaStreamResultado(
            reasoning = reasoningBuf.toString().ifBlank {
                if (wireTools.isNotEmpty()) "Olhei os anexos e respondi em cima disso." else ""
            },
            reasoningDuracao = "${dur}s",
            resposta = texto,
            actionRun = actionRun,
            // Falhou de rede e nada chegou → é aviso de erro, não fala real da Luna.
            erro = respostaBuf.isBlank() && erro != null,
        )
    }
}
