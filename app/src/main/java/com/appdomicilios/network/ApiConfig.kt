package com.appdomicilios.network

import com.appdomicilios.BuildConfig

object ApiConfig {
    val baseUrls: List<String> = buildList {
        add(BuildConfig.REMOTE_API_BASE_URL)
        add(BuildConfig.API_BASE_URL)
        if (BuildConfig.DEBUG) {
            add(BuildConfig.EMULATOR_API_BASE_URL)
            add(BuildConfig.LAN_API_BASE_URL)
        }
    }.map(::ensureTrailingSlash).distinct()

    fun resolveMediaUrl(path: String?): String? {
        if (path.isNullOrBlank()) {
            return null
        }

        return when {
            path.startsWith("http://", ignoreCase = true) || path.startsWith("https://", ignoreCase = true) -> path
            else -> baseUrls.firstOrNull()?.plus(path.trimStart('/'))
        }
    }

    private fun ensureTrailingSlash(value: String): String {
        return if (value.endsWith("/")) value else "$value/"
    }
}
