package com.ethan.orbitlab.data.local

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import androidx.core.content.ContextCompat
import com.ethan.orbitlab.data.PrefsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

/** Clima atual + previsão do dia, já reduzido ao que a Luna e o card usam. */
data class ClimaLab(
    val tempC: Double? = null,
    val sensacaoC: Double? = null,
    val umidade: Int? = null,
    val ventoKmh: Double? = null,
    val codigo: Int? = null,
    val descricao: String? = null,
    val maxC: Double? = null,
    val minC: Double? = null,
    val chuvaProb: Int? = null,
    val chuvaMm: Double? = null,
    /** Próximos dias (amanhã em diante) — pra Luna responder "vai chover amanhã?". */
    val previsao: List<DiaPrevisao> = emptyList(),
)

/** Um dia da previsão adiante. O rótulo ("amanhã", "sábado") já sai calculado no fuso do aparelho. */
data class DiaPrevisao(
    val rotulo: String,
    val maxC: Double? = null,
    val minC: Double? = null,
    val chuvaProb: Int? = null,
    val codigo: Int? = null,
    val descricao: String? = null,
)

/** Onde a pessoa está + o tempo lá agora. É o «onde» que faltava à Luna (ela só tinha o «quando»). */
data class LocalLab(
    val lat: Double? = null,
    val lon: Double? = null,
    val cidade: String? = null,
    val uf: String? = null,
    val pais: String? = null,
    val clima: ClimaLab? = null,
    /** Quando este retrato foi tirado (epoch ms) — pra saber se está fresco. */
    val atualizadoEm: Long = System.currentTimeMillis(),
)

/**
 * Localização + clima do aparelho, opt-in por permissão.
 *
 * Framework puro: [LocationManager] pro fix e [Geocoder] pra cidade/uf — sem play-services,
 * sem dependência nova. O clima vem do Open-Meteo (grátis, sem chave). Tudo aqui é best-effort:
 * se faltar permissão, GPS ou rede, a gente simplesmente não atualiza e a Luna segue só com o
 * relógio. O último retrato fica salvo em prefs, então o card aparece na hora na próxima abertura.
 */
object LocationRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val http = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val _atual = MutableStateFlow<LocalLab?>(null)
    val atual: StateFlow<LocalLab?> = _atual.asStateFlow()

    /** Carregando um fix agora — pro card mostrar o spinnerzinho. */
    private val _carregando = MutableStateFlow(false)
    val carregando: StateFlow<Boolean> = _carregando.asStateFlow()

    @Volatile private var ultimoFetch = 0L
    private const val FRESCOR_MS = 10 * 60 * 1000L // 10 min: abaixo disso, não re-busca à toa.

    fun init(context: Context) {
        if (_atual.value != null) return
        PrefsRepository.localSnapshot?.let { raw ->
            runCatching { desserializar(JSONObject(raw)) }.getOrNull()?.let { _atual.value = it }
        }
    }

    fun temPermissao(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Dispara uma atualização em segundo plano (fire-and-forget). Respeita o toggle, a permissão
     * e o [FRESCOR_MS] — a não ser que [forcar] mande buscar de novo (ex.: o usuário acabou de
     * ligar o clima). Seguro de chamar de qualquer tela; não trava a UI.
     */
    fun atualizarEmBackground(context: Context, forcar: Boolean = false) {
        if (!PrefsRepository.localizacaoAtiva.value) return
        if (!temPermissao(context)) return
        val agora = System.currentTimeMillis()
        if (!forcar && _atual.value != null && agora - ultimoFetch < FRESCOR_MS) return
        val app = context.applicationContext
        scope.launch { atualizar(app) }
    }

    /** Faz um fix + geocode + clima e publica. Devolve o retrato novo (ou null se não deu). */
    suspend fun atualizar(context: Context): LocalLab? {
        if (!temPermissao(context)) return null
        _carregando.value = true
        try {
            val loc = obterLocalizacao(context) ?: return null
            val (cidade, uf, pais) = geocodificar(context, loc.latitude, loc.longitude)
            val clima = buscarClima(loc.latitude, loc.longitude)
            val retrato = LocalLab(
                lat = loc.latitude,
                lon = loc.longitude,
                cidade = cidade,
                uf = uf,
                pais = pais,
                clima = clima,
            )
            _atual.value = retrato
            ultimoFetch = System.currentTimeMillis()
            PrefsRepository.localSnapshot = serializar(retrato).toString()
            return retrato
        } catch (_: Exception) {
            return null
        } finally {
            _carregando.value = false
        }
    }

    // ── Fix de localização ──────────────────────────────────────────────────────
    // Primeiro o last-known (instantâneo, sem esperar o GPS acordar). Se não houver, pede um
    // update único com timeout curto — sem prender a corrotina se o céu estiver fechado.

    private suspend fun obterLocalizacao(context: Context): Location? = withContext(Dispatchers.IO) {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return@withContext null
        val providers = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER,
        ).filter { runCatching { lm.isProviderEnabled(it) }.getOrDefault(false) }

        val conhecido = providers
            .mapNotNull { p -> runCatching { lm.getLastKnownLocation(p) }.getOrNull() }
            .maxByOrNull { it.time }
        // Um last-known de até ~30 min serve de sobra pra clima/cidade.
        if (conhecido != null && System.currentTimeMillis() - conhecido.time < 30 * 60 * 1000L) {
            return@withContext conhecido
        }

        val provider = when {
            LocationManager.NETWORK_PROVIDER in providers -> LocationManager.NETWORK_PROVIDER
            LocationManager.GPS_PROVIDER in providers -> LocationManager.GPS_PROVIDER
            else -> return@withContext conhecido
        }

        val novo = withTimeoutOrNull(8000L) {
            suspendCancellableCoroutine<Location?> { cont ->
                val listener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        runCatching { lm.removeUpdates(this) }
                        if (cont.isActive) cont.resume(location)
                    }

                    @Deprecated("Compat com API < 30")
                    override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {}
                    override fun onProviderEnabled(provider: String) {}
                    override fun onProviderDisabled(provider: String) {}
                }
                try {
                    lm.requestLocationUpdates(provider, 0L, 0f, listener, Looper.getMainLooper())
                    cont.invokeOnCancellation { runCatching { lm.removeUpdates(listener) } }
                } catch (_: SecurityException) {
                    if (cont.isActive) cont.resume(null)
                } catch (_: Exception) {
                    if (cont.isActive) cont.resume(null)
                }
            }
        }
        novo ?: conhecido
    }

    // ── Cidade / UF ─────────────────────────────────────────────────────────────

    private data class Lugar(val cidade: String?, val uf: String?, val pais: String?)

    @Suppress("DEPRECATION")
    private suspend fun geocodificar(context: Context, lat: Double, lon: Double): Lugar =
        withContext(Dispatchers.IO) {
            if (!Geocoder.isPresent()) return@withContext Lugar(null, null, null)
            try {
                val geo = Geocoder(context, Locale("pt", "BR"))
                // A versão assíncrona (API 33+) evita o warning, mas a síncrona ainda funciona e é
                // bem mais simples de encadear aqui; envolvida em IO + try, o strict-mode não morde.
                val addr = geo.getFromLocation(lat, lon, 1)?.firstOrNull() ?: return@withContext Lugar(null, null, null)
                val cidade = addr.locality
                    ?: addr.subAdminArea
                    ?: addr.adminArea
                Lugar(
                    cidade = cidade?.trim()?.takeIf { it.isNotEmpty() },
                    uf = addr.adminArea?.trim()?.takeIf { it.isNotEmpty() },
                    pais = addr.countryName?.trim()?.takeIf { it.isNotEmpty() },
                )
            } catch (_: Exception) {
                Lugar(null, null, null)
            }
        }

    // ── Clima (Open-Meteo) ──────────────────────────────────────────────────────

    private suspend fun buscarClima(lat: Double, lon: Double): ClimaLab? = withContext(Dispatchers.IO) {
        // forecast_days=4 pra ela alcançar "amanhã" (e uns dias além); weather_code no daily
        // dá o tempo predominante de cada dia adiante.
        val url = "https://api.open-meteo.com/v1/forecast" +
            "?latitude=$lat&longitude=$lon" +
            "&current=temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,wind_speed_10m" +
            "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max,precipitation_sum" +
            "&timezone=auto&forecast_days=4"
        try {
            val req = Request.Builder().url(url).header("Accept", "application/json").get().build()
            http.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext null
                val json = JSONObject(resp.body?.string().orEmpty())
                val cur = json.optJSONObject("current")
                val daily = json.optJSONObject("daily")
                val codigo = cur?.optInt("weather_code", -1)?.takeIf { it >= 0 }
                // Índice 0 = hoje; 1 em diante = amanhã, depois… O rótulo já sai em pt-BR
                // pra Luna não ter que adivinhar que dia da semana é.
                val previsao = mutableListOf<DiaPrevisao>()
                val nDias = daily?.optJSONArray("temperature_2m_max")?.length() ?: 0
                for (i in 1 until nDias) {
                    val cod = daily?.intEm("weather_code", i)
                    previsao.add(
                        DiaPrevisao(
                            rotulo = rotuloDia(i),
                            maxC = daily?.doubleEm("temperature_2m_max", i),
                            minC = daily?.doubleEm("temperature_2m_min", i),
                            chuvaProb = daily?.doubleEm("precipitation_probability_max", i)?.let { Math.round(it).toInt() },
                            codigo = cod,
                            descricao = cod?.let(::descricaoWMO),
                        ),
                    )
                }
                ClimaLab(
                    tempC = cur?.optDoubleOrNull("temperature_2m"),
                    sensacaoC = cur?.optDoubleOrNull("apparent_temperature"),
                    umidade = cur?.optDoubleOrNull("relative_humidity_2m")?.let { Math.round(it).toInt() },
                    ventoKmh = cur?.optDoubleOrNull("wind_speed_10m"),
                    codigo = codigo,
                    descricao = codigo?.let(::descricaoWMO),
                    maxC = daily?.primeiroDouble("temperature_2m_max"),
                    minC = daily?.primeiroDouble("temperature_2m_min"),
                    chuvaProb = daily?.primeiroDouble("precipitation_probability_max")?.let { Math.round(it).toInt() },
                    chuvaMm = daily?.primeiroDouble("precipitation_sum"),
                    previsao = previsao,
                )
            }
        } catch (_: Exception) {
            null
        }
    }

    // ── JSON pro servidor (bate com o schema `local` do mobile-api) ──────────────

    /** Monta o objeto `local` pro corpo do chat, ou null se o toggle está off / não há retrato. */
    fun paraJson(): JSONObject? {
        if (!PrefsRepository.localizacaoAtiva.value) return null
        val l = _atual.value ?: return null
        return serializar(l)
    }

    private fun serializar(l: LocalLab): JSONObject = JSONObject().apply {
        l.lat?.let { put("lat", it) }
        l.lon?.let { put("lon", it) }
        l.cidade?.let { put("cidade", it) }
        l.uf?.let { put("uf", it) }
        l.pais?.let { put("pais", it) }
        l.clima?.let { c ->
            put("clima", JSONObject().apply {
                c.tempC?.let { put("tempC", it) }
                c.sensacaoC?.let { put("sensacaoC", it) }
                c.umidade?.let { put("umidade", it) }
                c.ventoKmh?.let { put("ventoKmh", it) }
                c.codigo?.let { put("codigo", it) }
                c.descricao?.let { put("descricao", it) }
                c.maxC?.let { put("maxC", it) }
                c.minC?.let { put("minC", it) }
                c.chuvaProb?.let { put("chuvaProb", it) }
                c.chuvaMm?.let { put("chuvaMm", it) }
                if (c.previsao.isNotEmpty()) {
                    put("previsao", org.json.JSONArray().apply {
                        c.previsao.forEach { d ->
                            put(JSONObject().apply {
                                put("rotulo", d.rotulo)
                                d.maxC?.let { put("maxC", it) }
                                d.minC?.let { put("minC", it) }
                                d.chuvaProb?.let { put("chuvaProb", it) }
                                d.codigo?.let { put("codigo", it) }
                                d.descricao?.let { put("descricao", it) }
                            })
                        }
                    })
                }
            })
        }
        put("atualizadoEm", l.atualizadoEm)
    }

    private fun desserializar(o: JSONObject): LocalLab {
        val climaObj = o.optJSONObject("clima")
        val clima = climaObj?.let { c ->
            ClimaLab(
                tempC = c.optDoubleOrNull("tempC"),
                sensacaoC = c.optDoubleOrNull("sensacaoC"),
                umidade = c.optIntOrNull("umidade"),
                ventoKmh = c.optDoubleOrNull("ventoKmh"),
                codigo = c.optIntOrNull("codigo"),
                descricao = c.optString("descricao").takeIf { it.isNotBlank() },
                maxC = c.optDoubleOrNull("maxC"),
                minC = c.optDoubleOrNull("minC"),
                chuvaProb = c.optIntOrNull("chuvaProb"),
                chuvaMm = c.optDoubleOrNull("chuvaMm"),
                previsao = c.optJSONArray("previsao")?.let { arr ->
                    (0 until arr.length()).mapNotNull { i ->
                        val d = arr.optJSONObject(i) ?: return@mapNotNull null
                        val rotulo = d.optString("rotulo").takeIf { it.isNotBlank() } ?: return@mapNotNull null
                        DiaPrevisao(
                            rotulo = rotulo,
                            maxC = d.optDoubleOrNull("maxC"),
                            minC = d.optDoubleOrNull("minC"),
                            chuvaProb = d.optIntOrNull("chuvaProb"),
                            codigo = d.optIntOrNull("codigo"),
                            descricao = d.optString("descricao").takeIf { it.isNotBlank() },
                        )
                    }
                } ?: emptyList(),
            )
        }
        return LocalLab(
            lat = o.optDoubleOrNull("lat"),
            lon = o.optDoubleOrNull("lon"),
            cidade = o.optString("cidade").takeIf { it.isNotBlank() },
            uf = o.optString("uf").takeIf { it.isNotBlank() },
            pais = o.optString("pais").takeIf { it.isNotBlank() },
            clima = clima,
            atualizadoEm = o.optLong("atualizadoEm", System.currentTimeMillis()),
        )
    }

    fun limpar() {
        _atual.value = null
        PrefsRepository.localSnapshot = null
    }
}

/** Emoji do tempo pelo código WMO — pro card do Início. */
fun emojiWMO(codigo: Int?): String = when (codigo) {
    null -> "🌡️"
    0 -> "☀️"
    1, 2 -> "🌤️"
    3 -> "☁️"
    45, 48 -> "🌫️"
    in 51..57 -> "🌦️"
    in 61..67 -> "🌧️"
    in 71..77 -> "❄️"
    in 80..82 -> "🌧️"
    85, 86 -> "🌨️"
    in 95..99 -> "⛈️"
    else -> "🌡️"
}

/** Descrição curta em pt-BR pelo código WMO. */
fun descricaoWMO(codigo: Int?): String = when (codigo) {
    0 -> "Céu limpo"
    1 -> "Predominantemente limpo"
    2 -> "Parcialmente nublado"
    3 -> "Nublado"
    45, 48 -> "Nevoeiro"
    51, 53, 55 -> "Garoa"
    56, 57 -> "Garoa congelante"
    61, 63, 65 -> "Chuva"
    66, 67 -> "Chuva congelante"
    71, 73, 75 -> "Neve"
    77 -> "Grãos de neve"
    80, 81, 82 -> "Pancadas de chuva"
    85, 86 -> "Pancadas de neve"
    95 -> "Trovoada"
    96, 99 -> "Trovoada com granizo"
    else -> "Tempo"
}

// Helpers de JSON: optDouble não distingue "ausente" de 0.0, e às vezes o campo vem null.
private fun JSONObject.optDoubleOrNull(key: String): Double? =
    if (has(key) && !isNull(key)) optDouble(key).takeIf { !it.isNaN() } else null

private fun JSONObject.optIntOrNull(key: String): Int? =
    if (has(key) && !isNull(key)) optInt(key) else null

private fun JSONObject.primeiroDouble(key: String): Double? {
    val arr = optJSONArray(key) ?: return null
    if (arr.length() == 0 || arr.isNull(0)) return null
    return arr.optDouble(0).takeIf { !it.isNaN() }
}

// Versões indexadas: pegam o dia `i` dos arrays diários do Open-Meteo (0 = hoje).
private fun JSONObject.doubleEm(key: String, i: Int): Double? {
    val arr = optJSONArray(key) ?: return null
    if (i < 0 || i >= arr.length() || arr.isNull(i)) return null
    return arr.optDouble(i).takeIf { !it.isNaN() }
}

private fun JSONObject.intEm(key: String, i: Int): Int? {
    val arr = optJSONArray(key) ?: return null
    if (i < 0 || i >= arr.length() || arr.isNull(i)) return null
    return arr.optInt(i)
}

/** Rótulo em pt-BR do dia a `offset` dias de hoje: 1 → "amanhã", senão o dia da semana. */
private fun rotuloDia(offset: Int): String {
    if (offset == 1) return "amanhã"
    val ptBR = Locale("pt", "BR")
    val cal = java.util.Calendar.getInstance()
    cal.add(java.util.Calendar.DAY_OF_YEAR, offset)
    return cal.getDisplayName(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.LONG, ptBR)
        ?.lowercase(ptBR)
        ?: "daqui a $offset dias"
}
