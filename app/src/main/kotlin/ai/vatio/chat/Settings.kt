package ai.vatio.chat

import android.content.Context
import androidx.core.content.edit

/** Which agent the app talks to, and as whom. */
data class Settings(
    val workspace: String = AppConfig.WORKSPACE,
    val token: String = AppConfig.TOKEN,
    val baseUrl: String = AppConfig.BASE_URL,
    /**
     * A JWT your backend signed for a signed-in user, so the agent knows who
     * they are and can use private tools. Null for an anonymous visitor.
     */
    val visitorToken: String? = null,
)

/**
 * Keeps [Settings] in SharedPreferences. Clearing them falls back to
 * [AppConfig], so a newer build's defaults are not shadowed by old copies.
 */
class SettingsStore(context: Context) {
    // A file of its own: the SDK keeps the conversation and visitor id in "vatio".
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    fun load(): Settings {
        val defaults = Settings()
        return Settings(
            workspace = prefs.getString(WORKSPACE, null) ?: defaults.workspace,
            token = prefs.getString(TOKEN, null) ?: defaults.token,
            baseUrl = prefs.getString(BASE_URL, null) ?: defaults.baseUrl,
            visitorToken = prefs.getString(VISITOR_TOKEN, null),
        )
    }

    fun save(settings: Settings) = prefs.edit {
        putString(WORKSPACE, settings.workspace)
        putString(TOKEN, settings.token)
        putString(BASE_URL, settings.baseUrl)
        putString(VISITOR_TOKEN, settings.visitorToken)
    }

    fun clear() = prefs.edit { clear() }

    private companion object {
        const val WORKSPACE = "workspace"
        const val TOKEN = "token"
        const val BASE_URL = "baseUrl"
        const val VISITOR_TOKEN = "visitorToken"
    }
}
