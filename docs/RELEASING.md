# Releasing PIP-OS

PIP-OS publishes versioned, signed APK and AAB files using GitHub Actions. **The recommended release method is the Actions tab; no local Git tags or Gradle commands are required.** The traditional push-tag trigger is also supported.

## One-time signing setup

Production releases require a stable Android signing key. **Back up the keystore and passwords securely.** Never commit them. Losing the signing identity prevents installation of future updates over apps signed with the old identity.

Generate a keystore with a locally installed JDK:

```bash
keytool -genkeypair -v -keystore pipos-release.jks -alias pipos -keyalg RSA -keysize 3072 -validity 10000
```

Store these repository secrets under **Settings > Secrets and variables > Actions**:

| Secret | Value |
| --- | --- |
| `ANDROID_KEYSTORE_BASE64` | Base64 of the whole keystore, on one line |
| `ANDROID_KEYSTORE_PASSWORD` | Keystore password |
| `ANDROID_KEY_ALIAS` | `pipos`, or your alias |
| `ANDROID_KEY_PASSWORD` | Private-key password |

Encode the file in Windows PowerShell and copy it to the clipboard:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes((Resolve-Path ./pipos-release.jks))) | Set-Clipboard
```

Keep the original keystore outside the Git repository.

## Recommended: release from GitHub Actions

1. Ensure the normal **Android CI** checks are passing.
2. Open **Actions > Production release > Run workflow**.
3. Select the `main` branch.
4. Pick `patch`, `minor`, or `major` (defaults to `patch`). Optionally enter an **exact version** like `0.2.0`, which overrides the bump.
5. Select **Run workflow**.

**Version selection:** If there are no existing version tags, the default first release is `v0.2.0`. Thereafter the workflow chooses the next semantic version based on the latest stable `vMAJOR.MINOR.PATCH` tag: patch `0.2.0 -> 0.2.1`, minor `0.2.0 -> 0.3.0`, major `0.2.0 -> 1.0.0`. You can supply a newer exact version instead. Invalid, existing or non-increasing versions are rejected. Manual runs are restricted to `main`.

The workflow validates the version, runs unit tests and release lint, restores signing secrets, creates a signed APK and AAB, uploads build artifacts and SHA-256 checksums, and **only then creates the Git tag and publishes the GitHub Release**. If compilation/signing fails, the workflow doesn't create a new tag.

## Alternative: publish by pushing a tag

```bash
git switch main
git pull --ff-only
git tag -a v0.2.0 -m "PIP-OS 0.2.0"
git push origin v0.2.0
```

The same workflow runs on matching tags. Exact stable semantic versions are enforced. Existing tags are never moved by the workflow. When using this method the tag is created **before** build validation, unlike manual releases.

## Release files

GitHub **Releases** contains:

- `PIP-OS-vVERSION.apk` for direct installation
- `PIP-OS-vVERSION.aab` for supported app-store distribution
- `SHA256SUMS.txt` for integrity verification

The Android `versionName` comes from the chosen version and `versionCode` uses `major * 1000000 + minor * 1000 + patch`. Major must be at most 1999, minor and patch at most 999. This ensures increasing versionCodes for increasing supported versions.

**Debug-signed apps cannot be updated in place by an APK signed with the production keystore.** Uninstalling the debug app removes locally stored preferences. Once on a production-signed build, reuse the same signing key for all subsequent releases.

## Troubleshooting

- **No Run workflow button:** Ensure `.github/workflows/release.yml` exists on the default branch and you have repository write access.
- **Missing Android signing secrets:** Configure all four secrets above.
- **Version already exists or not newer:** Choose a new version or use patch/minor/major.
- **Tests/lint/build failed:** No release is published. Fix CI before retrying.
- **Actions permissions:** The release job needs `contents: write` to publish release assets and create its tag.

Ordinary pushes and pull requests to `main` still run independent unit-test, Android lint, debug APK, and emulator UI-test jobs. Debug artifacts are not production-signed.
