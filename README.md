# Afaq | Life Development Android Wrapper

Native Android WebView wrapper for the local static app "Afaq | Life Development" / "Afaq Arabic app".

## GitHub Actions Build

This project includes:

`.github/workflows/android-build.yml`

The workflow runs on:

- `push`
- manual run with `workflow_dispatch`

It installs JDK 17 and Android API 35, then runs:

```bash
./gradlew clean
./gradlew assembleDebug
./gradlew bundleRelease
```

## Upload to GitHub

1. Create a new GitHub repository.
2. Upload all files from this `AfaqLifeAndroid` folder to the repository root.
3. Make sure these files are at the top level:
   - `settings.gradle.kts`
   - `build.gradle.kts`
   - `gradlew`
   - `gradlew.bat`
   - `app/`
   - `.github/workflows/android-build.yml`
4. Commit and push.

## Run the Workflow

1. Open the repository on GitHub.
2. Go to **Actions**.
3. Select **Android Build**.
4. Click **Run workflow**.

The workflow also runs automatically after every push.

## Download APK/AAB Artifacts

After the workflow finishes:

1. Open the completed workflow run.
2. Scroll to **Artifacts**.
3. Download:
   - `afaq-debug-apk`
   - `afaq-release-aab-unsigned`
   - `afaq-mapping-files` if present

## Debug APK vs Release AAB

- Debug APK: for quick testing on your Android phone. It is not for Google Play release.
- Release AAB: Android App Bundle format for Google Play. This workflow creates an unsigned release AAB unless signing is configured later.

## Google Play Signing Later

For Google Play release, you will need:

- A Google Play Developer account.
- A release keystore, or Google Play App Signing setup.
- Signing configuration added to `app/build.gradle.kts`, usually through GitHub Actions secrets.

Do not upload the debug APK to Google Play.

## Local Build Without Android Studio

You can build locally without Android Studio if JDK 17 and Android SDK API 35 are installed:

```bash
./gradlew clean
./gradlew assembleDebug
./gradlew bundleRelease
```

If you do not have JDK/Android SDK installed, use the GitHub Actions workflow.

## Project Settings

- Package name: `com.afaq.life`
- Main activity: `com.afaq.life.MainActivity`
- minSdk: `23`
- targetSdk: `35`
- compileSdk: `35`
- Local WebView entry point: `file:///android_asset/web/index.html`

To change the package name later, update:

- `app/build.gradle.kts` -> `namespace` and `applicationId`
- Kotlin package declarations under `app/src/main/java/com/afaq/life/`

To change the app name later, update:

- `app/src/main/res/values/strings.xml`

## Local Web Assets

The web app is bundled locally under:

`app/src/main/assets/web/`

Included files:

- `index.html`
- `privacy.html`
- `manifest.webmanifest`
- `app.js`
- `app-ads.txt`
- `assets/icons/`

## Notifications

Android exposes this JavaScript bridge:

```javascript
window.AfaqAndroid.showNotification(title, body)
```

The copied web app first tries this bridge, then falls back to normal browser notifications or toast messages for normal web usage.
