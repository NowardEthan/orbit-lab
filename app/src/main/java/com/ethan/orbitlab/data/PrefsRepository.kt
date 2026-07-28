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
    private const val KEY_SESSAO_UID = "orbit.lab.sessao.uid"
    private const val KEY_AUTH_DIAG = "orbit.lab.sessao.diag"
    private const val KEY_ULTIMA_ABA = "orbit.lab.lugar.aba"
    private const val KEY_ULTIMA_CONVERSA = "orbit.lab.lugar.conversa"
    private const val KEY_ANCORA = "orbit.lab.lugar.ancora"
    private const val KEY_LOCALIZACAO = "orbit.lab.localizacao.ativa"
    private const val KEY_LOCAL_SNAPSHOT = "orbit.lab.localizacao.snapshot"

    private lateinit var prefs: SharedPreferences

    /** Raciocínio SEMPRE visível — não é mais opção do usuário. */
    val reasoningEnabled: StateFlow<Boolean> = MutableStateFlow(true).asStateFlow()

    /**
     * Chat SEMPRE pelo servidor Luna (Railway/core) — a Luna de verdade, com memória e
     * ferramentas. O OpenRouter direto foi aposentado (era uma Luna oca e dava pra esquecer ligada).
     */
    val lunaDirectEnabled: StateFlow<Boolean> = MutableStateFlow(false).asStateFlow()

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
     * Compartilhar localização + clima com a Luna — opt-in, DESLIGADO por default. Ligado,
     * o app capta o GPS, resolve cidade/uf e busca o clima; isso dá à Luna o «onde» (antes
     * ela só tinha o «quando»). Desligado, nada de local sai do aparelho.
     */
    private val _localizacaoAtiva = MutableStateFlow(false)
    val localizacaoAtiva: StateFlow<Boolean> = _localizacaoAtiva.asStateFlow()

    fun init(context: Context) {
        if (::prefs.isInitialized) return
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        _vibracao.value = prefs.getBoolean(KEY_VIBRACAO, true)
        _pesquisaProfunda.value = prefs.getBoolean(KEY_PESQUISA_PROFUNDA, false)
        _localizacaoAtiva.value = prefs.getBoolean(KEY_LOCALIZACAO, false)
    }

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

    fun setLocalizacaoAtiva(enabled: Boolean) {
        _localizacaoAtiva.value = enabled
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_LOCALIZACAO, enabled).apply()
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
        _localizacaoAtiva.value = false
    }
}
