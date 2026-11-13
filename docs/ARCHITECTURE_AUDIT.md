# Signal Android – Complete Codebase Architecture Audit

## 1. Project Structure and App Entry Points
Signal Android is structured as an advanced multi-module Android project designed for scale, modularity, and compile-time isolation:
- `app/`: Primary application module containing the core client UI, business logic, SQLCipher database, domain repositories, background jobs, and application lifecycle observers.
- `core/`: Foundational shared libraries including:
  - `core/ui`: Shared UI components, window helpers (`WindowExtensions.kt`), theme styling, and base dialog fragments.
  - `core/util`: Concurrency utilities, streams, logging primitives (`Log.java`), and common extensions.
  - `core/models`: Core domain and database data models (e.g., `AttachmentId`, `RecipientId`).
- `feature/`: Feature modules providing self-contained user flows:
  - `feature/settings`: Application and chat settings screens.
  - `feature/registration`: Onboarding, phone verification, PIN creation, and account recovery.
  - `feature/media-send`: Media picker, camera capture, image editor, and attachment preparation.
  - `feature/media-viewer`: Standalone/shared media viewing components.
- `lib/`: Independent foundational libraries:
  - `lib/signal-service`: Low-level protocol, cryptographic primitives, and server network layer.
  - `lib/image-editor`: Canvas and sticker editing tools.
  - `lib/video`: Video transcoding and streaming tools.

### Application Entry Points
- `org.thoughtcrime.securesms.ApplicationContext`: The primary `android.app.Application` subclass initialized at process start. It initializes `AppDependencies`, `SignalStore`, SQLCipher database encryption, cryptographic secrets, logging, background job queues, and security policies.
- `org.thoughtcrime.securesms.MainActivity`: Primary launcher activity routing users to either registration/onboarding (`RegistrationActivity`) or the main conversation list (`ConversationListFragment`).
- `org.thoughtcrime.securesms.conversation.v2.ConversationActivity`: Primary chat activity hosting 1:1 and group messaging UI (`ConversationFragment`).
- `org.thoughtcrime.securesms.mediaviewer.MediaViewerActivity` / `org.thoughtcrime.securesms.mediapreview.MediaPreviewActivity`: Primary entry points for viewing received and sent media (images, videos, documents).

## 2. Activity and Fragment Architecture
- **Base Activity**: `org.thoughtcrime.securesms.BaseActivity` extends `AppCompatActivity` and serves as the common base class for top-level activities. It handles edge-to-edge system bar configuration, dynamic theme resolution, and window screenshot security via `WindowUtil.initializeScreenshotSecurity(this, getWindow())`.
- **Base Dialogs**:
  - `FullScreenDialogFragment` and `WrapperDialogFragment`: Common dialog abstractions in `app`.
  - `FixedRoundedCornerBottomSheetDialogFragment` and `ComposeFullScreenDialogFragment`: Base classes in `core/ui` configuring window flags, system insets, and screenshot security.
- **Fragments**: Signal utilizes Jetpack Navigation and fragment transactions across feature flows, hosting both legacy XML view bindings and modern Jetpack Compose layouts.

## 3. Jetpack Compose and XML UI Usage
The repository is in an active hybrid state:
- **XML Views**: Legacy conversation lists, bubble views (`ConversationItem`), custom widgets (`ComposeText`), and classic dialogs utilize standard Android XML layouts and data binding/view binding.
- **Jetpack Compose**: Modernized settings, registration flows, stories, avatar picker, and the new media viewer (`MediaViewerActivity`) are implemented in Jetpack Compose.
- **Window Interoperability**: Security controls must operate at the Android `Window` level (`android.view.Window`) rather than purely within Compose or XML layout hierarchies to ensure full platform-level coverage across all UI implementations.

## 4. Navigation Architecture
- **Activities**: Top-level entry points (`MainActivity`, `ConversationActivity`, `MediaViewerActivity`, `AppSettingsActivity`) are coordinated via explicit Android Intents.
- **Jetpack Navigation Component**: Used extensively within modern modules (`feature/registration`, `feature/settings`) for intra-feature screen routing.
- **Bottom Sheets & Dialogs**: Managed via `FragmentManager` using specialized base dialog fragments that bind to host windows.

## 5. Conversation and Group Messaging Implementations
- **1:1 Messaging**: Messages are stored in the local encrypted database (`MessageTable`, `MmsDatabase`) and transmitted using the Signal Protocol (Double Ratchet, Prekeys) via `SignalWebSocketHealthMonitor` and encrypted REST endpoints.
- **Group Messaging**: Signal Groups (GroupsV2) use client-side sender keys and decrypted profile keys. Group state is managed via `GroupManager` and `GroupDatabase`. Attachments in groups are encrypted once with an ephemeral symmetric key and sent to CDN, while keys are delivered via encrypted sender key messages to group members.

## 6. Attachment Selection and Upload Workflows
- **Selection**: Initiated from `ConversationFragment` via attachment menu or system photo picker (`ActivityResultContracts.PickVisualMedia` / `PickMultipleVisualMedia`), preventing unrestricted gallery read access.
- **Preparation & Staging**: Handled by `MediaSendV3Repository` and `BlobProvider`. Raw content streams are processed, transcoded (if video), stripped of sensitive metadata (EXIF GPS), and staged in app-private temporary blob storage.
- **Encryption & Upload**: `AttachmentUploadJob` encrypts the attachment payload using AES-256-GCM / streaming encryption with random keys and uploads the ciphertext to the Signal CDN. The resulting encryption keys, digest, and CDN location are attached to the outgoing encrypted message payload.

## 7. Media Download and Rendering Workflows
- **Download**: When a message with media is received, `AttachmentDownloadJob` fetches the ciphertext from the Signal CDN.
- **Storage At Rest**: Ciphertext from CDN is decrypted and immediately re-encrypted at rest using Signal's app-specific `AttachmentSecret` (AES-CBC + HMAC) and saved into the app's private `parts/` directory via `AttachmentTable`.
- **Streaming & Rendering**: Media is loaded on-demand via `PartAuthority.getAttachmentStream()`, which invokes `ModernDecryptingPartInputStream` to decrypt data on-the-fly directly into memory buffers for Glide image rendering or ExoPlayer video playback. Plaintext media is never persisted unencrypted to disk during standard viewing.

## 8. Database and Attachment Persistence Mechanisms
- **Encrypted Database**: Signal uses SQLCipher (`SignalDatabase`) with a master secret derived from passphrase/keystore.
- **Attachment Metadata**: Tracked in `AttachmentTable` (`app/src/main/java/org/thoughtcrime/securesms/database/AttachmentTable.kt`), storing content types, byte sizes, blurhashes, transfer state, and relative file paths within the private `app_parts` directory.
- **Integrity**: Each attachment part has associated cryptographic hashes (`DATA_HASH_START`, `DATA_HASH_END`) and a random salt (`DATA_RANDOM`) preventing unauthorized tampering or cross-file correlation.

## 9. Application Lifecycle Management
- **Startup**: `AppStartup` orchestrates multi-phase initialization without blocking critical UI rendering.
- **Process & Key Locking**: `KeyCachingService` manages memory-resident decryption keys and locks secrets when the application is backgrounded or when timeout expires.
- **Window Lifecycle**: Base activities and window observers update window properties (`FLAG_SECURE`) during `onCreate()`, `onResume()`, and window focus transitions.

## 10. Existing Screenshot Protection Analysis
- **Core Implementation**: `org.signal.core.ui.WindowExtensionsKt.initializeScreenshotSecurity(Window)` applies `WindowManager.LayoutParams.FLAG_SECURE` based on `CoreUiDependencies.isScreenSecurityEnabled`.
- **Settings Store**: `SignalStore.settings.isScreenSecurityEnabled` stores user preference (defaults to `false` in upstream Signal).
- **Temporary Security**: `TemporaryScreenshotSecurity` allows temporary acquisition of `FLAG_SECURE` for sensitive operations (e.g., passphrase entry, PIN screens).
- **Audit Findings**:
  - Upstream allows disabling screen security via user preferences.
  - Dialog windows and sub-activities do not always inherit the flag unless explicitly invoked in each fragment's lifecycle.
  - Mandatory privacy customization requires enforcement of `FLAG_SECURE` universally across all sensitive windows without accidental clearing.

## 11. External Sharing and Export Mechanisms
- **Media Saving**: `SaveAttachmentUtil.kt` and `AttachmentSaver.kt` provide user-initiated saving of media attachments directly to Android `MediaStore` (public `Pictures/`, `Movies/`, `Download/` directories).
- **External App Sharing**:
  - `MediaViewerActivity.share()` and `MediaPreviewFragment.share()` invoke `ShareCompat.IntentBuilder` with `PartAuthority.getAttachmentPublicUri(uri)`.
  - `PartProvider` (`${applicationId}.part`) is configured with `android:grantUriPermissions="true"`, allowing foreign applications to stream decrypted media out of the app sandbox.
- **Stories Sharing**: `StoryContextMenu.kt` exposes direct share intents with public attachment streams.
- **Audit Findings**: These entry points allow received media to be exported to external third-party apps and stored permanently in shared device storage, violating the privacy requirements.

## 12. Existing Media Cleanup Mechanisms
- **Thread Trimming**: `TrimThreadsJob` prunes old messages based on user storage thresholds.
- **Disappearing Messages**: Handled by expiration timers in `ExpiringMessageManager`, deleting database entries and calling `AttachmentTable.deleteAttachmentsForMessage()`.
- **Part File Deletion**: When an attachment record is deleted, `AttachmentTable` deletes the corresponding encrypted file from `parts/`.
- **Audit Findings**: Upstream lacks an ephemeral session-based or bounded memory/cache retention policy specifically for received media files to prevent accumulation while preserving conversation history metadata.

## 13. Background Workers and Services
- **Job Engine**: Custom `JobManager` manages persistent, fault-tolerant background tasks (`AttachmentUploadJob`, `AttachmentDownloadJob`, `SendJob`, `TrimThreadsJob`).
- **WorkManager**: Used for periodic tasks, maintenance, and database compaction.
- **Services**: `KeyCachingService` for secret caching; `PushNotificationService` for FCM/WebSocket notification processing.

## 14. Build Variants and Application Identifiers
- `flavorDimensions`: Configured for distribution channels (`play`, `website`) and environments (`prod`, `staging`).
- `applicationId`: Defined in `app/build.gradle.kts` (default `org.thoughtcrime.securesms`).
- ContentProvider authorities (`.part`, `.avatar`, `.blob`, `.fileprovider`) are dynamically qualified with `${applicationId}`.

## 15. Existing Test Infrastructure
- **Unit Testing**: JUnit 4, Robolectric for Android framework simulation, MockK, AssertK.
- **Integration & Database Tests**: Located in `app/src/test/` and `app/src/androidTest/` covering `AttachmentTable`, `SaveAttachmentUtil`, `ConversationViewModel`, and cryptographic primitives.
- **Verification Strategy**: Automated unit tests can validate window security flag states, export policy enforcement, and attachment interceptors without requiring physical device hardware.
