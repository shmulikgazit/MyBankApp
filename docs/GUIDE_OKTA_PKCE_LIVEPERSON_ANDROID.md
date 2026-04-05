# Okta Authorization Code + PKCE with LivePerson (Android)

End-to-end pattern: the **mobile app** obtains an Okta **authorization code** with **PKCE** and passes **`authorization_code` + `code_verifier`** to the LivePerson SDK; **LivePerson’s IdP** calls Okta **`/token`** (with **client_secret** for a **Web** Okta app). The app **never** calls Okta `/token`.

Implementation: `MainActivity.java`, AppAuth, LP Messaging SDK **5.27.x**.

## Architecture

| Step | Who | What |
|------|-----|------|
| 1 | **LivePerson** | `LivePerson.getPKCEParams(...)` → store `code_verifier` / use `code_challenge` |
| 2 | **App** | AppAuth opens Okta **authorize** with `code_challenge` + `S256` |
| 3 | **Okta** | Redirects with **`authorization_code`** |
| 4 | **App** | `setAuthKey(code)` + `setCodeVerifier(stored_verifier)` → `showConversation` |
| 5 | **LP IdP** | Exchanges code at Okta **`/token`** (e.g. Web app + secret) |

**One `client_id`** for authorize and token. **Okta Web application** + **PKCE on authorize** matches **LP sending `client_secret`** on `/token`.

## Okta (Web app)

- OIDC **Web Application**; **Authorization Code** + **PKCE** where required.
- **Sign-in redirect URI** matches app (e.g. `com.mybrand.livepersondemo://callback`).
- **Token endpoint authentication** aligned with LivePerson (secret POST or Basic).
- Same **Client ID** (and secret in LP) as in **Settings**.

## Android

- **AppAuth** `RedirectUriReceiverActivity` + **`appAuthRedirectScheme`** in `build.gradle`.
- **`MainActivity`**: `launchMode="singleTop"`; OAuth completion **`PendingIntent` uses `FLAG_MUTABLE`** (API 31+).
- **Long LP `code_verifier`:** AppAuth limits 128 chars; authorize URL uses LP’s **challenge**; a short **dummy** verifier satisfies AppAuth; **real** verifier goes to **`setCodeVerifier`**.

## Troubleshooting

| Issue | Hint |
|--------|------|
| Empty OAuth Intent after browser | **`FLAG_MUTABLE`** on completion `PendingIntent` |
| LP **1057** | Pass **`setCodeVerifier`** from **`getPKCEParams`** |
| Okta **`invalid_token_endpoint_auth_method`** | **Web** Okta app + LP secret; not Native-only |
| AppAuth crash verifier length | Dummy verifier + LP challenge (this repo) |

**Logcat:** tag `LivePersonDemo`.

## References

- [RFC 7636](https://datatracker.ietf.org/doc/html/rfc7636)
- [AppAuth Android](https://github.com/openid/AppAuth-Android)
- [LP Android Authentication](https://developers.liveperson.com/mobile-app-messaging-sdk-for-android-resources-authentication.html)
