# Lesson 08 — Try AUTH flow (no Monitoring)

Goal: Start a conversation without Monitoring by authenticating.

**Production Okta + PKCE + LivePerson:** see [`docs/GUIDE_OKTA_PKCE_LIVEPERSON_ANDROID.md`](../docs/GUIDE_OKTA_PKCE_LIVEPERSON_ANDROID.md).

## Steps
1) In `MainActivity.java`, find where we build `LPAuthenticationParams`.
2) Change:
```java
LPAuthenticationParams params = new LPAuthenticationParams(LPAuthenticationType.UN_AUTH);
```
   to:
```java
LPAuthenticationParams params = new LPAuthenticationParams(LPAuthenticationType.AUTH);
params.setAuthKey("YOUR_AUTH_CODE");
```
3) Build and run → tap Start Conversation.

Notes
- You need a valid auth code from your LP environment for the Code Flow.
- Switch back to UN_AUTH + Monitoring by restoring `UN_AUTH` and removing `setAuthKey`.
