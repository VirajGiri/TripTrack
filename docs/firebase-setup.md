# Firebase setup

Firebase is not linked to a project-specific configuration yet.

1. Create/select a Firebase project.
2. Register Android package `com.virajgiri.triptrack`.
3. Download `google-services.json` into `app/google-services.json` (do not commit service-account credentials).
4. Enable the chosen Firebase Authentication provider.
5. Create a Cloud Firestore database.
6. Configure restrictive Firestore Security Rules so each user can only access their own records.
7. Configure the Google Services Gradle plugin after the config file is available.
8. Test offline operation, reconnect, duplicate-write prevention and conflict handling.

Firebase SDK dependencies are present, but runtime configuration and sync are not complete.
