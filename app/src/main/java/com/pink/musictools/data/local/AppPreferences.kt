package com.pink.musictools.data.local

import android.content.Context

/**
 * 应用本地偏好设置，存储 API Key 等用户配置。
 */
class AppPreferences(context: Context) {

    private val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    var openAiApiKey: String
        get() = prefs.getString(KEY_OPENAI_API_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_OPENAI_API_KEY, value).apply()

    companion object {
        private const val KEY_OPENAI_API_KEY = "openai_api_key"
    }
}
