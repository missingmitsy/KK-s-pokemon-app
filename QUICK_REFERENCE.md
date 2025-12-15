# Quick Reference Card

## Backend API Endpoints

| Endpoint | Method | Description | Example |
|----------|--------|-------------|---------|
| `/` | GET | Health check | `curl $URL/` |
| `/start-monitoring` | POST | Start monitoring loop | `curl -X POST $URL/start-monitoring` |
| `/stop-monitoring` | POST | Stop monitoring loop | `curl -X POST $URL/stop-monitoring` |
| `/status` | GET | Check current status | `curl $URL/status` |
| `/test-notification` | POST | Send test notification | `curl -X POST $URL/test-notification` |

## Common Commands

### Backend Deployment
```bash
# Build and deploy
gcloud builds submit --tag gcr.io/PROJECT_ID/backend
gcloud run deploy kk-pokemon-alert-backend \
  --image gcr.io/PROJECT_ID/backend \
  --platform managed --region us-central1

# View logs
gcloud run logs read kk-pokemon-alert-backend --limit 50
gcloud run logs tail kk-pokemon-alert-backend
```

### Android Development
```bash
# Build debug APK
./gradlew assembleDebug

# Install on device
adb install app/build/outputs/apk/debug/app-debug.apk

# View logs
adb logcat | grep PokemonAlert
adb logcat | grep FCM
```

### Firebase Management
```bash
# Create secret (backend credentials)
gcloud secrets create firebase-adminsdk --data-file=firebase-adminsdk.json

# Update secret
gcloud secrets versions add firebase-adminsdk --data-file=firebase-adminsdk.json
```

## File Locations

### Backend
- Main code: `backend/main.py`
- Dependencies: `backend/requirements.txt`
- Config: `backend/.env`
- Docker: `backend/Dockerfile`
- Credentials: `backend/firebase-adminsdk.json` (not in repo)

### Android
- Main Activity: `android/app/src/main/java/com/kk/pokemonalert/MainActivity.kt`
- FCM Service: `android/app/src/main/java/com/kk/pokemonalert/PokemonFirebaseMessagingService.kt`
- Preferences: `android/app/src/main/java/com/kk/pokemonalert/PreferencesManager.kt`
- Firebase Config: `android/app/google-services.json` (not in repo)

## Environment Variables

### Backend (.env)
```bash
FIREBASE_CREDENTIALS_PATH=./firebase-adminsdk.json
TARGET_URL=https://pokemoncenter.com/en-ca
FCM_TOPIC=pokemon_queue_alerts
```

## Configuration Values

| Setting | Value | Location |
|---------|-------|----------|
| Package Name | `com.kk.pokemonalert` | `android/app/build.gradle` |
| FCM Topic | `pokemon_queue_alerts` | Backend & Android |
| Check Interval | 3 minutes (180 seconds) | `backend/main.py` |
| Snooze Duration | 2 hours (7200 seconds) | `android/.../MainActivity.kt` |
| Target URL | `pokemoncenter.com/en-ca` | `backend/.env` |
| Keywords | virtual, queue, imperva | `backend/main.py` |

## Status Codes

### App Display
- 🔴 **Red**: Queue Up (queue detected)
- 🟢 **Green**: No Queue (no queue)
- 🟡 **Gray**: Snoozed (notifications disabled)
- ⚫ **Dark Gray**: Unknown (waiting for first update)

### Backend Status
- `"Queue Up"`: Keywords found on website
- `"No Queue"`: Keywords not found
- `"Error"`: Website check failed

## Monitoring Schedule

| Event | Frequency | Description |
|-------|-----------|-------------|
| Website Check | Every 3 minutes | Backend scrapes Pokemon Center |
| FCM Notification | On status change | Only when status changes |
| App Update | Real-time | When notification received |

## Resource Requirements

### Backend (Cloud Run)
- **Memory**: 512 MB
- **CPU**: 1
- **Region**: us-central1 (or your choice)
- **Concurrency**: 1 (default)

### Android App
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 34 (Android 14)
- **Compile SDK**: 34

## Important URLs

### Development
- Firebase Console: https://console.firebase.google.com/
- Google Cloud Console: https://console.cloud.google.com/
- Cloud Run: https://console.cloud.google.com/run
- Secret Manager: https://console.cloud.google.com/security/secret-manager

### Documentation
- Firebase Cloud Messaging: https://firebase.google.com/docs/cloud-messaging
- Cloud Run Docs: https://cloud.google.com/run/docs
- Jetpack Compose: https://developer.android.com/jetpack/compose

## Permissions Required

### Android App
- `INTERNET`: Network access for Firebase
- `POST_NOTIFICATIONS`: Display notifications (Android 13+)

### Google Cloud
- Cloud Run Admin
- Secret Manager Admin
- Cloud Build Editor
- Container Registry Writer

## Troubleshooting Quick Fixes

| Problem | Quick Fix |
|---------|-----------|
| No notifications | Check: Permissions, Firebase config, Backend running |
| Backend not responding | Check: Cloud Run logs, Billing enabled |
| App won't build | Check: google-services.json, Gradle sync |
| Firebase init fails | Check: Credentials file, Secret Manager |
| High costs | Check: Request logs, Monitoring frequency |

## Testing Checklist

- [ ] Backend imports successfully
- [ ] Backend starts without errors
- [ ] Website check returns result
- [ ] Firebase initializes correctly
- [ ] FCM test notification works
- [ ] Android app installs
- [ ] Notification permission granted
- [ ] App receives notifications
- [ ] Status updates in real-time
- [ ] Snooze button works
- [ ] Un-snooze resumes notifications

## Maintenance Tasks

### Weekly
- Check Cloud Run logs for errors
- Verify monitoring is active
- Test notification delivery

### Monthly
- Review Google Cloud billing
- Check Firebase usage statistics
- Update dependencies if needed

### As Needed
- Rotate Firebase credentials
- Update Android app version
- Adjust monitoring interval
- Add new keywords to detect

## Support Resources

- **Backend README**: `backend/README.md`
- **Android README**: `android/README.md`
- **Setup Guide**: `SETUP_GUIDE.md`
- **Main README**: `README.md`

## Quick Start Commands

```bash
# Complete setup in 3 commands (after Firebase config)
cd backend
gcloud builds submit --tag gcr.io/PROJECT_ID/backend && \
gcloud run deploy kk-pokemon-alert-backend --image gcr.io/PROJECT_ID/backend \
  --platform managed --allow-unauthenticated && \
curl -X POST $(gcloud run services describe kk-pokemon-alert-backend \
  --region us-central1 --format 'value(status.url)')/start-monitoring
```

---

**Tip**: Bookmark this page for quick access to common commands and configurations!
