package com.nuvio.app.core.auth

internal expect object AuthStorage {
    fun loadAnonymousUserId(): String?
    fun saveAnonymousUserId(userId: String)
    fun clearAnonymousUserId()

    fun loadPanelAccessToken(): String?
    fun loadPanelUserId(): String?
    fun loadPanelEmail(): String?
    fun savePanelSession(
        accessToken: String,
        refreshToken: String?,
        userId: String,
        email: String,
        displayName: String?,
        serverUrl: String?,
    )
    fun clearPanelSession()
}
