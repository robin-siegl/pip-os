# PIP-OS

PIP-OS is an experimental Android home launcher with a monochrome, retro-futuristic interface. It is built for touch first, with a terminal input for shortcuts and commands. Root access is not required.

The design is original and inspired by classic monochrome computers and game-style wrist terminals. No Fallout artwork, fonts, branding, or other game assets are bundled.

## Features

- Native home-screen replacement using Android's supported launcher intent
- Pip-Boy-inspired tabs, monochrome icon silhouettes, technical panels, status gauges and optional CRT scanlines
- Installed-app list, app search, pinned favorites, hiding and restoring applications
- Editable pinned order with accessible up/down controls and remove actions
- Choice of pinned-app grid or list layout
- Animated section transitions, terminal command suggestions and a short boot sequence
- Reduced-motion preference, available alongside phosphor green, amber and ice themes
- STAT tab with battery and internal-storage gauges
- RADIO tab: album artwork, track/artist metadata, previous/play/pause/next and progress/seek controls where supported
- DATA and MAP shortcuts with no location or calendar permissions
- Local DataStore settings, pure unit tests and Android UI smoke tests

## Requirements

- Android Studio and Android SDK 35
- JDK 17
- Gradle 8.11.1 (or the version required by your installed Android Gradle plugin)
- Android 10 or newer; Pixel 10 Pro is the initial target

The source tree does not include a Gradle wrapper. In Android Studio, open the `pip-os` folder and let Gradle sync. You can generate a wrapper with a local Gradle installation:

```bash
gradle wrapper --gradle-version 8.11.1
```

Then build and test:

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
./gradlew :app:assembleDebug
./gradlew :app:connectedDebugAndroidTest
```

Use `gradlew.bat` instead of `./gradlew` on Windows.

After installing, choose **PIP-OS** from **Settings > Apps > Default apps > Home app**. To return to your normal launcher, select **Pixel Launcher** in the same screen.

## Using the launcher

The **APPS** tab is the default home screen. Use the star/plus button next to an installed app to pin it. Tap **EDIT** to reorder pins with the up/down controls or remove one. The selection persists after restarting. Pinned items display as a grid or list, selectable from **CFG**.

Long-press an application in the full list to access **HIDE APP**. Restore all hidden apps in **CFG**.

Type in the bottom input to search apps. Commands start with `>`:

```text
>help
>apps
>open Spotify
>battery
>storage
>settings
>theme amber
```

Green and ice are also available as theme arguments. The previous-command button recalls commands from local history.

## Media access

Android requires notification-listener access for third-party apps to discover active media sessions. PIP-OS asks for this **only if you open RADIO and choose to enable it**. The launcher does not read, save, or upload notification text. The listener component is declared solely to let Android authorize the media-session API.

Controllers are subscribed only while the RADIO screen is visible and are unregistered on exit. A one-second UI timer interpolates track position **only while a session is playing**. Track duration, artwork, seeking and skip controls depend on metadata/actions published by the player, so features can be unavailable for some apps or streams.

## Project structure

```text
app/src/main/java/dev/pipos/launcher/
  MainActivity.kt              Home screen and section navigation
  LauncherViewModel.kt         Launcher state and actions
  data/
    AppRepository.kt           Installed launchable activities
    FavoritesRepository.kt     Ordered pinned-app persistence and migration
    FavoriteOrder.kt           Pure favorite-list operations
    LauncherPreferencesRepository.kt
  media/
    MediaControllerPanel.kt    Media sessions, artwork and controls
    MediaProgress.kt           Seek/position calculations
  ui/
    PipComponents.kt           Technical panels, actions and gauges
    MonochromeAppIcon.kt       Cached icon conversion
    TerminalCommand.kt        Command parser
    Search.kt                 App matching
    AppCategory.kt            Heuristic categories
```

Favorites created with the older, unordered storage scheme are migrated without losing pins. They receive a deterministic initial order. Subsequent changes retain their chosen order.

## Testing on the Pixel 10 Pro

Run the PowerShell helper from the project root after connecting the phone over USB with USB debugging enabled:

```powershell
./scripts/pixel-smoke.ps1
```

The script reports startup timing, a memory snapshot and graphics-frame diagnostics. Those numbers are diagnostic samples, not a benchmark. For visual and accessibility validation, verify these manually on the device:

1. Set PIP-OS as home and confirm Home navigation and back handling work.
2. Pin three apps, move the middle app up/down, change between grid and list, restart, verify persistence.
3. Search for and launch an application; long-press to hide one and restore it in settings.
4. Enable large font/display sizes and TalkBack; check all tabs, settings, and reorder controls.
5. Play music in Spotify or another supported player. Grant notification access, test artwork, progress, pause/skip and seeking. Revoke access and confirm RADIO still works safely.
6. Toggle reduced motion and scanlines. Check for clipping, scrolling, overheating and unusual battery consumption.
7. Switch back to Pixel Launcher to confirm recovery.

The Android UI tests in `app/src/androidTest` cover navigation and the search accessibility node. The GitHub Actions workflow includes separate jobs for unit tests, lint, assembly, and emulator UI tests.

## Home app setup and first-run flow

On first launch, PIP-OS offers **Set as Home app** through Android's official HOME role dialog. The choice is optional: **Not now** leaves Pixel Launcher unchanged. The prompt is dismissed after the user skips or successfully selects PIP-OS, and the choice is kept in local DataStore. The **CFG** screen always provides a Home app status indicator, a button to request the Home role (when needed), and a **Change Home app** shortcut to system settings so users can switch back.

The **APPS** panel observes favorites directly, ensuring the pinned section appears immediately after pinning the first application without navigating away.

## Known limitations

- PIP-OS does not replace system settings, the notification shade, lock screen, or Android gesture navigation.
- App categories are heuristic rather than user-defined.
- Pinned apps can be reordered by long-pressing and dragging the handle in Edit mode; up/down buttons remain for accessibility.
- DATA delegates to Android's calendar/settings; MAP delegates to an installed maps app.
- Media-session support depends on the playback app and Android notification access.
- Animated page transitions and panel visuals are implemented, but there is no continuous CRT flicker or screen curvature effect.
- **The project has not been compiled against an Android SDK or tested on a physical Pixel 10 Pro in the authoring environment.** Source validation and pure Kotlin compilation were performed; Android Studio/emulator/device verification is still required.

## Privacy

PIP-OS has no backend, analytics, telemetry, or internet permission. Favorites, preferences, hidden apps and command history are kept locally using Android DataStore. Notification access, when explicitly enabled, is used to discover and control active media sessions only.

## CI and downloadable production releases

Use **Actions > Production release > Run workflow** on `main` to publish a new version without creating a tag yourself. Choose patch, minor or major version bump, or enter an exact version. The first release defaults to `v0.2.0` if no tags exist. The workflow runs tests and lint, builds signed APK/AAB files, and publishes the GitHub Release with a newly created tag only after packaging succeeds. Production builds require four repository signing secrets, configured once. You can still push an annotated `vMAJOR.MINOR.PATCH` tag to trigger the same workflow.

Pull requests and pushes to `main` separately run unit tests, Android lint, debug APK assembly, and emulator UI tests. Debug APKs are Actions artifacts, not production-signed releases. See [Releasing PIP-OS](docs/RELEASING.md) for instructions and signing warnings. Never commit signing keys.
