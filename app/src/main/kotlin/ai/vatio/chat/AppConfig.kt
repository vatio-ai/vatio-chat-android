package ai.vatio.chat

/**
 * Who the app talks to out of the box: Vatio's own agent, the one that
 * answers on vatio.ai. Settings can override all of it at runtime.
 *
 * To point the app at your own agent, put your workspace here with a
 * publishable key that allows this app's origin:
 *
 *     vatio keys create publishable --env preview --label android --origin android-app://ai.vatio.chat
 *
 * (`--env live` once the agent is published; `vatio keys origins PREFIX
 * URL...` adds the origin to a token you already have.) The origin is
 * `android-app://` plus the applicationId, so it changes if you change that.
 *
 * A publishable key (`vatpub_`) is meant to ship inside an app. A developer
 * token (`vat_`) never is: anyone can read it out of the APK.
 */
object AppConfig {
    const val WORKSPACE = "vatio"
    const val TOKEN = "vatpub_472bb67e31a2a3c47c9d11c057cbfb76d4854d19ea582e59"
    const val BASE_URL = "https://vatio.ai"

    const val REPOSITORY_URL = "https://github.com/vatio-ai/vatio-chat-android"
    const val DOCS_URL = "https://vatio.ai/docs/sdk/android"
}
