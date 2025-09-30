# Lesson 03 — Change brand colors

Goal: Make the app look like your brand in 2 minutes.

## Files you’ll edit
- `app/src/main/res/values/colors.xml`

## What to change
Open `colors.xml` and set:
```
<color name="primary_color">#0057D9</color>
<color name="primary_dark">#003E99</color>
<color name="secondary_color">#00A389</color>
```
Pick any hex codes you like.

Why it works:
- The app theme (`styles.xml`) uses `primary_color` / `primary_dark`.
- The Start button background and the title text take `primary_color`.

## Mini visual
```
Before:  purple header/button
After:   your brand blue/teal
```

## Test
- Build → Rebuild Project, Run.
- Verify the header and Start button use the new colors.
