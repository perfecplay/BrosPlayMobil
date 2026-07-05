package com.nuvio.app.core.auth

import android.content.Context
import android.content.SharedPreferences

actual object AuthStorage {
    private const val PREFS_NAME = "nuvio_auth"
    private const val KEY_ANONYMOUS_USER_ID = "anonymous_user_id"
    private const val KEY_PANEL_ACCESS_TOKEN = "panel_access_token"
    private const val KEY_PANEL_REFRESH_TOKEN = "panel_refresh_token"
    private const val KEY_PANEL_USER_ID = "panel_user_id"
    private const val KEY_PANEL_EMAIL = "panel_email"
    private const val KEY_PANEL_DISPLAY_NAME = "panel_display_name"
    private const val KEY_PANEL_SERVER_URL = "panel_server_url"

    private var preferences: SharedPreferences? = null

    fun initialize(context: Context) {
        preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    actual fun loadAnonymousUserId(): String? =
        preferences?.getString(KEY_ANONYMOUS_USER_ID, null)

    actual fun saveAnonymousUserId(userId: String) {
        preferences?.edit()?.putString(KEY_ANONYMOUS_USER_ID, userId)?.apply()
    }

    actual fun clearAnonymousUserId() {
        preferences?.edit()?.remove(KEY_ANONYMOUS_USER_ID)?.apply()
    }

    actual fun loadPanelAccessToken(): String? = preferences?.getString(KEY_PANEL_ACCESS_TOKEN, null)

    actual fun loadPanelUserId(): String? = preferences?.getString(KEY_PANEL_USER_ID, null)

    actual fun loadPanelEmail(): String? = preferences?.getString(KEY_PANEL_EMAIL, null)

    actual fun savePanelSession(
        accessToken: String,
        refreshToken: String?,
        userId: String,
        email: String,
        displayName: String?,
        serverUrl: String?,
    ) {
        preferences?.edit()?.apply {
            putString(KEY_PANEL_ACCESS_TOKEN, accessToken)
            if (refreshToken != null) putString(KEY_PANEL_REFRESH_TOKEN, refreshToken) else remove(KEY_PANEL_REFRESH_TOKEN)
            putString(KEY_PANEL_USER_ID, userId)
            putString(KEY_PANEL_EMAIL, email)
            if (displayName != null) putString(KEY_PANEL_DISPLAY_NAME, displayName) else remove(KEY_PANEL_DISPLAY_NAME)
            if (serverUrl != null) putString(KEY_PANEL_SERVER_URL, serverUrl) else remove(KEY_PANEL_SERVER_URL)
        }?.apply()
    }

    actual fun clearPanelSession() {
        preferences?.edit()
            ?.remove(KEY_PANEL_ACCESS_TOKEN)
            ?.remove(KEY_PANEL_REFRESH_TOKEN)
            ?.remove(KEY_PANEL_USER_ID)
            ?.remove(KEY_PANEL_EMAIL)
            ?.remove(KEY_PANEL_DISPLAY_NAME)
            ?.remove(KEY_PANEL_SERVER_URL)
            ?.apply()
    }
}
