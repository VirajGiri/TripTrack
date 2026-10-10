# TripTrack Android

Offline-first Android application for manual driver trip entry, client rate cards, expenses, reports and billing.

## Stack
- Kotlin, Jetpack Compose and Material 3
- Room/SQLite local persistence with account-scoped records
- Firebase Authentication email/password
- Cloud Firestore pull/upload worker with WorkManager retry and UID-scoped rules
- Android PDF generation and share sheet

## Phase 1 behavior
- Manual trip entry only. No GPS or automatic trip tracking.
- Track duty time, odometer KM, toll and other expenses.
- Client-specific rate cards. Blank rates are treated as unknown and are never guessed.
- Edit/delete trips and generate invoice PDFs with immutable local invoice snapshots.
- Local data drives screens and calculations; sign-in does not automatically transfer pre-login local data.
- Cloud sync requires Firebase setup; see `docs/firebase-setup.md`.

## Package allowances
- Pickup/drop: 4 hours or 40 KM.
- Local: 8 hours or 80 KM.
- Outstation: 300 KM/day.
- Outstation extra distance: ₹12/km.
- Other base and excess rates must be configured per client.

## Build and tests
GitHub Actions workflow: `.github/workflows/android-build.yml`. It uses Gradle 8.11.1 to run unit tests and assemble a debug APK. The workflow result has not yet been verified in this session.

## Current limitations
- Multi-day outstation trips need day-wise distance entries.
- Existing local pending edits are protected during cloud pull; conflict resolution uses timestamps for synced trips/clients.
- Billing currently prorates excess time by minute and charges excess distance independently; confirm that this matches the agreed package policy.
