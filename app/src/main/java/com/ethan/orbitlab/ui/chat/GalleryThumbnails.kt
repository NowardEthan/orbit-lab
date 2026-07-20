package com.ethan.orbitlab.ui.chat

import android.content.Context
import coil.request.ImageRequest
import coil.size.Size

/** Pedido Coil enxuto para a grade de anexos. */
fun galleryThumbRequest(
    context: Context,
    item: GalleryMedia,
    sizePx: Int,
): ImageRequest =
    ImageRequest.Builder(context)
        .data(item.uri)
        .size(Size(sizePx, sizePx))
        .memoryCacheKey(item.id)
        .diskCacheKey(item.id)
        .crossfade(false)
        .allowHardware(true)
        .build()

fun attachmentThumbRequest(
    context: Context,
    item: ComposerAttachment,
    sizePx: Int,
): ImageRequest =
    ImageRequest.Builder(context)
        .data(item.uri)
        .size(Size(sizePx, sizePx))
        .memoryCacheKey(item.id)
        .diskCacheKey(item.id)
        .crossfade(false)
        .allowHardware(true)
        .build()
