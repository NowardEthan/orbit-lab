package com.ethan.orbitlab.ui.chat

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.ethan.orbitlab.ui.theme.OrbitMetrics
import com.ethan.orbitlab.ui.theme.OrbitMotion
import com.ethan.orbitlab.ui.theme.OrbitTokens
import com.ethan.orbitlab.ui.theme.orbitPressable
import kotlinx.coroutines.launch

private enum class AttachTab { GALERIA, ARQUIVO }

/**
 * Explorer de anexos — galeria paginada, thumbs leves, seleção por id.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComposerAttachSheet(
    visible: Boolean,
    imageBudget: Int = MAX_ATTACH_IMAGES,
    fileBudget: Int = MAX_ATTACH_FILES,
    onDismiss: () -> Unit,
    onConfirm: (List<ComposerAttachment>) -> Unit,
) {
    if (!visible) return

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var tab by remember { mutableStateOf(AttachTab.GALERIA) }
    var albumId by remember { mutableStateOf("all") }
    var showingAlbums by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var loadingMore by remember { mutableStateOf(false) }
    var gallery by remember {
        mutableStateOf(GalleryLoadResult(emptyList(), emptyList(), permissionGranted = false))
    }
    var reloadKey by remember { mutableStateOf(0) }
    var cameraModo by remember { mutableStateOf<ModoCaptura?>(null) }

    // id → media (só itens selecionados; a grade só recebe Set de ids)
    val selectedById = remember { mutableStateOf(emptyMap<String, GalleryMedia>()) }
    val selectedIds by remember {
        derivedStateOf { selectedById.value.keys }
    }

    val launchers = rememberAttachMediaLaunchers(
        context = context,
        onPicked = { items -> onConfirm(items) },
        onPermissionResult = {
            GalleryCache.invalidate()
            reloadKey += 1
        },
    )

    LaunchedEffect(reloadKey) {
        loading = true
        gallery = loadDeviceGallery(context, forceRefresh = reloadKey > 0)
        loading = false
    }

    val selectedCount = selectedById.value.size
    val albumTitle = gallery.albums.firstOrNull { it.id == albumId }?.title ?: "Fotos e vídeos"
    val mediaList = remember(gallery.media, albumId) { mediaForAlbum(gallery.media, albumId) }

    fun canSelectMedia(item: GalleryMedia): Boolean {
        if (selectedById.value.containsKey(item.id)) return true
        val images = selectedById.value.values.count { !it.isVideo }
        val files = selectedById.value.values.count { it.isVideo }
        return if (item.isVideo) files < fileBudget else images < imageBudget
    }

    fun toggleMedia(item: GalleryMedia) {
        val cur = selectedById.value.toMutableMap()
        if (cur.containsKey(item.id)) {
            cur.remove(item.id)
        } else if (canSelectMedia(item)) {
            cur[item.id] = item
        }
        selectedById.value = cur
    }

    fun loadMore() {
        if (loadingMore || !gallery.hasMore || albumId != "all") return
        scope.launch {
            loadingMore = true
            gallery = loadMoreGallery(context, gallery)
            loadingMore = false
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = OrbitTokens.surfaceRaised,
        dragHandle = {
            Box(
                Modifier
                    .padding(top = 10.dp, bottom = 6.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(OrbitTokens.borderSoft),
            )
        },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.78f)
                .navigationBarsPadding(),
        ) {
            AttachSheetHeader(
                tab = tab,
                albumTitle = albumTitle,
                showingAlbums = showingAlbums,
                onToggleAlbums = {
                    if (tab == AttachTab.GALERIA && gallery.permissionGranted) {
                        showingAlbums = !showingAlbums
                    }
                },
                onBackFromAlbums = { showingAlbums = false },
            )

            if (selectedCount > 0) {
                LimitBanner(
                    images = selectedById.value.values.count { !it.isVideo },
                    files = selectedById.value.values.count { it.isVideo },
                    imageBudget = imageBudget,
                    fileBudget = fileBudget,
                )
            }

            Box(Modifier.weight(1f).fillMaxWidth()) {
                AnimatedContent(
                    targetState = tab to showingAlbums,
                    transitionSpec = {
                        fadeIn(OrbitMotion.tweenFast) togetherWith fadeOut(OrbitMotion.tweenInstant)
                    },
                    label = "attachPanel",
                ) { (atual, albums) ->
                    when {
                        atual == AttachTab.GALERIA && albums -> {
                            AlbumList(
                                albums = gallery.albums,
                                selectedId = albumId,
                                onSelect = { id ->
                                    albumId = id
                                    showingAlbums = false
                                },
                            )
                        }
                        atual == AttachTab.GALERIA -> {
                            when {
                                loading -> {
                                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(
                                            color = OrbitTokens.accent,
                                            strokeWidth = 2.dp,
                                            modifier = Modifier.size(28.dp),
                                        )
                                    }
                                }
                                !gallery.permissionGranted -> {
                                    PermissionEmpty(
                                        onAllow = launchers.requestGalleryPermission,
                                        onSystemPicker = launchers.pickPhotos,
                                    )
                                }
                                else -> {
                                    GalleryGrid(
                                        media = mediaList,
                                        selectedIds = selectedIds,
                                        canSelect = ::canSelectMedia,
                                        onToggle = ::toggleMedia,
                                        onCamera = { cameraModo = ModoCaptura.FOTO },
                                        onVideo = { cameraModo = ModoCaptura.VIDEO },
                                        onSystemPicker = launchers.pickPhotos,
                                        hasMore = gallery.hasMore && albumId == "all",
                                        loadingMore = loadingMore,
                                        onLoadMore = ::loadMore,
                                    )
                                }
                            }
                        }
                        else -> {
                            FileBrowserPanel(
                                fileBudget = fileBudget,
                                onPickSystem = launchers.pickFiles,
                            )
                        }
                    }
                }
            }

            if (selectedCount > 0) {
                ConfirmAttachButton(
                    count = selectedCount,
                    onClick = {
                        onConfirm(selectedById.value.values.map { it.toAttachment() })
                    },
                )
            }

            AttachTabBar(
                active = tab,
                onChange = {
                    tab = it
                    showingAlbums = false
                },
                onDismiss = onDismiss,
            )
        }
    }

    // Câmera aqui dentro (não a do sistema): num celular apertado de memória, abrir outro
    // app de câmera fazia o Android matar o Orbit e a foto ia embora com ele.
    CameraNoAppSheet(
        visivel = cameraModo != null,
        modoInicial = cameraModo ?: ModoCaptura.FOTO,
        onFechar = { cameraModo = null },
        onCapturado = { anexo ->
            cameraModo = null
            onConfirm(listOf(anexo))
        },
    )
}

@Composable
private fun AttachSheetHeader(
    tab: AttachTab,
    albumTitle: String,
    showingAlbums: Boolean,
    onToggleAlbums: () -> Unit,
    onBackFromAlbums: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = OrbitMetrics.pagePadding, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when {
            tab == AttachTab.GALERIA && showingAlbums -> {
                Row(
                    Modifier.orbitPressable(onClick = onBackFromAlbums),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                        contentDescription = "Voltar",
                        tint = OrbitTokens.textMid,
                        modifier = Modifier.size(22.dp),
                    )
                    Text(
                        "Pastas",
                        color = OrbitTokens.textHigh,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            tab == AttachTab.GALERIA -> {
                Row(
                    Modifier.orbitPressable(onClick = onToggleAlbums),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        albumTitle,
                        color = OrbitTokens.textHigh,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Icon(
                        Icons.Rounded.KeyboardArrowDown,
                        contentDescription = "Pastas",
                        tint = OrbitTokens.textMid,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
            else -> {
                Text(
                    "Arquivos",
                    color = OrbitTokens.textHigh,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun LimitBanner(
    images: Int,
    files: Int,
    imageBudget: Int,
    fileBudget: Int,
) {
    val msg = buildString {
        if (images >= imageBudget) append("A Luna analisa até $MAX_ATTACH_IMAGES imagens por mensagem. ")
        if (files >= fileBudget) append("A Luna analisa até $MAX_ATTACH_FILES arquivos/vídeos por mensagem.")
        if (isEmpty() && (images > 0 || files > 0)) {
            append("$images imagem${if (images == 1) "" else "ns"}")
            if (files > 0) append(" · $files arquivo${if (files == 1) "" else "s"}")
        }
    }
    if (msg.isBlank()) return
    Text(
        text = msg.trim(),
        color = OrbitTokens.textMid,
        fontSize = 12.sp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = OrbitMetrics.pagePadding, vertical = 4.dp),
    )
}

@Composable
private fun PermissionEmpty(
    onAllow: () -> Unit,
    onSystemPicker: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(OrbitMetrics.pagePadding),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "Acesse fotos e vídeos",
            color = OrbitTokens.textHigh,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Permita o acesso à galeria ou use o seletor do sistema.",
            color = OrbitTokens.textMid,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp,
        )
        Spacer(Modifier.height(20.dp))
        PrimaryChip(label = "Permitir acesso", onClick = onAllow)
        Spacer(Modifier.height(10.dp))
        SecondaryChip(label = "Abrir seletor do sistema", onClick = onSystemPicker)
    }
}

@Composable
private fun PrimaryChip(label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(OrbitMetrics.radiusPill))
            .background(OrbitTokens.accent)
            .orbitPressable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Text(label, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SecondaryChip(label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(OrbitMetrics.radiusPill))
            .border(1.dp, OrbitTokens.borderSoft, RoundedCornerShape(OrbitMetrics.radiusPill))
            .orbitPressable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Text(label, color = OrbitTokens.textHigh, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun GalleryGrid(
    media: List<GalleryMedia>,
    selectedIds: Set<String>,
    canSelect: (GalleryMedia) -> Boolean,
    onToggle: (GalleryMedia) -> Unit,
    onCamera: () -> Unit,
    onVideo: () -> Unit,
    onSystemPicker: () -> Unit,
    hasMore: Boolean,
    loadingMore: Boolean,
    onLoadMore: () -> Unit,
) {
    val gridState = rememberLazyGridState()
    val shouldLoadMore by remember {
        derivedStateOf {
            val info = gridState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            val total = info.totalItemsCount
            hasMore && !loadingMore && total > 0 && last >= total - 6
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) onLoadMore()
    }

    val density = LocalDensity.current
    val thumbPx = with(density) { 120.dp.roundToPx() }.coerceIn(96, 160)

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        state = gridState,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(key = "camera") {
            ActionTile(
                label = "Câmera",
                icon = {
                    Icon(
                        Icons.Rounded.PhotoCamera,
                        null,
                        tint = OrbitTokens.textHigh,
                        modifier = Modifier.size(26.dp),
                    )
                },
                onClick = onCamera,
            )
        }
        item(key = "video") {
            ActionTile(
                label = "Vídeo",
                icon = {
                    Icon(
                        Icons.Rounded.Videocam,
                        null,
                        tint = OrbitTokens.textHigh,
                        modifier = Modifier.size(26.dp),
                    )
                },
                onClick = onVideo,
            )
        }
        item(key = "picker") {
            ActionTile(
                label = "Mais",
                icon = {
                    Icon(
                        Icons.Rounded.Image,
                        null,
                        tint = OrbitTokens.textHigh,
                        modifier = Modifier.size(26.dp),
                    )
                },
                onClick = onSystemPicker,
            )
        }
        items(media, key = { it.id }) { item ->
            val isSelected = selectedIds.contains(item.id)
            val blocked = !isSelected && !canSelect(item)
            MediaTile(
                item = item,
                selected = isSelected,
                enabled = !blocked,
                thumbPx = thumbPx,
                onClick = { onToggle(item) },
            )
        }
        if (loadingMore) {
            item(key = "loading-more") {
                Box(
                    Modifier
                        .aspectRatio(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        color = OrbitTokens.accent,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionTile(
    label: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(10.dp))
            .background(OrbitTokens.surface)
            .border(1.dp, OrbitTokens.borderSoft, RoundedCornerShape(10.dp))
            .orbitPressable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            icon()
            Spacer(Modifier.height(6.dp))
            Text(label, color = OrbitTokens.textMid, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun MediaTile(
    item: GalleryMedia,
    selected: Boolean,
    enabled: Boolean,
    thumbPx: Int,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    Box(
        Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(10.dp))
            .background(OrbitTokens.surface)
            .then(if (enabled) Modifier.orbitPressable(onClick = onClick) else Modifier)
            .then(
                if (selected) Modifier.border(2.dp, OrbitTokens.accent, RoundedCornerShape(10.dp))
                else Modifier,
            ),
    ) {
        AsyncImage(
            model = galleryThumbRequest(context, item, thumbPx),
            contentDescription = item.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        if (item.isVideo) {
            Box(
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(6.dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
        if (selected) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(OrbitTokens.accent),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
            }
        }
    }
}

@Composable
private fun AlbumList(
    albums: List<GalleryAlbum>,
    selectedId: String,
    onSelect: (String) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = OrbitMetrics.pagePadding, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items(albums, key = { it.id }) { album ->
            val ativa = album.id == selectedId
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (ativa) OrbitTokens.accentSoft else Color.Transparent)
                    .orbitPressable { onSelect(album.id) }
                    .padding(horizontal = 12.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(OrbitTokens.surface),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.Folder,
                        null,
                        tint = OrbitTokens.accentText,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        album.title,
                        color = OrbitTokens.textHigh,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        "${album.count} item${if (album.count == 1) "" else "s"}",
                        color = OrbitTokens.textLow,
                        fontSize = 12.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun FileBrowserPanel(
    fileBudget: Int,
    onPickSystem: () -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = OrbitMetrics.pagePadding, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(OrbitMetrics.radiusCard))
                    .background(OrbitTokens.surface)
                    .border(1.dp, OrbitTokens.borderSoft, RoundedCornerShape(OrbitMetrics.radiusCard))
                    .orbitPressable(
                        enabled = fileBudget > 0,
                        onClick = onPickSystem,
                    )
                    .padding(16.dp),
            ) {
                Text(
                    "Escolher arquivos",
                    color = OrbitTokens.textHigh,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Abre o gerenciador do sistema pra anexar PDFs, docs e outros.",
                    color = OrbitTokens.textMid,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                )
                if (fileBudget == 0) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Limite de arquivos atingido nesta mensagem.",
                        color = OrbitTokens.textLow,
                        fontSize = 12.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun ConfirmAttachButton(count: Int, onClick: () -> Unit) {
    val label = if (count == 1) "Anexar 1 item" else "Anexar $count itens"
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = OrbitMetrics.pagePadding, vertical = 8.dp)
            .clip(RoundedCornerShape(OrbitMetrics.radiusPill))
            .background(OrbitTokens.accent)
            .orbitPressable(onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun AttachTabBar(
    active: AttachTab,
    onChange: (AttachTab) -> Unit,
    onDismiss: () -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(OrbitTokens.borderSoft.copy(alpha = 0.45f)),
        )
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AttachTab.entries.forEach { item ->
                val selected = item == active
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .orbitPressable { onChange(item) }
                        .padding(vertical = 4.dp),
                ) {
                    Box(
                        Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (selected) OrbitTokens.accentSoft else Color.Transparent),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector =
                                if (item == AttachTab.GALERIA) Icons.Rounded.Image
                                else Icons.Rounded.Description,
                            contentDescription = null,
                            tint = if (selected) OrbitTokens.accentText else OrbitTokens.textMid,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    Text(
                        if (item == AttachTab.GALERIA) "Galeria" else "Arquivo",
                        color = if (selected) OrbitTokens.accentText else OrbitTokens.textMid,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
            Box(
                Modifier
                    .padding(end = 12.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .orbitPressable(onClick = onDismiss),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.KeyboardArrowDown,
                    contentDescription = "Fechar anexos",
                    tint = OrbitTokens.textLow,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}
