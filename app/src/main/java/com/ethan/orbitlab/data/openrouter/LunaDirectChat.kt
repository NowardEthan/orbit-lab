package com.ethan.orbitlab.data.openrouter

import android.content.Context
import com.ethan.orbitlab.data.Mensagem
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
        // Histórico curto (últimas 12 mensagens antes do turno actual)
        historico.takeLast(12).forEach { msg ->
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
        onEstado(
            LunaStreamEstado.Raciocinando(
                parcial = "",
                actionRun = actionRun,
            ),
        )

        val resposta = runCatching {
            OpenRouterClient.chat(apiMessages)
        }.getOrElse { e ->
            "Não consegui responder: ${e.message ?: "desconhecido"}"
        }

        val dur = ((System.currentTimeMillis() - t0) / 1000.0).roundToInt().coerceAtLeast(1)

        return LunaStreamResultado(
            reasoning = if (wireTools.isNotEmpty()) {
                "Olhei os anexos e respondi em cima disso."
            } else {
                ""
            },
            reasoningDuracao = "${dur}s",
            resposta = resposta.ifBlank { "…" },
            actionRun = actionRun,
        )
    }
}
