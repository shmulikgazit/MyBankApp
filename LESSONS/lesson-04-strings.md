# Lesson 04 — Change words and titles

Goal: Update UI text without touching Java code.

## Files you’ll edit
- `app/src/main/res/values/strings.xml`

## Try these edits
- `app_title`: "LivePerson Android SDK Demo" → "My Bank Support Chat"
- `start_conversation`: "Start Conversation" → "Start Chat"
- `demo_info`: Replace with your own guidance text.

Why it works:
- Android pulls all copy from `strings.xml`; layouts and code reference ids like `@string/app_title`.

## Test
- Build and run. Titles, button labels, and helper text update automatically.
