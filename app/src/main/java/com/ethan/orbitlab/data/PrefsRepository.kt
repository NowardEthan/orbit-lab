package com.ethan.orbitlab.data

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Preferências locais do lab — espelho leve do AsyncStorage do orbit-mobile.
 */
object PrefsRepository {
    private const val PREFS = "orbitlab_prefs"
    private const val KEYSTORE_ALIAS = "orbitlab_llm_pessoal"
    private const val SECURE_PREFIX = "secure:"
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
    private const val KEY_LOCALIZACAO = "orbit.lab.localizacao.ativa"
    private const val KEY_LOCAL_SNAPSHOT = "orbit.lab.localizacao.snapshot"
    private const val KEY_LLM_PESSOAL_ATIVO = "orbit.lab.llm.pessoal.ativo"
    private const val KEY_LLM_PESSOAL_BASE_URL = "orbit.lab.llm.pessoal.base_url"
    private const val KEY_LLM_PESSOAL_API_KEY = "orbit.lab.llm.pessoal.api_key"
    private const val KEY_LLM_PESSOAL_MODEL = "orbit.lab.llm.pessoal.model"
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
     * Compartilhar localização + clima com a Luna — opt-in, DESLIGADO por default. Ligado,
     * o app capta o GPS, resolve cidade/uf e busca o clima; isso dá à Luna o «onde» (antes
     * ela só tinha o «quando»). Desligado, nada de local sai do aparelho.
     */
    private val _localizacaoAtiva = MutableStateFlow(false)
    val localizacaoAtiva: StateFlow<Boolean> = _localizacaoAtiva.asStateFlow()

    private val _llmPessoalAtivo = MutableStateFlow(false)
    val llmPessoalAtivo: StateFlow<Boolean> = _llmPessoalAtivo.asStateFlow()

    private val _llmPessoalBaseUrl = MutableStateFlow("")
    val llmPessoalBaseUrl: StateFlow<String> = _llmPessoalBaseUrl.asStateFlow()

    private val _llmPessoalApiKey = MutableStateFlow("")
    val llmPessoalApiKey: StateFlow<String> = _llmPessoalApiKey.asStateFlow()

    private val _llmPessoalModel = MutableStateFlow("claude-fable-5")
    val llmPessoalModel: StateFlow<String> = _llmPessoalModel.asStateFlow()

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
        _localizacaoAtiva.value = prefs.getBoolean(KEY_LOCALIZACAO, false)
        _llmPessoalAtivo.value = prefs.getBoolean(KEY_LLM_PESSOAL_ATIVO, false)
        _llmPessoalBaseUrl.value = prefs.getString(KEY_LLM_PESSOAL_BASE_URL, "").orEmpty()
        _llmPessoalApiKey.value = lerTextoSeguro(KEY_LLM_PESSOAL_API_KEY).orEmpty()
        _llmPessoalModel.value = prefs.getString(KEY_LLM_PESSOAL_MODEL, "claude-fable-5").orEmpty()
            .ifBlank { "claude-fable-5" }
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

    fun setLocalizacaoAtiva(enabled: Boolean) {
        _localizacaoAtiva.value = enabled
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_LOCALIZACAO, enabled).apply()
    }

    fun setLlmPessoalAtivo(enabled: Boolean) {
        _llmPessoalAtivo.value = enabled
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_LLM_PESSOAL_ATIVO, enabled).apply()
    }

    fun salvarLlmPessoal(baseUrl: String, apiKey: String, model: String) {
        val url = baseUrl.trim().trimEnd('/')
        val key = apiKey.trim()
        val modelo = model.trim().ifBlank { "claude-fable-5" }
        _llmPessoalBaseUrl.value = url
        _llmPessoalApiKey.value = key
        _llmPessoalModel.value = modelo
        if (::prefs.isInitialized) {
            prefs.edit()
                .putString(KEY_LLM_PESSOAL_BASE_URL, url)
                .putString(KEY_LLM_PESSOAL_API_KEY, protegerTexto(key))
                .putString(KEY_LLM_PESSOAL_MODEL, modelo)
                .apply()
        }
    }

    fun llmPessoalConfigurado(): Boolean =
        _llmPessoalBaseUrl.value.startsWith("http") &&
            _llmPessoalApiKey.value.isNotBlank() &&
            _llmPessoalModel.value.isNotBlank()

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

    private fun chaveSecreta(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getEntry(KEYSTORE_ALIAS, null) as? KeyStore.SecretKeyEntry)?.secretKey?.let {
            return it
        }
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            "AndroidKeyStore",
        )
        keyGenerator.init(
            KeyGenParameterSpec.Builder(
                KEYSTORE_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build(),
        )
        return keyGenerator.generateKey()
    }

    private fun protegerTexto(valor: String): String {
        if (valor.isBlank()) return ""
        return runCatching {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, chaveSecreta())
            val iv = cipher.iv
            val cifrado = cipher.doFinal(valor.toByteArray(Charsets.UTF_8))
            SECURE_PREFIX +
                Base64.encodeToString(iv, Base64.NO_WRAP) +
                ":" +
                Base64.encodeToString(cifrado, Base64.NO_WRAP)
        }.getOrDefault("")
    }

    private fun lerTextoSeguro(chave: String): String? {
        val bruto = if (::prefs.isInitialized) prefs.getString(chave, null) else null
        if (bruto.isNullOrBlank()) return null
        if (!bruto.startsWith(SECURE_PREFIX)) return bruto
        return runCatching {
            val partes = bruto.removePrefix(SECURE_PREFIX).split(":", limit = 2)
            val iv = Base64.decode(partes.getOrNull(0).orEmpty(), Base64.NO_WRAP)
            val cifrado = Base64.decode(partes.getOrNull(1).orEmpty(), Base64.NO_WRAP)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, chaveSecreta(), GCMParameterSpec(128, iv))
            String(cipher.doFinal(cifrado), Charsets.UTF_8)
        }.getOrNull()
    }

    /** Limpa prefs locais (após «apagar dados»). */
    fun reset() {
        prefs.edit().clear().apply()
        _vibracao.value = true
        _pesquisaProfunda.value = false
        _modoTecnico.value = false
        _modoAgentico.value = false
        _localizacaoAtiva.value = false
        _llmPessoalAtivo.value = false
        _llmPessoalBaseUrl.value = ""
        _llmPessoalApiKey.value = ""
        _llmPessoalModel.value = "claude-fable-5"
        _bolhaAtiva.value = false
        bolhaLadoEsquerdo = true
        bolhaY = BOLHA_Y_DEFAULT
        bolhaOnboardingVisto = false
        bolhaLastLunaMsgId = null
        idsTecnico.clear()
    }
}
