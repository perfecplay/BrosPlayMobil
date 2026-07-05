package com.nuvio.app.core.auth

import platform.Foundation.NSUserDefaults

actual object AuthStorage {
    private const val KEY_ANONYMOUS_USER_ID = "anonymous_user_id"
    private const val KEY_PANEL_ACCESS_TOKEN = "panel_access_token"
    private const val KEY_PANEL_REFRESH_TOKEN = "panel_refresh_token"
    private const val KEY_PANEL_USER_ID = "panel_user_id"
    private const val KEY_PANEL_EMAIL = "panel_email"
    private const val KEY_PANEL_DISPLAY_NAME = "panel_display_name"
    private const val KEY_PANEL_SERVER_URL = "panel_server_url"

    actual fun loadAnonymousUserId(): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(KEY_ANONYMOUS_USER_ID)

    actual fun saveAnonymousUserId(userId: String) {
        NSUserDefaults.standardUserDefaults.setObject(userId, forKey = KEY_ANONYMOUS_USER_ID)
    }

    actual fun clearAnonymousUserId() {
        NSUserDefaults.standardUserDefaults.removeObjectForKey(KEY_ANONYMOUS_USER_ID)
    }

    actual fun loadPanelAccessToken(): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(KEY_PANEL_ACCESS_TOKEN)

    actual fun loadPanelUserId(): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(KEY_PANEL_USER_ID)

    actual fun loadPanelEmail(): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(KEY_PANEL_EMAIL)

    actual fun savePanelSession(
        accessToken: String,
        refreshToken: String?,
        userId: String,
        email: String,
        displayName: String?,
        serverUrl: String?,
    ) {
        val defaults = NSUserDefaults.standardUserDefaults
        defaults.setObject(accessToken, forKey = KEY_PANEL_ACCESS_TOKEN)
        refreshToken?.let { defaults.setObject(it, forKey = KEY_PANEL_REFRESH_TOKEN) } ?: defaults.removeObjectForKey(KEY_PANEL_REFRESH_TOKEN)
        defaults.setObject(userId, forKey = KEY_PANEL_USER_ID)
        defaults.setObject(email, forKey = KEY_PANEL_EMAIL)
        displayName?.let { defaults.setObject(it, forKey = KEY_PANEL_DISPLAY_NAME) } ?: defaults.removeObjectForKey(KEY_PANEL_DISPLAY_NAME)
        serverUrl?.let { defaults.setObject(it, forKey = KEY_PANEL_SERVER_URL) } ?: defaults.removeObjectForKey(KEY_PANEL_SERVER_URL)
    }

    actual fun clearPanelSession() {
        val defaults = NSUserDefaults.standardUserDefaults
        defaults.removeObjectForKey(KEY_PANEL_ACCESS_TOKEN)
        defaults.removeObjectForKey(KEY_PANEL_REFRESH_TOKEN)
        defaults.removeObjectForKey(KEY_PANEL_USER_ID)
        defaults.removeObjectForKey(KEY_PANEL_EMAIL)
        defaults.removeObjectForKey(KEY_PANEL_DISPLAY_NAME)
        defaults.removeObjectForKey(KEY_PANEL_SERVER_URL)
    }
}
