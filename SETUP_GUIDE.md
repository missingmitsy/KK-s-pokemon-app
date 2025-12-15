# Setup Guide for KK's Pokemon Alert

This guide will walk you through setting up the complete Pokemon Alert system from scratch.

## Prerequisites Checklist

Before you begin, make sure you have:

- [ ] Google account (for Firebase and Cloud Run)
- [ ] Credit card (for Google Cloud, though free tier should cover usage)
- [ ] Android device or emulator (Android 7.0+ / API 24+)
- [ ] Python 3.11+ installed (for local backend testing)
- [ ] Android Studio installed (for building the app)
- [ ] Git installed

## Step-by-Step Setup

### Part 1: Firebase Setup (15 minutes)

#### 1.1 Create Firebase Project

1. Go to https://console.firebase.google.com/
2. Click "Add project"
3. Enter project name: `kk-pokemon-alert` (or your choice)
4. Enable Google Analytics (optional)
5. Click "Create project"

#### 1.2 Enable Firebase Cloud Messaging

1. In your Firebase project, click the gear icon → "Project settings"
2. Go to "Cloud Messaging" tab
3. Note down:
   - **Sender ID**: `____________`
   - **Server Key**: `____________`

#### 1.3 Generate Backend Credentials

1. Still in "Project settings", go to "Service accounts" tab
2. Click "Generate new private key"
3. Confirm by clicking "Generate key"
4. Save the downloaded JSON file as `firebase-adminsdk.json`
5. **Important**: Keep this file secure! Never share or commit it.

#### 1.4 Add Android App to Firebase

1. In Firebase Console, click "Add app" (at the top)
2. Select Android icon
3. Enter package name: `com.kk.pokemonalert`
4. Enter app nickname: `KK Pokemon Alert` (optional)
5. Click "Register app"
6. Download `google-services.json`
7. Click "Next" through the remaining steps

### Part 2: Backend Setup (20 minutes)

#### 2.1 Prepare Backend Files

```bash
# Clone the repository (if not already done)
git clone https://github.com/YOUR_USERNAME/KK-s-pokemon-app.git
cd KK-s-pokemon-app/backend

# Copy Firebase credentials
cp ~/Downloads/firebase-adminsdk.json .

# Create environment file
cp .env.example .env
```

#### 2.2 Test Backend Locally (Optional)

```bash
# Install dependencies
pip install -r requirements.txt

# Test the backend
python test_backend.py

# Run the server
python main.py

# In another terminal, test endpoints
curl http://localhost:8080/
curl http://localhost:8080/status
```

#### 2.3 Deploy to Google Cloud Run

```bash
# Install Google Cloud SDK if not already installed
# Follow instructions at: https://cloud.google.com/sdk/docs/install

# Authenticate
gcloud auth login

# Create a new project or use existing one
gcloud projects create kk-pokemon-alert-project --name="KK Pokemon Alert"
gcloud config set project kk-pokemon-alert-project

# Enable required APIs
gcloud services enable cloudbuild.googleapis.com
gcloud services enable run.googleapis.com
gcloud services enable containerregistry.googleapis.com

# Build and deploy
gcloud builds submit --tag gcr.io/kk-pokemon-alert-project/backend

gcloud run deploy kk-pokemon-alert-backend \
  --image gcr.io/kk-pokemon-alert-project/backend \
  --platform managed \
  --region us-central1 \
  --allow-unauthenticated \
  --memory 512Mi \
  --cpu 1

# The command will output a service URL, save it:
# Service URL: https://kk-pokemon-alert-backend-XXXXX-uc.a.run.app
```

#### 2.4 Upload Firebase Credentials to Cloud Run

```bash
# Create secret in Secret Manager
gcloud secrets create firebase-adminsdk --data-file=firebase-adminsdk.json

# Redeploy with secret
gcloud run deploy kk-pokemon-alert-backend \
  --image gcr.io/kk-pokemon-alert-project/backend \
  --platform managed \
  --region us-central1 \
  --allow-unauthenticated \
  --set-secrets=/app/firebase-adminsdk.json=firebase-adminsdk:latest \
  --set-env-vars FIREBASE_CREDENTIALS_PATH=/app/firebase-adminsdk.json
```

#### 2.5 Start Monitoring

```bash
# Replace with your actual service URL
SERVICE_URL="https://kk-pokemon-alert-backend-XXXXX-uc.a.run.app"

# Start the monitoring loop
curl -X POST $SERVICE_URL/start-monitoring

# Verify it's running
curl $SERVICE_URL/
```

### Part 3: Android App Setup (30 minutes)

#### 3.1 Prepare Android Project

```bash
cd ../android

# Copy Firebase configuration
cp ~/Downloads/google-services.json app/
```

#### 3.2 Open in Android Studio

1. Launch Android Studio
2. Select "Open an existing project"
3. Navigate to the `android/` directory
4. Click "OK"
5. Wait for Gradle sync to complete (this may take a few minutes)

#### 3.3 Build and Install

**Option A: Using Android Studio**
1. Connect your Android device via USB or start an emulator
2. Enable USB debugging on your device (Settings → Developer options)
3. Click the "Run" button (green play icon)
4. Select your device
5. Wait for build and installation

**Option B: Using Command Line**
```bash
# Build APK
./gradlew assembleDebug

# Install on connected device
adb install app/build/outputs/apk/debug/app-debug.apk
```

#### 3.4 Grant Permissions

1. Open the app on your device
2. When prompted, grant notification permission
3. The app will subscribe to notifications automatically

### Part 4: Testing (10 minutes)

#### 4.1 Test Notification

```bash
# Send a test notification from backend
curl -X POST $SERVICE_URL/test-notification
```

You should see:
- A notification appear on your Android device
- The app screen update with the status

#### 4.2 Verify Monitoring

```bash
# Check backend status
curl $SERVICE_URL/

# Expected response:
# {
#   "service": "KK's Pokemon Alert Backend",
#   "status": "running",
#   "monitoring": true,
#   "last_status": "Queue Up" or "No Queue",
#   "timestamp": "2024-..."
# }
```

#### 4.3 Test Snooze Feature

1. Open the app
2. Tap "SNOOZE FOR 2 HOURS"
3. Screen should turn gray
4. Send a test notification (step 4.1)
5. Verify no notification appears (snoozed)
6. Tap "RESUME NOTIFICATIONS"
7. Screen color should return to red or green

### Part 5: Ongoing Usage

#### Daily Usage

- The app will automatically receive notifications every 3 minutes (when status changes)
- Open the app anytime to see current status
- Use snooze when you don't want to be disturbed

#### Monitoring Backend Status

```bash
# View Cloud Run logs
gcloud run logs read kk-pokemon-alert-backend --limit 50

# Check if monitoring is running
curl $SERVICE_URL/
```

#### Stopping/Restarting Monitoring

```bash
# Stop monitoring
curl -X POST $SERVICE_URL/stop-monitoring

# Start monitoring again
curl -X POST $SERVICE_URL/start-monitoring
```

## Troubleshooting

### Backend Issues

**Problem**: Firebase initialization error
- **Solution**: Verify `firebase-adminsdk.json` is uploaded correctly
- Check Secret Manager in Google Cloud Console
- Redeploy with correct secret configuration

**Problem**: Website checks failing
- **Solution**: Check Cloud Run logs for errors
- Verify internet connectivity from Cloud Run
- Website might be blocking requests (try different User-Agent)

**Problem**: Cloud Run deployment fails
- **Solution**: 
  - Verify billing is enabled for your Google Cloud project
  - Check that all required APIs are enabled
  - Ensure you have correct permissions

### Android Issues

**Problem**: App won't build
- **Solution**:
  - Ensure `google-services.json` is in `app/` directory
  - Sync Gradle: File → Sync Project with Gradle Files
  - Clean and rebuild: Build → Clean Project, then Build → Rebuild Project

**Problem**: Notifications not received
- **Solution**:
  - Check notification permission in device Settings
  - Verify app is subscribed to topic (check Logcat)
  - Ensure backend is running and sending notifications
  - Check Do Not Disturb is off

**Problem**: App crashes on launch
- **Solution**:
  - Check Logcat for error messages
  - Verify `google-services.json` matches package name
  - Reinstall the app

### Firebase Issues

**Problem**: Messages not being delivered
- **Solution**:
  - Check Firebase Console → Cloud Messaging for send logs
  - Verify FCM API is enabled in Cloud Console
  - Confirm topic name matches in both backend and app

## Cost Estimation

### Google Cloud Run
- **Free tier**: 2 million requests/month
- **Expected usage**: ~14,400 requests/month (1 check every 3 min)
- **Cost**: $0 (within free tier)

### Firebase Cloud Messaging
- **Cost**: Free (unlimited messages)

### Total Monthly Cost
- **Expected**: $0 (within free tiers)
- **Note**: May incur minimal charges after free tier exhausted

## Security Best Practices

1. **Never commit credentials**:
   - `firebase-adminsdk.json`
   - `google-services.json` (though less sensitive)
   - API keys in `.env` files

2. **Use Secret Manager**: Store sensitive data in Google Secret Manager, not as environment variables

3. **Restrict API access**: Consider adding authentication to backend API endpoints in production

4. **Monitor usage**: Regularly check Google Cloud Console for unusual activity

5. **Rotate credentials**: Periodically regenerate Firebase credentials

## Next Steps

After successful setup, consider:

- [ ] Customize monitoring interval (currently 3 minutes)
- [ ] Add multiple website monitoring
- [ ] Implement historical status tracking
- [ ] Create a dashboard for status visualization
- [ ] Add webhook support for other notification channels
- [ ] Set up monitoring alerts for backend issues

## Support

If you encounter issues:

1. Check the relevant README files:
   - `backend/README.md` for backend issues
   - `android/README.md` for app issues

2. Review logs:
   - Backend: `gcloud run logs read kk-pokemon-alert-backend`
   - Android: `adb logcat`

3. Verify configuration:
   - Firebase Console for FCM setup
   - Google Cloud Console for Cloud Run status

4. Open an issue on GitHub with:
   - Description of the problem
   - Relevant log outputs
   - Steps to reproduce

## Success Checklist

After completing setup, you should have:

- [ ] Firebase project created and configured
- [ ] Backend deployed and running on Cloud Run
- [ ] Monitoring loop started (check endpoint returns `"monitoring": true`)
- [ ] Android app installed on device
- [ ] Notification permission granted
- [ ] Test notification received successfully
- [ ] App displays current status
- [ ] Snooze feature works correctly

Congratulations! Your Pokemon Alert system is now live! 🎉
