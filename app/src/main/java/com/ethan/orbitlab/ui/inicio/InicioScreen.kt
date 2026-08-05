package com.ethan.orbitlab.ui.inicio

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Umbrella
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.ethan.orbitlab.BuildConfig
import com.ethan.orbitlab.data.updates.ApkInstaller
import com.ethan.orbitlab.data.updates.UpdatesRepository
import com.ethan.orbitlab.data.updates.compareVersions
import com.ethan.orbitlab.data.updates.isNewer
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethan.orbitlab.data.AuthRepository
import com.ethan.orbitlab.data.ChatRepository
import com.ethan.orbitlab.data.Conversa
import com.ethan.orbitlab.data.PrefsRepository
import com.ethan.orbitlab.data.UserProfileRepository
import com.ethan.orbitlab.data.financas.FinancasRepository
import com.ethan.orbitlab.data.financas.PeriodoExtrato
import com.ethan.orbitlab.data.financas.faixaDoPeriodo
import com.ethan.orbitlab.data.financas.filtrarPorPeriodo
import com.ethan.orbitlab.data.financas.filtrarPorPeriodoAteHoje
import com.ethan.orbitlab.data.financas.formatarReais
import com.ethan.orbitlab.data.financas.lancamentosFuturos
import com.ethan.orbitlab.data.financas.resumoDoPeriodo
import com.ethan.orbitlab.data.firebase.DocumentoUi
import com.ethan.orbitlab.data.firebase.FirestoreDocumentos
import com.ethan.orbitlab.data.local.DiaPrevisao
import com.ethan.orbitlab.data.local.LocalLab
import com.ethan.orbitlab.data.local.LocationRepository
import com.ethan.orbitlab.data.local.emojiWMO
import kotlin.math.roundToInt
import com.ethan.orbitlab.ui.theme.Bricolage
import com.ethan.orbitlab.ui.theme.OrbitMetrics
import com.ethan.orbitlab.ui.theme.OrbitTokens
import com.ethan.orbitlab.ui.theme.orbitPressable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InicioScreen(
    onAbrirConversa: (String) -> Unit = {},
    onNovaConversa: () -> Unit = {},
    onNovaConversaComTexto: (String) -> Unit = {},
    onAbrirFinancas: () -> Unit = {},
    onAbrirEstante: () -> Unit = {},
    idleAtivo: Boolean = true,
) {
    val density = LocalDensity.current
    val context = LocalContext.current
    val session by AuthRepository.session.collectAsState()
    val profile by UserProfileRepository.profile.collectAsState()
    val conversas by ChatRepository.conversas.collectAsState()
    val lancamentos by FinancasRepository.lancamentos.collectAsState()
    val manifest by UpdatesRepository.manifest.collectAsState()
    val updateAvailable = remember(manifest) {
        val m = manifest ?: return@remember false
        val latestCode = m.latestVersionCode
        if (latestCode != null) latestCode > BuildConfig.VERSION_CODE
        else isNewer(m.latestVersion, BuildConfig.VERSION_NAME)
    }
    val latestVersion = manifest?.latestVersion
    val apkUrl = manifest?.apkUrl
    val mandatory = remember(manifest, updateAvailable) {
        val m = manifest ?: return@remember false
        if (!updateAvailable) return@remember false
        if (m.mandatory) return@remember true
        val min = m.minSupportedVersion ?: return@remember false
        compareVersions(BuildConfig.VERSION_NAME, min) < 0
    }
    var downloading by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }

    val localizacaoAtiva by PrefsRepository.localizacaoAtiva.collectAsState()
    val clima by LocationRepository.atual.collectAsState()
    val climaCarregando by LocationRepository.carregando.collectAsState()
    val scope = rememberCoroutineScope()
    var refreshManual by remember { mutableStateOf(false) }
    val permClima = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { concessoes ->
        if (concessoes.values.any { it }) {
            PrefsRepository.setLocalizacaoAtiva(true)
            LocationRepository.atualizarEmBackground(context, forcar = true)
        }
    }
    // Enquanto o Início está aberto e o clima ligado, o card se atualiza SOZINHO — sem precisar
    // tocar nele. Dispara ao abrir e depois a cada 10 min (o repositório respeita o frescor lá
    // dentro, então isto nunca vira uma enxurrada de chamadas de rede).
    LaunchedEffect(localizacaoAtiva) {
        if (!localizacaoAtiva) return@LaunchedEffect
        while (true) {
            LocationRepository.atualizarEmBackground(context)
            delay(10 * 60 * 1000L)
        }
    }

    val nomeCompleto = profile.displayName.ifBlank { session?.displayName.orEmpty() }
    val nome = nomeCompleto.substringBefore(' ').ifBlank { nomeCompleto }.ifBlank { "você" }
    val saudacao = remember { saudacaoDoDia() }
    val recentes = remember(conversas) { conversas.take(5) }
    val conversaPrincipal = remember(recentes) {
        recentes.firstOrNull { !ChatRepository.ehConversaFinancas(it.id) } ?: recentes.firstOrNull()
    }
    val agora = System.currentTimeMillis()
    val faixaMes = faixaDoPeriodo(PeriodoExtrato.MES, agora)
    val resumoMes = remember(lancamentos, faixaMes, agora) {
        resumoDoPeriodo(filtrarPorPeriodoAteHoje(lancamentos, faixaMes, agora))
    }
    val planejadosMes = remember(lancamentos, faixaMes, agora) {
        filtrarPorPeriodo(lancamentosFuturos(lancamentos, agora), faixaMes)
    }
    var artefatos by remember { mutableStateOf<List<DocumentoUi>>(emptyList()) }
    val uid = session?.uid
    var showcaseDismissed by remember {
        mutableStateOf(PrefsRepository.showcaseLunaV1Dismissed)
    }
    LaunchedEffect(uid) {
        if (uid == null) {
            artefatos = emptyList()
            return@LaunchedEffect
        }
        val reg = FirestoreDocumentos.subscribeTodos(
            uid = uid,
            onChange = { artefatos = it },
            onError = { artefatos = emptyList() },
        )
        try {
            awaitCancellation()
        } finally {
            reg.remove()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Transparent)) {
        InicioOrbs(idleAtivo = idleAtivo, density = density)

        PullToRefreshBox(
            isRefreshing = refreshManual,
            onRefresh = {
                refreshManual = true
                scope.launch {
                    if (localizacaoAtiva) LocationRepository.atualizar(context)
                    UpdatesRepository.refresh(force = true)
                    refreshManual = false
                }
            },
            modifier = Modifier.fillMaxSize(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(
                        top = 8.dp,
                        start = OrbitMetrics.pagePadding,
                        end = OrbitMetrics.pagePadding,
                        bottom = 108.dp,
                    ),
                verticalArrangement = Arrangement.spacedBy(OrbitMetrics.sectionGap),
            ) {
                if (updateAvailable && !apkUrl.isNullOrBlank()) {
                    UpdateBanner(
                        version = latestVersion,
                        mandatory = mandatory,
                        downloading = downloading,
                        progress = progress,
                        onUpdate = {
                            if (downloading) return@UpdateBanner
                            val url = apkUrl ?: return@UpdateBanner
                            downloading = true
                            progress = 0f
                            ChatRepository.launch {
                                try {
                                    ApkInstaller.downloadAndInstall(context, url) { progress = it }
                                } catch (_: Exception) {
                                    ApkInstaller.openInBrowser(context, url)
                                } finally {
                                    downloading = false
                                    progress = 0f
                                }
                            }
                        },
                    )
                }
                HeaderSecao(
                    saudacao = saudacao,
                    nome = nome,
                )
                LunaSecao(
                    linha = remember(saudacao, clima) { linhaDaLuna(saudacao, clima?.clima) },
                    temUltima = recentes.isNotEmpty(),
                    onFalar = {
                        val ultima = recentes.firstOrNull()
                        if (ultima != null) onAbrirConversa(ultima.id) else onNovaConversa()
                    },
                )
                CockpitHojeSecao(
                    conversa = conversaPrincipal,
                    saiuCentavos = resumoMes.saiuCentavos,
                    entrouCentavos = resumoMes.entrouCentavos,
                    planejadosMes = planejadosMes.size,
                    artefato = artefatos.firstOrNull(),
                    updateDisponivel = updateAvailable,
                    onAbrirConversa = { conversa ->
                        if (conversa != null) onAbrirConversa(conversa.id) else onNovaConversa()
                    },
                    onAbrirFinancas = onAbrirFinancas,
                    onAbrirEstante = onAbrirEstante,
                )
                if (!showcaseDismissed) {
                    ShowcaseNovidadesLuna(
                        onAbrirFinancas = onAbrirFinancas,
                        onLunaDesenha = { onNovaConversaComTexto("Desenha algo pra mim") },
                        onAbrirEstante = onAbrirEstante,
                        onDismiss = {
                            PrefsRepository.showcaseLunaV1Dismissed = true
                            showcaseDismissed = true
                        },
                    )
                }
                ClimaSecao(
                    ativo = localizacaoAtiva,
                    local = clima,
                    carregando = climaCarregando,
                    onAtivar = {
                        if (LocationRepository.temPermissao(context)) {
                            PrefsRepository.setLocalizacaoAtiva(true)
                            LocationRepository.atualizarEmBackground(context, forcar = true)
                        } else {
                            permClima.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION,
                                ),
                            )
                        }
                    },
                    onAtualizar = { LocationRepository.atualizarEmBackground(context, forcar = true) },
                )
            }
        }

        ComposerEntrada(
            onEnviar = { texto -> onNovaConversaComTexto(texto) },
            onAbrirVazio = onNovaConversa,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = OrbitMetrics.pagePadding)
                .padding(bottom = 16.dp),
        )
    }
}

private fun saudacaoDoDia(): String {
    val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
        h in 5..11 -> "Bom dia"
        h in 12..18 -> "Boa tarde"
        else -> "Boa noite"
    }
}

@Composable
private fun CockpitHojeSecao(
    conversa: Conversa?,
    saiuCentavos: Long,
    entrouCentavos: Long,
    planejadosMes: Int,
    artefato: DocumentoUi?,
    updateDisponivel: Boolean,
    onAbrirConversa: (Conversa?) -> Unit,
    onAbrirFinancas: () -> Unit,
    onAbrirEstante: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Hoje no Orbit",
                color = OrbitTokens.textHiN,
                fontSize = 18.sp,
                fontFamily = Bricolage,
                fontWeight = FontWeight.Bold,
                lineHeight = 22.sp,
            )
            Text(
                "ao vivo",
                color = OrbitTokens.bluePastel,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(OrbitMetrics.radiusPill))
                    .background(OrbitTokens.bluePastel.copy(alpha = 0.12f))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            CockpitTile(
                icone = Icons.Rounded.ChatBubbleOutline,
                rotulo = "Conversa",
                titulo = conversa?.titulo?.takeIf { it.isNotBlank() } ?: "Começar agora",
                subtitulo = conversa?.preview?.takeIf { it.isNotBlank() } ?: "A Luna te espera em uma conversa nova.",
                detalhe = conversa?.let { "Atualizada ${tempoRelativoInicio(it.ultimaAtualizacao)}" } ?: "Nova conversa",
                acento = OrbitTokens.bluePastel,
                modifier = Modifier.weight(1f).height(142.dp),
                onClick = { onAbrirConversa(conversa) },
            )
            CockpitTile(
                icone = Icons.Rounded.AccountBalanceWallet,
                rotulo = "Finanças",
                titulo = tituloFinanceiroInicio(saiuCentavos, entrouCentavos, planejadosMes),
                subtitulo = subtituloFinanceiroInicio(saiuCentavos, entrouCentavos, planejadosMes),
                detalhe = if (planejadosMes > 0) planejadosInicioLabel(planejadosMes) else "Realizado até hoje",
                acento = if (saiuCentavos > 0) OrbitTokens.warning else OrbitTokens.online,
                modifier = Modifier.weight(1f).height(142.dp),
                onClick = onAbrirFinancas,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            CockpitTile(
                icone = Icons.Rounded.Description,
                rotulo = "Artefatos",
                titulo = artefato?.titulo?.takeIf { it.isNotBlank() } ?: "Nada recente",
                subtitulo = artefato?.let { origemArtefatoInicio(it) } ?: "Quando a Luna criar ou editar algo, aparece aqui.",
                detalhe = artefato?.let { "Editado ${tempoRelativoInicio(it.updatedAtMs)}" } ?: "Estante vazia",
                acento = OrbitTokens.violet,
                modifier = Modifier.weight(1f).height(132.dp),
                onClick = onAbrirEstante,
            )
            CockpitTile(
                icone = Icons.Rounded.CheckCircle,
                rotulo = "Sistema",
                titulo = if (updateDisponivel) "Update pronto" else "Tudo em dia",
                subtitulo = if (updateDisponivel) {
                    "Tem uma versão nova esperando no topo."
                } else {
                    "Lab ${BuildConfig.VERSION_NAME} rodando normal."
                },
                detalhe = if (updateDisponivel) "Atualização disponível" else "Canal lab",
                acento = if (updateDisponivel) OrbitTokens.bluePastel else OrbitTokens.online,
                modifier = Modifier.weight(1f).height(132.dp),
            )
        }
    }
}

@Composable
private fun CockpitTile(
    icone: ImageVector,
    rotulo: String,
    titulo: String,
    subtitulo: String,
    detalhe: String,
    acento: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(OrbitMetrics.radiusCard)
    val press = if (onClick != null) Modifier.orbitPressable(onClick = onClick) else Modifier
    Column(
        modifier
            .clip(shape)
            .background(OrbitTokens.graphiteSurf)
            .border(1.dp, OrbitTokens.graphiteHair, shape)
            .then(press)
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(acento.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    icone,
                    contentDescription = null,
                    tint = acento,
                    modifier = Modifier.size(16.dp),
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                rotulo,
                color = OrbitTokens.textLowN,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Column {
            Text(
                titulo,
                color = OrbitTokens.textHiN,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 18.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                subtitulo,
                color = OrbitTokens.textMidN,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                detalhe,
                color = acento,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun tituloFinanceiroInicio(
    saiuCentavos: Long,
    entrouCentavos: Long,
    planejadosMes: Int,
): String {
    if (saiuCentavos == 0L && entrouCentavos == 0L) {
        return if (planejadosMes > 0) "Só planejado" else "Tudo zerado"
    }
    return when {
        saiuCentavos > 0 -> "Saiu ${formatarReais(saiuCentavos)}"
        entrouCentavos > 0 -> "Entrou ${formatarReais(entrouCentavos)}"
        else -> "Sem movimento"
    }
}

private fun subtituloFinanceiroInicio(
    saiuCentavos: Long,
    entrouCentavos: Long,
    planejadosMes: Int,
): String {
    if (saiuCentavos == 0L && entrouCentavos == 0L) {
        return if (planejadosMes > 0) {
            "Nada realizado ainda; futuros ficam fora da conta."
        } else {
            "Seu mês ainda não tem movimentos."
        }
    }
    val saldo = entrouCentavos - saiuCentavos
    return "Saldo do mês: ${formatarReais(saldo)}"
}

private fun origemArtefatoInicio(artefato: DocumentoUi): String =
    when (artefato.updatedBy.lowercase(Locale.getDefault())) {
        "user" -> "Último toque seu na Estante."
        else -> "Último toque da Luna na Estante."
    }

private fun planejadosInicioLabel(total: Int): String =
    if (total == 1) "1 futuro neste mês" else "$total futuros neste mês"

private fun tempoRelativoInicio(epochMs: Long): String {
    if (epochMs <= 0L) return "agora"
    val deltaMin = ((System.currentTimeMillis() - epochMs).coerceAtLeast(0L)) / 60000L
    return when {
        deltaMin < 1 -> "agora"
        deltaMin < 60 -> "há $deltaMin min"
        deltaMin < 1440 -> "há ${deltaMin / 60} h"
        deltaMin < 43200 -> "há ${deltaMin / 1440} d"
        else -> {
            val meses = deltaMin / 43200
            if (meses == 1L) "há 1 mês" else "há $meses meses"
        }
    }
}

@Composable
private fun ClimaSecao(
    ativo: Boolean,
    local: LocalLab?,
    carregando: Boolean,
    onAtivar: () -> Unit,
    onAtualizar: () -> Unit,
) {
    val shape = RoundedCornerShape(OrbitMetrics.radiusCard)

    // Convite (desligado) ou espera (ligado, mas ainda sem o primeiro fix).
    if (!ativo || local?.clima == null) {
        val buscandoAgora = ativo && local?.clima == null
        Row(
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(OrbitTokens.graphiteSurf)
                .border(1.dp, OrbitTokens.graphiteHair, shape)
                .orbitPressable(onClick = if (ativo) onAtualizar else onAtivar)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("🌤️", fontSize = 26.sp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "Clima no seu lugar",
                    color = OrbitTokens.textHiN,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    if (buscandoAgora) "Buscando o tempo agora…"
                    else "Veja o tempo agora e dê à Luna o seu lugar.",
                    color = OrbitTokens.textMidN,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                )
            }
            if (!ativo) {
                Spacer(Modifier.width(10.dp))
                Text(
                    "Ativar",
                    color = OrbitTokens.onBluePastel,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(OrbitMetrics.radiusPill))
                        .background(OrbitTokens.bluePastel)
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                )
            }
        }
        return
    }

    val c = local.clima
    var mostrarDetalhe by remember { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(OrbitTokens.graphiteSurf)
            .border(1.dp, OrbitTokens.graphiteHair, shape)
            .orbitPressable(onClick = { mostrarDetalhe = true })
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(emojiWMO(c.codigo), fontSize = 34.sp)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                c.tempC?.let { "${it.roundToInt()}°" } ?: "--°",
                color = OrbitTokens.textHiN,
                fontSize = 28.sp,
                fontFamily = Bricolage,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-1).sp,
                lineHeight = 30.sp,
            )
            Text(
                c.descricao?.takeIf { it.isNotBlank() } ?: subtituloClima(c),
                color = OrbitTokens.textMidN,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.widthIn(max = 150.dp),
        ) {
            Text(
                lugarLabel(local),
                color = OrbitTokens.textMidN,
                fontSize = 12.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (c.maxC != null || c.minC != null) {
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    c.maxC?.let {
                        Text(
                            "${it.roundToInt()}°",
                            color = OrbitTokens.bluePastel,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    c.minC?.let {
                        Text(
                            " / ${it.roundToInt()}°",
                            color = OrbitTokens.textMidN,
                            fontSize = 12.5.sp,
                        )
                    }
                    c.umidade?.let {
                        Text(
                            " · $it%",
                            color = OrbitTokens.textMidN,
                            fontSize = 12.5.sp,
                        )
                    }
                }
            }
            if (carregando) {
                Spacer(Modifier.height(2.dp))
                Text("atualizando…", color = OrbitTokens.textLowN, fontSize = 10.sp)
            }
        }
    }

    if (mostrarDetalhe) {
        ClimaDetalheSheet(
            local = local,
            onDismiss = { mostrarDetalhe = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClimaDetalheSheet(
    local: LocalLab,
    onDismiss: () -> Unit,
) {
    val c = local.clima ?: return
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var refreshing by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = OrbitTokens.graphiteRaised,
    ) {
        // Puxar pra baixo atualiza — sem botão, o gesto natural (igual à tela de Início).
        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = {
                refreshing = true
                scope.launch {
                    LocationRepository.atualizar(context)
                    refreshing = false
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = OrbitMetrics.pagePadding)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // Herói imersivo: o Céu Vivo por trás do lugar + temperatura.
            val paleta = paletaCeu(c.codigo, horaLocal())
            val heroShape = RoundedCornerShape(20.dp)
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(heroShape)
                    .border(1.dp, OrbitTokens.graphiteHair, heroShape),
            ) {
                CeuVivoFundo(paleta, Modifier.matchParentSize(), compacto = false)
                Column(Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.LocationOn,
                            contentDescription = null,
                            tint = OrbitTokens.textMidN,
                            modifier = Modifier.size(15.dp),
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            lugarLabel(local),
                            color = OrbitTokens.textHiN,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            if (refreshing) "atualizando…" else atualizadoLabel(local.atualizadoEm),
                            color = OrbitTokens.textLowN,
                            fontSize = 11.sp,
                        )
                    }
                    Spacer(Modifier.height(18.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(emojiWMO(c.codigo), fontSize = 60.sp)
                        Spacer(Modifier.width(18.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                c.tempC?.let { "${it.roundToInt()}°" } ?: "--°",
                                color = OrbitTokens.textHiN,
                                fontSize = 56.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-2).sp,
                            )
                            Text(
                                subtituloClima(c),
                                color = OrbitTokens.textMidN,
                                fontSize = 14.sp,
                            )
                        }
                    }
                }
            }

            // "Agora" — grade de detalhes em cartões (2 por linha).
            val stats = buildList {
                c.sensacaoC?.let { add("Sensação" to "${it.roundToInt()}°") }
                c.umidade?.let { add("Umidade" to "$it%") }
                c.ventoKmh?.let { add("Vento" to "${it.roundToInt()} km/h") }
                c.chuvaProb?.let { add("Chance de chuva" to "$it%") }
                if (c.maxC != null || c.minC != null) {
                    val mx = c.maxC?.let { "${it.roundToInt()}°" } ?: "--"
                    val mn = c.minC?.let { "${it.roundToInt()}°" } ?: "--"
                    add("Máx / Mín" to "$mx / $mn")
                }
                c.chuvaMm?.let {
                    if (it > 0) add("Precipitação" to String.format(Locale.getDefault(), "%.1f mm", it))
                }
            }
            if (stats.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SecaoLabel("AGORA")
                    stats.chunked(2).forEach { par ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            par.forEach { (rotulo, valor) ->
                                DetalheStat(rotulo, valor, Modifier.weight(1f))
                            }
                            if (par.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }

            // "Próximos dias" — a previsão que o card não mostra.
            if (c.previsao.isNotEmpty()) {
                Column {
                    SecaoLabel("PRÓXIMOS DIAS")
                    Spacer(Modifier.height(4.dp))
                    c.previsao.forEachIndexed { i, dia ->
                        DiaPrevisaoRow(dia)
                        if (i < c.previsao.lastIndex) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(OrbitTokens.graphiteHair),
                            )
                        }
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun SecaoLabel(texto: String) {
    Text(
        texto,
        color = OrbitTokens.textLowN,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 1.5.sp,
    )
}

@Composable
private fun DetalheStat(rotulo: String, valor: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(OrbitTokens.graphiteSurf)
            .padding(14.dp),
    ) {
        Text(rotulo, color = OrbitTokens.textLowN, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(5.dp))
        Text(valor, color = OrbitTokens.textHiN, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun DiaPrevisaoRow(dia: DiaPrevisao) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(emojiWMO(dia.codigo), fontSize = 24.sp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                rotuloCap(dia.rotulo),
                color = OrbitTokens.textHiN,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
            )
            dia.descricao?.takeIf { it.isNotBlank() }?.let {
                Text(
                    it,
                    color = OrbitTokens.textLowN,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        dia.chuvaProb?.let {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.Umbrella,
                    contentDescription = null,
                    tint = OrbitTokens.textLowN,
                    modifier = Modifier.size(13.dp),
                )
                Spacer(Modifier.width(3.dp))
                Text("$it%", color = OrbitTokens.textMidN, fontSize = 13.sp)
            }
            Spacer(Modifier.width(14.dp))
        }
        val mx = dia.maxC?.let { "↑${it.roundToInt()}°" }
        val mn = dia.minC?.let { "↓${it.roundToInt()}°" }
        Text(
            listOfNotNull(mx, mn).joinToString("  "),
            color = OrbitTokens.textMidN,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

private fun rotuloCap(r: String): String =
    r.trim().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

private fun atualizadoLabel(epochMs: Long): String {
    val min = (System.currentTimeMillis() - epochMs) / 60000L
    return when {
        min < 1 -> "atualizado agora"
        min < 60 -> "atualizado há $min min"
        min < 1440 -> "atualizado há ${min / 60} h"
        else -> "atualizado há ${min / 1440} d"
    }
}

private fun lugarLabel(local: LocalLab): String {
    val partes = listOfNotNull(
        local.cidade?.takeIf { it.isNotBlank() },
        local.uf?.takeIf { it.isNotBlank() },
    )
    return if (partes.isNotEmpty()) partes.joinToString(", ") else "Seu lugar"
}

private fun subtituloClima(c: com.ethan.orbitlab.data.local.ClimaLab): String {
    val desc = c.descricao?.takeIf { it.isNotBlank() }
    val sensacao = c.sensacaoC?.let { s ->
        c.tempC?.let { t -> if (kotlin.math.abs(s - t) >= 2) "sensação ${s.roundToInt()}°" else null }
    }
    return listOfNotNull(desc, sensacao).joinToString(" · ").ifBlank { "Tempo agora" }
}

@Composable
private fun BoxScope.InicioOrbs(idleAtivo: Boolean, density: Density) {
    // Grafite minimalista: um único brilho pastel bem discreto no topo, respirando de
    // leve. Nada de violeta/dourado — a cor mora só nos detalhes (CTA, ativo).
    val drift = if (idleAtivo) 6f else 0f
    Box(
        modifier = Modifier
            .align(Alignment.TopStart)
            .offset(x = (-30).dp, y = (-40).dp)
            .size(240.dp)
            .graphicsLayer {
                translationX = with(density) { drift.dp.toPx() }
            }
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(OrbitTokens.bluePastel.copy(alpha = 0.045f), Color.Transparent),
                ),
            ),
    )
}

@Composable
private fun HeaderSecao(
    saudacao: String,
    nome: String,
) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            "$saudacao,",
            color = OrbitTokens.textMidN,
            fontSize = OrbitMetrics.bodySize,
            lineHeight = 18.sp,
        )
        Text(
            "$nome.",
            color = OrbitTokens.textHiN,
            fontSize = OrbitMetrics.titleSize,
            fontFamily = Bricolage,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.4).sp,
            lineHeight = 30.sp,
        )
    }
}

@Composable
private fun LunaSecao(
    linha: String,
    temUltima: Boolean,
    onFalar: () -> Unit,
) {
    val shape = RoundedCornerShape(OrbitMetrics.radiusCard)
    Box(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(OrbitTokens.graphiteSurf)
            .border(1.dp, OrbitTokens.graphiteHair, shape)
            .orbitPressable(onClick = onFalar),
    ) {
        // Brilho pastel discreto no canto — a "presença" da Luna, sem violeta.
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .offset(x = 30.dp, y = (-40).dp)
                .size(150.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            OrbitTokens.bluePastel.copy(alpha = 0.14f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFEAF1FB),
                                    OrbitTokens.bluePastel,
                                    Color(0xFF6E8FC4),
                                ),
                            ),
                        ),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    "Luna",
                    color = OrbitTokens.textHiN,
                    fontSize = 15.sp,
                    fontFamily = Bricolage,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                linha,
                color = OrbitTokens.textHiN.copy(alpha = 0.92f),
                fontSize = 15.sp,
                lineHeight = 21.sp,
            )
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (temUltima) "Continuar com a Luna" else "Falar com a Luna",
                    color = OrbitTokens.bluePastel,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.width(6.dp))
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = null,
                    tint = OrbitTokens.bluePastel,
                    modifier = Modifier.size(15.dp),
                )
            }
        }
    }
}

@Composable
private fun ComposerEntrada(
    onEnviar: (String) -> Unit,
    onAbrirVazio: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Composer de verdade: digita aqui e o envio ABRE UMA CONVERSA NOVA já mandando o texto.
    // Vazio, o botão só abre um chat em branco (o antigo "toca pra conversar").
    var texto by remember { mutableStateOf("") }
    val temTexto = texto.isNotBlank()
    val keyboard = LocalSoftwareKeyboardController.current
    val enviar = {
        val t = texto.trim()
        if (t.isNotEmpty()) {
            texto = ""
            keyboard?.hide()
            onEnviar(t)
        } else {
            onAbrirVazio()
        }
    }
    val shape = RoundedCornerShape(22.dp)
    Row(
        modifier
            .clip(shape)
            .background(OrbitTokens.graphiteRaised)
            .border(1.dp, OrbitTokens.graphiteHair, shape)
            .padding(start = 18.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicTextField(
            value = texto,
            onValueChange = { texto = it },
            modifier = Modifier.weight(1f),
            textStyle = TextStyle(
                color = OrbitTokens.textHiN,
                fontSize = 15.sp,
                lineHeight = 20.sp,
            ),
            cursorBrush = SolidColor(OrbitTokens.bluePastel),
            maxLines = 5,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Send,
            ),
            keyboardActions = KeyboardActions(onSend = { enviar() }),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (texto.isEmpty()) {
                        Text(
                            "Fala comigo…",
                            color = OrbitTokens.textMidN,
                            fontSize = 15.sp,
                        )
                    }
                    inner()
                }
            },
        )
        Spacer(Modifier.width(8.dp))
        Box(
            Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(OrbitTokens.bluePastel)
                .orbitPressable(onClick = enviar),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.ArrowUpward,
                contentDescription = if (temTexto) "Enviar" else "Nova conversa",
                tint = OrbitTokens.onBluePastel,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

private fun linhaDaLuna(saudacao: String, c: com.ethan.orbitlab.data.local.ClimaLab?): String {
    val t = c?.tempC?.roundToInt()
    val desc = c?.descricao?.takeIf { it.isNotBlank() }?.lowercase(Locale.getDefault())
    val clima = when {
        t == null -> null
        desc != null -> "Tá $t° e $desc por aí."
        else -> "Tá $t° por aí."
    }
    val abertura = "$saudacao."
    val nudge = when (saudacao) {
        "Bom dia" -> "Por onde a gente começa?"
        "Boa tarde" -> "Como tá indo o dia?"
        else -> "Me conta como foi o dia."
    }
    return listOfNotNull(abertura, clima, nudge).joinToString(" ")
}

