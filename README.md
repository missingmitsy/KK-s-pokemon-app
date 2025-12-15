# KK's Pokemon Alert 🎮

A comprehensive monitoring system for Pokemon Center website queue status with real-time push notifications to your Android device.

## Overview

This project monitors `pokemoncenter.com/en-ca` every 3 minutes and sends push notifications to your Android phone when a virtual queue is detected. Built with Python (FastAPI) backend deployed on Google Cloud Run and a native Android app using Kotlin and Jetpack Compose.

## Features

### 🔍 Backend Monitoring
- Monitors Pokemon Center website every 3 minutes
- Detects keywords: "virtual", "queue", "imperva"
- Sends Firebase Cloud Messaging (FCM) notifications
- Deployed to Google Cloud Run for 24/7 operation
- RESTful API for control and testing

### 📱 Android App
- **Real-time Status Display**
  - 🔴 Red screen for "Queue Up"
  - 🟢 Green screen for "No Queue"
  - 🟡 Gray screen when snoozed
- **Push Notifications** in notification tray
- **Snooze Button** to silence alerts for 2 hours
- Modern UI with Jetpack Compose
- Material Design 3

### 🔥 Firebase Integration
- Shared Firebase project for backend and app
- Firebase Admin SDK for backend notifications
- Firebase Cloud Messaging in Android app
- Topic-based messaging (all devices receive updates)

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     Google Cloud Run                         │
│  ┌──────────────────────────────────────────────────────┐   │
│  │  Python Backend (FastAPI)                            │   │
│  │  • Monitors pokemoncenter.com every 3 minutes        │   │
│  │  • Detects queue keywords                            │   │
│  │  • Sends FCM notifications                           │   │
│  └──────────────────┬───────────────────────────────────┘   │
└─────────────────────┼───────────────────────────────────────┘
                      │
                      │ FCM Messages
                      ▼
         ┌────────────────────────┐
         │  Firebase Cloud        │
         │  Messaging             │
         │  (Topic: pokemon_      │
         │   queue_alerts)        │
         └────────┬───────────────┘
                  │
                  │ Push Notifications
                  ▼
     ┌────────────────────────────┐
     │   Android Device           │
     │  ┌──────────────────────┐  │
     │  │  KK Pokemon Alert    │  │
     │  │  • Displays status   │  │
     │  │  • Shows notifications│ │
     │  │  • Snooze control    │  │
     │  └──────────────────────┘  │
     └────────────────────────────┘
```

## Project Structure

```
KK-s-pokemon-app/
├── backend/                      # Python backend service
│   ├── main.py                   # FastAPI application
│   ├── requirements.txt          # Python dependencies
│   ├── Dockerfile                # Docker configuration
│   ├── .env.example              # Environment template
│   └── README.md                 # Backend documentation
├── android/                      # Android application
│   ├── app/
│   │   ├── src/main/
│   │   │   ├── java/com/kk/pokemonalert/
│   │   │   │   ├── MainActivity.kt
│   │   │   │   ├── PokemonFirebaseMessagingService.kt
│   │   │   │   └── PreferencesManager.kt
│   │   │   ├── res/              # Resources
│   │   │   └── AndroidManifest.xml
│   │   ├── build.gradle          # App dependencies
│   │   └── google-services.json  # Firebase config (not in repo)
│   ├── build.gradle              # Root build config
│   └── README.md                 # Android documentation
├── .gitignore
└── README.md                     # This file
```

## Quick Start

### Prerequisites

1. **Firebase Project**: Create at [Firebase Console](https://console.firebase.google.com/)
2. **Google Cloud Account**: For Cloud Run deployment
3. **Python 3.11+**: For backend development
4. **Android Studio**: For Android app development

### Step 1: Firebase Setup

#### Create Firebase Project

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Click "Add project"
3. Follow setup wizard
4. Enable Google Analytics (optional)

#### Enable Cloud Messaging

1. Navigate to **Project Settings** → **Cloud Messaging**
2. Note down **Server Key** and **Sender ID**

#### Generate Service Account (Backend)

1. Go to **Project Settings** → **Service Accounts**
2. Click **Generate New Private Key**
3. Download JSON file as `firebase-adminsdk.json`
4. Place in `backend/` directory

#### Add Android App

1. In Firebase Console, click **Add app** → **Android**
2. Package name: `com.kk.pokemonalert`
3. Download `google-services.json`
4. Place in `android/app/` directory

### Step 2: Backend Setup

```bash
# Navigate to backend
cd backend

# Install dependencies
pip install -r requirements.txt

# Configure environment
cp .env.example .env
# Edit .env with your settings

# Place Firebase credentials
# Copy firebase-adminsdk.json to backend/

# Run locally
python main.py

# Start monitoring
curl -X POST http://localhost:8080/start-monitoring
```

See [backend/README.md](backend/README.md) for detailed instructions.

### Step 3: Deploy Backend to Cloud Run

```bash
# Authenticate with Google Cloud
gcloud auth login
gcloud config set project YOUR_PROJECT_ID

# Build and deploy
gcloud builds submit --tag gcr.io/YOUR_PROJECT_ID/kk-pokemon-alert-backend

gcloud run deploy kk-pokemon-alert-backend \
  --image gcr.io/YOUR_PROJECT_ID/kk-pokemon-alert-backend \
  --platform managed \
  --region us-central1 \
  --allow-unauthenticated

# Start monitoring
curl -X POST https://YOUR-SERVICE-URL.run.app/start-monitoring
```

### Step 4: Android App Setup

```bash
# Open in Android Studio
# File → Open → Select android/ directory

# Place google-services.json in android/app/

# Build and run
# Click Run button or use:
./gradlew assembleDebug

# Install on device
adb install app/build/outputs/apk/debug/app-debug.apk
```

See [android/README.md](android/README.md) for detailed instructions.

## Usage

### Starting the System

1. **Deploy Backend**: Ensure backend is running on Cloud Run
2. **Start Monitoring**: Call `/start-monitoring` endpoint
3. **Install App**: Install Android app on your device
4. **Grant Permissions**: Accept notification permission
5. **Wait for Updates**: First notification arrives within 3 minutes

### Using the App

- **View Status**: Open app to see current queue status
- **Receive Notifications**: Notifications arrive every 3 minutes (if status changes)
- **Snooze Alerts**: Tap "SNOOZE FOR 2 HOURS" to silence notifications
- **Resume Alerts**: Tap "RESUME NOTIFICATIONS" to un-snooze

### Backend API Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/` | GET | Health check and status |
| `/start-monitoring` | POST | Start monitoring loop |
| `/stop-monitoring` | POST | Stop monitoring loop |
| `/status` | GET | Check current website status |
| `/test-notification` | POST | Send test FCM notification |

## Configuration

### Backend Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `FIREBASE_CREDENTIALS_PATH` | Path to Firebase Admin SDK JSON | `./firebase-adminsdk.json` |
| `TARGET_URL` | URL to monitor | `https://pokemoncenter.com/en-ca` |
| `FCM_TOPIC` | FCM topic name | `pokemon_queue_alerts` |

### Android Customization

- **Package Name**: `com.kk.pokemonalert` (in `build.gradle`)
- **FCM Topic**: `pokemon_queue_alerts` (in `MainActivity.kt`)
- **Snooze Duration**: 2 hours (in `MainActivity.kt`)

## Monitoring and Logs

### Backend Logs (Cloud Run)

```bash
# View logs
gcloud run logs read kk-pokemon-alert-backend --limit 50

# Tail logs
gcloud run logs tail kk-pokemon-alert-backend
```

### Android Logs (Logcat)

```bash
# View app logs
adb logcat -s PokemonAlert

# View FCM logs
adb logcat | grep FCM
```

## Troubleshooting

### Backend Issues

**Firebase initialization fails**:
- Verify `firebase-adminsdk.json` exists and is valid
- Check file path in `.env` or environment variable

**Website check fails**:
- Verify internet connectivity
- Check if website is blocking requests
- Try different User-Agent header

### Android Issues

**No notifications received**:
- Check notification permissions in device settings
- Verify `google-services.json` is configured
- Ensure app subscribed to correct topic (check Logcat)
- Confirm backend is running and sending notifications

**App won't build**:
- Sync Gradle: `./gradlew clean build`
- Check Android SDK 34 is installed
- Verify `google-services.json` is in `app/` directory

### Firebase Issues

**Messages not delivered**:
- Check Firebase Console → Cloud Messaging for send logs
- Verify FCM API is enabled
- Confirm topic name matches in backend and app

## Development

### Backend Development

```bash
cd backend

# Install dependencies
pip install -r requirements.txt

# Run with auto-reload
uvicorn main:app --reload --host 0.0.0.0 --port 8080

# Test endpoints
curl http://localhost:8080/status
curl -X POST http://localhost:8080/test-notification
```

### Android Development

```bash
cd android

# Build debug version
./gradlew assembleDebug

# Run tests
./gradlew test

# Run on emulator
./gradlew installDebug
```

## Security Notes

⚠️ **Never commit these files**:
- `firebase-adminsdk.json` (Backend credentials)
- `google-services.json` (Android Firebase config)
- `.env` (Environment variables)
- Service account keys
- API keys

These files are already in `.gitignore`.

### Using Secrets in Production

**Backend (Cloud Run)**:
```bash
# Use Secret Manager
gcloud secrets create firebase-adminsdk --data-file=firebase-adminsdk.json

# Deploy with secret
gcloud run deploy ... \
  --set-secrets=/app/firebase-adminsdk.json=firebase-adminsdk:latest
```

**Android**:
- `google-services.json` is safe to include in production APK
- It only contains public configuration data
- Keep signing keys and API keys secure

## Cost Considerations

### Google Cloud Run

- **Free Tier**: 2 million requests/month
- **Pricing**: Pay per request and CPU time
- **Estimate**: Checking every 3 minutes = ~14,400 requests/month
- **Expected Cost**: Free tier should cover usage

### Firebase Cloud Messaging

- **Free Tier**: Unlimited messages
- **Pricing**: FCM is free for all usage
- **Expected Cost**: $0

## Future Enhancements

- [ ] Add iOS app support
- [ ] Configurable monitoring interval
- [ ] Multiple website monitoring
- [ ] Historical status tracking
- [ ] Webhook support for other notification channels
- [ ] User authentication for backend API
- [ ] Admin dashboard for monitoring status

## Contributing

Contributions are welcome! Please:

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Test thoroughly
5. Submit a pull request

## License

MIT License

Copyright (c) 2024 KK's Pokemon Alert

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

## Support

For issues and questions:
- Open an issue on GitHub
- Check documentation in `backend/README.md` and `android/README.md`

## Acknowledgments

- Pokemon Center for providing the service to monitor
- Firebase for cloud messaging infrastructure
- Google Cloud for hosting platform
- Jetpack Compose for modern Android UI

---

Made with ❤️ for Pokemon fans who don't want to miss out on drops!