# Callbrize - Indian Card Game (offline, Kotlin + Jetpack Compose)

Package `com.nh.callbrize` · minSdk 26 · targetSdk/compileSdk 34.
Single Player and Pass-and-Play are fully offline. Wi-Fi play uses the INTERNET permission only to open a direct TCP connection to a friend's phone on the same network/hotspot (no servers). Bluetooth play uses BLUETOOTH_CONNECT.

## Rules (important)
No verifiable "official" Callbrize ruleset could be established, so the app does **not** claim one.
The default is a documented **Call-Break-style** trick-taking game (see `game/GameRules.kt`,
class `TrickTakingRules`). Change trump, rounds, bid range or the "must beat highest" rule through
its constructor, or implement `GameRules` yourself.

## Open & build
1. Android Studio Koala (2024.1) or newer, JDK 17 (bundled JBR is fine). Internet is needed **once** to download Gradle/dependencies.
2. File > Open > select the `Callbrize` folder. Let Gradle sync (it downloads Gradle 8.9 from `gradle-wrapper.properties`).
3. Build > Build APK(s), or run `./gradlew assembleDebug` if you generated the wrapper (`gradle wrapper --gradle-version 8.9`).
   APK: `app/build/outputs/apk/debug/app-debug.apk`
4. Install: USB debugging + Run, or `adb install app-debug.apk`.
5. Test offline: enable airplane mode, open the app; everything works.

## Unit tests
`./gradlew test` (or right-click `app/src/test` > Run). Covers deck, dealing, legal moves, trick winners,
scoring, rejected actions and full bot-vs-bot matches.

## Structure
`domain` cards/deck · `game` rules, state, engine (single source of truth) · `ai` computer players (Easy/Medium/Hard) ·
`data` DataStore persistence · `navigation` · `ui`, `ui.components`, `ui.screens` · `util` sound/haptics · `settings` avatars.

## Wi-Fi / Bluetooth play
Host phone runs the game (engine); up to 3 friends join; computers fill empty seats. Each guest only receives their own hand.
- Wi-Fi: all phones on the same Wi-Fi or the host's hotspot. Host shows its address, friends type it (port 52525).
- Bluetooth: pair phones in Android Settings first; friends tap the host's phone in the list.
This could not be tested on real devices in the build sandbox, so please test it on two phones.

## Controls
Tap a card to lift it; swipe a card upwards (past ~90dp) to play it. There is no Play button.

## Known limitations
- Background music is not included (only sound-effect/vibration toggles).
- Interrupted-match restore is not implemented (an unfinished match is paused when the app is backgrounded, but not saved across process death).
- There is no custom UI reference image in this package; the look follows the written style spec (navy, emerald felt, gold).
- Cards animate in/out (deal and play); there is no 3D flip animation.
