# Daily Gold Price UAE (Android App)

A professional, luxury-designed Android application to monitor gold prices in the UAE.

## Features
- **Real-time Prices**: Live gold spot prices from reliable APIs.
- **Multiple Karats**: Supports 24K, 22K, 21K, and 18K.
- **Dual Currency**: Switch between UAE Dirham (AED) and US Dollar (USD).
- **Luxury UI**: Modern Dark & Gold design using Jetpack Compose.
- **Bilingual**: Full support for Arabic and English.
- **AdMob Integrated**: Built-in AdMob banner ads for monetization.

## Project Structure
- `com.uae.goldprice`: Root package.
- `GoldViewModel.kt`: Handles price fetching and business logic.
- `MainActivity.kt`: Main UI entry point.
- `GoldTheme.kt`: Custom luxury theme configuration.

## Build Instructions
1. Ensure you have JDK 17 installed.
2. Run `./gradlew assembleDebug` to build the test APK.
3. For release, configure your keystore in `app/build.gradle.kts`.

## API Source
The app uses `gold-api.com` for real-time gold spot prices.

## Google Play Publishing
- **Package Name**: `com.uae.goldprice`
- **Target SDK**: 35 (Latest Android)
- **Minimum SDK**: 24 (Android 7.0+)
