# TripTrack Android

Offline-first Android application for manual driver trip entry, client rate cards, expenses, reports and billing.

## Phase 1
- Manual trip entry only (no GPS or automatic tracking)
- Kotlin, Jetpack Compose and Material 3
- Room/SQLite local database (integration next)
- Firebase Authentication and Cloud Firestore (configuration required)
- Configurable rates; no guessed rates

## Open in Android Studio
Open this folder as a Gradle project using JDK 17. Firebase is not configured yet; see `docs/firebase-setup.md`.

## Billing requirements
Pickup/drop: 4 hours or 40 KM. Local: 8 hours or 80 KM. Outstation: 300 KM/day, with ₹12/km extra distance. Base rates and unconfirmed excess rates must be configured per client.

## Current implementation status
See `docs/implementation-status.md`. Room-backed manual trip saving, basic input validation, dashboard aggregates, and local trip history are included. Firebase sync and billing are still pending.
