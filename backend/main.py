"""
KK's Pokemon Alert - Backend Service
Monitors pokemoncenter.com for queue status and sends Firebase notifications
"""

import os
import time
import logging
from typing import Dict
from datetime import datetime

import requests
from bs4 import BeautifulSoup
from fastapi import FastAPI, BackgroundTasks
from dotenv import load_dotenv
import firebase_admin
from firebase_admin import credentials, messaging

# Load environment variables
load_dotenv()

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

# Initialize FastAPI app
app = FastAPI(title="KK's Pokemon Alert Backend")

# Global variables
firebase_app = None
last_status = None
monitoring_active = False


def initialize_firebase():
    """Initialize Firebase Admin SDK"""
    global firebase_app
    
    if firebase_app:
        return
    
    creds_path = os.getenv('FIREBASE_CREDENTIALS_PATH', './firebase-adminsdk.json')
    
    if not os.path.exists(creds_path):
        logger.error(f"Firebase credentials file not found at {creds_path}")
        raise FileNotFoundError(f"Firebase credentials file not found at {creds_path}")
    
    try:
        cred = credentials.Certificate(creds_path)
        firebase_app = firebase_admin.initialize_app(cred)
        logger.info("Firebase Admin SDK initialized successfully")
    except Exception as e:
        logger.error(f"Failed to initialize Firebase: {e}")
        raise


def check_website_status() -> Dict[str, any]:
    """
    Check pokemoncenter.com for queue status
    Returns dict with status and keywords found
    """
    target_url = os.getenv('TARGET_URL', 'https://pokemoncenter.com/en-ca')
    keywords = ['virtual', 'queue', 'imperva']
    
    try:
        logger.info(f"Checking {target_url}")
        
        # Make request with timeout and headers
        headers = {
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36'
        }
        response = requests.get(target_url, headers=headers, timeout=30)
        response.raise_for_status()
        
        # Parse HTML content
        soup = BeautifulSoup(response.text, 'html.parser')
        page_text = soup.get_text().lower()
        
        # Check for keywords
        found_keywords = [kw for kw in keywords if kw in page_text]
        
        if found_keywords:
            status = "Queue Up"
            logger.info(f"Queue detected! Keywords found: {found_keywords}")
        else:
            status = "No Queue"
            logger.info("No queue detected")
        
        return {
            'status': status,
            'keywords_found': found_keywords,
            'timestamp': datetime.now().isoformat(),
            'url': target_url
        }
        
    except requests.exceptions.RequestException as e:
        logger.error(f"Error checking website: {e}")
        return {
            'status': 'Error',
            'error': str(e),
            'timestamp': datetime.now().isoformat()
        }


def send_fcm_notification(status: str, keywords: list = None):
    """Send Firebase Cloud Messaging notification to topic"""
    global last_status
    
    # Don't send duplicate notifications
    if status == last_status:
        logger.info(f"Status unchanged ({status}), skipping notification")
        return
    
    try:
        topic = os.getenv('FCM_TOPIC', 'pokemon_queue_alerts')
        
        # Prepare notification message
        if status == "Queue Up":
            title = "🔴 Queue Up!"
            body = "Virtual queue is active on Pokemon Center"
            if keywords:
                body += f" (Keywords: {', '.join(keywords)})"
        elif status == "No Queue":
            title = "🟢 No Queue"
            body = "Pokemon Center is accessible without queue"
        else:
            title = "⚠️ Status Unknown"
            body = "Unable to determine queue status"
        
        # Create FCM message
        message = messaging.Message(
            notification=messaging.Notification(
                title=title,
                body=body,
            ),
            data={
                'status': status,
                'timestamp': datetime.now().isoformat(),
                'keywords': ','.join(keywords) if keywords else ''
            },
            topic=topic,
            android=messaging.AndroidConfig(
                priority='high',
                notification=messaging.AndroidNotification(
                    icon='ic_notification',
                    color='#FF0000' if status == "Queue Up" else '#00FF00',
                    sound='default'
                )
            )
        )
        
        # Send message
        response = messaging.send(message)
        logger.info(f"FCM notification sent successfully: {response}")
        
        # Update last status
        last_status = status
        
    except Exception as e:
        logger.error(f"Failed to send FCM notification: {e}")


def monitor_loop():
    """Background task that monitors the website every 3 minutes"""
    global monitoring_active
    
    logger.info("Starting monitoring loop")
    monitoring_active = True
    
    while monitoring_active:
        try:
            # Check website status
            result = check_website_status()
            
            # Send notification if Firebase is initialized
            if firebase_app and result.get('status') != 'Error':
                send_fcm_notification(
                    result['status'],
                    result.get('keywords_found')
                )
            
            # Wait 3 minutes (180 seconds)
            logger.info("Waiting 3 minutes before next check...")
            time.sleep(180)
            
        except Exception as e:
            logger.error(f"Error in monitoring loop: {e}")
            time.sleep(60)  # Wait 1 minute on error before retrying


@app.on_event("startup")
async def startup_event():
    """Initialize Firebase on startup"""
    try:
        initialize_firebase()
    except Exception as e:
        logger.error(f"Failed to initialize Firebase on startup: {e}")


@app.get("/")
async def root():
    """Health check endpoint"""
    return {
        "service": "KK's Pokemon Alert Backend",
        "status": "running",
        "monitoring": monitoring_active,
        "last_status": last_status,
        "timestamp": datetime.now().isoformat()
    }


@app.post("/start-monitoring")
async def start_monitoring(background_tasks: BackgroundTasks):
    """Start the monitoring loop"""
    global monitoring_active
    
    if monitoring_active:
        return {"message": "Monitoring is already active"}
    
    # Add monitoring task to background
    background_tasks.add_task(monitor_loop)
    
    return {
        "message": "Monitoring started",
        "interval": "3 minutes"
    }


@app.post("/stop-monitoring")
async def stop_monitoring():
    """Stop the monitoring loop"""
    global monitoring_active
    
    if not monitoring_active:
        return {"message": "Monitoring is not active"}
    
    monitoring_active = False
    return {"message": "Monitoring stopped"}


@app.get("/status")
async def get_status():
    """Get current website status"""
    result = check_website_status()
    return result


@app.post("/test-notification")
async def test_notification():
    """Send a test notification"""
    try:
        if not firebase_app:
            initialize_firebase()
        
        send_fcm_notification("Queue Up", ["test"])
        return {"message": "Test notification sent"}
    except Exception as e:
        return {"error": str(e)}


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8080)
