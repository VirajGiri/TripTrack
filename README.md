# TripTrack Android

Offline-first Android application for manual driver trip entry, client rate cards, expenses, reports and billing.

## Stack
- Kotlin, Jetpack Compose and Material 3
- Room/SQLite local persistence
- Firebase Authentication email/password UI (requires project configuration)
- Firebase Cloud Firestore SDK dependencies; synchronization remains pending
- WorkManager dependency for future retryable sync
- Android PDF generation and share sheet

## Phase 1 behavior
- Manual trip entry only. No GPS or automatic trip tracking.
- Track duty time, odometer KM, toll and other expenses.
- Client-specific rate cards. Blank rates are treated as unknown and are never guessed.
- Edit/delete trips and generate invoice PDFs with immutable local invoice snapshots.
- Offline local database is the source of truth for screens and calculations.

## Package allowances
- Pickup/drop: 4 hours or 40 KM.
- Local: 8 hours or 80 KM.
- Outstation: 300 KM/day.
- Outstation extra distance: ₹12/km.
- Other base and excess rates must be configured per client.

## Open in Android Studio
Open this repository as a Gradle project using JDK 17. Firebase is not configured yet; see `docs/firebase-setup.md`.

## Build and tests
GitHub Actions workflow: `.github/workflows/android-build.yml`. It uses Gradle 8.11.1 to run unit tests and assemble a debug APK. The workflow result has not yet been verified in this session.

## Current implementation status
See `docs/implementation-status.md`. Firestore sync, driver/vehicle profile editing and multi-day outstation billing remain pending. Billing currently prorates excess time by minute and charges excess distance independently; confirm that this matches the agreed package policy.
