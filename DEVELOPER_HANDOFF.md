# LivePerson Mobile SDK demo – developer handoff (mobile engineers / app team)

This repo contains an Android demo app integrating LivePerson Mobile App Messaging (Android SDK **5.25.1**) plus an in-app **Troubleshoot & Export Logs** screen designed for **release-grade debugging**.

Primary goals of this integration:
- deterministic “test run” steps/results (DNS → init → showConversation → post-checks)
- exportable logs that include **SDK internal history** (not just host-app logs)
- safe-by-default sharing via **data masking**

## Android: where to look in this repo

- **Conversation launch + init flow**: `app/src/main/java/com/mybrand/livepersondemo/MainActivity.java`
- **Troubleshoot test runner + export**: `app/src/main/java/com/mybrand/livepersondemo/TroubleshootActivity.java`
- **Rolling app log buffer (exported file source)**: `app/src/main/java/com/mybrand/livepersondemo/TestLogStore.java`
- **App-wide LivePerson logging defaults**: `app/src/main/java/com/mybrand/livepersondemo/MyHostApplication.java`
- **Manifest wiring**: `app/src/main/AndroidManifest.xml` (`android:name=".MyHostApplication"`)

## Android: LivePerson logging (SDK history snapshots, masking, log levels)

LivePerson’s Android SDK exposes an in-memory log history cache you can configure and export (separate from Logcat). See: [Android Advanced Features – Logging](https://developers.liveperson.com/mobile-app-messaging-sdk-for-android-advanced-features-logging.html).

### Recommended defaults (what we do)

We set these early in an `Application` subclass:

- `LivePerson.Logging.setSDKLoggingLevel(LogLevel.DEBUG)` in debug builds / `LogLevel.INFO` in release builds
- `LivePerson.Logging.setDataMaskingEnabled(true)` always (safer to ship; avoids leaking tokens/PII)
- (best effort) `LivePerson.setIsDebuggable(BuildConfig.DEBUG)`

Implementation:
- `MyHostApplication.onCreate()`

### Exporting SDK log history into an app file (what makes “retries” visible)

At the end of a troubleshooting run, we append the SDK’s internal history block to the exported file:

- `LivePerson.Logging.getLogSnapshotStringBlock(LogLevel.DEBUG)`

This is critical because transient network/retry/handshake issues often show up only in SDK logs. A snapshot lets support/debugging happen even when Logcat is unavailable in production.

Implementation:
- `TroubleshootActivity.appendLivePersonSdkSnapshot()`
- called at “end of test” and on init failure

### When to clear history

We clear the SDK’s in-memory log cache at the start of each test run so the snapshot is scoped and readable:

- `LivePerson.Logging.clearHistory()`

## Android: why we validated APIs via artifact inspection (“classes.jar”)

Docs are necessary but not sufficient for build correctness because:

- **API surface differs by SDK version** (package names and signatures can shift)
- this repo previously switched between **remote Maven artifacts vs local AARs**, so “what’s on the classpath” was the source of truth
- we needed to guarantee *compiles with resolved artifacts* rather than *compiles against documentation*

In practice we validated the exact resolved Android types:
- `com.liveperson.messaging.sdk.api.LivePerson$Logging`
- `com.liveperson.infra.log.LogLevel`

These are the concrete types used by the logging calls described in the Android logging doc page.

## iOS (Swift): engineer-facing integration notes (equivalent concept)

LivePerson iOS has the same core concepts (log level control + masking + snapshot export). See: [iOS Advanced Features – Logging](https://developers.liveperson.com/mobile-app-messaging-sdk-for-ios-advanced-features-logging.html).

Suggested iOS wiring:

- **App startup** (`AppDelegate` / scene bootstrap):
  - set SDK log level for debug vs release
  - enable masking for release (and strongly consider “masking always on” if exporting logs from device)
- **Troubleshoot screen**:
  - clear SDK log history at start of run
  - append SDK “log snapshot” to the exported payload at end of run
  - write to a file and share (Share Sheet / Files)

Swift skeleton (names must follow the iOS doc page; treat as structure, not exact API):

```swift
// AppDelegate.swift / app startup path
func configureLivePersonLogging() {
    // Example structure only; use exact APIs from iOS logging docs.
    // LivePersonLogging.setLevel(.debug / .info)
    // LivePersonLogging.setMaskingEnabled(true)
}

// Troubleshoot flow (your in-app screen)
func runTest() {
    // LivePersonLogging.clearHistory()
    // ... run DNS checks, init, show conversation ...
    // let sdkLogBlock = LivePersonLogging.getLogSnapshotStringBlock(filter: .debug)
    // append sdkLogBlock to your exported file
}
```

## Practical debugging notes / gotchas

- **Prefer SDK snapshots over Logcat** for end-user exports. Snapshots are intended to be safe and exportable; Logcat is not.
- **Masking ON** is safest when logs are leaving the device.
- **DNS failures are often intermittent** (esp. emulators / enterprise DNS). The Troubleshoot test re-checks critical domains after opening the conversation to catch “worked → failed” transitions.


