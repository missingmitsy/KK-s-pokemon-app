# System Architecture

## Overview

KK's Pokemon Alert is a distributed monitoring system that tracks the Pokemon Center website for queue status and delivers real-time push notifications to Android devices.

## High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                       Internet                                   │
│                                                                  │
│  ┌───────────────────────────┐                                  │
│  │  pokemoncenter.com/en-ca  │ ◄──────┐                        │
│  └───────────────────────────┘        │                        │
└────────────────────────────────────────┼────────────────────────┘
                                         │
                                    HTTP GET
                                         │
┌────────────────────────────────────────┼────────────────────────┐
│                   Google Cloud Platform│                        │
│                                         │                        │
│  ┌─────────────────────────────────────┼───────────────────┐   │
│  │  Cloud Run (Serverless Container)   │                   │   │
│  │                                      │                   │   │
│  │  ┌──────────────────────────────────▼────────────────┐  │   │
│  │  │  Python Backend (FastAPI)                         │  │   │
│  │  │                                                    │  │   │
│  │  │  Components:                                       │  │   │
│  │  │  • Web Scraper (BeautifulSoup + Requests)        │  │   │
│  │  │  • Keyword Detector (virtual, queue, imperva)    │  │   │
│  │  │  • Monitoring Loop (every 3 minutes)             │  │   │
│  │  │  • REST API (FastAPI)                            │  │   │
│  │  │                                                    │  │   │
│  │  │  Dependencies:                                     │  │   │
│  │  │  • firebase-admin SDK                             │  │   │
│  │  │  • requests, beautifulsoup4                       │  │   │
│  │  └──────────────────────┬─────────────────────────────┘  │   │
│  │                          │                                │   │
│  └──────────────────────────┼────────────────────────────────┘   │
│                             │ FCM Message                        │
│  ┌──────────────────────────┼────────────────────────────────┐   │
│  │  Secret Manager          │                                │   │
│  │  • firebase-adminsdk.json│                                │   │
│  └──────────────────────────┘                                │   │
└────────────────────────────────────────────────────────────────┘
                              │
                              │ FCM Protocol
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Firebase Cloud Messaging                      │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  FCM Backend                                             │   │
│  │  • Topic: pokemon_queue_alerts                           │   │
│  │  • Message Routing                                       │   │
│  │  • Delivery Guarantees                                   │   │
│  └──────────────────┬───────────────────────────────────────┘   │
└────────────────────┼───────────────────────────────────────────┘
                     │
                     │ Push Notification
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Android Device                                │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  KK Pokemon Alert App (Kotlin + Jetpack Compose)         │   │
│  │                                                           │   │
│  │  ┌────────────────────────────────────────────────────┐  │   │
│  │  │  PokemonFirebaseMessagingService                   │  │   │
│  │  │  • Receives FCM messages                           │  │   │
│  │  │  • Checks snooze state                             │  │   │
│  │  │  • Creates notifications                           │  │   │
│  │  │  • Updates stored status                           │  │   │
│  │  └────────────────┬───────────────────────────────────┘  │   │
│  │                   │                                       │   │
│  │  ┌────────────────▼───────────────────────────────────┐  │   │
│  │  │  PreferencesManager (DataStore)                    │  │   │
│  │  │  • Stores queue status                             │  │   │
│  │  │  • Stores snooze timestamp                         │  │   │
│  │  │  • Reactive data flows                             │  │   │
│  │  └────────────────┬───────────────────────────────────┘  │   │
│  │                   │                                       │   │
│  │  ┌────────────────▼───────────────────────────────────┐  │   │
│  │  │  MainActivity (Jetpack Compose UI)                 │  │   │
│  │  │  • Displays status (Red/Green/Gray)                │  │   │
│  │  │  • Snooze button                                   │  │   │
│  │  │  • Subscribes to FCM topic                         │  │   │
│  │  └────────────────────────────────────────────────────┘  │   │
│  └──────────────────────────────────────────────────────────┘   │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  Android System                                          │   │
│  │  • Notification Manager                                  │   │
│  │  • Firebase SDK                                          │   │
│  └──────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

## Component Details

### 1. Backend Service (Python/FastAPI)

**Location**: Google Cloud Run (serverless container)

**Responsibilities**:
- Periodically check Pokemon Center website (every 3 minutes)
- Parse HTML and detect keywords
- Determine queue status
- Send FCM notifications via Firebase Admin SDK
- Expose REST API for control and monitoring

**Key Files**:
- `backend/main.py`: Main application logic
- `backend/Dockerfile`: Container configuration
- `backend/requirements.txt`: Python dependencies

**External Dependencies**:
- Firebase Admin SDK (for FCM)
- Requests (for HTTP)
- BeautifulSoup (for HTML parsing)

### 2. Firebase Cloud Messaging

**Location**: Firebase/Google Cloud

**Responsibilities**:
- Receive messages from backend
- Route to subscribed devices
- Handle offline devices (message queuing)
- Guarantee delivery

**Configuration**:
- Topic-based messaging: `pokemon_queue_alerts`
- All app instances subscribe to this topic
- No device tokens needed (topic handles it)

### 3. Android Application

**Location**: User's Android device

**Components**:

#### a) PokemonFirebaseMessagingService
- Extends `FirebaseMessagingService`
- Receives FCM messages in background
- Checks snooze state before displaying
- Stores status in DataStore
- Creates system notifications

#### b) PreferencesManager
- Uses DataStore for persistent storage
- Stores queue status string
- Stores snooze timestamp (Long)
- Provides reactive Flow-based access

#### c) MainActivity
- Built with Jetpack Compose
- Displays status with color-coded background
- Handles snooze button clicks
- Subscribes to FCM topic on launch
- Requests notification permission

**Key Files**:
- `MainActivity.kt`: UI and logic
- `PokemonFirebaseMessagingService.kt`: FCM handler
- `PreferencesManager.kt`: Data persistence

## Data Flow

### 1. Monitoring Flow

```
┌─────────────────┐
│  Monitoring     │
│  Loop Start     │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  Wait 3 min     │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  Fetch Website  │
│  (HTTP GET)     │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  Parse HTML     │
│  (BeautifulSoup)│
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  Search Keywords│
│  (virtual, etc) │
└────────┬────────┘
         │
         ▼
    ┌────┴────┐
    │ Found?  │
    └─┬────┬──┘
  Yes │    │ No
      │    │
      ▼    ▼
  ┌─────┐ ┌─────┐
  │Queue│ │ No  │
  │ Up  │ │Queue│
  └──┬──┘ └──┬──┘
     │       │
     └───┬───┘
         │
         ▼
┌─────────────────┐
│  Status changed?│
└────────┬────────┘
         │ Yes
         ▼
┌─────────────────┐
│  Send FCM       │
│  Notification   │
└────────┬────────┘
         │
         └──────► Back to Wait
```

### 2. Notification Flow

```
┌─────────────────┐
│  Backend sends  │
│  FCM message    │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  FCM routes to  │
│  topic devices  │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  Device receives│
│  FCM message    │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  FCM Service    │
│  onMessageRcvd  │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  Check snooze   │
│  state          │
└────────┬────────┘
         │
     ┌───┴───┐
     │Snoozed│
     └─┬───┬─┘
   Yes │   │ No
       │   │
       ▼   ▼
    ┌────┐ ┌────────────┐
    │Skip│ │Show Notif. │
    └────┘ └──────┬─────┘
                  │
                  ▼
           ┌──────────────┐
           │Update Status │
           │in DataStore  │
           └──────┬───────┘
                  │
                  ▼
           ┌──────────────┐
           │ UI Auto      │
           │ Updates      │
           └──────────────┘
```

### 3. Snooze Flow

```
┌─────────────────┐
│  User taps      │
│  Snooze button  │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  Calculate time │
│  Now + 2 hours  │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  Save timestamp │
│  to DataStore   │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  UI turns gray  │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  Future FCM     │
│  messages       │
│  silenced       │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  After 2 hours  │
│  auto-resume    │
└─────────────────┘
```

## Security Considerations

### 1. Credentials
- **Backend**: Uses Firebase Admin SDK service account
- **Storage**: Google Secret Manager (encrypted at rest)
- **Access**: IAM roles restrict access
- **Android**: Uses `google-services.json` (public config)

### 2. API Security
- **Backend API**: Currently unauthenticated
- **FCM**: Topic-based (anyone can subscribe)
- **Recommendation**: Add authentication for production

### 3. Data Privacy
- **No PII**: System doesn't collect user data
- **Local Storage**: Only stores status and snooze time
- **No Backend DB**: Stateless backend

## Scalability

### Current Capacity
- **Backend**: Scales automatically with Cloud Run
- **FCM**: Handles millions of devices
- **Cost**: Within free tiers for personal use

### Scaling Considerations
- **Backend**: Increase Cloud Run instances for more users
- **FCM**: No changes needed (auto-scales)
- **Database**: Add if tracking needed

## Monitoring & Observability

### Backend
- **Logs**: Cloud Run logging (stdout)
- **Metrics**: Request count, latency
- **Alerts**: Can set up Cloud Monitoring alerts

### Android
- **Logs**: Logcat (during development)
- **Crash Reporting**: Can add Firebase Crashlytics
- **Analytics**: Can add Firebase Analytics

## Deployment Model

### Development
- **Backend**: Local testing with Python
- **Android**: Android Studio with emulator

### Production
- **Backend**: Google Cloud Run (serverless)
- **Android**: APK distribution or Play Store

## Technology Stack

### Backend
- **Language**: Python 3.11+
- **Framework**: FastAPI
- **Web Scraping**: BeautifulSoup4, Requests
- **Cloud**: Firebase Admin SDK
- **Container**: Docker
- **Platform**: Google Cloud Run

### Android
- **Language**: Kotlin
- **UI**: Jetpack Compose
- **Architecture**: MVVM-like with Compose
- **Storage**: DataStore (Preferences)
- **Push**: Firebase Cloud Messaging
- **Min SDK**: 24 (Android 7.0)

### Infrastructure
- **Hosting**: Google Cloud Run
- **Secrets**: Google Secret Manager
- **Notifications**: Firebase Cloud Messaging
- **Monitoring**: Google Cloud Logging

## Future Enhancements

### Planned
- [ ] iOS app support
- [ ] Historical status tracking
- [ ] Configurable monitoring interval
- [ ] Multiple website support
- [ ] Web dashboard

### Considerations
- [ ] User authentication
- [ ] Database for history
- [ ] Email notifications
- [ ] Webhook support
- [ ] Status API for third parties
