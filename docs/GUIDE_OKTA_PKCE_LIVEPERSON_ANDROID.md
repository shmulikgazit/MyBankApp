# Okta Authorization Code + PKCE with LivePerson (Android)

This guide describes the **end-to-end pattern** used in this demo app: the **mobile app** performs the Okta **authorize** step with **PKCE**, then passes **`authorization_code` + `code_verifier`** into the LivePerson SDK. **LivePerson’s IdP** exchanges the code at Okta’s **`/token`** endpoint (typically using a **Web** Okta application and **client_secret** configured on the LivePerson side). The app **does not** call Okta `/token` for this flow.

**Reference implementation:** `MainActivity.java`, **LP Messaging SDK 5.27.x**, **AppAuth Android 0.11.x**.

---

## Why this pattern

- **Single OAuth client for authorize and token:** The same O **`client_id`** is used in the browser (authorize) and when LP’s backend calls `/token`.
- **Secret stays off the device:** Okta **client_secret** lives in LivePerson’s IdP configuration, not in the APK.
- **PKCE still applies:** The authorization request uses `code_challenge` / `S256` per RFC 7636; the **verifier** must be sent to LP so the token exchange can complete.

---

## High-level sequence

| Step | Component | Action |
|------|-----------|--------|
| 1 | **LivePerson SDK** | After init, `LivePerson.getPKCEParams(...)` supplies `code_verifier`, `code_challenge`, and method (store these). |
| 2 | **App (AppAuth)** | Discover OIDC from **issuer**, build **authorize** URL with LP’s `code_challenge` + method. Open Chrome Custom Tab. |
| 3 | **Okta** | User signs in; redirect returns **`authorization_code`** to the app’s redirect URI. |
| 4 | **App** | `LPAuthenticationParams`: `setAuthKey(authorization_code)`, **`setCodeVerifier(stored LP verifier)`**, `setHostAppRedirectUri`, optional `setIssuerDisplayName`. Then `showConversation`. |
| 5 | **LivePerson IdP** | Server-side **code + verifier** exchange at Okta `/token` (with **client_secret** for Web app). |

---

## Okta administration

### Application type

Use an Okta **OIDC Web application** (not “Native only” if LP must send `client_secret` on `/token`).

### Redirect URI

Must match what you configure in the app’s **Settings** (and `AndroidManifest` / AppAuth):

- Example: `com.mybrand.livepersondemo://callback`

Add this under the app’s **Sign-in redirect URIs**.

### Grant and PKCE

- **Authorization Code** grant.
- Enable **Proof Key for Code Exchange (PKCE)** as required by your Okta policy; the authorize request from this app sends `code_challenge` / `S256` from **LivePerson’s** `getPKCEParams`.

### Token endpoint authentication (critical for LP)

The method must match what **LivePerson** is configured to use when calling Okta (e.g. **client secret as POST body** or **HTTP Basic**). Mismatches surface as Okta errors such as **`invalid_token_endpoint_auth_method`** on the server side (LP logs / IdP traces).

### Scopes

This demo requests `openid`, `profile`, and `email` in `AuthorizationRequest.Builder`. Adjust if your tenant or LP IdP requires different scopes.

---

## LivePerson configuration (conceptual)

- Configure your brand’s **IdP** with Okta’s **authorize** and **token** endpoints, **client ID**, and **client secret** (Web app).
- Ensure the IdP is wired for **authorization code + PKCE** flows that expect the mobile app to supply **code** and **verifier** to the SDK (per LP’s authenticated messaging docs).

Exact console steps vary by LP product version; use LP’s official IdP configuration docs alongside this guide.

---

## Android project setup

### Gradle (`app/build.gradle`)

- **`buildFeatures { buildConfig true }`** for debug logging around auth.
- **Manifest placeholders** for AppAuth redirect **scheme** (host/path come from your redirect URI string):

  ```gradle
  manifestPlaceholders += [
      appAuthRedirectScheme: 'com.mybrand.livepersondemo'
  ]
  ```

  The full redirect URI is still `scheme://host...` (e.g. `com.mybrand.livepersondemo://callback`).

### Dependencies

- `com.liveperson.android:lp_messaging_sdk:5.27.0`
- `net.openid:appauth:0.11.1`

### Manifest

- Declare **`net.openid.appauth.RedirectUriReceiverActivity`** with intent filters for your redirect (see project `AndroidManifest.xml`).
- **`MainActivity`**: `launchMode="singleTop"` so OAuth completion can deliver the result to the existing activity.

### Runtime: `PendingIntent` mutability (API 31+)

OAuth completion `PendingIntent`s must include **`PendingIntent.FLAG_MUTABLE`** on Android 12+ when the platform expects mutable pending intents for this flow. Without it, you may see **empty or missing OAuth data** when returning from the browser. This demo uses `authFlowPendingIntentFlags()` in `MainActivity`.

---

## Demo app flow (mapping to code)

1. **Authenticated chat:** User taps **Start authenticated**. The app may call `LivePerson.logOut` first to clear a prior session, then **`LivePerson.initialize`**.
2. On **init success**, **`beginOktaPkceLogin`**: builds `LPAuthenticationParams(AUTH)` with `setHostAppRedirectUri` and optional `setIssuerDisplayName` (from Settings), then **`LivePerson.getPKCEParams`**.
3. On **PKCE success**, **`fetchOidcConfigAndOpenBrowser`**: AppAuth **`AuthorizationServiceConfiguration.fetchFromIssuer`**, then **`performAuthorizationRequest`** with LP’s **`code_challenge`** / method on the request.
4. Browser returns to **`MainActivity`** via **`Intent`**; **`processReturnFromBrowser`** parses **`AuthorizationResponse`**.
5. **`openConversationWithAuthKey`**: `setAuthKey(authorization_code)`, **`setCodeVerifier`** from **stored `PKCEParams`**, redirect URI, optional display name → **`LivePerson.showConversation`**.

**Unauthenticated chat:** Uses `LPAuthenticationType.UN_AUTH`. After an authenticated session, the app sets a preference so the next visitor chat may **`logOut` first** (avoids mixed-session issues); there is a short **delay** after init before opening UN_AUTH (see `DELAY_MS_AFTER_INIT_BEFORE_UNAUTH_CONVERSATION`).

---

## App Settings screen (`SettingsActivity`)

Stored under SharedPreferences name **`lp_demo`**:

| Key / field | Purpose |
|-------------|---------|
| **Brand ID / App Install ID** | LP monitoring / init. |
| **Issuer** | Okta issuer URL for OIDC discovery (`fetchFromIssuer`). |
| **Okta Client ID** | OAuth `client_id` on the authorize request (required for AUTH). |
| **Redirect URI** | Must match Okta and manifest (e.g. `com.mybrand.livepersondemo://callback`). |
| **IdP display name (optional)** | `LPAuthenticationParams.setIssuerDisplayName` when non-empty. |

---

## Long `code_verifier` from LivePerson

RFC 7636 limits the verifier to **128 characters**. **AppAuth** enforces that when building the request. LivePerson **may** return a longer verifier.

**This repo’s workaround:**

- Build `AuthorizationRequest` with a **short random verifier** from `CodeVerifierUtil.generateRandomCodeVerifier()` **only to satisfy AppAuth**.
- Keep the **authorize URL** correct by passing LP’s **`code_challenge`** and **method** into **`setCodeVerifier(dummy, challenge, method)`**.
- On redirect, pass the **real** verifier from **`PKCEParams`** into **`LPAuthenticationParams.setCodeVerifier`** (not the dummy).

If LP’s verifier is short enough, behavior is unchanged; the important case is **verifier length > 128**.

---

## Troubleshooting

| Symptom | Likely cause / fix |
|--------|---------------------|
| No `AuthorizationResponse` after browser; `Intent` looks empty | **FLAG_MUTABLE** on completion `PendingIntent`; correct redirect URI and `singleTop` activity. |
| LP error **1057** / PKCE-related IdP failure | **`setCodeVerifier`** must be the verifier from **`getPKCEParams`**, matching the **code_challenge** sent to Okta. |
| Okta **`invalid_token_endpoint_auth_method`** (on token, via LP) | Okta app type + token auth method must match LP’s **client_secret** usage (**Web** app, not Native-only misconfiguration). |
| AppAuth crash or error on verifier | Long verifier: use **dummy verifier + LP challenge** pattern above. |
| AUTH works but UN_AUTH behaves oddly | Prior AUTH sets `PREF_NEED_LOGOUT_BEFORE_UNAUTH`; demo may **logOut** before next visitor session—by design for this sample. |

**Logcat:** filter tag **`LivePersonDemo`**. Avoid logging production secrets; debug logs may print code values only in **`BuildConfig.DEBUG`**.

---

## Security notes

- Treat **authorization codes** as single-use and short-lived; do not log them in production builds.
- **client_secret** belongs in **LivePerson / server** configuration, not in the Android app.
- Use **HTTPS** issuer and standard Okta tenant hardening (MFA, policies) as required by your organization.

---

## References

- [RFC 7636 – PKCE](https://datatracker.ietf.org/doc/html/rfc7636)
- [AppAuth Android](https://github.com/openid/AppAuth-Android)
- [LivePerson – Android authentication](https://developers.liveperson.com/mobile-app-messaging-sdk-for-android-resources-authentication.html)
