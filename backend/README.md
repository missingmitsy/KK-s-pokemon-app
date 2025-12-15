# KK's Pokemon Alert - Backend

Python backend service that monitors pokemoncenter.com for queue status and sends push notifications via Firebase Cloud Messaging.

## Features

- Monitors `pokemoncenter.com/en-ca` every 3 minutes
- Detects keywords: "virtual", "queue", "imperva"
- Sends FCM push notifications based on queue status
- RESTful API endpoints for control and testing
- Designed for deployment to Google Cloud Run

## Prerequisites

1. **Python 3.11+**
2. **Firebase Project** with Cloud Messaging enabled
3. **Firebase Admin SDK credentials** (service account JSON file)

## Firebase Setup

### 1. Create a Firebase Project

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Click "Add project" and follow the setup wizard
3. Enable Google Analytics (optional)

### 2. Enable Firebase Cloud Messaging

1. In your Firebase project, go to **Project Settings** (gear icon)
2. Navigate to the **Cloud Messaging** tab
3. Note down your **Sender ID** and **Server Key** (for Android app)

### 3. Generate Service Account Credentials

1. In Firebase Console, go to **Project Settings** > **Service Accounts**
2. Click **Generate New Private Key**
3. Download the JSON file
4. Rename it to `firebase-adminsdk.json`
5. Place it in the `backend/` directory

⚠️ **IMPORTANT**: Never commit this file to version control! It's already in `.gitignore`.

## Local Development

### 1. Install Dependencies

```bash
cd backend
pip install -r requirements.txt
```

### 2. Configure Environment

```bash
cp .env.example .env
# Edit .env and set your Firebase credentials path
```

### 3. Place Firebase Credentials

Copy your `firebase-adminsdk.json` file to the `backend/` directory.

### 4. Run the Server

```bash
python main.py
```

Or with uvicorn:

```bash
uvicorn main:app --host 0.0.0.0 --port 8080 --reload
```

The API will be available at `http://localhost:8080`

## API Endpoints

### Health Check
```
GET /
```
Returns service status and monitoring state.

### Start Monitoring
```
POST /start-monitoring
```
Starts the background monitoring loop (checks every 3 minutes).

### Stop Monitoring
```
POST /stop-monitoring
```
Stops the background monitoring loop.

### Check Status
```
GET /status
```
Manually checks the website status and returns result.

### Test Notification
```
POST /test-notification
```
Sends a test notification to verify FCM setup.

## Docker Build

Build the Docker image:

```bash
docker build -t kk-pokemon-alert-backend .
```

Run locally with Docker:

```bash
docker run -p 8080:8080 \
  -v $(pwd)/firebase-adminsdk.json:/app/firebase-adminsdk.json \
  -e FIREBASE_CREDENTIALS_PATH=/app/firebase-adminsdk.json \
  kk-pokemon-alert-backend
```

## Google Cloud Run Deployment

### 1. Install Google Cloud SDK

Download from [Google Cloud SDK](https://cloud.google.com/sdk/docs/install)

### 2. Authenticate and Configure

```bash
gcloud auth login
gcloud config set project YOUR_PROJECT_ID
```

### 3. Build and Push to Google Container Registry

```bash
# Enable necessary APIs
gcloud services enable containerregistry.googleapis.com
gcloud services enable run.googleapis.com

# Build and push
gcloud builds submit --tag gcr.io/YOUR_PROJECT_ID/kk-pokemon-alert-backend

# Or use Docker and push manually
docker build -t gcr.io/YOUR_PROJECT_ID/kk-pokemon-alert-backend .
docker push gcr.io/YOUR_PROJECT_ID/kk-pokemon-alert-backend
```

### 4. Deploy to Cloud Run

```bash
gcloud run deploy kk-pokemon-alert-backend \
  --image gcr.io/YOUR_PROJECT_ID/kk-pokemon-alert-backend \
  --platform managed \
  --region us-central1 \
  --allow-unauthenticated \
  --set-env-vars FIREBASE_CREDENTIALS_PATH=/app/firebase-adminsdk.json \
  --memory 512Mi \
  --cpu 1
```

### 5. Upload Firebase Credentials as Secret

For production, use Google Secret Manager:

```bash
# Create secret
gcloud secrets create firebase-adminsdk --data-file=firebase-adminsdk.json

# Deploy with secret
gcloud run deploy kk-pokemon-alert-backend \
  --image gcr.io/YOUR_PROJECT_ID/kk-pokemon-alert-backend \
  --platform managed \
  --region us-central1 \
  --allow-unauthenticated \
  --set-secrets=/app/firebase-adminsdk.json=firebase-adminsdk:latest \
  --set-env-vars FIREBASE_CREDENTIALS_PATH=/app/firebase-adminsdk.json
```

### 6. Start Monitoring

After deployment, call the start-monitoring endpoint:

```bash
curl -X POST https://YOUR-SERVICE-URL.run.app/start-monitoring
```

## Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `FIREBASE_CREDENTIALS_PATH` | Path to Firebase Admin SDK JSON | `./firebase-adminsdk.json` |
| `TARGET_URL` | URL to monitor | `https://pokemoncenter.com/en-ca` |
| `FCM_TOPIC` | FCM topic name for notifications | `pokemon_queue_alerts` |

## How It Works

1. **Monitoring Loop**: Every 3 minutes, the service fetches the Pokemon Center website
2. **Keyword Detection**: Searches page content for "virtual", "queue", "imperva"
3. **Status Determination**:
   - If keywords found → "Queue Up" 🔴
   - If keywords not found → "No Queue" 🟢
4. **FCM Notification**: Sends push notification to all subscribed devices via the `pokemon_queue_alerts` topic
5. **Smart Updates**: Only sends notification when status changes (avoids spam)

## Troubleshooting

### Firebase Initialization Error
- Verify `firebase-adminsdk.json` is in the correct location
- Check file permissions
- Ensure the service account has FCM permissions

### No Notifications Received
- Verify Android app is subscribed to the correct topic
- Check Firebase Console > Cloud Messaging for send logs
- Test with `/test-notification` endpoint

### Website Check Fails
- Check internet connectivity
- Verify TARGET_URL is correct
- The website might be blocking requests (check User-Agent)

## License

MIT License - See root README for details.
