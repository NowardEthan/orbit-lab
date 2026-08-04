package com.ethan.orbitlab.data

import android.app.Application
import com.ethan.orbitlab.data.firebase.ChatMediaUpload
import com.ethan.orbitlab.data.firebase.ConversaMeta
import com.ethan.orbitlab.data.firebase.FirestoreChat
import com.ethan.orbitlab.data.firebase.toConversa
import com.ethan.orbitlab.ui.chat.ComposerAttachment
import com.ethan.orbitlab.ui.chat.ImagemGerada
import com.ethan.orbitlab.ui.chat.LunaActionRun
import com.ethan.orbitlab.ui.chat.ThreadReference
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import com.ethan.orbitlab.ui.chat.LunaStreamEstado
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class Mensagem(
    val id: String = UUID.randomUUID().toString(),
    val texto: String,
    val isLuna: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    /** Raciocínio interno — só Luna; UI quieta acima da resposta. */
    val reasoning: String? = null,
    /** Duração aproximada do raciocínio (ex.: "3s") pra meta no rótulo. */
    val reasoningDuracao: String? = null,
    /** Timeline de ações da Luna (tools e/ou pesquisa profunda). */
    val actionRun: LunaActionRun? = null,
    /** Anexos (imagens / arquivos) — tipicamente do usuário. */
    val attachments: List<ComposerAttachment> = emptyList(),
    /** Imagens que a LUNA desenhou (ferramenta `gerar_imagem`) — viram cartões no lado dela. */
    val imagensGeradas: List<ImagemGerada> = emptyList(),
    /** Referência contextual (mensagem / imagem citada). */
    val reference: ThreadReference? = null,
    /** Balão de falha (rede/servidor) — local, some no retry, NUNCA vira memória da Luna. */
    val erro: Boolean = false,
)

data class Conversa(
    val id: String = UUID.randomUUID().toString(),
    val titulo: String,
    val mensagens: List<Mensagem> = emptyList(),
    val ultimaAtualizacao: Long = System.currentTimeMillis(),
    /** Preview já limpo (sem markdown) — evita regex na composition. */
    val preview: String = limparPreviewMensagem(mensagens.lastOrNull()?.texto),
    /** Hora curta pré-formatada (ex.: "14:32"). */
    val horaFormatada: String = formatarHoraCurta(ultimaAtualizacao),
    /** Contagem Firestore (quando mensagens ainda não foram carregadas). */
    val messageCount: Int = mensagens.size,
) {
    /** Rótulo de agrupamento: Hoje / Ontem / dia da semana / data. */
    val grupoDia: String
        get() = rotuloGrupoDia(ultimaAtualizacao)
}

private val localeBr = Locale("pt", "BR")
private val horaFmt = ThreadLocal.withInitial {
    SimpleDateFormat("HH:mm", localeBr)
}

fun formatarHoraCurta(timestamp: Long): String =
    horaFmt.get()!!.format(Date(timestamp))

/** Preview sem ruído de markdown (fences, headings). */
fun limparPreviewMensagem(fonte: String?): String {
    if (fonte.isNullOrBlank()) return "Nenhuma mensagem ainda."
    return fonte
        .lineSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() && !it.startsWith("```") && !it.startsWith("#") }
        .joinToString(" ")
        .replace(Regex("""\*\*|`|>\s*"""), "")
        .take(120)
        .ifBlank { "Nenhuma mensagem ainda." }
}

fun rotuloGrupoDia(timestamp: Long, agora: Long = System.currentTimeMillis()): String {
    val calAgora = Calendar.getInstance().apply { timeInMillis = agora }
    val calAlvo = Calendar.getInstance().apply { timeInMillis = timestamp }

    fun mesmoDia(a: Calendar, b: Calendar): Boolean =
        a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
            a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)

    if (mesmoDia(calAgora, calAlvo)) return "Hoje"

    val ontem = Calendar.getInstance().apply {
        timeInMillis = agora
        add(Calendar.DAY_OF_YEAR, -1)
    }
    if (mesmoDia(ontem, calAlvo)) return "Ontem"

    val inicioSemana = Calendar.getInstance().apply {
        timeInMillis = agora
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        add(Calendar.DAY_OF_YEAR, -6)
    }
    if (timestamp >= inicioSemana.timeInMillis) {
        val dia = SimpleDateFormat("EEEE", localeBr).format(Date(timestamp))
        return dia.replaceFirstChar { if (it.isLowerCase()) it.titlecase(localeBr) else it.toString() }
    }

    return SimpleDateFormat("d MMM", localeBr).format(Date(timestamp))
}

/**
 * Chat sincronizado com Firestore + Storage (mesmo projeto do orbit-mobile).
 * Sem seed demo local — lista e mídia vêm da nuvem quando logado.
 */
object ChatRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var app: Application? = null

    private val _conversas = MutableStateFlow<List<Conversa>>(emptyList())
    val conversas: StateFlow<List<Conversa>> = _conversas.asStateFlow()

    private var conversationsReg: ListenerRegistration? = null
    private val messageRegs = ConcurrentHashMap<String, ListenerRegistration>()
    private val metaById = ConcurrentHashMap<String, ConversaMeta>()
    private var currentUid: String? = null

    fun init() {
        // Observa uid — liga/desliga sync
        scope.launch {
            AuthRepository.uidFlow().collect { uid ->
                detachAll()
                currentUid = uid
                if (uid == null) {
                    _conversas.value = emptyList()
                } else {
                    attachConversations(uid)
                }
            }
        }
    }

    /** Escopo de app — sobrevive à tela (não usa rememberCoroutineScope). */
    fun launch(block: suspend CoroutineScope.() -> Unit) {
        scope.launch(block = block)
    }

    /**
     * Turno da Luna em andamento por conversa — sobrevive ao leave/return do ChatScreen.
     * Sem isto, ao voltar o auto-retry via órfã disparava um 2º HTTP com o mesmo lunaMsgId
     * e a resposta que chegava por último sobrescrevia a outra (pior em geração de imagem).
     */
    private val turnosAtivos = ConcurrentHashMap<String, Job>()
    private val streamPorConversa = ConcurrentHashMap<String, MutableStateFlow<LunaStreamEstado>>()
    private val lunaMsgIdsEmVoo = ConcurrentHashMap<String, String>()

    fun turnoEmAndamento(conversaId: String): Boolean =
        turnosAtivos[conversaId]?.isActive == true

    fun streamDaConversa(conversaId: String): StateFlow<LunaStreamEstado> =
        streamFlowInterno(conversaId).asStateFlow()

    fun lunaMsgIdEmVoo(conversaId: String): String? = lunaMsgIdsEmVoo[conversaId]

    private fun streamFlowInterno(conversaId: String): MutableStateFlow<LunaStreamEstado> =
        streamPorConversa.getOrPut(conversaId) {
            MutableStateFlow(LunaStreamEstado.Idle)
        }

    fun publicarStream(conversaId: String, estado: LunaStreamEstado) {
        streamFlowInterno(conversaId).value = estado
    }

    /**
     * Lança o turno da Luna nesta conversa. Se já houver um ativo, **ignora** o 2º disparo
     * (não cancela o primeiro — ele continua gerando fora da tela).
     * @return false se já havia turno em andamento
     */
    fun launchTurno(
        conversaId: String,
        lunaMessageId: String?,
        block: suspend CoroutineScope.() -> Unit,
    ): Boolean {
        if (turnoEmAndamento(conversaId)) return false
        if (!lunaMessageId.isNullOrBlank()) {
            lunaMsgIdsEmVoo[conversaId] = lunaMessageId
        }
        publicarStream(conversaId, LunaStreamEstado.Raciocinando(""))
        val job = scope.launch {
            try {
                block()
            } finally {
                turnosAtivos.remove(conversaId)
                lunaMsgIdsEmVoo.remove(conversaId)
                publicarStream(conversaId, LunaStreamEstado.Idle)
            }
        }
        turnosAtivos[conversaId] = job
        return true
    }

    /** Contexto da Application — necessário pro upload de content://. */
    fun bindApp(application: Application) {
        app = application
    }

    fun appContext(): android.content.Context? = app?.applicationContext

    private fun attachConversations(uid: String) {
        conversationsReg = FirestoreChat.subscribeConversations(
            uid = uid,
            onChange = { metas ->
                metaById.clear()
                metas.forEach { metaById[it.id] = it }
                _conversas.update { prev ->
                    val prevMsgs = prev.associate { it.id to it.mensagens }
                    metas.map { meta ->
                        val msgs = prevMsgs[meta.id]
                            ?: meta.legacyMessages.takeIf { it.isNotEmpty() }
                            ?: emptyList()
                        meta.toConversa(msgs)
                    }
                }
                // Reanexa listeners de mensagens já abertos
                messageRegs.keys.toList().forEach { id ->
                    if (metas.none { it.id == id }) {
                        messageRegs.remove(id)?.remove()
                    } else {
                        reattachMessages(uid, id)
                    }
                }
            },
            onError = { /* offline / regras — lista fica como está */ },
        )
    }

    private fun ensureMessagesListener(conversationId: String) {
        val uid = currentUid ?: return
        if (messageRegs.containsKey(conversationId)) return
        reattachMessages(uid, conversationId)
    }

    private fun reattachMessages(uid: String, conversationId: String) {
        messageRegs.remove(conversationId)?.remove()
        val meta = metaById[conversationId]
        val reg = FirestoreChat.subscribeMessages(
            uid = uid,
            conversationId = conversationId,
            deletedIds = meta?.deletedMessageIds.orEmpty(),
            legacy = meta?.legacyMessages.orEmpty(),
            onChange = { msgs ->
                _conversas.update { lista ->
                    lista.map { conv ->
                        if (conv.id != conversationId) conv
                        else {
                        val idsNuvem = msgs.mapTo(HashSet()) { it.id }
                        val deletados = metaById[conversationId]?.deletedMessageIds.orEmpty()
                        val locaisPorId = conv.mensagens.associateBy { it.id }
                        // 1. Ancora o timestamp da nuvem no timestamp LOCAL que já temos. O
                        //    carimbo do servidor oscila enquanto não assenta (cada snapshot
                        //    recalcula agora()) e faz os balões pularem de lugar. O local é
                        //    estável e já está na ordem certa (tua fala antes da Luna).
                        val daNuvem = msgs.map { nuvem ->
                            val local = locaisPorId[nuvem.id] ?: return@map nuvem
                            // O Railway às vezes grava a bolha dela sem `imagens[]` (ou o
                            // snapshot chega antes do write local). Sem isto o cartão some
                            // mesmo com a URL já na UI — pior no free, onde o sync bate mais.
                            nuvem.copy(
                                timestamp = local.timestamp,
                                imagensGeradas = nuvem.imagensGeradas.ifEmpty { local.imagensGeradas },
                                attachments = nuvem.attachments.ifEmpty { local.attachments },
                                actionRun = nuvem.actionRun ?: local.actionRun,
                                reasoning = nuvem.reasoning ?: local.reasoning,
                            )
                        }
                        // 2. Mensagens que só existem local (otimista recém-enviada que a nuvem
                        //    ainda não devolveu, ou balão de erro). Mantém até sincronizar —
                        //    senão o snapshot as apaga e elas piscam/somem no meio do envio.
                        val soLocais = conv.mensagens.filter {
                            it.id !in idsNuvem && it.id !in deletados
                        }
                        val mensagensFinais = ordenarMensagensChat(daNuvem + soLocais)
                        conv.copy(
                            mensagens = mensagensFinais,
                            preview = limparPreviewMensagem(
                                mensagensFinais.lastOrNull()?.texto
                                    ?: meta?.preview,
                            ),
                            ultimaAtualizacao = mensagensFinais.lastOrNull()?.timestamp
                                ?: conv.ultimaAtualizacao,
                            horaFormatada = formatarHoraCurta(
                                mensagensFinais.lastOrNull()?.timestamp ?: conv.ultimaAtualizacao,
                            ),
                        )
                        }
                    }
                }
            },
        )
        messageRegs[conversationId] = reg
    }

    private fun detachAll() {
        conversationsReg?.remove()
        conversationsReg = null
        messageRegs.values.forEach { it.remove() }
        messageRegs.clear()
        metaById.clear()
    }

    fun criarConversa(): String {
        val id = UUID.randomUUID().toString()
        val nova = Conversa(id = id, titulo = "Nova conversa")
        _conversas.update { listOf(nova) + it.filterNot { c -> c.id == id } }
        val uid = currentUid
        if (uid != null) {
            scope.launch {
                runCatching { FirestoreChat.ensureConversation(uid, id) }
            }
        }
        return id
    }

    /**
     * Conversa fixa do módulo Finanças (FAB).
     *
     * Id estável [`FirestoreChat.ID_CONVERSA_FINANCAS`] no Firestore — não depende de
     * SharedPreferences nem de UUID aleatório. Rebuild/reinstall reabre a MESMA
     * conversa e o histórico volta com o listener.
     */
    fun garantirConversaFinancas(): String {
        val id = FirestoreChat.ID_CONVERSA_FINANCAS
        val titulo = FirestoreChat.TITULO_CONVERSA_FINANCAS
        PrefsRepository.conversaFinancas = id
        val existente = getConversa(id)
        if (existente == null) {
            val nova = Conversa(id = id, titulo = titulo)
            _conversas.update { listOf(nova) + it.filterNot { c -> c.id == id } }
        } else if (!existente.titulo.equals(titulo, ignoreCase = true)) {
            _conversas.update { lista ->
                lista.map { c -> if (c.id == id) c.copy(titulo = titulo) else c }
            }
        }
        ensureMessagesListener(id)
        val uid = currentUid
        if (uid != null) {
            scope.launch {
                runCatching {
                    FirestoreChat.ensureConversation(
                        uid = uid,
                        conversationId = id,
                        title = titulo,
                        titleLocked = true,
                    )
                }
            }
        }
        return id
    }

    fun ehConversaFinancas(conversaId: String): Boolean =
        FirestoreChat.isSessaoFinancas(conversaId)

    fun getConversa(id: String): Conversa? {
        return _conversas.value.find { it.id == id }
    }

    /**
     * Texto de uma resposta REAL da Luna já presente na conversa para [messageId], ou null.
     *
     * Anti-geração-dupla: se o servidor gravou a resposta no Firestore e o listener já a
     * trouxe, o retry adota ESSA em vez de repetir a chamada (que faria a Luna responder de
     * novo e reescrever a fala por cima). Ignora balão de erro (não é resposta) e vazio.
     */
    fun respostaJaChegou(conversaId: String, messageId: String?): String? {
        if (messageId.isNullOrBlank()) return null
        val msg = getConversa(conversaId)?.mensagens?.find { it.id == messageId } ?: return null
        if (!msg.isLuna || msg.erro) return null
        return msg.texto.takeIf { it.isNotBlank() }
    }

    /** Renomeia a conversa (título gerado pela Luna) — otimista na UI + grava na nuvem. */
    fun renomearConversa(conversaId: String, titulo: String) {
        val limpo = titulo.trim()
        if (limpo.isBlank()) return
        _conversas.update { lista ->
            lista.map { c -> if (c.id == conversaId) c.copy(titulo = limpo) else c }
        }
        val uid = currentUid ?: return
        scope.launch {
            runCatching {
                com.ethan.orbitlab.data.firebase.FirestoreChat.renomearConversa(uid, conversaId, limpo)
            }
        }
    }

    /**
     * Deixa a Luna batizar a conversa conforme o assunto — UMA vez, no 1º par.
     * Chama `POST /v1/conversa/titulo` no luna-core (barato). Se falhar, fica o
     * palpite truncado da 1ª fala e não tenta de novo. Finanças: título fixo do módulo.
     */
    fun talvezRenomearPelaLuna(conversaId: String) {
        // Módulo Finanças: o título é identidade do lugar — não vira assunto do 1º turno.
        if (ehConversaFinancas(conversaId)) return
        val conv = getConversa(conversaId) ?: return
        val turnosUsuario = conv.mensagens.count { !it.isLuna && !it.erro }
        val deveRenomear = turnosUsuario == 1
        if (!deveRenomear) return
        scope.launch {
            val msgs = getConversa(conversaId)?.mensagens ?: return@launch
            val novo = com.ethan.orbitlab.data.openrouter.LunaTitler.gerarTitulo(msgs) ?: return@launch
            // Não repinta se saiu igual — evita flicker do header à toa.
            if (novo.equals(getConversa(conversaId)?.titulo, ignoreCase = true)) return@launch
            renomearConversa(conversaId, novo)
        }
    }

    /** Observa só a conversa [id] — liga listener de mensagens sob demanda. */
    fun observarConversa(id: String): Flow<Conversa?> {
        ensureMessagesListener(id)
        return conversas
            .map { lista -> lista.find { it.id == id } }
            .distinctUntilChanged()
    }

    /**
     * Garante as mensagens completas de [id] (pra exportar do histórico sem abrir o chat).
     * Se já estão em memória, devolve na hora; senão liga o listener e espera popular.
     */
    suspend fun carregarMensagens(id: String): List<Mensagem> {
        getConversa(id)?.mensagens?.takeIf { it.isNotEmpty() }?.let { return it }
        ensureMessagesListener(id)
        return withTimeoutOrNull(5000) {
            conversas
                .map { lista -> lista.find { it.id == id }?.mensagens.orEmpty() }
                .first { it.isNotEmpty() }
        } ?: getConversa(id)?.mensagens.orEmpty()
    }

    /**
     * Reenviar a partir de uma mensagem (estilo ChatGPT): mantém tudo ATÉ [ancoraId]
     * (inclusive) e apaga o que vem depois — local e na nuvem (soft-delete).
     */
    fun truncarApos(conversaId: String, ancoraId: String) {
        val conv = getConversa(conversaId) ?: return
        val idx = conv.mensagens.indexOfFirst { it.id == ancoraId }
        if (idx < 0) return
        val mantidas = conv.mensagens.take(idx + 1)
        val removidas = conv.mensagens.drop(idx + 1).map { it.id }
        if (removidas.isEmpty()) return

        _conversas.update { lista ->
            lista.map { c ->
                if (c.id != conversaId) c
                else c.copy(
                    mensagens = mantidas,
                    preview = limparPreviewMensagem(mantidas.lastOrNull()?.texto),
                )
            }
        }

        val uid = currentUid
        val meta = metaById[conversaId]
        if (meta != null) {
            metaById[conversaId] = meta.copy(deletedMessageIds = meta.deletedMessageIds + removidas)
            if (uid != null) reattachMessages(uid, conversaId)
        }
        if (uid != null) {
            scope.launch {
                runCatching { FirestoreChat.marcarMensagensApagadas(uid, conversaId, removidas) }
            }
        }
    }

    fun deletarConversas(ids: List<String>) {
        if (ids.isEmpty()) return
        _conversas.update { lista -> lista.filterNot { it.id in ids } }
        ids.forEach { messageRegs.remove(it)?.remove() }
        val uid = currentUid ?: return
        scope.launch {
            ids.forEach { id ->
                runCatching { FirestoreChat.softDeleteConversation(uid, id) }
            }
        }
    }

    /** Soft-delete de todas as conversas na nuvem + limpa lista local. */
    fun apagarTodosOsDados() {
        val ids = _conversas.value.map { it.id }
        val uid = currentUid
        _conversas.value = emptyList()
        messageRegs.values.forEach { it.remove() }
        messageRegs.clear()
        if (uid != null && ids.isNotEmpty()) {
            scope.launch {
                ids.forEach { id ->
                    runCatching { FirestoreChat.softDeleteConversation(uid, id) }
                }
            }
        }
    }

    fun enviarMensagem(
        conversaId: String,
        texto: String,
        isLuna: Boolean = false,
        reasoning: String? = null,
        reasoningDuracao: String? = null,
        actionRun: LunaActionRun? = null,
        attachments: List<ComposerAttachment> = emptyList(),
        /** Imagens desenhadas pela Luna neste turno (só Luna). */
        imagensGeradas: List<ImagemGerada> = emptyList(),
        reference: ThreadReference? = null,
        /** Id fixo (Railway/idempotência) — se omitido, gera UUID. */
        messageId: String? = null,
        /** false = só UI local (servidor já persiste o mesmo id). */
        persistirNuvem: Boolean = true,
        /** Balão de falha (só Luna) — não vira memória; some quando o retro reusa o id. */
        erro: Boolean = false,
    ): String {
        val id = messageId?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()
        val agora = System.currentTimeMillis()
        val localAttachments = if (isLuna) emptyList() else attachments
        val localImagens = if (isLuna) imagensGeradas else emptyList()
        val localRef = if (isLuna) null else reference

        // Optimista na UI — upsert pelo id (evita duplicar quando o Firestore já syncou)
        _conversas.update { lista ->
            val idx = lista.indexOfFirst { it.id == conversaId }
            if (idx < 0) return@update lista
            val conv = lista[idx]
            val tituloBase = when {
                ehConversaFinancas(conversaId) -> null
                !isLuna && texto.isNotBlank() -> texto
                !isLuna && attachments.isNotEmpty() -> attachments.first().name
                !isLuna && reference != null -> "Sobre referência"
                else -> null
            }
            val novoTitulo = if (tituloBase != null && (
                    conv.mensagens.isEmpty() ||
                        conv.titulo.equals("Nova conversa", ignoreCase = true)
                    )
            ) {
                tituloBase.take(20) + if (tituloBase.length > 20) "…" else ""
            } else {
                conv.titulo
            }
            val timestamp = if (isLuna) {
                val userId = userMessageIdForLuna(id)
                val userTs = userId?.let { uid -> conv.mensagens.find { it.id == uid }?.timestamp }
                maxOf(agora, (userTs ?: agora) + 1L)
            } else {
                agora
            }
            val nova = Mensagem(
                id = id,
                texto = texto,
                isLuna = isLuna,
                timestamp = timestamp,
                reasoning = if (isLuna) reasoning else null,
                reasoningDuracao = if (isLuna) reasoningDuracao else null,
                actionRun = if (isLuna) actionRun else null,
                attachments = localAttachments,
                imagensGeradas = localImagens,
                reference = localRef,
                erro = if (isLuna) erro else false,
            )
            val jaExiste = conv.mensagens.any { it.id == id }
            val novasMensagens = ordenarMensagensChat(
                if (jaExiste) {
                    conv.mensagens.map { if (it.id == id) {
                        // Preserva timestamp/anexos locais se o patch vier sem
                        it.copy(
                            texto = texto.ifBlank { it.texto },
                            reasoning = nova.reasoning ?: it.reasoning,
                            reasoningDuracao = nova.reasoningDuracao ?: it.reasoningDuracao,
                            actionRun = nova.actionRun ?: it.actionRun,
                            attachments = localAttachments.ifEmpty { it.attachments },
                            imagensGeradas = localImagens.ifEmpty { it.imagensGeradas },
                            reference = localRef ?: it.reference,
                            // Retry reusa o id: uma resposta real (erro=false) limpa o balão de falha.
                            erro = if (isLuna) erro else it.erro,
                        )
                    } else it }
                } else {
                    conv.mensagens + nova
                },
            )
            val previewFonte = when {
                texto.isNotBlank() -> texto
                attachments.isNotEmpty() ->
                    if (attachments.size == 1) attachments.first().name
                    else "${attachments.size} anexos · ${attachments.first().name}"
                else -> novasMensagens.lastOrNull()?.texto
            }
            val atualizada = conv.copy(
                titulo = novoTitulo,
                mensagens = novasMensagens,
                ultimaAtualizacao = agora,
                preview = limparPreviewMensagem(previewFonte),
                horaFormatada = formatarHoraCurta(agora),
            )
            ArrayList<Conversa>(lista.size).also { out ->
                for (i in lista.indices) {
                    out.add(if (i == idx) atualizada else lista[i])
                }
            }
        }

        ensureMessagesListener(conversaId)

        if (!persistirNuvem) return id

        val uid = currentUid ?: return id
        val titleHint = _conversas.value.find { it.id == conversaId }?.titulo
        scope.launch {
            runCatching {
                if (isLuna) {
                    FirestoreChat.writeLunaMessage(
                        uid = uid,
                        conversationId = conversaId,
                        messageId = id,
                        text = texto,
                        reasoning = reasoning,
                        actionRun = actionRun,
                        imagensGeradas = localImagens,
                    )
                } else {
                    val ctx = app?.applicationContext
                    val uploaded = if (ctx != null && localAttachments.isNotEmpty()) {
                        ChatMediaUpload.uploadAttachments(
                            context = ctx,
                            uid = uid,
                            conversationId = conversaId,
                            messageId = id,
                            attachments = localAttachments,
                        )
                    } else {
                        localAttachments
                    }
                    // Atualiza URIs remotas na bolha local
                    if (uploaded !== localAttachments) {
                        _conversas.update { lista ->
                            lista.map { conv ->
                                if (conv.id != conversaId) conv
                                else conv.copy(
                                    mensagens = conv.mensagens.map { msg ->
                                        if (msg.id != id) msg
                                        else msg.copy(attachments = uploaded)
                                    },
                                )
                            }
                        }
                    }
                    FirestoreChat.writeUserMessage(
                        uid = uid,
                        conversationId = conversaId,
                        messageId = id,
                        text = texto,
                        attachments = uploaded,
                        reference = localRef,
                        titleHint = titleHint,
                    )
                }
            }
        }
        return id
    }
}
