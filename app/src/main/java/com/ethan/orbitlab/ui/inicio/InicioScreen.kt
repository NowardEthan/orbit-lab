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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Umbrella
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.ethan.orbitlab.data.AuthRepository
import com.ethan.orbitlab.data.ChatRepository
import com.ethan.orbitlab.data.Conversa
import com.ethan.orbitlab.data.PrefsRepository
import com.ethan.orbitlab.data.UserProfileRepository
import com.ethan.orbitlab.data.local.LocalLab
import com.ethan.orbitlab.data.local.LocationRepository
import com.ethan.orbitlab.data.local.emojiWMO
import kotlin.math.roundToInt
import com.ethan.orbitlab.ui.theme.OrbitMetrics
import com.ethan.orbitlab.ui.theme.OrbitTokens
import com.ethan.orbitlab.ui.theme.orbitPressable
import java.util.Calendar

@Composable
fun InicioScreen(
    onAbrirNovidades: () -> Unit = {},
    temNovidade: Boolean = false,
    onAbrirConversa: (String) -> Unit = {},
    onNovaConversa: () -> Unit = {},
    idleAtivo: Boolean = true,
    onMarkNovidadesVistas: () -> Unit = {},
) {
    val density = LocalDensity.current
    val context = LocalContext.current
    val session by AuthRepository.session.collectAsState()
    val profile by UserProfileRepository.profile.collectAsState()
    val conversas by ChatRepository.conversas.collectAsState()
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
    val permClima = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { concessoes ->
        if (concessoes.values.any { it }) {
            PrefsRepository.setLocalizacaoAtiva(true)
            LocationRepository.atualizarEmBackground(context, forcar = true)
        }
    }
    // Ao abrir o Início: se o clima está ligado, atualiza em 2º plano (respeita o frescor de 10 min).
    LaunchedEffect(localizacaoAtiva) {
        if (localizacaoAtiva) LocationRepository.atualizarEmBackground(context)
    }

    LaunchedEffect(temNovidade) {
        if (temNovidade) onMarkNovidadesVistas()
    }
    val nomeCompleto = profile.displayName.ifBlank { session?.displayName.orEmpty() }
    val nome = nomeCompleto.substringBefore(' ').ifBlank { nomeCompleto }.ifBlank { "você" }
    val saudacao = remember { saudacaoDoDia() }
    val recentes = remember(conversas) { conversas.take(5) }
    val totalMsgs = remember(conversas) {
        conversas.sumOf { it.messageCount.coerceAtLeast(it.mensagens.size) }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Transparent)) {
        InicioOrbs(idleAtivo = idleAtivo, density = density)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    top = 24.dp,
                    start = OrbitMetrics.pagePadding,
                    end = OrbitMetrics.pagePadding,
                    bottom = 120.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(OrbitMetrics.sectionGap),
        ) {
            HeaderSecao(
                saudacao = saudacao,
                nome = nome,
                avatarUrl = profile.avatarUrl,
                onAbrirNovidades = onAbrirNovidades,
                temNovidade = temNovidade,
            )
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
            ResumoSecao(
                conversas = conversas.size,
                mensagens = totalMsgs,
            )
            ContinuarSecao(
                ultima = recentes.firstOrNull(),
                onContinuar = { id -> onAbrirConversa(id) },
                onNova = onNovaConversa,
            )
            if (recentes.isNotEmpty()) {
                RecentesSecao(
                    conversas = recentes,
                    onAbrir = onAbrirConversa,
                )
            }
        }
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
                .background(OrbitTokens.surface)
                .border(1.dp, OrbitTokens.borderSoft, shape)
                .orbitPressable(onClick = if (ativo) onAtualizar else onAtivar)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("🌤️", fontSize = 26.sp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "Clima no seu lugar",
                    color = OrbitTokens.textHigh,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    if (buscandoAgora) "Buscando o tempo agora…"
                    else "Veja o tempo agora e dê à Luna o seu lugar.",
                    color = OrbitTokens.textMid,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                )
            }
            if (!ativo) {
                Spacer(Modifier.width(10.dp))
                Text(
                    "Ativar",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(OrbitMetrics.radiusPill))
                        .background(OrbitTokens.accent)
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                )
            }
        }
        return
    }

    val c = local.clima
    val (topo, base) = climaGradiente(c.codigo)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.linearGradient(listOf(topo, base)))
            .border(1.dp, OrbitTokens.borderSoft, shape)
            .orbitPressable(onClick = onAtualizar)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Rounded.LocationOn,
                contentDescription = null,
                tint = OrbitTokens.textMid,
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                lugarLabel(local),
                color = OrbitTokens.textMid,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (carregando) {
                Text("atualizando…", color = OrbitTokens.textLow, fontSize = 11.sp)
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(emojiWMO(c.codigo), fontSize = 44.sp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    c.tempC?.let { "${it.roundToInt()}°" } ?: "--°",
                    color = OrbitTokens.textHigh,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-1).sp,
                )
                Text(
                    subtituloClima(c),
                    color = OrbitTokens.textMid,
                    fontSize = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            if (c.maxC != null || c.minC != null) {
                val max = c.maxC?.let { "↑${it.roundToInt()}°" }
                val min = c.minC?.let { "↓${it.roundToInt()}°" }
                StatClima(texto = listOfNotNull(max, min).joinToString("  "))
            }
            c.chuvaProb?.let { StatClima(icone = Icons.Rounded.Umbrella, texto = "$it%") }
            c.umidade?.let { StatClima(icone = Icons.Rounded.WaterDrop, texto = "$it%") }
            c.ventoKmh?.let { StatClima(icone = Icons.Rounded.Air, texto = "${it.roundToInt()} km/h") }
        }
    }
}

@Composable
private fun StatClima(
    texto: String,
    icone: androidx.compose.ui.graphics.vector.ImageVector? = null,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (icone != null) {
            Icon(
                icone,
                contentDescription = null,
                tint = OrbitTokens.textLow,
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(4.dp))
        }
        Text(texto, color = OrbitTokens.textMid, fontSize = 12.sp, fontWeight = FontWeight.Medium)
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

/** Fundo do card levemente tingido pelo tempo — quente no sol, frio na chuva, neutro no resto. */
private fun climaGradiente(codigo: Int?): Pair<Color, Color> = when (codigo) {
    0, 1 -> Color(0xFF2E2A1E) to Color(0xFF181A1F) // sol — âmbar discreto
    in 51..67, in 80..82, in 95..99 -> Color(0xFF1C2531) to Color(0xFF15181E) // chuva — azul
    in 71..77, 85, 86 -> Color(0xFF232A32) to Color(0xFF171B20) // neve — azul claro
    else -> Color(0xFF24272F) to Color(0xFF181A1F) // nuvem/neblina — grafite
}

@Composable
private fun BoxScope.InicioOrbs(idleAtivo: Boolean, density: Density) {
    val drift = if (idleAtivo) 6f else 0f
    Box(
        modifier = Modifier
            .align(Alignment.TopStart)
            .offset(x = (-40).dp, y = (-20).dp)
            .size(200.dp)
            .graphicsLayer {
                translationX = with(density) { drift.dp.toPx() }
            }
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(OrbitTokens.violet.copy(alpha = 0.05f), Color.Transparent),
                ),
            ),
    )
    Box(
        modifier = Modifier
            .align(Alignment.TopEnd)
            .offset(x = 36.dp, y = 80.dp)
            .size(160.dp)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(OrbitTokens.gold.copy(alpha = 0.04f), Color.Transparent),
                ),
            ),
    )
}

@Composable
private fun HeaderSecao(
    saudacao: String,
    nome: String,
    avatarUrl: String?,
    onAbrirNovidades: () -> Unit,
    temNovidade: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(OrbitTokens.surfaceRaised)
                    .border(1.dp, OrbitTokens.borderSoft, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (!avatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = "Seu avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Icon(
                        Icons.Rounded.Person,
                        contentDescription = null,
                        tint = OrbitTokens.textMid,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
            Column {
                Text(
                    "$saudacao,",
                    color = OrbitTokens.textMid,
                    fontSize = OrbitMetrics.bodySize,
                )
                Text(
                    "$nome.",
                    color = OrbitTokens.textHigh,
                    fontSize = OrbitMetrics.titleSize,
                    fontWeight = OrbitMetrics.titleWeight,
                    letterSpacing = (-0.3).sp,
                )
            }
        }
        Box(
            Modifier
                .size(OrbitMetrics.iconBtn)
                .clip(CircleShape)
                .background(OrbitTokens.surfaceRaised)
                .border(1.dp, OrbitTokens.borderSoft, CircleShape)
                .orbitPressable(onClick = onAbrirNovidades),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.Notifications,
                contentDescription = "Novidades",
                tint = OrbitTokens.textHigh,
                modifier = Modifier.size(20.dp),
            )
            if (temNovidade) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(OrbitTokens.accent),
                )
            }
        }
    }
}

@Composable
private fun ResumoSecao(conversas: Int, mensagens: Int) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        MetricCard(
            titulo = "Conversas",
            valor = conversas.toString(),
            modifier = Modifier.weight(1f),
        )
        MetricCard(
            titulo = "Mensagens",
            valor = mensagens.toString(),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun MetricCard(
    titulo: String,
    valor: String,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(OrbitMetrics.radiusCard)
    Column(
        modifier
            .clip(shape)
            .background(OrbitTokens.surface.copy(alpha = 0.9f))
            .border(1.dp, OrbitTokens.borderSoft, shape)
            .padding(14.dp),
    ) {
        Text(titulo, color = OrbitTokens.textMid, fontSize = 12.sp)
        Spacer(Modifier.height(4.dp))
        Text(
            valor,
            color = OrbitTokens.textHigh,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun ContinuarSecao(
    ultima: Conversa?,
    onContinuar: (String) -> Unit,
    onNova: () -> Unit,
) {
    val shape = RoundedCornerShape(OrbitMetrics.radiusCard)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(OrbitTokens.surface)
            .border(1.dp, OrbitTokens.borderSoft, shape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            if (ultima != null) "Continuar de onde parou" else "Começar com a Luna",
            color = OrbitTokens.textHigh,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
        )
        if (ultima != null) {
            Text(
                ultima.titulo,
                color = OrbitTokens.textMid,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                ultima.preview,
                color = OrbitTokens.textLow,
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        } else {
            Text(
                "Suas conversas sincronizam com a Conta Aura. Toque para criar a primeira.",
                color = OrbitTokens.textMid,
                fontSize = 13.sp,
                lineHeight = 18.sp,
            )
        }
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(OrbitMetrics.radiusPill))
                .background(OrbitTokens.accent)
                .orbitPressable {
                    if (ultima != null) onContinuar(ultima.id) else onNova()
                }
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                if (ultima != null) Icons.Rounded.PlayArrow else Icons.AutoMirrored.Rounded.Chat,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                if (ultima != null) "Continuar conversa" else "Nova conversa",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun RecentesSecao(
    conversas: List<Conversa>,
    onAbrir: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            "Recentes",
            color = OrbitTokens.textHigh,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
        )
        conversas.forEach { conv ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(OrbitTokens.surface.copy(alpha = 0.85f))
                    .border(1.dp, OrbitTokens.borderSoft, RoundedCornerShape(14.dp))
                    .orbitPressable { onAbrir(conv.id) }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        conv.titulo,
                        color = OrbitTokens.textHigh,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "${conv.horaFormatada} · ${conv.preview}",
                        color = OrbitTokens.textLow,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Icon(
                    Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = OrbitTokens.textLow,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}
