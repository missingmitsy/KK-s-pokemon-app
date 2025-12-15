# KK's Pokemon Alert - Android App

Native Android app built with Kotlin and Jetpack Compose that displays real-time Pokemon Center queue status and receives push notifications via Firebase Cloud Messaging.

## Features

- **Real-time Status Display**:
  - 🔴 **Red Screen**: "Queue Up" - Virtual queue is active
  - 🟢 **Green Screen**: "No Queue" - Site accessible without queue
  - 🟡 **Gray Screen**: "Snoozed" - Notifications temporarily disabled

- **Push Notifications**:
  - Receives FCM notifications every 3 minutes from backend
  - Displays in notification tray
  - High priority for "Queue Up" alerts

- **Snooze Functionality**:
  - Silence notifications for 2 hours
  - Automatically resumes after snooze period
  - Visual indicator when snoozed

- **Modern UI**:
  - Built with Jetpack Compose
  - Material Design 3
  - Clean, minimal interface

## Prerequisites

1. **Android Studio** (Arctic Fox or later)
2. **Kotlin** 1.9.20+
3. **Android SDK** 34+
4. **Firebase Project** (shared with backend)

## Firebase Setup

### 1. Add Android App to Firebase Project

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your project (same one used for backend)
3. Click **Add app** and select **Android**
4. Enter package name: `com.kk.pokemonalert`
5. Download `google-services.json`

### 2. Configure Firebase in App

1. Place `google-services.json` in `android/app/` directory
   ```bash
   cp ~/Downloads/google-services.json android/app/
   ```

2. The app will automatically subscribe to the `pokemon_queue_alerts` topic on first launch

⚠️ **IMPORTANT**: Never commit `google-services.json` to version control! It's in `.gitignore`.

### 3. Enable Firebase Cloud Messaging

1. In Firebase Console, go to **Cloud Messaging**
2. Ensure Firebase Cloud Messaging API is enabled
3. Note: The backend handles sending messages to the topic

## Building the App

### Open in Android Studio

1. Open Android Studio
2. Select **File** > **Open**
3. Navigate to the `android/` directory
4. Click **OK**

Android Studio will automatically sync Gradle dependencies.

### Build from Command Line

```bash
cd android
./gradlew build
```

### Generate Debug APK

```bash
cd android
./gradlew assembleDebug
```

The APK will be located at:
```
android/app/build/outputs/apk/debug/app-debug.apk
```

### Generate Release APK

```bash
cd android
./gradlew assembleRelease
```

You'll need to set up signing configuration for production releases.

## Installation

### Install via Android Studio

1. Connect your Android device via USB or start an emulator
2. Click the **Run** button (green play icon)
3. Select your device
4. App will be installed and launched

### Install via ADB

```bash
adb install android/app/build/outputs/apk/debug/app-debug.apk
```

## Permissions

The app requests the following permissions:

- **INTERNET**: Required for Firebase communication
- **POST_NOTIFICATIONS** (Android 13+): Required to display notifications

Users will be prompted to grant notification permission on first launch.

## Usage

### First Launch

1. **Grant Permissions**: Accept notification permission when prompted
2. **Wait for Status**: The app will display "CHECKING..." until first notification arrives
3. **View Status**: Screen will turn red (Queue Up) or green (No Queue)

### Snoozing Notifications

1. Tap **"SNOOZE FOR 2 HOURS"** button
2. Notifications will be silenced for 2 hours
3. Screen turns gray to indicate snoozed state
4. Tap **"RESUME NOTIFICATIONS"** to un-snooze early

### Viewing Notifications

- Notifications appear in the notification tray
- Tap notification to open the app
- App updates in real-time with each notification

## Project Structure

```
android/
├── app/
│   ├── src/main/
│   │   ├── java/com/kk/pokemonalert/
│   │   │   ├── MainActivity.kt                      # Main UI with Compose
│   │   │   ├── PokemonFirebaseMessagingService.kt   # FCM message handler
│   │   │   └── PreferencesManager.kt                # DataStore for snooze state
│   │   ├── res/
│   │   │   ├── values/
│   │   │   │   ├── strings.xml                      # String resources
│   │   │   │   └── themes.xml                       # App theme
│   │   │   ├── drawable/
│   │   │   │   └── ic_notification.xml              # Notification icon
│   │   │   └── xml/
│   │   │       ├── backup_rules.xml
│   │   │       └── data_extraction_rules.xml
│   │   └── AndroidManifest.xml                      # App manifest
│   ├── build.gradle                                  # App dependencies
│   └── google-services.json                          # Firebase config (not in repo)
├── build.gradle                                      # Root build config
├── settings.gradle                                   # Project settings
└── gradle.properties                                 # Gradle properties
```

## Key Components

### MainActivity.kt
- Main UI using Jetpack Compose
- Displays queue status with color-coded background
- Handles snooze button logic
- Subscribes to FCM topic on launch

### PokemonFirebaseMessagingService.kt
- Extends `FirebaseMessagingService`
- Receives FCM messages from backend
- Checks snooze state before showing notifications
- Updates stored queue status

### PreferencesManager.kt
- Uses DataStore for persistent storage
- Stores queue status and snooze timestamp
- Provides Flow-based reactive data access

## Customization

### Change Package Name

If you want to use a different package name:

1. Update `namespace` in `app/build.gradle`
2. Update `applicationId` in `app/build.gradle`
3. Update package in Firebase Console
4. Download new `google-services.json`
5. Refactor package in Android Studio

### Change FCM Topic

To use a different FCM topic:

1. Update topic name in `MainActivity.kt`:
   ```kotlin
   FirebaseMessaging.getInstance().subscribeToTopic("your_topic_name")
   ```

2. Update backend `.env` file:
   ```
   FCM_TOPIC=your_topic_name
   ```

### Adjust Snooze Duration

To change snooze duration from 2 hours:

In `MainActivity.kt`, modify:
```kotlin
val twoHoursInMillis = 2 * 60 * 60 * 1000L  // Change this value
```

## Troubleshooting

### No Notifications Received

**Check Firebase Setup**:
- Verify `google-services.json` is in `app/` directory
- Check package name matches Firebase configuration
- Ensure app successfully subscribes to topic (check Logcat)

**Check Permissions**:
- Verify notification permission granted in Settings
- Check if Do Not Disturb is enabled on device

**Check Backend**:
- Ensure backend is running and sending notifications
- Verify backend uses same FCM topic name

### App Won't Build

**Sync Gradle**:
```bash
./gradlew clean build
```

**Check Android SDK**:
- Ensure SDK 34 is installed in Android Studio
- Update build tools if necessary

**Check Dependencies**:
- Verify internet connection for dependency download
- Check `build.gradle` for any version conflicts

### Status Not Updating

**Check FCM Connection**:
- Look for Firebase initialization in Logcat
- Verify token registration

**Check Data Storage**:
- Clear app data in device Settings if needed
- Reinstall app

### Notification Icon Missing

The app includes a basic notification icon. For a custom icon:

1. Create `ic_notification.xml` in `res/drawable/`
2. Use white icon on transparent background
3. Size: 24x24 dp

## Testing

### Test Notification Reception

1. Use backend's `/test-notification` endpoint
2. Check Logcat for FCM messages
3. Verify notification appears in tray

### Test Snooze Functionality

1. Enable snooze in app
2. Trigger backend notification
3. Verify no notification shown
4. Check app shows "SNOOZED" status

### Test Status Display

1. Ensure backend is running
2. Wait for notifications (or trigger manually)
3. Verify screen color changes appropriately

## Production Considerations

### Release Build

1. **Create Keystore**:
   ```bash
   keytool -genkey -v -keystore release.keystore -alias kk-pokemon-alert -keyalg RSA -keysize 2048 -validity 10000
   ```

2. **Configure Signing** in `app/build.gradle`:
   ```gradle
   android {
       signingConfigs {
           release {
               storeFile file('release.keystore')
               storePassword 'your_password'
               keyAlias 'kk-pokemon-alert'
               keyPassword 'your_password'
           }
       }
       buildTypes {
           release {
               signingConfig signingConfigs.release
               minifyEnabled true
               proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
           }
       }
   }
   ```

3. **Build Release APK**:
   ```bash
   ./gradlew assembleRelease
   ```

### Play Store Deployment

1. Generate signed release APK or AAB
2. Create developer account
3. Upload to Play Console
4. Complete store listing
5. Submit for review

## License

MIT License - See root README for details.
