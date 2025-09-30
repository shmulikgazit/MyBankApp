# Lesson 05 — Adjust spacing and buttons

Goal: Make small layout tweaks confidently.

## File
- `app/src/main/res/layout/activity_main.xml`

## Change spacing
- Increase spacing under the Start button:
  - Find `tv_status` and change `android:layout_marginTop` to `16dp` or `20dp`.

## Rename Settings button
- Find the Button with id `btn_open_settings` and change `android:text` to `"Edit IDs"`.

## Test
- Rebuild and run. Check that the status spacing and button label updated.

Tip: If elements overlap, check each view’s `app:layout_constraintTop_toBottomOf` target and margins.
