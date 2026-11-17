# YapPay

A lightweight Android expense tracker with a voice-first capture flow: speak an expense
("coffee 120 at Blue Tokai"), and YapPay parses the amount, category, and note, then saves it.

## Features

- **Voice entry** — speech recognition with on-device parsing of amount, category, and note
- **Quick capture widget** — home-screen widget with press-and-hold voice capture
- **Dashboard & summaries** — today / this month totals, category breakdowns
- **Categories** — 13 built-ins plus custom user-created categories with auto-classification
- **CSV export** — share your expenses as a spreadsheet
- **Manual add/edit** — full form for editing or adding expenses without the mic

## Tech stack

- Kotlin + Jetpack Compose (Material 3)
- Room database
- MVVM with ViewModel + StateFlow
- `SpeechRecognizer` platform API (online recognition, no third-party SDK)

## Building

Open the project in Android Studio (Ladybug or newer), or run:

```bash
./gradlew assembleDebug
```

Requires JDK 17 (Android Studio's bundled JBR works).

## Running tests

```bash
./gradlew testDebugUnitTest
```

## Package

`com.yappay.app` — application ID and Kotlin package.
