package com.ethan.orbitlab.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Preferências locais do lab — espelho leve do AsyncStorage do orbit-mobile.
 */
object PrefsRepository {
    private const val PREFS = "orbitlab_prefs"
    private const val KEY_VIBRACAO = "orbit.lab.vibracao"
    private const val KEY_PESQUISA_PROFUNDA = "orbit.lab.pesquisa.profunda"
    private const val KEY_MODO_TECNICO = "orbit.lab.modo.tecnico"
    private const val KEY_MODO_AGENTICO = "orbit.lab.modo.agentico"
    private const val KEY_SESSAO_UID = "orbit.lab.sessao.uid"
    private const val KEY_AUTH_DIAG = "orbit.lab.sessao.diag"
    private const val KEY_ULTIMA_ABA = "orbit.lab.lugar.aba"
    private const val KEY_ULTIMA_CONVERSA = "orbit.lab.lugar.conversa"
    private const val KEY_ANCORA = "orbit.lab.lugar.ancora"
    private const val KEY_GAVETA_FINANCAS = "orbit.lab.gaveta.financas.aberta"
    private const val KEY_CONVERSA_FINANCAS = "orbit.lab.financas.conversa"
    private const val KEY_CONTEXTO_COMERCIAL = "orbit.lab.contexto.comercial"
    private const val KEY_LOCALIZACAO = "orbit.lab.localizacao.ativa"
    private const val KEY_LOCAL_SNAPSHOT = "orbit.lab.localizacao.snapshot"
    private const val KEY_TECNICO_IDS = "orbit.lab.tecnico.msgids"
    private const val KEY_CPF_CNPJ = "orbit.lab.billing.cpfcnpj"
    /** Showcase V1 da Início (Finanças / desenha / artefatos) — versionado pra reaparecer em capítulos novos. */
    private const val KEY_SHOWCASE_LUNA_V1 = "orbit.lab.inicio.showcase.luna.v1.dismissed"
    /** Bolha flutuante (chat-head) ligada pelo usuário — religa ao abrir o app se a permissão existir. */
    private const val KEY_BOLHA_ATIVA = "orbit.lab.bolha.ativa"
    /** Posição da bolha: lado (esq=true) + Y em px de tela (Gravity.TOP\|START). */
    private const val KEY_BOLHA_LADO_ESQ = "orbit.lab.bolha.lado.esq"
    private const val KEY_BOLHA_Y = "orbit.lab.bolha.y"
    private const val KEY_BOLHA_ONBOARDING = "orbit.lab.bolha.onboarding.visto"
    private const val KEY_BOLHA_LAST_LUNA_MSG = "orbit.lab.bolha.last.luna.msg"
    private const val BOLHA_Y_DEFAULT = 240
    private const val MAX_TECNICO_IDS = 500

    private lateinit var prefs: SharedPreferences

    /**
     * Ids das mensagens que nasceram no Modo técnico. Marcador local (o servidor grava o texto
     * cru e o listener do Firestore sobrescreve o texto local a cada snapshot, então corrigir o
     * texto guardado não sobrevive ao reload). Com o id aqui, a correção de registro roda na
     * EXIBIÇÃO — à prova de sync e não-destrutiva. LinkedHashSet pra podar o mais antigo primeiro.
     */
    private val idsTecnico = LinkedHashSet<String>()

    /** Raciocínio SEMPRE visível — não é mais opção do usuário. */
    val reasoningEnabled: StateFlow<Boolean> = MutableStateFlow(true).asStateFlow()

    private val _vibracao = MutableStateFlow(true)
    val vibracao: StateFlow<Boolean> = _vibracao.asStateFlow()

    /**
     * Modo pesquisa profunda — opt-in, DESLIGADO por default. Ligado, a Luna cruza as
     * fontes (uma chamada extra ao modelo) antes de escrever; isso custa latência, então
     * fica na mão do usuário. Desligado, o chat comum segue leve como sempre.
     */
    private val _pesquisaProfunda = MutableStateFlow(false)
    val pesquisaProfunda: StateFlow<Boolean> = _pesquisaProfunda.asStateFlow()

    /**
     * Modo técnico — opt-in, DESLIGADO por default. A voz calorosa e concisa da Luna é o
     * default e continua sendo. Ligado, ela responde de primeira com mais profundidade e
     * rigor (detalhe, termos exatos, estrutura) sem que o usuário precise insistir; o
     * cliente também sobe o raciocínio pra «high» junto. Fica grudado até desligar.
     */
    private val _modoTecnico = MutableStateFlow(false)
    val modoTecnico: StateFlow<Boolean> = _modoTecnico.asStateFlow()

    /**
     * Legado A1 — seletor Conversa/Técnico/Ação removido. Prefs mortas (sempre false);
     * o core usa soft router. Finanças força agentico no body, não aqui.
     */
    private val _modoAgentico = MutableStateFlow(false)
    val modoAgentico: StateFlow<Boolean> = _modoAgentico.asStateFlow()

    /**
     * Contexto visível da sidebar: pessoal por padrão, comercial quando o usuário troca.
     * Por enquanto é uma preferência de UI; a separação de dados vem quando houver contrato.
     */
    private val _contextoComercial = MutableStateFlow(false)
    val contextoComercial: StateFlow<Boolean> = _contextoComercial.asStateFlow()

    /**
     * Compartilhar localização + clima com a Luna — opt-in, DESLIGADO por default. Ligado,
     * o app capta o GPS, resolve cidade/uf e busca o clima; isso dá à Luna o «onde» (antes
     * ela só tinha o «quando»). Desligado, nada de local sai do aparelho.
     */
    private val _localizacaoAtiva = MutableStateFlow(false)
    val localizacaoAtiva: StateFlow<Boolean> = _localizacaoAtiva.asStateFlow()

    private val _bolhaAtiva = MutableStateFlow(false)
    /** Preferência: usuário quer a bolha ligada (o serviço pode estar morto até religar). */
    val bolhaAtiva: StateFlow<Boolean> = _bolhaAtiva.asStateFlow()

    /** Último lado em que a bolha grudou (true = esquerda). */
    var bolhaLadoEsquerdo: Boolean = true
        private set

    /** Último Y (px) da bolha na tela. */
    var bolhaY: Int = BOLHA_Y_DEFAULT
        private set

    /** Onboarding da bolha já foi visto uma vez. */
    var bolhaOnboardingVisto: Boolean = false
        private set

    /** Última msg da Luna “vista” pela bolha (pra badge de novidade). */
    var bolhaLastLunaMsgId: String? = null
        private set

    fun init(context: Context) {
        if (::prefs.isInitialized) return
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        _vibracao.value = prefs.getBoolean(KEY_VIBRACAO, true)
        _pesquisaProfunda.value = prefs.getBoolean(KEY_PESQUISA_PROFUNDA, false)
        // A1: apaga sticky legado do seletor (não volta a forçar agentico/técnico).
        if (prefs.contains(KEY_MODO_TECNICO) || prefs.contains(KEY_MODO_AGENTICO)) {
            prefs.edit().remove(KEY_MODO_TECNICO).remove(KEY_MODO_AGENTICO).apply()
        }
        _modoTecnico.value = false
        _modoAgentico.value = false
        _contextoComercial.value = prefs.getBoolean(KEY_CONTEXTO_COMERCIAL, false)
        _localizacaoAtiva.value = prefs.getBoolean(KEY_LOCALIZACAO, false)
        _bolhaAtiva.value = prefs.getBoolean(KEY_BOLHA_ATIVA, false)
        bolhaLadoEsquerdo = prefs.getBoolean(KEY_BOLHA_LADO_ESQ, true)
        bolhaY = prefs.getInt(KEY_BOLHA_Y, BOLHA_Y_DEFAULT)
        bolhaOnboardingVisto = prefs.getBoolean(KEY_BOLHA_ONBOARDING, false)
        bolhaLastLunaMsgId = prefs.getString(KEY_BOLHA_LAST_LUNA_MSG, null)
        idsTecnico.clear()
        idsTecnico.addAll(prefs.getStringSet(KEY_TECNICO_IDS, emptySet()).orEmpty())
    }

    /** Marca que [id] é uma resposta do Modo técnico (pra corrigir o registro na exibição). */
    fun marcarMensagemTecnica(id: String) {
        if (id.isBlank() || !idsTecnico.add(id)) return
        while (idsTecnico.size > MAX_TECNICO_IDS) {
            val it = idsTecnico.iterator()
            if (it.hasNext()) { it.next(); it.remove() } else break
        }
        if (::prefs.isInitialized) {
            prefs.edit().putStringSet(KEY_TECNICO_IDS, LinkedHashSet(idsTecnico)).apply()
        }
    }

    /** [id] nasceu no Modo técnico? (então a exibição passa o corretor de registro nela). */
    fun mensagemEhTecnica(id: String): Boolean = id in idsTecnico

    /**
     * Uid da última conta que entrou — marcador de «já tem sessão neste aparelho».
     * Serve pra segurar a tela de login enquanto o Firebase lê a conta do disco: num
     * celular apertado de memória o app é recriado toda hora, e sem isto ele piscava
     * o login (e pedia a conta Google de novo) só porque o Firebase ainda não respondera.
     */
    var sessaoUid: String?
        get() = if (::prefs.isInitialized) prefs.getString(KEY_SESSAO_UID, null) else null
        set(value) {
            if (!::prefs.isInitialized) return
            val ed = prefs.edit()
            if (value.isNullOrBlank()) ed.remove(KEY_SESSAO_UID) else ed.putString(KEY_SESSAO_UID, value)
            ed.apply()
        }

    /** Diário da última abertura (ver [AuthDiag]) — some quando os dados são limpos. */
    var authDiag: String?
        get() = if (::prefs.isInitialized) prefs.getString(KEY_AUTH_DIAG, null) else null
        set(value) {
            if (!::prefs.isInitialized) return
            prefs.edit().putString(KEY_AUTH_DIAG, value.orEmpty()).apply()
        }

    /**
     * Último lugar onde ele estava — pra voltar exatamente ali, não pro começo.
     *
     * São três pistas: a aba, a conversa aberta (null = não estava numa) e a âncora de
     * leitura dentro dela. A âncora guarda «conversa|mensagem»: se ele estava no fim do
     * histórico não guardamos nada, porque o fim é justamente onde o app já abre.
     */
    var ultimaAba: String?
        get() = texto(KEY_ULTIMA_ABA)
        set(value) = setTexto(KEY_ULTIMA_ABA, value)

    var ultimaConversa: String?
        get() = texto(KEY_ULTIMA_CONVERSA)
        set(value) = setTexto(KEY_ULTIMA_CONVERSA, value)

    /**
     * Seção Finanças da gaveta expandida? Default fechado.
     * Persiste o gesto — colapsar/expandir não reseta a cada abertura do app.
     */
    var gavetaFinancasAberta: Boolean
        get() = if (::prefs.isInitialized) prefs.getBoolean(KEY_GAVETA_FINANCAS, false) else false
        set(value) {
            if (!::prefs.isInitialized) return
            prefs.edit().putBoolean(KEY_GAVETA_FINANCAS, value).apply()
        }

    /** Conversa dedicada «Finanças» — FAB da Luna nas telas do módulo. */
    var conversaFinancas: String?
        get() = texto(KEY_CONVERSA_FINANCAS)
        set(value) = setTexto(KEY_CONVERSA_FINANCAS, value)

    /**
     * Card «O que a Luna ganhou» na Início já foi dispensado?
     * Chave V1 — um capítulo futuro pode usar V2 e reaparecer sem apagar o V1.
     */
    var showcaseLunaV1Dismissed: Boolean
        get() = if (::prefs.isInitialized) prefs.getBoolean(KEY_SHOWCASE_LUNA_V1, false) else false
        set(value) {
            if (!::prefs.isInitialized) return
            prefs.edit().putBoolean(KEY_SHOWCASE_LUNA_V1, value).apply()
        }

    /** Mensagem no topo da tela quando ele saiu de [conversaId] — null se estava no fim. */
    fun ancoraDe(conversaId: String): String? {
        val bruto = texto(KEY_ANCORA) ?: return null
        val corte = bruto.indexOf('|')
        if (corte <= 0) return null
        if (bruto.substring(0, corte) != conversaId) return null
        return bruto.substring(corte + 1).takeIf { it.isNotBlank() }
    }

    fun setAncora(conversaId: String, mensagemId: String?) {
        setTexto(KEY_ANCORA, if (mensagemId == null) null else "$conversaId|$mensagemId")
    }

    /** CPF/CNPJ salvo (só dígitos) pra pré-preencher o checkout — espelha o `cpfCnpj.ts` do RN. */
    var cpfCnpjSalvo: String?
        get() = texto(KEY_CPF_CNPJ)
        set(value) = setTexto(KEY_CPF_CNPJ, value)

    private fun texto(chave: String): String? =
        if (::prefs.isInitialized) prefs.getString(chave, null)?.takeIf { it.isNotBlank() } else null

    private fun setTexto(chave: String, value: String?) {
        if (!::prefs.isInitialized) return
        val ed = prefs.edit()
        if (value.isNullOrBlank()) ed.remove(chave) else ed.putString(chave, value)
        ed.apply()
    }

    fun setVibracao(enabled: Boolean) {
        _vibracao.value = enabled
        prefs.edit().putBoolean(KEY_VIBRACAO, enabled).apply()
    }

    fun setPesquisaProfunda(enabled: Boolean) {
        _pesquisaProfunda.value = enabled
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_PESQUISA_PROFUNDA, enabled).apply()
    }

    fun setModoTecnico(enabled: Boolean) {
        _modoTecnico.value = enabled
        // Exclusivo com «Mãos à obra»: ligar o técnico apaga o agêntico (o seletor é escolha-uma).
        if (enabled) _modoAgentico.value = false
        if (::prefs.isInitialized) {
            prefs.edit()
                .putBoolean(KEY_MODO_TECNICO, enabled)
                .apply { if (enabled) putBoolean(KEY_MODO_AGENTICO, false) }
                .apply()
        }
    }

    fun setModoAgentico(enabled: Boolean) {
        _modoAgentico.value = enabled
        // Exclusivo com o técnico: ligar o agêntico apaga o técnico.
        if (enabled) _modoTecnico.value = false
        if (::prefs.isInitialized) {
            prefs.edit()
                .putBoolean(KEY_MODO_AGENTICO, enabled)
                .apply { if (enabled) putBoolean(KEY_MODO_TECNICO, false) }
                .apply()
        }
    }

    fun setContextoComercial(enabled: Boolean) {
        _contextoComercial.value = enabled
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_CONTEXTO_COMERCIAL, enabled).apply()
    }

    fun setLocalizacaoAtiva(enabled: Boolean) {
        _localizacaoAtiva.value = enabled
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_LOCALIZACAO, enabled).apply()
    }

    fun setBolhaAtiva(enabled: Boolean) {
        _bolhaAtiva.value = enabled
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_BOLHA_ATIVA, enabled).apply()
    }

    /** Persiste canto + altura da bolha após snap (ou ao religar). */
    fun setBolhaPosicao(ladoEsquerdo: Boolean, y: Int) {
        bolhaLadoEsquerdo = ladoEsquerdo
        bolhaY = y.coerceAtLeast(0)
        if (::prefs.isInitialized) {
            prefs.edit()
                .putBoolean(KEY_BOLHA_LADO_ESQ, bolhaLadoEsquerdo)
                .putInt(KEY_BOLHA_Y, bolhaY)
                .apply()
        }
    }

    fun setBolhaOnboardingVisto(visto: Boolean) {
        bolhaOnboardingVisto = visto
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_BOLHA_ONBOARDING, visto).apply()
    }

    fun setBolhaLastLunaMsgId(id: String?) {
        bolhaLastLunaMsgId = id
        if (::prefs.isInitialized) {
            if (id == null) prefs.edit().remove(KEY_BOLHA_LAST_LUNA_MSG).apply()
            else prefs.edit().putString(KEY_BOLHA_LAST_LUNA_MSG, id).apply()
        }
    }

    /** Último local/clima captado (JSON) — pra Luna ter o «onde» sem esperar um fix novo. */
    var localSnapshot: String?
        get() = texto(KEY_LOCAL_SNAPSHOT)
        set(value) = setTexto(KEY_LOCAL_SNAPSHOT, value)

    /** Limpa prefs locais (após «apagar dados»). */
    fun reset() {
        prefs.edit().clear().apply()
        _vibracao.value = true
        _pesquisaProfunda.value = false
        _modoTecnico.value = false
        _modoAgentico.value = false
        _localizacaoAtiva.value = false
        _bolhaAtiva.value = false
        bolhaLadoEsquerdo = true
        bolhaY = BOLHA_Y_DEFAULT
        bolhaOnboardingVisto = false
        bolhaLastLunaMsgId = null
        idsTecnico.clear()
    }
}
