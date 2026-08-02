package com.ethan.orbitlab.ui.chat

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ethan.orbitlab.ui.theme.OrbitMotion
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Size
import com.ethan.orbitlab.ui.theme.orbitPressable
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Viewer imersivo com pager: desliza entre mídias da mensagem.
 * Pinch / double-tap / pan em imagens; ExoPlayer em vídeos.
 */
@Composable
fun MediaViewerDialog(
    items: List<ComposerAttachment>,
    initialIndex: Int = 0,
    onDismiss: () -> Unit,
    onReferenciar: ((ComposerAttachment) -> Unit)? = null,
    // Ações extra pra imagens que vivem numa URL remota (ex.: as da Luna): baixar e
    // compartilhar precisam trazer os bytes de volta, então quem chama injeta a lógica.
    onDownload: (() -> Unit)? = null,
    onShareOverride: (() -> Unit)? = null,
) {
    val media = remember(items) {
        items.filter {
            (it.kind == AttachmentKind.IMAGE || it.kind == AttachmentKind.VIDEO) && it.uri != null
        }
    }
    if (media.isEmpty()) return

    val start = initialIndex.coerceIn(0, media.lastIndex)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var chromeVisible by remember { mutableStateOf(true) }
    var pageZoomed by remember { mutableStateOf(false) }

    val pagerState = rememberPagerState(
        initialPage = start,
        pageCount = { media.size },
    )
    val stripState = rememberLazyListState()

    // Ao trocar de página, libera zoom e centraliza a miniatura
    LaunchedEffect(pagerState.currentPage) {
        pageZoomed = false
        if (media.size > 1) {
            val target = (pagerState.currentPage - 1).coerceAtLeast(0)
            stripState.animateScrollToItem(target)
        }
    }

    val atual = media[pagerState.currentPage]
    val uri = atual.uri ?: return

    fun toggleChrome() {
        chromeVisible = !chromeVisible
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black),
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = !pageZoomed,
                beyondViewportPageCount = 1,
                key = { page -> media[page].id },
            ) { page ->
                val item = media[page]
                val pageUri = item.uri ?: return@HorizontalPager
                when (item.kind) {
                    AttachmentKind.IMAGE -> {
                        ZoomableImage(
                            uri = pageUri,
                            contentDescription = "Imagem",
                            onTap = { toggleChrome() },
                            onSwipeDismiss = onDismiss,
                            onScaleChange = { scale ->
                                if (page == pagerState.currentPage) {
                                    pageZoomed = scale > 1.05f
                                }
                            },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    AttachmentKind.VIDEO -> {
                        // Só reproduz a página visível — evita vários players
                        if (page == pagerState.currentPage) {
                            ImmersiveVideoPlayer(
                                uri = pageUri,
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            Box(Modifier.fillMaxSize().background(Color.Black))
                        }
                    }
                    AttachmentKind.FILE -> Unit
                }
            }

            AnimatedVisibility(
                visible = chromeVisible,
                enter = fadeIn(OrbitMotion.tweenFast) + slideInVertically { -it / 3 },
                exit = fadeOut(OrbitMotion.tweenInstant) + slideOutVertically { -it / 3 },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth(),
            ) {
                val contador = if (media.size > 1) {
                    "${pagerState.currentPage + 1} / ${media.size}"
                } else {
                    ""
                }
                val titulo = when (atual.kind) {
                    AttachmentKind.VIDEO -> "Vídeo"
                    else -> "Foto"
                }
                ViewerChrome(
                    title = if (contador.isNotBlank()) "$titulo · $contador" else titulo,
                    subtitle = atual.sizeLabel.takeIf { it != "—" } ?: "",
                    onClose = onDismiss,
                    onDownload = onDownload,
                    onShare = onShareOverride ?: {
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = atual.mime.ifBlank { "*/*" }
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(
                            Intent.createChooser(send, "Partilhar"),
                        )
                    },
                    onReferenciar = onReferenciar?.let { cb ->
                        {
                            cb(atual)
                            onDismiss()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding(),
                )
            }

            // Carrossel de miniaturas — aparece ao tocar (junto com o chrome)
            AnimatedVisibility(
                visible = chromeVisible && media.size > 1,
                enter = fadeIn(OrbitMotion.tweenFast) + slideInVertically { it / 2 },
                exit = fadeOut(OrbitMotion.tweenInstant) + slideOutVertically { it / 2 },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
            ) {
                MediaFilmstrip(
                    items = media,
                    selectedIndex = pagerState.currentPage,
                    listState = stripState,
                    onSelect = { index ->
                        scope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    },
                )
            }
        }
    }
}

/** Compat: um único item. */
@Composable
fun MediaViewerDialog(
    item: ComposerAttachment,
    onDismiss: () -> Unit,
    onReferenciar: ((ComposerAttachment) -> Unit)? = null,
    onDownload: (() -> Unit)? = null,
    onShareOverride: (() -> Unit)? = null,
) {
    MediaViewerDialog(
        items = listOf(item),
        initialIndex = 0,
        onDismiss = onDismiss,
        onReferenciar = onReferenciar,
        onDownload = onDownload,
        onShareOverride = onShareOverride,
    )
}

@Composable
private fun MediaFilmstrip(
    items: List<ComposerAttachment>,
    selectedIndex: Int,
    listState: LazyListState,
    onSelect: (Int) -> Unit,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val thumbPx = with(density) { 72.dp.roundToPx() }.coerceIn(56, 96)

    Column(
        Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    0f to Color.Transparent,
                    0.35f to Color.Black.copy(alpha = 0.55f),
                    1f to Color.Black.copy(alpha = 0.82f),
                ),
            )
            .navigationBarsPadding()
            .padding(top = 20.dp, bottom = 12.dp),
    ) {
        LazyRow(
            state = listState,
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
                val selected = index == selectedIndex
                val shape = RoundedCornerShape(10.dp)
                Box(
                    Modifier
                        .size(width = 56.dp, height = 56.dp)
                        .clip(shape)
                        .then(
                            if (selected) {
                                Modifier.border(2.dp, Color.White, shape)
                            } else {
                                Modifier.border(1.dp, Color.White.copy(alpha = 0.2f), shape)
                            },
                        )
                        .orbitPressable(onClick = { onSelect(index) }),
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(item.uri)
                            .size(Size(thumbPx, thumbPx))
                            .memoryCacheKey("strip-${item.id}")
                            .diskCacheKey("strip-${item.id}")
                            .crossfade(false)
                            .allowHardware(true)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                    if (!selected) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.28f)),
                        )
                    }
                    if (item.kind == AttachmentKind.VIDEO) {
                        Box(
                            Modifier
                                .align(Alignment.Center)
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f)),
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
                }
            }
        }
    }
}

@Composable
private fun ViewerChrome(
    title: String,
    subtitle: String,
    onClose: () -> Unit,
    onShare: () -> Unit,
    onReferenciar: (() -> Unit)? = null,
    onDownload: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .background(Color.Black.copy(alpha = 0.42f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ChromeIconButton(onClick = onClose, contentDescription = "Fechar") {
            Icon(
                Icons.Rounded.Close,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle.isNotBlank()) {
                Text(
                    subtitle,
                    color = Color.White.copy(alpha = 0.65f),
                    fontSize = 12.sp,
                )
            }
        }
        if (onReferenciar != null) {
            ChromeIconButton(onClick = onReferenciar, contentDescription = "Referenciar") {
                Icon(
                    Icons.Rounded.FormatQuote,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(4.dp))
        }
        if (onDownload != null) {
            ChromeIconButton(onClick = onDownload, contentDescription = "Salvar na galeria") {
                Icon(
                    Icons.Rounded.Download,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(4.dp))
        }
        ChromeIconButton(onClick = onShare, contentDescription = "Partilhar") {
            Icon(
                Icons.Rounded.Share,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun ChromeIconButton(
    onClick: () -> Unit,
    contentDescription: String,
    content: @Composable () -> Unit,
) {
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.12f))
            .orbitPressable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
private fun ZoomableImage(
    uri: Uri,
    contentDescription: String?,
    onTap: () -> Unit,
    onSwipeDismiss: () -> Unit,
    onScaleChange: (Float) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var scale by remember(uri) { mutableFloatStateOf(1f) }
    var offset by remember(uri) { mutableStateOf(Offset.Zero) }
    val dismissY = remember(uri) { Animatable(0f) }
    val dismissAlpha = remember(uri) { Animatable(1f) }

    LaunchedEffect(scale) { onScaleChange(scale) }

    Box(
        modifier
            .graphicsLayer {
                translationY = dismissY.value
                alpha = dismissAlpha.value
            }
            .pointerInput(uri) {
                detectTapGestures(
                    onTap = { onTap() },
                    onDoubleTap = {
                        if (scale > 1.05f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            scale = 2.5f
                        }
                    },
                )
            }
            .pointerInput(uri) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        val canceled = event.changes.fastAny { it.isConsumed }
                        if (!canceled) {
                            val zoomChange = event.calculateZoom()
                            val panChange = event.calculatePan()
                            val singleFinger = event.changes.count { it.pressed } == 1
                            val zoomed = scale > 1.05f

                            // Em 1x, swipe horizontal fica pro pager — não consumir
                            val horizontalForPager = !zoomed &&
                                singleFinger &&
                                zoomChange == 1f &&
                                abs(panChange.x) > abs(panChange.y) * 1.15f &&
                                abs(panChange.x) > 2f

                            if (horizontalForPager) {
                                // deixa o HorizontalPager receber o gesto
                            } else if (!zoomed &&
                                singleFinger &&
                                abs(panChange.y) > abs(panChange.x) * 1.35f &&
                                abs(panChange.y) > 2f
                            ) {
                                val next = (dismissY.value + panChange.y).coerceAtLeast(0f)
                                scope.launch {
                                    dismissY.snapTo(next)
                                    dismissAlpha.snapTo(
                                        (1f - next / size.height.toFloat()).coerceIn(0.35f, 1f),
                                    )
                                }
                                event.changes.fastForEach {
                                    if (it.positionChanged()) it.consume()
                                }
                            } else if (zoomChange != 1f || (zoomed && panChange != Offset.Zero)) {
                                val newScale = (scale * zoomChange).coerceIn(1f, 5f)
                                scale = newScale
                                if (newScale > 1f) {
                                    val maxX = size.width * (newScale - 1f) / 2f
                                    val maxY = size.height * (newScale - 1f) / 2f
                                    offset = Offset(
                                        x = (offset.x + panChange.x).coerceIn(-maxX, maxX),
                                        y = (offset.y + panChange.y).coerceIn(-maxY, maxY),
                                    )
                                } else {
                                    offset = Offset.Zero
                                }
                                if (newScale > 1.05f && dismissY.value != 0f) {
                                    scope.launch {
                                        dismissY.snapTo(0f)
                                        dismissAlpha.snapTo(1f)
                                    }
                                }
                                event.changes.fastForEach {
                                    if (it.positionChanged()) it.consume()
                                }
                            }
                        }
                    } while (event.changes.fastAny { it.pressed })

                    if (dismissY.value > size.height * 0.18f) {
                        onSwipeDismiss()
                    } else if (dismissY.value > 0f) {
                        scope.launch {
                            dismissY.animateTo(0f, spring())
                            dismissAlpha.animateTo(1f, spring())
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(uri)
                .size(Size.ORIGINAL)
                .memoryCacheKey("viewer-$uri")
                .crossfade(false)
                .allowHardware(true)
                .build(),
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                },
        )
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun ImmersiveVideoPlayer(
    uri: Uri,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val player = remember(uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(player) {
        onDispose { player.release() }
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                useController = true
                controllerShowTimeoutMs = 2800
                controllerHideOnTouch = true
                setShowNextButton(false)
                setShowPreviousButton(false)
                this.player = player
            }
        },
        update = { it.player = player },
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .background(Color.Black),
    )
}

fun openAttachmentExternally(context: Context, item: ComposerAttachment) {
    val uri = item.uri ?: return
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, item.mime.ifBlank { "*/*" })
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    runCatching {
        context.startActivity(Intent.createChooser(intent, "Abrir com"))
    }
}
