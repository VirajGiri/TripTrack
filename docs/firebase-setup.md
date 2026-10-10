# Firebase setup

Firebase Authentication UI is implemented, but Firebase is not linked to a project-specific configuration yet and Firestore synchronization is not implemented.

1. Create/select a Firebase project.
2. Register Android package `com.virajgiri.triptrack`.
3. Download `google-services.json` into `app/google-services.json`. This file contains app configuration, not a service-account private key; do not add service-account credentials to the Android app or repository.
4. Rebuild. The Google Services Gradle plugin is applied only when this file exists, so the app can still build without Firebase configured.
5. In Firebase Console, enable Authentication > Email/Password.
6. Create a Cloud Firestore database.
7. Deploy the repository's `firestore.rules`. Rules restrict documents to the authenticated user's UID namespace and deny access elsewhere.
8. Before implementing sync, add explicit local-record ownership and an opt-in import flow. Do not upload all device records automatically on sign-in; that could expose another user's data on a shared device.
9. Implement retryable WorkManager upload/download, conflict handling and sync status only after ownership is enforced in both Room queries and Firestore paths.

Firebase SDK dependencies and the email/password UI are present. Authentication cannot work until the Firebase project configuration and provider are enabled.
