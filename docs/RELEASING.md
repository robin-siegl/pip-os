# Releasing PIP-OS

PIP-OS uses semantic version tags (for example `v0.2.0`) as the source of truth for production versions. The tag workflow tests, lints, signs and builds an installable APK and a Play-ready Android App Bundle (AAB). It uploads both files and SHA-256 checksums to the corresponding GitHub Release.

## One-time signing setup

A production Android signing key must be kept private and reused for every update. **Never commit it**, post it in an issue, or regenerate it for every release. Store a secure backup outside GitHub. Losing the signing key prevents updates to existing installations signed with it.

Generate a key with your locally installed JDK:

```bash
keytool -genkeypair -v -keystore pipos-release.jks -alias pipos -keyalg RSA -keysize 3072 -validity 10000
```

Create four repository Actions secrets in **Settings > Secrets and variables > Actions**:

| Secret | Value |
| --- | --- |
| `ANDROID_KEYSTORE_BASE64` | Base64 of the entire `.jks` file, on one line |
| `ANDROID_KEYSTORE_PASSWORD` | Keystore password |
| `ANDROID_KEY_ALIAS` | `pipos` (or your chosen alias) |
| `ANDROID_KEY_PASSWORD` | Private-key password |

On Windows PowerShell, encode the file with:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes((Resolve-Path ./pipos-release.jks))) | Set-Clipboard
```

Do not keep the signing key in the repository directory. The `.gitignore` excludes common key extensions, but the safest approach is storing it elsewhere.

## Publish

First make sure the Android CI jobs are green. Then create and push an **annotated tag** pointing to the desired main-branch commit:

```bash
git switch main
git pull --ff-only
git tag -a v0.2.0 -m "PIP-OS 0.2.0"
git push origin v0.2.0
```

The workflow triggered by that tag publishes **Releases > PIP-OS 0.2.0** with:

- `PIP-OS-v0.2.0.apk` (install directly on a compatible Android phone)
- `PIP-OS-v0.2.0.aab` (for Android app-store distribution)
- `SHA256SUMS.txt` (download integrity hashes)

Never reassign an existing release tag. For fixes, create a new version such as `v0.2.1`.

Android `versionName` comes from the tag; `versionCode` is calculated as `major * 1,000,000 + minor * 1,000 + patch`. Each minor and patch component is limited to 999. For example, `v0.2.0` has versionCode 2000, so it upgrades the previous local development builds with versionCode 2, provided their signing certificate matches. **A debug-signed APK cannot be updated in place by the separately signed production APK**; uninstall the debug build first, which will remove local app data. Back up any settings you need before switching signing identities.

## CI versus release

- Pushes and pull requests to `main` run independent jobs for unit tests, Android lint, a debug APK and an API 35 emulator test.
- Debug APKs are downloaded from the Actions run as short-lived artifacts. They are not production signed.
- Tags matching `v*.*.*` trigger the production workflow. An exact semver tag is required. The release fails with a useful error until signing secrets are set.
- Android builds use JDK 17, Gradle 8.11.1 and Android SDK 35.

## Security notes

The GitHub release job receives `contents: write` only where needed. Signing secrets are used solely during the release job and the generated keystore is kept in the runner's temporary directory. Avoid running unreviewed pull-request code in a trusted release workflow.
