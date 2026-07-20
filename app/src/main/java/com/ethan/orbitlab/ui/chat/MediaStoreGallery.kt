package com.ethan.orbitlab.ui.chat

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

const val GALLERY_PAGE_SIZE = 60
private const val GALLERY_CACHE_TTL_MS = 60_000L
private const val GALLERY_HARD_CAP = 600

data class GalleryLoadResult(
    val albums: List<GalleryAlbum>,
    val media: List<GalleryMedia>,
    val permissionGranted: Boolean,
    val hasMore: Boolean = false,
)

private data class GalleryCacheEntry(
    val result: GalleryLoadResult,
    val fetchedAtMs: Long,
)

/**
 * Cache em processo: evita re-query pesada ao reabrir a sheet.
 */
object GalleryCache {
    @Volatile
    private var entry: GalleryCacheEntry? = null

    fun getIfFresh(): GalleryLoadResult? {
        val e = entry ?: return null
        if (System.currentTimeMillis() - e.fetchedAtMs > GALLERY_CACHE_TTL_MS) return null
        return e.result
    }

    fun put(result: GalleryLoadResult) {
        entry = GalleryCacheEntry(result, System.currentTimeMillis())
    }

    fun invalidate() {
        entry = null
    }
}

fun galleryPermissionGranted(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= 33) {
        val images = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_MEDIA_IMAGES,
        ) == PackageManager.PERMISSION_GRANTED
        val videos = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_MEDIA_VIDEO,
        ) == PackageManager.PERMISSION_GRANTED
        images || videos
    } else {
        ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
        ) == PackageManager.PERMISSION_GRANTED
    }
}

fun galleryPermissions(): Array<String> =
    if (Build.VERSION.SDK_INT >= 33) {
        arrayOf(
            android.Manifest.permission.READ_MEDIA_IMAGES,
            android.Manifest.permission.READ_MEDIA_VIDEO,
        )
    } else {
        arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
    }

/**
 * Carrega a primeira página (ou usa cache fresco).
 * [forceRefresh] ignora o cache (ex.: após conceder permissão).
 */
suspend fun loadDeviceGallery(
    context: Context,
    forceRefresh: Boolean = false,
): GalleryLoadResult = withContext(Dispatchers.IO) {
    if (!galleryPermissionGranted(context)) {
        GalleryCache.invalidate()
        return@withContext GalleryLoadResult(emptyList(), emptyList(), permissionGranted = false)
    }
    if (!forceRefresh) {
        GalleryCache.getIfFresh()?.let { return@withContext it }
    }
    val page = loadGalleryPage(context, offset = 0, limit = GALLERY_PAGE_SIZE)
    GalleryCache.put(page)
    page
}

/**
 * Acrescenta a próxima página ao resultado atual.
 */
suspend fun loadMoreGallery(
    context: Context,
    current: GalleryLoadResult,
): GalleryLoadResult = withContext(Dispatchers.IO) {
    if (!current.permissionGranted || !current.hasMore) return@withContext current
    if (!galleryPermissionGranted(context)) {
        return@withContext current.copy(permissionGranted = false, hasMore = false)
    }
    val next = loadGalleryPage(context, offset = current.media.size, limit = GALLERY_PAGE_SIZE)
    val merged = (current.media + next.media).distinctBy { it.id }
    val albums = buildAlbums(merged)
    val result = GalleryLoadResult(
        albums = albums,
        media = merged,
        permissionGranted = true,
        hasMore = next.hasMore && merged.size < GALLERY_HARD_CAP,
    )
    GalleryCache.put(result)
    result
}

private fun loadGalleryPage(context: Context, offset: Int, limit: Int): GalleryLoadResult {
    // Busca um pouco a mais de cada tipo para mesclar por data sem buracos óbvios.
    val fetch = (offset + limit).coerceAtMost(GALLERY_HARD_CAP)
    val images = queryImages(context, limit = fetch)
    val videos = queryVideos(context, limit = fetch)
    val sorted = (images + videos).sortedByDescending { it.dateAddedSec }
    val page = sorted.drop(offset).take(limit)
    val hasMore = sorted.size > offset + page.size && (offset + page.size) < GALLERY_HARD_CAP
    return GalleryLoadResult(
        albums = buildAlbums(page),
        media = page,
        permissionGranted = true,
        hasMore = hasMore,
    )
}

private fun buildAlbums(media: List<GalleryMedia>): List<GalleryAlbum> = buildList {
    add(GalleryAlbum("all", "Fotos e vídeos", media.size))
    media.groupBy { it.albumId }
        .entries
        .sortedByDescending { it.value.size }
        .forEach { (id, items) ->
            if (id.isNotBlank() && id != "all") {
                add(GalleryAlbum(id, id, items.size))
            }
        }
}

private fun queryImages(context: Context, limit: Int): List<GalleryMedia> {
    val out = mutableListOf<GalleryMedia>()
    val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    val projection = arrayOf(
        MediaStore.Images.Media._ID,
        MediaStore.Images.Media.DISPLAY_NAME,
        MediaStore.Images.Media.SIZE,
        MediaStore.Images.Media.MIME_TYPE,
        MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
        MediaStore.Images.Media.DATE_ADDED,
    )
    queryMediaStore(context, collection, projection, limit)?.use { cursor ->
        val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
        val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
        val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
        val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
        val bucketCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
        val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
        var n = 0
        while (cursor.moveToNext() && n < limit) {
            val id = cursor.getLong(idCol)
            out += GalleryMedia(
                id = "img-$id",
                albumId = cursor.getString(bucketCol)?.ifBlank { "Outros" } ?: "Outros",
                name = cursor.getString(nameCol) ?: "foto.jpg",
                uri = ContentUris.withAppendedId(collection, id),
                mime = cursor.getString(mimeCol) ?: "image/jpeg",
                sizeBytes = cursor.getLong(sizeCol).coerceAtLeast(0L),
                isVideo = false,
                dateAddedSec = cursor.getLong(dateCol),
            )
            n++
        }
    }
    return out
}

private fun queryVideos(context: Context, limit: Int): List<GalleryMedia> {
    val out = mutableListOf<GalleryMedia>()
    val collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
    val projection = arrayOf(
        MediaStore.Video.Media._ID,
        MediaStore.Video.Media.DISPLAY_NAME,
        MediaStore.Video.Media.SIZE,
        MediaStore.Video.Media.MIME_TYPE,
        MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
        MediaStore.Video.Media.DATE_ADDED,
    )
    queryMediaStore(context, collection, projection, limit)?.use { cursor ->
        val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
        val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
        val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
        val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
        val bucketCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
        val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
        var n = 0
        while (cursor.moveToNext() && n < limit) {
            val id = cursor.getLong(idCol)
            out += GalleryMedia(
                id = "vid-$id",
                albumId = cursor.getString(bucketCol)?.ifBlank { "Vídeos" } ?: "Vídeos",
                name = cursor.getString(nameCol) ?: "video.mp4",
                uri = ContentUris.withAppendedId(collection, id),
                mime = cursor.getString(mimeCol) ?: "video/mp4",
                sizeBytes = cursor.getLong(sizeCol).coerceAtLeast(0L),
                isVideo = true,
                dateAddedSec = cursor.getLong(dateCol),
            )
            n++
        }
    }
    return out
}

private fun queryMediaStore(
    context: Context,
    collection: Uri,
    projection: Array<String>,
    limit: Int,
): Cursor? {
    val sortOrder = "${MediaStore.MediaColumns.DATE_ADDED} DESC"
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val args = Bundle().apply {
            putInt(ContentResolver.QUERY_ARG_LIMIT, limit)
            putStringArray(
                ContentResolver.QUERY_ARG_SORT_COLUMNS,
                arrayOf(MediaStore.MediaColumns.DATE_ADDED),
            )
            putInt(
                ContentResolver.QUERY_ARG_SORT_DIRECTION,
                ContentResolver.QUERY_SORT_DIRECTION_DESCENDING,
            )
        }
        context.contentResolver.query(collection, projection, args, null)
    } else {
        context.contentResolver.query(collection, projection, null, null, sortOrder)
    }
}

fun mediaForAlbum(all: List<GalleryMedia>, albumId: String): List<GalleryMedia> =
    if (albumId == "all") all else all.filter { it.albumId == albumId }
