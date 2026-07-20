package com.ethan.orbitlab.updates

/**
 * Schema do manifesto público em orbit-releases (`updates.json` / `updates-beta.json`).
 * Mesmo contrato do orbit-mobile — o Lab vai substituir o Expo no mesmo canal.
 */
data class OrbitNewsItem(
    val id: String,
    val date: String,
    val tag: OrbitNewsTag,
    val title: String,
    val body: String,
    val version: String? = null,
)

enum class OrbitNewsTag {
    NOVIDADE,
    CORRECAO,
    AVISO,
}

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

/** Agrupa o mural por versão (mais recentes primeiro; sem versão no fim). */
fun groupNewsByVersion(news: List<OrbitNewsItem>): List<OrbitNewsSection> {
    val order = mutableListOf<String>()
    val map = linkedMapOf<String, MutableList<OrbitNewsItem>>()
    val dates = mutableMapOf<String, String>()

    for (item in news) {
        val key = item.version ?: "__sem_versao__"
        if (key !in map) {
            map[key] = mutableListOf()
            order.add(key)
            dates[key] = item.date
        }
        map.getValue(key).add(item)
        val current = dates.getValue(key)
        if (item.date > current) dates[key] = item.date
    }

    return order
        .map { key ->
            OrbitNewsSection(
                key = key,
                version = key.takeUnless { it == "__sem_versao__" },
                date = dates.getValue(key),
                items = map.getValue(key),
            )
        }
        .sortedBy { if (it.version == null) 1 else 0 }
}
