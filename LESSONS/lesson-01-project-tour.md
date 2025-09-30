# Lesson 01 — Project tour (5 minutes)

Goal: Understand where things live so you can change, rebuild, and test with confidence.

## The map

```
MyBankApp/
├─ app/
│  ├─ src/main/
│  │  ├─ java/com/mybrand/livepersondemo/
│  │  │  ├─ MainActivity.java         ← Start button, LP init, status updates
│  │  │  └─ SettingsActivity.java     ← Edit & save Brand ID / App Install ID
│  │  ├─ res/
│  │  │  ├─ layout/
│  │  │  │  ├─ activity_main.xml      ← Main screen layout (title, button, status)
│  │  │  │  └─ activity_settings.xml  ← Settings screen layout (inputs + Save/Cancel)
│  │  │  ├─ values/
│  │  │  │  ├─ strings.xml            ← All UI text
│  │  │  │  ├─ colors.xml             ← Brand colors
│  │  │  │  └─ styles.xml             ← App theme
│  │  │  └─ drawable/                 ← Icons and button background
│  │  └─ AndroidManifest.xml          ← Declares activities (screens)
│  └─ build.gradle                     ← App dependencies incl. LivePerson SDK
├─ build.gradle                         ← Project-level gradle config
├─ settings.gradle                      ← Repositories incl. LP SDK repo
└─ README.md                            ← How to run/configure
```

## Visual anatomy (not to scale)

```
┌────────────────────────────┐
│  Title: LivePerson Demo    │  ← color: @color/primary_color
├────────────────────────────┤
│  LivePerson Android SDK…   │  (tv_title)
│  This demo app…            │  (tv_description)
│            [chat icon]     │  (iv_logo)
│  [ START CONVERSATION ]    │  (btn_start_conversation)
│  Status: Idle              │  (tv_status)
│  [ SETTINGS ]              │  (btn_open_settings)
│  Helper text (tv_info)     │
│  Version (tv_version)      │
└────────────────────────────┘
```

## Your daily loop

1) Open Android Studio → Run (green ▶) → choose emulator/device.
2) Tap Settings → confirm IDs → Save → back → Start Conversation.
3) View → Tool Windows → Logcat → filter by `LivePersonDemo`.

Next: go to Lesson 02 to see how the tap flows through code.
