package com.kk.pokemonalert

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            subscribeToTopic()
        }
    }
    
    companion object {
        private const val TAG = "MainActivity"
        private const val SNOOZE_DURATION_HOURS = 2
        private const val SNOOZE_DURATION_MILLIS = SNOOZE_DURATION_HOURS * 60 * 60 * 1000L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Request notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                subscribeToTopic()
            }
        } else {
            subscribeToTopic()
        }
        
        setContent {
            PokemonAlertTheme {
                MainScreen()
            }
        }
    }
    
    private fun subscribeToTopic() {
        FirebaseMessaging.getInstance().subscribeToTopic("pokemon_queue_alerts")
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Subscribed to pokemon_queue_alerts topic")
                }
            }
    }
}

@Composable
fun PokemonAlertTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(),
        content = content
    )
}

@Composable
fun MainScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val preferencesManager = remember { PreferencesManager(context) }
    
    var queueStatus by remember { mutableStateOf<String?>(null) }
    var snoozeUntil by remember { mutableStateOf<Long?>(null) }
    var isSnoozed by remember { mutableStateOf(false) }
    
    // Load initial state
    LaunchedEffect(Unit) {
        queueStatus = preferencesManager.getQueueStatus().first()
        snoozeUntil = preferencesManager.getSnoozeUntil().first()
        isSnoozed = snoozeUntil?.let { it > System.currentTimeMillis() } ?: false
    }
    
    // Update snooze state
    LaunchedEffect(snoozeUntil) {
        isSnoozed = snoozeUntil?.let { it > System.currentTimeMillis() } ?: false
    }
    
    val backgroundColor = when {
        isSnoozed -> Color(0xFF808080) // Gray when snoozed
        queueStatus == "Queue Up" -> Color(0xFFFF0000) // Red for Queue Up
        queueStatus == "No Queue" -> Color(0xFF00FF00) // Green for No Queue
        else -> Color(0xFF333333) // Dark gray for unknown
    }
    
    val textColor = Color.White
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            // Status text
            Text(
                text = when {
                    isSnoozed -> "SNOOZED"
                    queueStatus == "Queue Up" -> "🔴 QUEUE UP"
                    queueStatus == "No Queue" -> "🟢 NO QUEUE"
                    else -> "CHECKING..."
                },
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Additional info
            if (isSnoozed && snoozeUntil != null) {
                val dateFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
                val snoozeTime = dateFormat.format(Date(snoozeUntil!!))
                Text(
                    text = "Notifications snoozed until $snoozeTime",
                    fontSize = 16.sp,
                    color = textColor.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Snooze button
            val scope = rememberCoroutineScope()
            Button(
                onClick = {
                    if (isSnoozed) {
                        // Un-snooze
                        scope.launch {
                            preferencesManager.setSnoozeUntil(0L)
                            snoozeUntil = 0L
                            isSnoozed = false
                        }
                    } else {
                        // Snooze for configured duration
                        val snoozeTime = System.currentTimeMillis() + SNOOZE_DURATION_MILLIS
                        scope.launch {
                            preferencesManager.setSnoozeUntil(snoozeTime)
                            snoozeUntil = snoozeTime
                            isSnoozed = true
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSnoozed) Color(0xFF4CAF50) else Color(0xFF2196F3)
                ),
                modifier = Modifier
                    .padding(16.dp)
                    .height(56.dp)
            ) {
                Text(
                    text = if (isSnoozed) "RESUME NOTIFICATIONS" else "SNOOZE FOR $SNOOZE_DURATION_HOURS HOURS",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Info text
            Text(
                text = "Updates every 3 minutes",
                fontSize = 14.sp,
                color = textColor.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        }
    }
}
