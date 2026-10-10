# Firebase setup

Firebase Authentication and a UID-scoped Firestore sync worker are implemented, but live cloud operation requires a project-specific configuration.

1. Create/select a Firebase project.
2. Register Android package `com.virajgiri.triptrack`.
3. Download `google-services.json` into `app/google-services.json`. Do not add service-account credentials to the Android app or repository.
4. Rebuild. The Google Services Gradle plugin is applied only when this file exists.
5. In Firebase Console, enable Authentication > Email/Password.
6. Create a Cloud Firestore database.
7. Deploy the repository's `firestore.rules`. Rules restrict documents to the authenticated user's UID namespace and deny access elsewhere.
8. Sign in from Profile. Existing records created before sign-in stay in the local account until the user explicitly selects **Import local device records to this account**.
9. The sync worker runs when the user requests sync or after record changes. It requires network connectivity, retries failures, restores missing records, uploads pending records, and sends deletion tombstones.
10. Existing local pending edits are not overwritten during pull. Synced trip/client records are updated when the remote modification timestamp is newer. Bills are immutable snapshots.

Cloud sync has not been exercised against a live Firebase project in this session. Validate auth, Firestore rules, offline/reconnect behavior and multi-device conflict cases before production use.
