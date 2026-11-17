# YapPay

A lightweight Android expense tracker with a voice-first capture flow: speak an expense
("coffee 120 at Blue Tokai"), and YapPay parses the amount, category, and note, then saves it.

## ⭐ The widget — zero-effort expense logging

YapPay's best feature is its **home-screen quick capture widget**. No app-opening, no
navigation — the entire expense gets logged from the home screen in a few seconds:

1. Tap the widget and hold — the mic opens right where you are
2. Say the expense — *"auto 400 petrol"* or *"lunch 250 with team"*
3. Done. Amount, category, and note are parsed and saved automatically

The widget also shows your live totals (today / this month) at a glance, so you always
know where you stand without opening the app at all. This is what makes YapPay feel
effortless: logging an expense becomes faster than forgetting it.

## Features

- **Quick capture widget** — the star of the show (see above): press-and-hold voice
  capture from the home screen with live today/month totals
- **Voice entry** — speech recognition with on-device parsing of amount, category, and note
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
