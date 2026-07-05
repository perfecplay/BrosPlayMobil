package com.nuvio.app.core.panel

import com.nuvio.app.core.auth.AuthStorage
import com.nuvio.app.features.addons.RawHttpResponse
import com.nuvio.app.features.addons.httpGetTextWithHeaders
import com.nuvio.app.features.addons.httpPostJsonWithHeaders
import com.nuvio.app.features.addons.httpRequestRaw
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

internal object PanelCloudService {
    private const val DEFAULT_PANEL_BASE_URL = "https://cockpit.hiremco.xyz/apps/cockpit/cockpit-v2-8-2/api/nuvio/"

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private val baseUrl: String
        get() = DEFAULT_PANEL_BASE_URL.trimEnd('/') + "/"

    fun hasSession(): Boolean = !AuthStorage.loadPanelAccessToken().isNullOrBlank()

    fun accessToken(): String? = AuthStorage.loadPanelAccessToken()

    suspend fun getPortals(): List<PanelPortalDto> {
        return runCatching {
            val payload = httpGetTextWithHeaders(
                url = baseUrl + "portals.php",
                headers = mapOf("Accept" to "application/json"),
            )
            json.decodeFromString<PanelPortalListResponse>(payload).portals
        }.getOrElse { emptyList() }
    }

    suspend fun login(username: String, password: String, serverUrl: String? = null): PanelLoginResponse {
        val normalizedUsername = username.trim()
        val explicitServerUrl = serverUrl?.trim()?.takeIf { it.isNotBlank() }
        val portalUrls = if (explicitServerUrl != null) {
            listOf(explicitServerUrl)
        } else {
            getPortals().mapNotNull { it.url.trim().takeIf { url -> url.isNotBlank() } }
        }

        val requests = mutableListOf<JsonObject>()

        // Try the compact payload first for compatibility with old panel builds.
        requests += buildJsonObject {
            put("username", normalizedUsername)
            put("password", password)
        }

        // Current Cockpit panel may require server_url. TV mod logs proved this is the
        // working flow, so mobile tries every portal from portals.php when no portal
        // is selected in the UI.
        for (portalUrl in portalUrls) {
            requests += buildJsonObject {
                put("server_url", portalUrl)
                put("username", normalizedUsername)
                put("password", password)
                put("device_name", "NuvioMobile")
            }
        }

        var lastError: Throwable? = null
        for (request in requests.distinctBy { it.toString() }) {
            try {
                val payload = httpPostJsonWithHeaders(
                    url = baseUrl + "api/auth/login",
                    body = request.toString(),
                    headers = mapOf("Accept" to "application/json"),
                )
                return json.decodeFromString<PanelLoginResponse>(payload)
            } catch (error: Throwable) {
                lastError = error
            }
        }
        throw lastError ?: IllegalStateException("Panel login failed")
    }

    suspend fun getAddons(profileId: Int): List<PanelAddonDto> {
        val token = accessToken() ?: return emptyList()
        val payload = httpGetTextWithHeaders(
            url = baseUrl + "api/addons?profile_id=$profileId",
            headers = mapOf("Authorization" to "Bearer $token"),
        )
        return json.decodeFromString<PanelAddonListResponse>(payload).addons
    }

    suspend fun getPluginsRaw(profileId: Int): RawHttpResponse {
        val token = accessToken() ?: return RawHttpResponse(401, "Unauthorized", baseUrl, "", emptyMap())
        return httpRequestRaw(
            method = "GET",
            url = baseUrl + "api/plugins?profile_id=$profileId",
            headers = mapOf(
                "Accept" to "application/json",
                "Authorization" to "Bearer $token",
            ),
            body = "",
        )
    }

    fun decodePlugins(body: String): List<PanelPluginDto> =
        json.decodeFromString<PanelPluginListResponse>(body).plugins

    suspend fun isStremioAddonManifest(url: String): Boolean {
        val normalized = normalizeAddonUrl(url)
        return runCatching {
            val payload = httpGetTextWithHeaders(normalized, mapOf("Accept" to "application/json"))
            val root = json.parseToJsonElement(payload).jsonObject
            root["id"]?.jsonPrimitive?.contentOrNull?.isNotBlank() == true
        }.getOrDefault(false)
    }

    suspend fun isPluginRepositoryUrl(url: String): Boolean {
        val clean = url.substringBefore("?").trimEnd('/').lowercase()
        val fileName = clean.substringAfterLast('/')
        if (fileName == "repo.json" || fileName == "plugins.json") return true
        if (fileName != "manifest.json") return false
        return !isStremioAddonManifest(url)
    }

    fun normalizeAddonUrl(url: String): String {
        val trimmed = url.trim()
        val withScheme = when {
            trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
            trimmed.startsWith("stremio://") -> "https://${trimmed.removePrefix("stremio://")}"
            else -> "https://$trimmed"
        }
        val query = withScheme.substringAfter("?", "")
        val path = withScheme.substringBefore("?").trimEnd('/')
        val manifestPath = if (path.endsWith("/manifest.json") || path.endsWith(".json")) path else "$path/manifest.json"
        return if (query.isBlank()) manifestPath else "$manifestPath?$query"
    }

    fun normalizePluginUrl(url: String): String {
        val trimmed = url.trim()
        val withScheme = when {
            trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
            else -> "https://$trimmed"
        }
        val query = withScheme.substringAfter("?", "")
        val path = withScheme.substringBefore("?").trimEnd('/')
        val normalizedPath = when {
            path.endsWith("/manifest.json") -> path
            path.endsWith("/repo.json") -> path
            path.endsWith("/plugins.json") -> path
            path.endsWith(".json") -> path
            else -> "$path/manifest.json"
        }
        return if (query.isBlank()) normalizedPath else "$normalizedPath?$query"
    }
}

@Serializable
internal data class PanelPortalListResponse(
    val portals: List<PanelPortalDto> = emptyList(),
)

@Serializable
internal data class PanelPortalDto(
    val id: Int = 0,
    val name: String = "",
    val url: String = "",
)

@Serializable
internal data class PanelLoginResponse(
    val user: PanelUserDto,
    val tokens: PanelTokenDto,
)

@Serializable
internal data class PanelUserDto(
    val id: String,
    val email: String,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("server_url") val serverUrl: String? = null,
)

@Serializable
internal data class PanelTokenDto(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("expires_in") val expiresIn: Long? = null,
    @SerialName("token_type") val tokenType: String? = null,
)

@Serializable
internal data class PanelAddonListResponse(
    val addons: List<PanelAddonDto> = emptyList(),
)

@Serializable
internal data class PanelPluginListResponse(
    val plugins: List<PanelPluginDto> = emptyList(),
)

@Serializable
internal data class PanelAddonDto(
    val url: String,
    val name: String? = null,
    val enabled: Boolean = true,
    @SerialName("sort_order") val sortOrder: Int = 0,
    @SerialName("profile_id") val profileId: Int = 1,
)

@Serializable
internal data class PanelPluginDto(
    val url: String,
    val name: String? = null,
    val enabled: Boolean = true,
    @SerialName("sort_order") val sortOrder: Int = 0,
    @SerialName("profile_id") val profileId: Int = 1,
    @SerialName("repo_type") val repoType: String? = null,
)
