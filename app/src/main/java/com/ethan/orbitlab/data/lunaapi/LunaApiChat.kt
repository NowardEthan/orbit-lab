package com.ethan.orbitlab.data.lunaapi

import android.content.Context
import com.ethan.orbitlab.data.AuthRepository
import com.ethan.orbitlab.data.PrefsRepository
import com.ethan.orbitlab.data.UserProfileRepository
import com.ethan.orbitlab.data.firebase.ChatMediaUpload
import com.ethan.orbitlab.data.latencia.LatenciaProbe
import com.ethan.orbitlab.data.local.LocationRepository
import com.ethan.orbitlab.ui.chat.AttachmentKind
import com.ethan.orbitlab.ui.chat.ComposerAttachment
import com.ethan.orbitlab.ui.chat.LunaActionProfile
import com.ethan.orbitlab.ui.chat.LunaActionRun
import com.ethan.orbitlab.ui.chat.LunaActionRunStatus
import com.ethan.orbitlab.ui.chat.LunaActionStep
import com.ethan.orbitlab.ui.chat.LunaActionStepKind
import com.ethan.orbitlab.ui.chat.LunaActionStepStatus
import com.ethan.orbitlab.ui.chat.LunaFonteStatus
import com.ethan.orbitlab.ui.chat.LunaStreamEstado
import com.ethan.orbitlab.ui.chat.LunaStreamResultado
import com.ethan.orbitlab.ui.chat.LunaWebFonte
import com.ethan.orbitlab.ui.chat.ThreadReference
import com.ethan.orbitlab.ui.chat.ehFerramentaDeWeb
import com.ethan.orbitlab.ui.chat.formalizarTecnico
import com.ethan.orbitlab.ui.chat.formatMessageWithReference
import com.ethan.orbitlab.ui.chat.toolMeta
import android.os.Handler
import android.os.Looper
import org.json.JSONArray
import org.json.JSONObject
import java.util.TimeZone
import kotlin.math.roundToInt

/** Falha transitória (rede/DNS/timeout/conexão abortada/5xx) que vale re-tentar. */
private fun erroTransitorio(msg: String?): Boolean {
    if (msg == null) return true
    val m = msg.lowercase()
    return listOf(
        "unable to resolve host", "failed to connect", "timeout", "timed out",
        "econnreset", "connection reset", "connection closed", "connection abort",
        "unexpected end", "network", "http 5", " 500", " 502", " 503", " 504",
        "stream fechou", "falha de rede",
    ).any { m.contains(it) }
}

/**
 * Chat via mobile-api no Railway — SSE em `/v1/chat/stream`.
 * Mostra fases (analisando / memória / escrevendo) e pinta o texto conforme chega.
 */
object LunaApiChat {

    private fun rotuloFase(phase: String): String = when (phase) {
        "analysing" -> "Analisando…"
        "memory" -> "Memória…"
        "writing" -> "Escrevendo…"
        else -> "Pensando…"
    }

    private fun pareceCodigoOuPayloadTecnico(texto: String): Boolean {
        val t = texto.lowercase().trim()
        return t.startsWith("{") || t.startsWith("[") || t.startsWith("```") ||
            t.contains("import ") || t.contains("export ") || t.contains("function") ||
            t.contains("class ") || t.contains("fun ") || t.contains("diff") ||
            t.contains("<html>") || t.contains("const ") || t.contains("val ") ||
            t.contains("void ") || t.contains("return ") || t.contains("\n")
    }

    private fun rotuloEnvio(anexos: List<ComposerAttachment>): String = when {
        anexos.size > 1 -> "Enviando os anexos…"
        anexos.firstOrNull()?.kind == AttachmentKind.IMAGE -> "Enviando a foto…"
        anexos.firstOrNull()?.kind == AttachmentKind.VIDEO -> "Enviando o vídeo…"
        else -> "Enviando o arquivo…"
    }

    /** Argumento legível de um evento `acao` — o que a pessoa vê no passo (query, título de documento, url…). */
    private fun extrairArgAcao(ferramenta: String, argumentos: JSONObject?): String {
        if (argumentos == null) return ""
        val bruto = when (ferramenta) {
            "web_search" -> argumentos.optString("query")
            "ler_url" -> argumentos.optString("url")
            "consultar_atlas" -> argumentos.optString("termo").ifBlank { argumentos.optString("query") }
            "criar_artefato", "editar_artefato", "editar_trecho_artefato", "ler_artefato", "ler_secao", "buscar_no_artefato" -> {
                val titulo = argumentos.optString("titulo").takeIf { it.isNotBlank() }
                val alteracao = argumentos.optString("alteracao").takeIf { it.isNotBlank() }
                val secao = argumentos.optString("secao").takeIf { it.isNotBlank() }
                val query = argumentos.optString("query").takeIf { it.isNotBlank() }
                when {
                    !titulo.isNullOrBlank() && !alteracao.isNullOrBlank() -> "$titulo ($alteracao)"
                    !titulo.isNullOrBlank() -> titulo
                    !secao.isNullOrBlank() -> secao
                    !query.isNullOrBlank() -> query
                    !alteracao.isNullOrBlank() -> alteracao
                    else -> ""
                }
            }
            else -> {
                val chavesPreferenciais = listOf("titulo", "nome", "termo", "query", "url", "secao", "passo")
                val chave = chavesPreferenciais.firstOrNull { argumentos.has(it) && argumentos.optString(it).isNotBlank() }
                if (chave != null) argumentos.optString(chave) else ""
            }
        }
        return limparPayloadHumano(bruto)
    }

    private fun limparPayloadHumano(texto: String): String {
        val t = texto.trim()
        if (t.isBlank()) return ""
        if (pareceCodigoOuPayloadTecnico(t)) return ""
        val primeiraLinha = t.lineSequence().firstOrNull().orEmpty().trim()
        return if (primeiraLinha.length > 70) primeiraLinha.take(67) + "…" else primeiraLinha
    }

    /** Fontes que voltaram no `fim_ferramenta` (web_search / ler_url) — viram chips clicáveis. */
    private fun parseFontesAcao(ferramenta: String, arr: JSONArray?): List<LunaWebFonte> {
        if (arr == null) return emptyList()
        return (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            val url = o.optString("url").takeIf { it.isNotBlank() } ?: return@mapNotNull null
            LunaWebFonte(
                id = "f-$ferramenta-$i",
                title = o.optString("title").ifBlank { url },
                url = url,
                domain = runCatching { android.net.Uri.parse(url).host }.getOrNull().orEmpty(),
                status = LunaFonteStatus.CITADA,
            )
        }
    }

    suspend fun responder(
        context: Context,
        conversaId: String,
        historico: List<com.ethan.orbitlab.data.Mensagem>,
        textoUsuario: String,
        anexos: List<ComposerAttachment>,
        reference: ThreadReference?,
        userMessageId: String,
        lunaMessageId: String,
        onEstado: (LunaStreamEstado) -> Unit,
    ): LunaStreamResultado {
        if (!LunaApiConfig.isConfigured()) {
            return LunaStreamResultado(
                reasoning = "",
                reasoningDuracao = "",
                resposta = "URL da API Luna não configurada. Coloca `luna.api.url` no local.properties " +
                    "ou `EXPO_PUBLIC_LUNA_API_URL` no orbit-mobile/.env e recompila.",
                erro = true,
            )
        }

        val idToken = AuthRepository.getIdToken()
        if (idToken.isNullOrBlank()) {
            return LunaStreamResultado(
                reasoning = "",
                reasoningDuracao = "",
                resposta = "Precisa estar logado pra falar com o servidor Luna.",
                erro = true,
            )
        }

        val uid = AuthRepository.session.value?.uid
        if (uid.isNullOrBlank()) {
            return LunaStreamResultado(
                reasoning = "",
                reasoningDuracao = "",
                resposta = "Sessão inválida — tenta entrar de novo.",
                erro = true,
            )
        }

        // Modo técnico capturado no ENVIO (não no render): é o modo em que ESTA resposta nasce.
        // Marca o id agora pra correção de registro rodar depois na exibição, mesmo em reload.
        val modoTecnico = PrefsRepository.modoTecnico.value
        if (modoTecnico) PrefsRepository.marcarMensagemTecnica(lunaMessageId)
        // «Mãos à obra»: força o caminho agêntico no servidor (planejar/documentos/ferramentas
        // em todo turno). Exclusivo com o técnico — a PrefsRepository garante que só um fica ligado.
        val modoAgentico = PrefsRepository.modoAgentico.value ||
            (conversaId == PrefsRepository.conversaFinancas)

        val displayMessage = textoUsuario.trim()
        val message = when {
            reference != null -> formatMessageWithReference(displayMessage, reference)
            else -> displayMessage
        }.ifBlank {
            if (anexos.isNotEmpty()) "Veja os anexos." else "Oi"
        }

        val uploaded = if (anexos.isNotEmpty()) {
            // O anexo sobe ANTES da conversa começar; sem isso o "Pensando…" aparece parado
            // e a demora parece ser da Luna, quando é da foto subindo.
            onEstado(LunaStreamEstado.Raciocinando("", fase = rotuloEnvio(anexos)))
            ChatMediaUpload.uploadAttachments(
                context = context,
                uid = uid,
                conversationId = conversaId,
                messageId = userMessageId,
                attachments = anexos,
            )
        } else {
            anexos
        }

        val attachmentsJson = JSONArray()
        val documentsJson = JSONArray()
        uploaded.forEach { att ->
            val url = att.uri?.toString()?.takeIf { ChatMediaUpload.isRemoteUri(att.uri) }
            if (url.isNullOrBlank()) return@forEach
            when (att.kind) {
                AttachmentKind.FILE -> {
                    documentsJson.put(
                        JSONObject().apply {
                            put("id", att.id)
                            put("name", att.name)
                            put("mimeType", att.mime.ifBlank { "application/octet-stream" })
                            put("url", url)
                        },
                    )
                }
                AttachmentKind.IMAGE, AttachmentKind.VIDEO -> {
                    attachmentsJson.put(
                        JSONObject().apply {
                            put("id", att.id)
                            put("name", att.name)
                            put(
                                "mimeType",
                                att.mime.ifBlank {
                                    if (att.kind == AttachmentKind.VIDEO) "video/mp4" else "image/jpeg"
                                },
                            )
                            put("url", url)
                        },
                    )
                }
            }
        }

        val session = AuthRepository.session.value
        val profile = UserProfileRepository.profile.value
        val nome = profile.displayName.ifBlank { session?.displayName.orEmpty() }

        val body = JSONObject().apply {
            put("message", message)
            put("displayMessage", displayMessage.ifBlank { message })
            put("sessionId", conversaId)
            put("userMessageId", userMessageId)
            put("lunaMessageId", lunaMessageId)
            put("providerId", "auto")
            put("modelKey", "auto")
            put("timeZone", TimeZone.getDefault().id)
            // Modo técnico liga duas coisas juntas: a Luna responde com mais profundidade e
            // rigor (diretiva no core) E o raciocínio sobe pra «high». Desligado, tudo segue
            // no default caloroso/conciso de sempre.
            put("reasoningEnabled", PrefsRepository.reasoningEnabled.value)
            put("reasoningEffort", if (modoTecnico) "high" else "medium")
            put("pesquisaProfunda", PrefsRepository.pesquisaProfunda.value)
            put("modoTecnico", modoTecnico)
            put("modoAgentico", modoAgentico)
            // Artefatos: só o lab liga. Libera a ferramenta criar_artefato (o core é
            // partilhado com o estável, então a flag é o que a mantém invisível lá fora).
            put("documentosAtivo", true)
            LocationRepository.paraJson()?.let { put("local", it) }
            if (nome.isNotBlank()) put("userDisplayName", nome.take(64))
            if (attachmentsJson.length() > 0) put("attachments", attachmentsJson)
            if (documentsJson.length() > 0) put("documents", documentsJson)
        }

        val t0 = System.currentTimeMillis()
        val mainHandler = Handler(Looper.getMainLooper())
        onEstado(LunaStreamEstado.Raciocinando(""))

        val respostaBuf = StringBuilder()
        val reasoningBuf = StringBuilder()
        var faseAtual = ""
        // O rótulo rico que o servidor manda ("Escrevendo o documento…", "Aguardando o provedor…").
        // Quando vem, ele fala a verdade do passo; senão caímos no rótulo grosseiro por fase.
        var rotuloAtual = ""

        // Timeline de ações ao vivo: cada tool call do servidor (evento `acao`) nasce como um
        // passo RUNNING no `inicio_ferramenta` e fecha DONE/ERROR no `fim_ferramenta`. É o que
        // acende o painel de pesquisa que já existe — antes a gente só ignorava esses eventos.
        val acaoSteps = mutableListOf<LunaActionStep>()

        fun montarRun(status: LunaActionRunStatus): LunaActionRun? {
            if (acaoSteps.isEmpty()) return null
            val usouWeb = acaoSteps.any { it.ferramenta?.let(::ehFerramentaDeWeb) == true }
            // Ao fechar o turno, nenhum passo pode ficar preso em "…" (o fim_ferramenta pode
            // não ter chegado, e o passo Escrevendo nasce RUNNING). Vira DONE de fato.
            val steps = if (status == LunaActionRunStatus.RUNNING) {
                acaoSteps.toList()
            } else {
                acaoSteps.map { s ->
                    if (s.status != LunaActionStepStatus.RUNNING) return@map s
                    s.copy(
                        status = LunaActionStepStatus.DONE,
                        label = if (s.kind == LunaActionStepKind.WRITE) "Resposta escrita" else s.label,
                    )
                }
            }
            return LunaActionRun(
                id = "live-$lunaMessageId",
                title = if (usouWeb) "Pesquisa" else "Ferramentas",
                status = status,
                steps = steps,
                profile = if (usouWeb) LunaActionProfile.DEEP_RESEARCH else LunaActionProfile.TASK,
            )
        }

        /** Aplica um evento `acao` (inicio/fim) na timeline viva. */
        fun aplicarAcao(json: JSONObject) {
            val tipo = json.optString("tipo")
            val ferramenta = json.optString("ferramenta").ifBlank { return }
            val arg = extrairArgAcao(ferramenta, json.optJSONObject("argumentos"))
            val meta = toolMeta(ferramenta)
            if (tipo == "inicio_ferramenta") {
                acaoSteps += LunaActionStep(
                    id = "acao-${acaoSteps.size}",
                    label = meta.live(arg),
                    detail = arg.takeIf { it.isNotBlank() },
                    status = LunaActionStepStatus.RUNNING,
                    kind = meta.kind,
                    queries = listOfNotNull(arg.takeIf { it.isNotBlank() && meta.kind == com.ethan.orbitlab.ui.chat.LunaActionStepKind.SEARCH }),
                    ferramenta = ferramenta,
                )
            } else if (tipo == "fim_ferramenta") {
                val sucesso = json.optBoolean("sucesso", true)
                val fontes = parseFontesAcao(ferramenta, json.optJSONArray("fontes"))
                // Fecha o último passo aberto dessa ferramenta (pareia inicio↔fim).
                val idx = acaoSteps.indexOfLast {
                    it.ferramenta == ferramenta && it.status == LunaActionStepStatus.RUNNING
                }
                if (idx >= 0) {
                    val aberto = acaoSteps[idx]
                    acaoSteps[idx] = aberto.copy(
                        label = if (sucesso) meta.done(arg.ifBlank { aberto.detail.orEmpty() }) else aberto.label,
                        status = if (sucesso) LunaActionStepStatus.DONE else LunaActionStepStatus.ERROR,
                        sources = fontes.ifEmpty { aberto.sources },
                        citations = if (fontes.isNotEmpty()) {
                            fontes.mapIndexed { ci, f ->
                                com.ethan.orbitlab.ui.chat.LunaCitacao(
                                    id = "c-$idx-$ci", index = ci + 1, sourceId = f.id, title = f.title, url = f.url,
                                )
                            }
                        } else {
                            aberto.citations
                        },
                    )
                }
            }
        }

        /**
         * Cap honesto do roteiro: no instante em que o texto começa a sair, ela ESTÁ escrevendo.
         * Só entra quando houve pesquisa (passo de web) e uma vez só — sem chamada LLM extra e
         * sem inventar etapa. Não persiste (actionRunToFirestore só guarda passos de ferramenta):
         * fica como affordance ao vivo, e o histórico mostra o rastro de buscas/leituras já pronto.
         */
        fun marcarEscrevendo() {
            val temWeb = acaoSteps.any { it.ferramenta?.let(::ehFerramentaDeWeb) == true }
            if (!temWeb) return
            if (acaoSteps.any { it.kind == LunaActionStepKind.WRITE }) return
            acaoSteps += LunaActionStep(
                id = "acao-escrevendo",
                label = "Escrevendo a resposta",
                status = LunaActionStepStatus.RUNNING,
                kind = LunaActionStepKind.WRITE,
            )
        }

        fun emitirUi() {
            val texto = respostaBuf.toString().let { if (modoTecnico) formalizarTecnico(it) else it }
            val reasoning = reasoningBuf.toString()
            val durMs = System.currentTimeMillis() - t0
            val durLabel = "${(durMs / 1000.0).roundToInt().coerceAtLeast(1)}s"
            val run = montarRun(LunaActionRunStatus.RUNNING)
            val estado = if (texto.isNotEmpty()) {
                LunaStreamEstado.Respondendo(
                    reasoning = reasoning,
                    reasoningDuracao = durLabel,
                    respostaParcial = texto,
                    actionRun = run,
                )
            } else {
                // Raciocínio real (se houver) vira a caixa; a fase é só o rótulo do "Pensando…".
                // Antes a fase entrava no parcial e virava uma caixa de raciocínio fantasma.
                LunaStreamEstado.Raciocinando(
                    parcial = reasoning,
                    actionRun = run,
                    fase = rotuloAtual.ifBlank { if (faseAtual.isNotBlank()) rotuloFase(faseAtual) else "" },
                )
            }
            mainHandler.post { onEstado(estado) }
        }

        // Retry em falha de rede/conexão: se abortar SEM ter transmitido nada, evicta o
        // socket morto do pool e tenta de novo (não duplica texto já mostrado).
        var result = LunaApiClient.ChatResult(text = "", sessionId = "")
        var tentativa = 0
        val maxTentativas = 3
        while (true) {
            tentativa++
            respostaBuf.setLength(0)
            reasoningBuf.setLength(0)
            faseAtual = ""
            rotuloAtual = ""
            acaoSteps.clear()
            result = LunaApiClient.chatStream(idToken, body) { event ->
                when (event) {
                    is LunaApiClient.StreamEvent.Status -> {
                        faseAtual = event.phase
                        rotuloAtual = event.label ?: ""
                        if (respostaBuf.isEmpty()) emitirUi()
                    }
                    is LunaApiClient.StreamEvent.Reasoning -> {
                        reasoningBuf.append(event.delta)
                        emitirUi()
                    }
                    is LunaApiClient.StreamEvent.Content -> {
                        // Primeiro pedaço de texto = ela parou de buscar e começou a redigir.
                        if (respostaBuf.isEmpty()) marcarEscrevendo()
                        respostaBuf.append(event.delta)
                        emitirUi()
                    }
                    is LunaApiClient.StreamEvent.Acao -> {
                        // Acende a timeline ao vivo: o passo nasce/fecha e o painel de pesquisa
                        // (que já existia) reflete cada busca/leitura conforme acontece.
                        aplicarAcao(event.json)
                        emitirUi()
                    }
                    is LunaApiClient.StreamEvent.Error -> Unit
                }
            }
            // Só repete se o servidor NÃO deu sinal de ter recebido (sem TTFB e sem nenhuma
            // fase) E a resposta ainda não chegou pelo Firestore. Se ele já começou a gerar
            // e a conexão caiu no meio, repetir faria a Luna responder DUAS vezes — a 2ª
            // reescrevia a 1ª (foi o que aconteceu de manhã). Aí a gente mostra o erro + botão.
            val semSinalDoServidor = result.ttfbMs == null && result.phasesMs.isEmpty()
            val jaChegou = com.ethan.orbitlab.data.ChatRepository
                .respostaJaChegou(conversaId, lunaMessageId) != null
            val podeRepetir = result.error != null && respostaBuf.isEmpty() &&
                semSinalDoServidor && !jaChegou && erroTransitorio(result.error)
            if (podeRepetir && tentativa < maxTentativas) {
                LunaApiClient.evictConnections()
                mainHandler.post { onEstado(LunaStreamEstado.Raciocinando("")) }
                kotlinx.coroutines.delay(tentativa * 1200L)
                continue
            }
            break
        }

        val totalMs = System.currentTimeMillis() - t0
        val dur = (totalMs / 1000.0).roundToInt().coerceAtLeast(1)
        val phasesDetail = result.phasesMs.entries
            .joinToString(" ") { "${it.key}=${it.value}ms" }
            .ifBlank { null }

        if (result.error != null && result.text.isBlank()) {
            // A resposta real pode ter chegado pelo Firestore enquanto a conexão caía —
            // adota ela em vez de pintar um erro por cima (senão apagaria a fala da Luna).
            com.ethan.orbitlab.data.ChatRepository
                .respostaJaChegou(conversaId, lunaMessageId)?.let { texto ->
                    return LunaStreamResultado(
                        reasoning = "",
                        reasoningDuracao = "${dur}s",
                        resposta = texto,
                    )
                }
            LatenciaProbe.record(
                caminho = "paia_stream",
                totalMs = totalMs,
                ttfbMs = result.ttfbMs,
                chars = 0,
                ok = false,
                detalhe = listOfNotNull(result.error.take(80), phasesDetail).joinToString(" | "),
            )
            return LunaStreamResultado(
                reasoning = result.reasoning,
                reasoningDuracao = "${dur}s",
                resposta = "Não consegui responder: ${result.error}",
                erro = true,
            )
        }

        val textoBruto = result.text.ifBlank { respostaBuf.toString() }.ifBlank { "…" }
        val texto = if (modoTecnico) formalizarTecnico(textoBruto) else textoBruto
        val reasoning = result.reasoning.ifBlank { reasoningBuf.toString() }
        LatenciaProbe.record(
            caminho = "paia_stream",
            totalMs = totalMs,
            ttfbMs = result.ttfbMs,
            chars = texto.length,
            ok = true,
            detalhe = phasesDetail,
        )

        return LunaStreamResultado(
            reasoning = reasoning,
            reasoningDuracao = "${dur}s",
            resposta = texto,
            actionRun = montarRun(LunaActionRunStatus.DONE),
        )
    }
}
