package com.ethan.orbitlab.data.updates

/** Tag de uma novidade no manifesto público. */
enum class OrbitNewsTag {
    NOVIDADE,
    CORRECAO,
    AVISO,
}

data class OrbitNewsItem(
    val id: String,
    val date: String,
    val tag: OrbitNewsTag,
    val title: String,
    val body: String,
    val version: String? = null,
)

data class OrbitManifest(
    val app: String? = null,
    val manifestVersion: Int? = null,
    val latestVersion: String,
    val latestVersionCode: Int? = null,
    val publishedAt: String? = null,
    val apkUrl: String? = null,
    val minSupportedVersion: String? = null,
    val mandatory: Boolean = false,
    val news: List<OrbitNewsItem> = emptyList(),
)

data class OrbitNewsSection(
    val key: String,
    val version: String?,
    val date: String,
    val items: List<OrbitNewsItem>,
)
