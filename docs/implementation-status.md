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
- Trip edit and confirmed delete; prior invoice snapshots are retained.
- Firebase email/password sign-in and account creation UI, enabled when project config is present.
- Firestore security rules scoped to each authenticated user's UID.
- Unit tests for billing boundaries and trip validation.
- GitHub Actions workflow for Android unit tests and debug APK build.

## Pending / blocked on configuration
- Live Firestore synchronization and user-scoped ownership for local records. Do not treat authentication as data sync.
- Driver and vehicle profile editing.
- Multi-day outstation day-wise distance and billing.
- Build/test workflow execution has not yet been observed; check GitHub Actions after push.
- The Firebase Console must have Email/Password auth enabled; add `app/google-services.json` locally to initialize Firebase. Do not commit service-account keys.

## Billing notes
- Missing rates are never guessed.
- Excess time is prorated per minute from the configured hourly rate.
- If the commercial meaning of “X hours or Y KM” differs from charging excess time and excess distance independently, the policy must be changed explicitly.
