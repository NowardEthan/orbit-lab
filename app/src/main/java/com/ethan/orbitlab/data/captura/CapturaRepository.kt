package com.ethan.orbitlab.data.captura

import android.content.Context
import android.content.SharedPreferences
import com.ethan.orbitlab.data.financas.Carteira
import com.ethan.orbitlab.data.financas.FinancasRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/**
 * Estado da captura — tudo no aparelho até o usuário confirmar o lançamento.
 * Pendentes e vistos persistem em SharedPreferences (não somem ao matar o app).
 */
object CapturaRepository {
    private const val PREFS = "orbitlab_captura"
    private const val KEY_CONSENT = "consentimento"
    private const val KEY_ATIVA = "desejada"
    private const val KEY_ULTIMOS = "ultimo_aviso_por_banco"
    private const val KEY_ULTIMA = "ultima_captura_ms"
    private const val KEY_PENDENTES = "pendentes_json"
    private const val KEY_VISTOS = "vistos_json"
    private const val MAX_PENDENTES = 40
    private const val MAX_VISTOS = 120

    private lateinit var prefs: SharedPreferences
    private var appContext: Context? = null

    private val _consentimento = MutableStateFlow(false)
    val consentimento: StateFlow<Boolean> = _consentimento.asStateFlow()

    private val _desejada = MutableStateFlow(false)
    val desejada: StateFlow<Boolean> = _desejada.asStateFlow()

    private val _pendentes = MutableStateFlow<List<SugestaoCaptura>>(emptyList())
    val pendentes: StateFlow<List<SugestaoCaptura>> = _pendentes.asStateFlow()

    private val _ultimaCapturaMs = MutableStateFlow<Long?>(null)
    val ultimaCapturaMs: StateFlow<Long?> = _ultimaCapturaMs.asStateFlow()

    private val vistos = LinkedHashSet<String>()

    fun init(context: Context) {
        if (::prefs.isInitialized) return
        appContext = context.applicationContext
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        _consentimento.value = prefs.getBoolean(KEY_CONSENT, false)
        _desejada.value = prefs.getBoolean(KEY_ATIVA, false)
        _ultimaCapturaMs.value = prefs.getLong(KEY_ULTIMA, 0L).takeIf { it > 0 }
        vistos.clear()
        vistos.addAll(carregarVistos())
        _pendentes.value = carregarPendentes()
    }

    fun aceitarConsentimento() {
        _consentimento.value = true
        prefs.edit().putBoolean(KEY_CONSENT, true).apply()
    }

    fun setDesejada(ligada: Boolean) {
        _desejada.value = ligada
        prefs.edit().putBoolean(KEY_ATIVA, ligada).apply()
    }

    fun podeCapturar(): Boolean = _consentimento.value && _desejada.value

    fun onNotificacao(
        pacote: String,
        titulo: String?,
        texto: String?,
        quandoMs: Long,
        notifKey: String,
    ) {
        if (!podeCapturar()) return
        if (notifKey in vistos) return
        marcarVisto(notifKey)

        val aviso = CapturaParser.parsear(pacote, titulo, texto, quandoMs, notifKey) ?: return
        registrarUltimoAviso(aviso.bancoId, aviso.quandoMs)
        _ultimaCapturaMs.value = aviso.quandoMs
        prefs.edit().putLong(KEY_ULTIMA, aviso.quandoMs).apply()

        val carteiras = FinancasRepository.carteiras.value
        val sugestao = SugestaoCaptura(
            aviso = aviso,
            categoriaId = CapturaParser.palpiteCategoria(aviso.descricao),
            carteiraId = palpiteCarteira(aviso.bancoRotulo, carteiras),
        )
        setPendentes((_pendentes.value + sugestao).takeLast(MAX_PENDENTES))
    }

    fun simularDemo() {
        val aviso = CapturaParser.exemploDemo()
        registrarUltimoAviso(aviso.bancoId, aviso.quandoMs)
        _ultimaCapturaMs.value = aviso.quandoMs
        prefs.edit().putLong(KEY_ULTIMA, aviso.quandoMs).apply()
        val carteiras = FinancasRepository.carteiras.value
        val sugestao = SugestaoCaptura(
            aviso = aviso,
            categoriaId = CapturaParser.palpiteCategoria(aviso.descricao),
            carteiraId = palpiteCarteira(aviso.bancoRotulo, carteiras),
        )
        setPendentes((_pendentes.value + sugestao).takeLast(MAX_PENDENTES))
    }

    fun ignorar(id: String) {
        setPendentes(_pendentes.value.filter { it.aviso.id != id })
    }

    fun consumir(id: String): SugestaoCaptura? {
        val s = _pendentes.value.find { it.aviso.id == id } ?: return null
        setPendentes(_pendentes.value.filter { it.aviso.id != id })
        return s
    }

    fun statusGeral(context: Context): CapturaStatusGeral {
        val ultimos = carregarUltimos()
        val agora = System.currentTimeMillis()
        val quietoMs = CapturaRegras.DIAS_QUIETO * 24L * 60 * 60 * 1000
        val bancos = CapturaRegras.bancos.map { b ->
            val ultimo = ultimos[b.id]
            val saude = when {
                ultimo == null -> SaudeBanco.NUNCA
                agora - ultimo > quietoMs -> SaudeBanco.QUIETO
                else -> SaudeBanco.OK
            }
            val detalhe = when (saude) {
                SaudeBanco.OK -> "Escutando"
                SaudeBanco.NUNCA -> "Ainda sem aviso"
                SaudeBanco.QUIETO -> {
                    val dias = ((agora - (ultimo ?: agora)) / (24L * 60 * 60 * 1000)).toInt()
                    "Nenhum aviso há ${dias}d"
                }
            }
            StatusBancoCaptura(b, ultimo, saude, detalhe)
        }
        return CapturaStatusGeral(
            consentimento = _consentimento.value,
            listenerAtivo = CapturaPermissoes.listenerAtivo(context),
            bateriaOk = CapturaPermissoes.bateriaIgnorada(context),
            ultimaCapturaMs = _ultimaCapturaMs.value,
            pendentes = _pendentes.value.size,
            bancos = bancos,
        )
    }

    fun palpiteCarteira(bancoRotulo: String, carteiras: List<Carteira>): String? {
        if (carteiras.isEmpty()) return null
        val alvo = bancoRotulo.lowercase()
        val match = carteiras.firstOrNull { c ->
            !c.arquivada && (
                c.banco?.lowercase()?.contains(alvo.take(4)) == true ||
                    c.apelido.lowercase().contains(alvo.take(4)) ||
                    (alvo.contains("nubank") && (
                        c.banco?.contains("nubank", true) == true ||
                            c.apelido.contains("nubank", true)
                        ))
                )
        }
        return match?.id ?: carteiras.firstOrNull { !it.arquivada }?.id
    }

    private fun setPendentes(lista: List<SugestaoCaptura>) {
        _pendentes.value = lista
        salvarPendentes(lista)
    }

    private fun salvarPendentes(lista: List<SugestaoCaptura>) {
        if (!::prefs.isInitialized) return
        val arr = JSONArray()
        for (s in lista) {
            arr.put(
                JSONObject()
                    .put("avisoId", s.aviso.id)
                    .put("bancoId", s.aviso.bancoId)
                    .put("bancoRotulo", s.aviso.bancoRotulo)
                    .put("pacote", s.aviso.pacote)
                    .put("valorCentavos", s.aviso.valorCentavos)
                    .put("descricao", s.aviso.descricao)
                    .put("quandoMs", s.aviso.quandoMs)
                    .put("raw", s.aviso.raw)
                    .put("tipo", s.aviso.tipo)
                    .put("categoriaId", s.categoriaId)
                    .put("carteiraId", s.carteiraId)
                    .put("criadaEmMs", s.criadaEmMs),
            )
        }
        prefs.edit().putString(KEY_PENDENTES, arr.toString()).apply()
    }

    private fun carregarPendentes(): List<SugestaoCaptura> {
        val raw = prefs.getString(KEY_PENDENTES, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val aviso = CapturaAviso(
                        id = o.getString("avisoId"),
                        bancoId = o.optString("bancoId"),
                        bancoRotulo = o.optString("bancoRotulo"),
                        pacote = o.optString("pacote"),
                        valorCentavos = o.optLong("valorCentavos"),
                        descricao = o.optString("descricao"),
                        quandoMs = o.optLong("quandoMs"),
                        raw = o.optString("raw"),
                        tipo = o.optString("tipo", "saida"),
                    )
                    add(
                        SugestaoCaptura(
                            aviso = aviso,
                            categoriaId = o.optString("categoriaId"),
                            carteiraId = o.optString("carteiraId").takeIf { it.isNotBlank() },
                            criadaEmMs = o.optLong("criadaEmMs", System.currentTimeMillis()),
                        ),
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun registrarUltimoAviso(bancoId: String, ms: Long) {
        val map = carregarUltimos().toMutableMap()
        map[bancoId] = ms
        val json = JSONObject()
        for ((k, v) in map) json.put(k, v)
        prefs.edit().putString(KEY_ULTIMOS, json.toString()).apply()
    }

    private fun carregarUltimos(): Map<String, Long> {
        val raw = prefs.getString(KEY_ULTIMOS, null) ?: return emptyMap()
        return runCatching {
            val json = JSONObject(raw)
            buildMap {
                val keys = json.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    put(k, json.optLong(k, 0L))
                }
            }.filterValues { it > 0 }
        }.getOrDefault(emptyMap())
    }

    private fun marcarVisto(key: String) {
        vistos.add(key)
        while (vistos.size > MAX_VISTOS) {
            val it = vistos.iterator()
            if (it.hasNext()) {
                it.next()
                it.remove()
            } else break
        }
        salvarVistos()
    }

    private fun salvarVistos() {
        if (!::prefs.isInitialized) return
        val arr = JSONArray()
        for (v in vistos) arr.put(v)
        prefs.edit().putString(KEY_VISTOS, arr.toString()).apply()
    }

    private fun carregarVistos(): List<String> {
        val raw = prefs.getString(KEY_VISTOS, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) add(arr.getString(i))
            }
        }.getOrDefault(emptyList())
    }
}
