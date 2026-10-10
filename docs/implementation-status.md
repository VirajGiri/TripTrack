# Implementation status

## Implemented
- Kotlin + Jetpack Compose app scaffold with five-tab navigation.
- Room local database with non-destructive migrations for trips, clients/rate cards, invoice snapshots, account ownership and modification timestamps.
- Manual trip entry only; no GPS or automatic trip tracking.
- Validation for date/time, non-negative odometer and expense values; overnight duration support.
- Dashboard trip count, total kilometres and duty duration from Room.
- Client create/edit/delete and configurable package rates; blank rates remain unconfigured.
- Billing calculation for pickup/drop, local and single-day outstation trips; missing rates block billing.
- Invoice line-item snapshots and PDF generation/sharing through Android's share sheet.
- Saved invoice history with PDF re-export.
- Trip edit and confirmed delete; prior invoice snapshots are retained.
- Local driver and vehicle profile persisted on-device.
- Firebase email/password sign-in and account creation UI, enabled when project config is present.
- UID-scoped Room queries and an explicit opt-in flow to import pre-login local records into the signed-in account.
- WorkManager sync worker restores missing Firestore records, uploads pending trips/clients/bills, reconciles newer trip/client timestamps, and propagates trip/client deletions using tombstones.
- Firestore security rules scoped to each authenticated user's UID.
- Unit tests for billing boundaries and trip validation.
- GitHub Actions workflow for Android unit tests and debug APK build.

## Pending / blocked on configuration or further verification
- A real Firebase project, `app/google-services.json`, enabled Email/Password provider and deployed Firestore rules are required for live cloud operation.
- Sync keeps existing local pending edits rather than overwriting them. Cross-device conflicts are handled with last-write-wins timestamps for synced trips and clients; a richer conflict-resolution UI is not implemented.
- Driver/vehicle profile is local-only and is not part of cloud sync.
- Multi-day outstation day-wise distance and billing.
- Build/test workflow execution has not yet been observed; check GitHub Actions after push.

## Billing notes
- Missing rates are never guessed.
- Excess time is prorated per minute from the configured hourly rate.
- If the commercial meaning of “X hours or Y KM” differs from charging excess time and excess distance independently, the policy must be changed explicitly.
