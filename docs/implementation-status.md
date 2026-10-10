# Implementation status

## Implemented in the starter
- Kotlin + Jetpack Compose app scaffold with five-tab navigation.
- Room database and local `trips` table.
- Manual trip form persists trips locally.
- Validation for ISO date, time, non-negative odometer and expense values.
- Overnight trip duration support.
- Dashboard trip count, total kilometres and duty duration from Room.
- Local trip history list.
- Sync status field reserved for future Firebase synchronization.

## Pending
- Client CRUD and rate-card persistence.
- Billing totals and invoice/PDF export.
- Firebase configuration, authentication, Firestore security rules and synchronization worker.
- Edit/delete trip actions.
- Automated unit/UI tests and verified Android build.

Firebase is not connected until project-specific configuration and security rules are added.
