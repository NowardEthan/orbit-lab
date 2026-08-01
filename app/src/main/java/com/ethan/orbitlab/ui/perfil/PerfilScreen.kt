package com.ethan.orbitlab.ui.perfil

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Nightlight
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.ShowChart
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.ethan.orbitlab.R
import com.ethan.orbitlab.data.AuthRepository
import com.ethan.orbitlab.data.ChatRepository
import com.ethan.orbitlab.data.Conversa
import com.ethan.orbitlab.data.ProfileImageKind
import com.ethan.orbitlab.data.ProfileMilestones
import com.ethan.orbitlab.data.UserProfile
import com.ethan.orbitlab.data.UserProfileRepository
import com.ethan.orbitlab.ui.theme.Bricolage
import com.ethan.orbitlab.ui.theme.OrbitIconButton
import com.ethan.orbitlab.ui.theme.OrbitMetrics
import com.ethan.orbitlab.ui.theme.OrbitMotion
import com.ethan.orbitlab.ui.theme.OrbitTokens
import com.ethan.orbitlab.ui.theme.orbitPressable
import kotlinx.coroutines.launch

private enum class AbaPerfil(
    val label: String,
    val icone: ImageVector,
) {
    LUNA("Luna", Icons.Rounded.Nightlight),
    ATIVIDADE("Atividade", Icons.Rounded.ShowChart),
    CONQUISTAS("Conquistas", Icons.Rounded.WorkspacePremium),
}

private enum class PickTarget { AVATAR, COVER }

@Composable
fun PerfilScreen(
    onConversarComLuna: () -> Unit = {},
    onAbrirConversa: (String) -> Unit = {},
) {
    var aba by remember { mutableStateOf(AbaPerfil.LUNA) }
    var editando by remember { mutableStateOf(false) }
    val profile by UserProfileRepository.profile.collectAsState()
    val conversas by ChatRepository.conversas.collectAsState()
    val session by AuthRepository.session.collectAsState()

    var draftNome by remember { mutableStateOf("") }
    var draftUsername by remember { mutableStateOf("") }
    var draftBio by remember { mutableStateOf("") }
    var draftAvatar by remember { mutableStateOf<String?>(null) }
    var draftCover by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    var uploading by remember { mutableStateOf(false) }
    var erro by remember { mutableStateOf<String?>(null) }
    var pickTarget by remember { mutableStateOf<PickTarget?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(editando, profile) {
        if (editando) {
            draftNome = profile.displayName.ifBlank { session?.displayName.orEmpty() }
            draftUsername = profile.username
            draftBio = profile.bio
            draftAvatar = profile.avatarUrl
            draftCover = profile.coverUrl
            erro = null
        }
    }

    BackHandler(enabled = editando) {
        if (!saving && !uploading) editando = false
    }

    val pickImage = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? ->
        val target = pickTarget
        pickTarget = null
        if (uri == null || target == null) return@rememberLauncherForActivityResult
        uploading = true
        erro = null
        scope.launch {
            val kind = if (target == PickTarget.AVATAR) {
                ProfileImageKind.AVATAR
            } else {
                ProfileImageKind.COVER
            }
            UserProfileRepository.uploadProfileImage(context, kind, uri).fold(
                onSuccess = { url ->
                    if (kind == ProfileImageKind.AVATAR) draftAvatar = url
                    else draftCover = url
                    uploading = false
                },
                onFailure = {
                    uploading = false
                    erro = it.message ?: "Não deu pra enviar a imagem."
                },
            )
        }
    }

    fun abrirGaleria(target: PickTarget) {
        pickTarget = target
        pickImage.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
        )
    }

    fun salvar() {
        if (saving || uploading) return
        saving = true
        erro = null
        scope.launch {
            UserProfileRepository.updateProfile(
                displayName = draftNome,
                username = draftUsername,
                bio = draftBio,
            ).fold(
                onSuccess = {
                    saving = false
                    editando = false
                },
                onFailure = {
                    saving = false
                    erro = it.message ?: "Não deu pra salvar."
                },
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 120.dp),
    ) {
        if (editando) {
            BarraEdicao(
                saving = saving,
                uploading = uploading,
                onCancelar = { if (!saving && !uploading) editando = false },
                onSalvar = { salvar() },
            )
        }

        HeaderPerfil(
            coverUrl = if (editando) draftCover else profile.coverUrl,
            avatarUrl = if (editando) draftAvatar else profile.avatarUrl,
            editando = editando,
            uploading = uploading,
            onEditarPerfil = { editando = true },
            onTrocarCapa = { abrirGaleria(PickTarget.COVER) },
            onTrocarAvatar = { abrirGaleria(PickTarget.AVATAR) },
            onRemoverCapa = {
                if (!editando) return@HeaderPerfil
                scope.launch {
                    UserProfileRepository.removeProfileImage(ProfileImageKind.COVER)
                    draftCover = null
                }
            },
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (editando) {
            IdentidadeEditavel(
                nome = draftNome,
                onNome = { draftNome = it },
                username = draftUsername,
                onUsername = {
                    draftUsername = it.lowercase()
                        .removePrefix("@")
                        .filter { c -> c.isLetterOrDigit() || c == '_' }
                        .take(24)
                },
                bio = draftBio,
                onBio = { if (it.length <= 160) draftBio = it },
                erro = erro,
                uploading = uploading,
            )
        } else {
            IdentidadeEAcoes(
                profile = profile,
                sessionName = session?.displayName,
                conversas = conversas.size,
                mensagens = conversas.sumOf { it.messageCount.coerceAtLeast(it.mensagens.size) },
                onConversarComLuna = onConversarComLuna,
            )
            Spacer(modifier = Modifier.height(24.dp))
            AbasPerfil(aba = aba, onAba = { aba = it })
            Spacer(modifier = Modifier.height(20.dp))
            Box(modifier = Modifier.padding(horizontal = OrbitMetrics.pagePadding)) {
                when (aba) {
                    AbaPerfil.LUNA -> ConteudoLuna(
                        ultima = conversas.firstOrNull(),
                        onContinuar = {
                            val id = conversas.firstOrNull()?.id
                            if (id != null) onAbrirConversa(id) else onConversarComLuna()
                        },
                    )
                    AbaPerfil.ATIVIDADE -> ConteudoAtividade(
                        recentes = conversas.take(5),
                        onAbrir = onAbrirConversa,
                    )
                    AbaPerfil.CONQUISTAS -> ConteudoConquistas(profile.milestones)
                }
            }
        }
    }
}

@Composable
private fun BarraEdicao(
    saving: Boolean,
    uploading: Boolean,
    onCancelar: () -> Unit,
    onSalvar: () -> Unit,
) {
    val busy = saving || uploading
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = OrbitMetrics.pagePadding)
            .padding(top = 12.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "Cancelar",
            color = OrbitTokens.textMidN,
            fontSize = 15.sp,
            modifier = Modifier.orbitPressable(enabled = !busy, onClick = onCancelar),
        )
        Text(
            "Editar perfil",
            color = OrbitTokens.textHiN,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.orbitPressable(enabled = !busy, onClick = onSalvar),
        ) {
            if (busy) {
                CircularProgressIndicator(
                    color = OrbitTokens.bluePastel,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(14.dp),
                )
            }
            Text(
                if (saving) "Salvando…" else if (uploading) "Enviando…" else "Salvar",
                color = OrbitTokens.bluePastel,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun HeaderPerfil(
    coverUrl: String?,
    avatarUrl: String?,
    editando: Boolean,
    uploading: Boolean,
    onEditarPerfil: () -> Unit,
    onTrocarCapa: () -> Unit,
    onTrocarAvatar: () -> Unit,
    onRemoverCapa: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(156.dp),
    ) {
        // Faixa da capa — calma: imagem quando existe, senão só um grafite sutil.
        Box(
            Modifier
                .fillMaxWidth()
                .height(108.dp)
                .align(Alignment.TopCenter)
                .background(OrbitTokens.graphiteSurf)
                .then(
                    if (editando) {
                        Modifier.orbitPressable(enabled = !uploading, onClick = onTrocarCapa)
                    } else {
                        Modifier
                    },
                ),
        ) {
            if (!coverUrl.isNullOrBlank()) {
                AsyncImage(
                    model = coverUrl,
                    contentDescription = "Capa",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            if (editando) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.28f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Rounded.CameraAlt,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp),
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            if (coverUrl.isNullOrBlank()) "Adicionar capa" else "Trocar capa",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
                if (!coverUrl.isNullOrBlank()) {
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp)
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.45f))
                            .orbitPressable(onClick = onRemoverCapa),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = "Remover capa",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }

        if (!editando) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = OrbitMetrics.pagePadding)
                    .padding(top = 18.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OrbitIconButton(
                    icon = Icons.Rounded.Settings,
                    contentDescription = "Editar perfil",
                    onClick = onEditarPerfil,
                    tint = OrbitTokens.textHiN,
                    iconSize = 20.dp,
                )
            }
        }

        // Avatar recortado do fundo por um anel grafite (contraste, não halo claro).
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .size(92.dp)
                .clip(CircleShape)
                .background(OrbitTokens.graphiteRaised)
                .border(3.dp, OrbitTokens.graphiteBg, CircleShape)
                .then(
                    if (editando) {
                        Modifier.orbitPressable(enabled = !uploading, onClick = onTrocarAvatar)
                    } else {
                        Modifier
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (!avatarUrl.isNullOrBlank()) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = "Avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                )
            } else {
                Icon(
                    Icons.Rounded.Person,
                    contentDescription = "Avatar",
                    tint = OrbitTokens.textMidN,
                    modifier = Modifier.size(42.dp),
                )
            }
            if (editando) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.35f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.PhotoCamera,
                        contentDescription = "Trocar foto",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun IdentidadeEditavel(
    nome: String,
    onNome: (String) -> Unit,
    username: String,
    onUsername: (String) -> Unit,
    bio: String,
    onBio: (String) -> Unit,
    erro: String?,
    uploading: Boolean,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = OrbitMetrics.pagePadding)
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            "Toque na capa ou na foto para trocar. Nome, @ e bio ficam aqui.",
            color = OrbitTokens.textLowN,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        CampoEditavel(
            label = "Nome",
            value = nome,
            onValueChange = onNome,
            singleLine = true,
            enabled = !uploading,
        )
        CampoEditavel(
            label = "Nome de usuário",
            value = username,
            onValueChange = onUsername,
            singleLine = true,
            enabled = !uploading,
            prefix = "@",
            hint = "letras, números e _",
        )
        CampoEditavel(
            label = "Bio",
            value = bio,
            onValueChange = onBio,
            singleLine = false,
            enabled = !uploading,
            minHeight = 88.dp,
            hint = "${bio.length}/160",
        )

        if (!erro.isNullOrBlank()) {
            Text(erro, color = OrbitTokens.danger, fontSize = 13.sp, lineHeight = 17.sp)
        }
    }
}

@Composable
private fun CampoEditavel(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    singleLine: Boolean,
    enabled: Boolean,
    prefix: String? = null,
    hint: String? = null,
    minHeight: androidx.compose.ui.unit.Dp = 48.dp,
) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, color = OrbitTokens.textMidN, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            if (hint != null) {
                Text(hint, color = OrbitTokens.textLowN, fontSize = 11.sp)
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .height(minHeight)
                .clip(RoundedCornerShape(14.dp))
                .background(OrbitTokens.graphiteSurf)
                .border(1.dp, OrbitTokens.graphiteHair, RoundedCornerShape(14.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top,
        ) {
            if (prefix != null) {
                Text(prefix, color = OrbitTokens.textLowN, fontSize = 15.sp)
                Spacer(Modifier.width(2.dp))
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                singleLine = singleLine,
                textStyle = TextStyle(color = OrbitTokens.textHiN, fontSize = 15.sp),
                cursorBrush = SolidColor(OrbitTokens.bluePastel),
                modifier = Modifier.weight(1f),
                decorationBox = { inner ->
                    Box {
                        if (value.isEmpty()) {
                            Text(
                                when {
                                    prefix != null -> "seuusuario"
                                    label == "Bio" -> "Uma linha sobre você"
                                    else -> label
                                },
                                color = OrbitTokens.textLowN,
                                fontSize = 15.sp,
                            )
                        }
                        inner()
                    }
                },
            )
        }
    }
}

@Composable
private fun IdentidadeEAcoes(
    profile: UserProfile,
    sessionName: String?,
    conversas: Int,
    mensagens: Int,
    onConversarComLuna: () -> Unit,
) {
    val nome = profile.displayName.ifBlank { sessionName ?: "Você" }
    val handle = "@${profile.username.ifBlank { "orbit" }}"
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = OrbitMetrics.pagePadding),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            nome,
            color = OrbitTokens.textHiN,
            fontSize = 26.sp,
            fontFamily = Bricolage,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.4).sp,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(handle, color = OrbitTokens.textMidN, fontSize = OrbitMetrics.bodySize)
        if (profile.memberSinceLabel.isNotBlank() || profile.plan.isNotBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                listOfNotNull(
                    UserProfileRepository.planLabel(profile.plan),
                    profile.memberSinceLabel.takeIf { it.isNotBlank() },
                ).joinToString(" · "),
                color = OrbitTokens.textLowN,
                fontSize = 12.sp,
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            profile.bio.ifBlank { "Escreva uma bio curta — quem você é neste Orbit." },
            color = if (profile.bio.isBlank()) OrbitTokens.textLowN else OrbitTokens.textMidN,
            fontSize = OrbitMetrics.bodySize,
            lineHeight = 20.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(0.92f),
        )
        Spacer(modifier = Modifier.height(18.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(OrbitMetrics.radiusPill))
                .background(OrbitTokens.bluePastel)
                .orbitPressable(onClick = onConversarComLuna)
                .padding(horizontal = 12.dp, vertical = 13.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.AutoMirrored.Rounded.Chat,
                contentDescription = null,
                tint = OrbitTokens.onBluePastel,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                "Conversar com a Luna",
                color = OrbitTokens.onBluePastel,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
        }

        Spacer(modifier = Modifier.height(18.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            StatSocial(valor = conversas.toString(), label = "conversas")
            Box(
                Modifier
                    .padding(horizontal = 16.dp)
                    .width(1.dp)
                    .height(28.dp)
                    .background(OrbitTokens.graphiteHair),
            )
            StatSocial(valor = mensagens.toString(), label = "mensagens")
        }
    }
}

@Composable
private fun StatSocial(valor: String, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            valor,
            color = OrbitTokens.textHiN,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, color = OrbitTokens.textMidN, fontSize = 13.sp)
    }
}

@Composable
private fun AbasPerfil(aba: AbaPerfil, onAba: (AbaPerfil) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = OrbitMetrics.pagePadding),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        AbaPerfil.entries.forEach { item ->
            val ativa = item == aba
            val indicadorLargura by animateDpAsState(
                targetValue = if (ativa) 22.dp else 0.dp,
                animationSpec = tween(OrbitMotion.msFast),
                label = "abaPerfilIndicador",
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .orbitPressable { onAba(item) }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Text(
                    item.label,
                    color = if (ativa) OrbitTokens.textHiN else OrbitTokens.textLowN,
                    fontSize = 14.sp,
                    fontWeight = if (ativa) FontWeight.SemiBold else FontWeight.Medium,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    Modifier
                        .width(indicadorLargura)
                        .height(2.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(if (ativa) OrbitTokens.bluePastel else Color.Transparent),
                )
            }
        }
    }
}

@Composable
private fun ConteudoLuna(ultima: Conversa?, onContinuar: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(OrbitTokens.graphiteRaised),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.DarkMode,
                    contentDescription = null,
                    tint = OrbitTokens.bluePastel,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Sua Luna",
                    color = OrbitTokens.textHiN,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "Companheira de clareza — privada nesta conta.",
                    color = OrbitTokens.textMidN,
                    fontSize = OrbitMetrics.captionSize,
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        // «Continuar» é a afordância principal da aba — fundo sutil, sem moldura nem 2º pill.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(OrbitMetrics.radiusPill))
                .background(OrbitTokens.graphiteSurf)
                .orbitPressable(onClick = onContinuar)
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                if (ultima != null) {
                    Text(
                        "Continuar conversa",
                        color = OrbitTokens.textLowN,
                        fontSize = 11.sp,
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        ultima.titulo,
                        color = OrbitTokens.textHiN,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (ultima.preview.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            ultima.preview,
                            color = OrbitTokens.textMidN,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                } else {
                    Text(
                        "Conversar com a Luna",
                        color = OrbitTokens.textHiN,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        "Ainda não há conversas — comece um papo.",
                        color = OrbitTokens.textMidN,
                        fontSize = 12.sp,
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Icon(
                Icons.Rounded.PlayArrow,
                contentDescription = null,
                tint = OrbitTokens.bluePastel,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun ConteudoAtividade(
    recentes: List<Conversa>,
    onAbrir: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (recentes.isEmpty()) {
            Text(
                "Quando você conversar, a atividade aparece aqui.",
                color = OrbitTokens.textMidN,
                fontSize = 13.sp,
            )
        } else {
            recentes.forEachIndexed { i, conv ->
                if (i > 0) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(OrbitTokens.graphiteHair),
                    )
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .orbitPressable { onAbrir(conv.id) }
                        .padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            conv.titulo,
                            color = OrbitTokens.textHiN,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            "${conv.grupoDia} · ${conv.horaFormatada}",
                            color = OrbitTokens.textLowN,
                            fontSize = 12.sp,
                        )
                    }
                    Icon(
                        Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        tint = OrbitTokens.textLowN,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ConteudoConquistas(m: ProfileMilestones) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "Marcos da sua conta Aura",
            color = OrbitTokens.textLowN,
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        ConquistaRow(Icons.Rounded.Mic, "Mensagem de voz", "Enviou um áudio pra Luna", m.voiceMessage)
        Divisoria()
        ConquistaRow(Icons.Rounded.Image, "Imagem no chat", "Anexou uma foto", m.imageAttachment)
        Divisoria()
        ConquistaRow(
            Icons.Rounded.WorkspacePremium,
            "Arquivo anexado",
            "Mandou um documento",
            m.fileAttachment,
        )
        Divisoria()
        ConquistaRow(Icons.Rounded.Lock, "Referência", "Citou uma mensagem ou anexo", m.documentReference)
    }
}

@Composable
private fun ConquistaRow(
    icone: ImageVector,
    titulo: String,
    subtitulo: String,
    desbloqueada: Boolean,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icone,
            contentDescription = null,
            tint = if (desbloqueada) OrbitTokens.bluePastel else OrbitTokens.textLowN,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                titulo,
                color = if (desbloqueada) OrbitTokens.textHiN else OrbitTokens.textMidN,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )
            Text(subtitulo, color = OrbitTokens.textLowN, fontSize = 12.sp)
        }
        Text(
            if (desbloqueada) "✓" else "·",
            color = if (desbloqueada) OrbitTokens.bluePastel else OrbitTokens.textLowN,
            fontSize = 16.sp,
        )
    }
}

@Composable
private fun Divisoria() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(start = 34.dp)
            .height(1.dp)
            .background(OrbitTokens.graphiteHair),
    )
}
