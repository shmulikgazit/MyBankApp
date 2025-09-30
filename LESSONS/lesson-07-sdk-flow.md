# Lesson 07 — Monitoring + showConversation flow

Goal: Understand the two key steps for unauth conversations.

## Step 1: Monitoring init
```java
LivePerson.initialize(getApplicationContext(),
    new InitLivePersonProperties(
        brandId,
        APP_ID,
        new MonitoringInitParams(appInstallId),
        callback));
```
- Requires: a valid Mobile App channel and App Install ID.
- Expect in logs:
  - `adminlogin.liveperson.net` (CSDR lookup)
  - `*.v.liveperson.net` engagement (201 indicates returned engagement)

## Step 2: Show conversation (guest)
```java
LPAuthenticationParams params = new LPAuthenticationParams(LPAuthenticationType.UN_AUTH);
LivePerson.showConversation(this, params, new ConversationViewParams());
```
- Opens the built‑in conversation UI.

## Troubleshooting
- Emulator DNS: set Private DNS Off/Automatic, or AVD DNS to `8.8.8.8,1.1.1.1` and Cold Boot.
- Monitoring domain is regional; do not hardcode; the SDK discovers it from your `BRAND_ID`.
