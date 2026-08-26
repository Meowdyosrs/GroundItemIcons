# Ground Item Icons

Standalone RuneLite plugin that works alongside the unmodified Ground Items plugin.

## Features
- Show/hide icons with its own toggle.
- Configurable icon size.
- Reads the existing Ground Items configuration from ConfigManager.
- Respects Ground Items hidden/highlighted lists, quantity thresholds, show-highlighted-only, hide-under-value, ownership filter, price display mode, and price-based highlighting.
- Does not modify RuneLite's Ground Items source.

## Local testing

From the project directory:

    .\gradlew build

Then:

    .\gradlew run

The test launcher loads this plugin as a builtin plugin before starting RuneLite.

## Important

Because the official Ground Items plugin owns the text rendering, this standalone version places the icon immediately to the LEFT of the existing Ground Items text. This avoids modifying or covering the official Ground Items text.

Change `author=YourName` in `src/main/resources/runelite-plugin.properties` before publishing.
