# Lesson 06 — Edit Brand ID and App Install ID in‑app

Goal: Change IDs at runtime without code edits.

## Screen
- `SettingsActivity` (open via the Settings button on the main screen)
- Fields:
  - Brand ID (numeric, e.g., 45975138)
  - App Install ID (UUID from LP Console, Mobile App channel)
- Buttons:
  - Save: persists to SharedPreferences (`lp_demo`)
  - Cancel: discards changes

## How it’s stored
- Preferences file: `lp_demo`
  - `brand_id` and `app_install_id`
- `MainActivity` reads these before initializing Monitoring.

## Test
1) Open Settings, change IDs, tap Save.
2) Back to main, tap Start Conversation.
3) Watch status line: “Initializing for brand …”.
