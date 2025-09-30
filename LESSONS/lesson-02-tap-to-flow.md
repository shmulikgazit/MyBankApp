# Lesson 02 — What happens when you tap “Start Conversation”

Goal: Learn the runtime flow so logs make sense and you can reason about issues.

## Flow diagram (simplified)

```
[Tap START] → MainActivity.startUnauthenticatedConversation()
    ↓
Read Settings (brandId, appInstallId) ← SharedPreferences("lp_demo")
    ↓
Monitoring init → LivePerson.initialize(context,
                     new InitLivePersonProperties(brandId, APP_ID,
                     new MonitoringInitParams(appInstallId), callback))
    ↓ onInitSucceed
Show conversation (UN_AUTH) →
   LivePerson.showConversation(this,
       new LPAuthenticationParams(LPAuthenticationType.UN_AUTH),
       new ConversationViewParams())
```

## Where in code
- File: `app/src/main/java/com/mybrand/livepersondemo/MainActivity.java`
  - `startUnauthenticatedConversation()`
  - `setStatus(String)` updates the small status line

## What to look for in Logcat
- Filter by `LivePersonDemo` and `LivePerson`.
- Typical sequence:
  - `[[LivePerson]] === Initializing LivePerson SDK ===`
  - `[[HttpHandler]] URL: adminlogin.liveperson.net` (CSDR lookup)
  - `[[HttpHandler]] URL: *.v.liveperson.net` (Monitoring engagement)
  - `onSuccess! 201` for engagement → conversation UI opens
  - `[[AuthRequest]] authenticate: Consumer type: UN_AUTH`

## Exercise
- Add a quick status message before/after each major step.
  - In `startUnauthenticatedConversation()`, call `setStatus("Initializing...")`,
    then `setStatus("Monitoring initialized")`, then `setStatus("Conversation opening...")`.
- Build, run, and watch the status line change as you tap Start.
