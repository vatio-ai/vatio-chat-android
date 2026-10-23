# Vatio Chat for Android

A ChatGPT-style chat app for a [Vatio](https://vatio.ai) agent, written in
Kotlin with Jetpack Compose on the
[Vatio Android SDK](https://github.com/vatio-ai/android). Out of the box it
talks to Vatio's own agent, the one on vatio.ai. Point it at yours by
changing one file.

It is also a reference. If you are putting a Vatio agent in your own Android
app, read it, copy from it, or point your coding agent at it. Every file does
one thing, and everything goes through the public SDK.

## What it does

- **Chat** with the agent: your messages in bubbles, its replies as Markdown
  (lists, code blocks, links that open in the browser), a typing indicator,
  and a status line while connecting.
- **The agent's own look:** name, avatar, greeting, suggestions and accent
  color, all from `vatio.config()`.
- **Photos and voice notes:** up to four photos from the system photo picker,
  or a voice note recorded with the microphone. Vatio transcribes it, and the
  transcript replaces the "Audio" label.
- **Conversations drawer**, as in ChatGPT: start a new chat, or reopen one of
  your past conversations.
- **Feedback:** when Vatio offers a feedback moment, a card asks Good, So-so
  or Bad.
- **Errors you can recover from:** if a send fails, the text and files go
  back in the composer and the reason shows in a snackbar.
- **Settings:** workspace, token, base URL, and an optional visitor token for
  a signed-in user.
- Light and dark mode.

Android 8.0+ (API 26). It depends only on the Vatio SDK, Compose Material 3,
AndroidX Activity and Lifecycle, and Coil for images.

## Run it

Open this folder in Android Studio and run `app`. From the command line, with
JDK 17 or newer and an Android SDK (`ANDROID_HOME`):

```bash
./gradlew :app:installDebug
```

## Point it at your own agent

A publishable key only works from the origins it allows. This app's origin
is `android-app://` plus its `applicationId`: `android-app://ai.vatio.chat`.

1. Create a publishable key that allows it, from your agent's project:

   ```bash
   vatio keys create publishable --env preview --label android --origin android-app://ai.vatio.chat
   ```

   Use `--env live` once the agent is published. To add the origin to a key
   you already have, run `vatio keys origins PREFIX URL...`.
2. Put your workspace and the `vatpub_` key in
   [`AppConfig.kt`](app/src/main/kotlin/ai/vatio/chat/AppConfig.kt). To try a
   key without rebuilding, paste it in **Settings** instead.

If you change the `applicationId`, the origin changes with it. A publishable
key is meant to ship inside an app. A `vat_` token is a developer secret:
never put one in an app.

### Signed-in users

In a real app, your backend signs a visitor token for the signed-in user. The
agent then knows who they are and can use private tools. This app has no
sign-in of its own, so **Settings → Visitor token** lets you paste one to try
it. Changing the token starts a new chat, because a different person has
their own conversations. See
[Sessions and channels](https://vatio.ai/docs/authentication/sessions).

## File tour

Everything lives under `app/src/main/`. The Kotlin is in
`kotlin/ai/vatio/chat/`.

| File | What it shows how to do |
|---|---|
| `settings.gradle.kts`, `app/build.gradle.kts` | Add the SDK from JitPack: `com.github.vatio-ai:android` |
| `AppConfig.kt` | The workspace, publishable key and base URL the app ships with, and how to point them at your agent |
| `ChatViewModel.kt` | Own the `Vatio` client and the `VatioChat` in an `AndroidViewModel`: resume, send, and put the text back when a send fails. Also open a past conversation, start over, answer feedback, surface `lastError`, replace the chat when settings change, and close it in `onCleared()` |
| `Settings.kt` | Keep the overrides (workspace, token, base URL, visitor token) in SharedPreferences |
| `Draft.kt` | What the composer holds, and how a failed send goes back into it |
| `Limits.kt` | Vatio's limits: 4,000 characters, 4 files of 8 MB, 2,000-character feedback comments |
| `ErrorMessages.kt` | Turn a `VatioException` code into words a person can act on, such as the origin to allow for `origin_not_allowed` |
| `MainActivity.kt` | Edge-to-edge setup, the theme, and switching between the chat and Settings |
| `ui/ChatScreen.kt` | Collect the chat's StateFlows with `collectAsStateWithLifecycle`; the top bar with its status line, the drawer, the snackbar, and when to call `resume()` |
| `ui/MessageList.kt` | A bottom-up `LazyColumn` that follows the conversation, with the feedback card and typing dots at the end |
| `ui/MessageRow.kt` | Visitor bubble or agent Markdown; dim `isPending` messages, and skip `isMediaLabel` stand-ins |
| `ui/Attachments.kt` | Draw a message's `VatioAttachment`s: images from their signed `url`, other files as chips |
| `ui/Markdown.kt` | Render untrusted Markdown as an `AnnotatedString`, with links limited to http(s), mailto and tel |
| `ui/Composer.kt` | The text field, `PickMultipleVisualMedia`, the `RECORD_AUDIO` permission, and the send/mic button |
| `ui/VoiceRecorder.kt` | Record a voice note with `MediaRecorder` to an AAC `.m4a` |
| `ui/EmptyState.kt` | The agent's avatar (or initial on its accent color), greeting and suggestions from `vatio.config()` |
| `ui/TypingIndicator.kt` | Animated dots while `isTyping` |
| `ui/FeedbackCard.kt` | Answer `chat.feedback` with `rate(rating, comment)` or `dismissFeedback()` |
| `ui/ConversationsDrawer.kt` | List `vatio.conversations(visitorToken)` in a `ModalNavigationDrawer`, and switch with `chat.open()` |
| `ui/SettingsScreen.kt` | Edit the settings, including an obscured visitor token, and show the origin this app sends |
| `ui/Links.kt` | Open a URL without crashing when no app can handle it |
| `ui/theme/` | Material 3 light and dark color schemes, with the agent's accent as the primary color |
| `AndroidManifest.xml`, `res/` | The microphone permission, backup rules, and the adaptive launcher icon |

## Your own app

The quickest way to put an agent in your own app is to run
`npx @vatio-ai/skills` in its project, then `/vatio-init` and
`/vatio-add-to-app` in Claude Code. In Cursor or Codex, ask it to use them.

Docs: [Android SDK](https://vatio.ai/docs/sdk/android) ·
[Limits](https://vatio.ai/docs/sdk/limits) ·
[Sessions](https://vatio.ai/docs/authentication/sessions) ·
[vatio.ai](https://vatio.ai)

## Issues

Bugs and questions go to
[issues](https://github.com/vatio-ai/vatio-chat-android/issues). This
repository is a read-only mirror of the app as it lives in Vatio's monorepo,
so a pull request cannot be merged here. Open an issue describing the change
instead.

## License

MIT. See [LICENSE](LICENSE).
