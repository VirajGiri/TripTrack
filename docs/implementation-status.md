# Implementation status

## Implemented
- Kotlin + Jetpack Compose app scaffold with five-tab navigation.
- Room local database with non-destructive migrations for trips, clients/rate cards and invoice snapshots.
- Manual trip entry only; no GPS or automatic trip tracking.
- Validation for date/time, non-negative odometer and expense values; overnight duration support.
- Dashboard trip count, total kilometres and duty duration from Room.
- Client create/edit/delete and configurable package rates; blank rates remain unconfigured.
- Billing calculation for pickup/drop, local and single-day outstation trips; missing rates block billing.
- Invoice line-item snapshots and PDF generation/sharing through Android's share sheet.
- Saved invoice history with PDF re-export.
- Firestore security rules scoped to each authenticated user's UID.
- Unit tests for billing boundaries and trip validation.
- GitHub Actions workflow for Android unit tests and debug APK build.

## Pending / blocked on configuration
- Firebase Authentication UI and live Firestore synchronization. A real Firebase project and `google-services.json` are required before these can be completed safely.
- Driver and vehicle profile editing.
- Trip edit/delete.
- Multi-day outstation day-wise distance and billing.
- Build/test workflow execution has not yet been observed; check GitHub Actions after push.

## Billing notes
- Missing rates are never guessed.
- Excess time is prorated per minute from the configured hourly rate.
- If the commercial meaning of “X hours or Y KM” differs from charging excess time and excess distance independently, the policy must be changed explicitly.
