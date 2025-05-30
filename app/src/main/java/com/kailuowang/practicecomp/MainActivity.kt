package com.kailuowang.practicecomp

import android.Manifest
import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.kailuowang.practicecomp.ui.theme.PracticeCompTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import android.content.Context
import android.content.pm.PackageInfo

// Define navigation routes
object AppDestinations {
    const val PRACTICE_LIST = "practiceList"
    const val PRACTICE_SESSION = "practiceSession"
    const val PRACTICE_CALENDAR = "practiceCalendar"
    const val SETTINGS = "settings"
}

class MainActivity : ComponentActivity() {
    private val TAG = "MainActivity"
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate called, intent: ${intent?.extras?.keySet()?.joinToString()}")
        if (intent?.hasExtra("navigate_to") == true) {
            Log.d(TAG, "onCreate has navigate_to: ${intent.getStringExtra("navigate_to")}")
        }
        
        setContent {
            PracticeCompTheme {
                PracticeApp(intent = intent)
            }
        }
    }
    
    override fun onNewIntent(intent: Intent) {
        Log.d(TAG, "onNewIntent called, intent: ${intent?.extras?.keySet()?.joinToString()}")
        if (intent.hasExtra("navigate_to")) {
            Log.d(TAG, "onNewIntent has navigate_to: ${intent.getStringExtra("navigate_to")}")
        }
        
        super.onNewIntent(intent)
        setIntent(intent)
        // The activity will be recomposed, and the new intent will be passed to PracticeApp
    }
}

@Composable
fun PracticeApp(
    navController: NavHostController = rememberNavController(),
    intent: Intent? = null
) {
    // Get ViewModel from container
    val practiceViewModel = PracticeAppContainer.provideViewModel(LocalContext.current.applicationContext as Application)
    
    // Simple session state to track
    val isReturningFromSession = remember { mutableStateOf(false) }
    val isBackgroundSessionActive = remember { mutableStateOf(false) }
    
    // Track current route to detect navigation events
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    
    // Effect to refresh sessions when navigating to list screen after a session
    LaunchedEffect(currentRoute) {
        if (currentRoute == AppDestinations.PRACTICE_LIST && isReturningFromSession.value) {
            Log.d("PracticeApp", "Returning to list screen from session, refreshing")
            practiceViewModel.refreshSessions()
            isReturningFromSession.value = false
        }
    }
    
    // Handle navigation from notification
    LaunchedEffect(intent) {
        Log.d("PracticeApp", "LaunchedEffect(intent) called, intent extras: ${intent?.extras?.keySet()?.joinToString()}")
        intent?.getStringExtra("navigate_to")?.let { destination ->
            Log.d("PracticeApp", "Found navigate_to extra: $destination, currentRoute: $currentRoute")
            if (destination == AppDestinations.PRACTICE_SESSION && 
                currentRoute != AppDestinations.PRACTICE_SESSION) {
                Log.d("PracticeApp", "Navigating to $destination")
                navController.navigate(destination) {
                    // Pop up to the start destination to avoid building up a large stack
                    popUpTo(AppDestinations.PRACTICE_LIST) { saveState = true }
                    // Avoid multiple copies of the same destination on the back stack
                    launchSingleTop = true
                }
            } else {
                Log.d("PracticeApp", "Not navigating - already at destination or unknown destination")
            }
        }
    }
    
    NavHost(
        navController = navController,
        startDestination = AppDestinations.PRACTICE_LIST,
        modifier = Modifier.fillMaxSize()
    ) {
        composable(route = AppDestinations.PRACTICE_LIST) {
            PracticeListScreen(
                onStartPracticeClick = { 
                    navController.navigate(AppDestinations.PRACTICE_SESSION) 
                },
                isBackgroundSessionActive = isBackgroundSessionActive.value,
                onResumeSession = {
                    navController.navigate(AppDestinations.PRACTICE_SESSION)
                },
                onCalendarClick = {
                    navController.navigate(AppDestinations.PRACTICE_CALENDAR)
                },
                onSettingsClick = {
                    navController.navigate(AppDestinations.SETTINGS)
                }
            )
        }
        composable(route = AppDestinations.PRACTICE_SESSION) {
            PracticeSessionScreen(
                viewModel = practiceViewModel,
                onEndSession = {
                    // Mark that we're returning from a session
                    isReturningFromSession.value = true
                    
                    // Reset state and navigate back, but don't stop service here
                    // Service will only be stopped when End Session button is pressed
                    navController.popBackStack()
                }
            )
        }
        composable(route = AppDestinations.PRACTICE_CALENDAR) {
            PracticeCalendarScreen(
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
        composable(route = AppDestinations.SETTINGS) {
            SettingsScreen(
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
    }
}

@Composable
fun FlashingDot(modifier: Modifier = Modifier) {
    // Create an infinite transition for the pulsating animation
    val infiniteTransition = rememberInfiniteTransition(label = "flashing")
    
    // Animate the alpha (opacity) between 0.3 and 1.0
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    
    // The dot
    Box(
        modifier = modifier
            .size(16.dp)
            .background(
                color = MaterialTheme.colorScheme.error.copy(alpha = alpha),
                shape = CircleShape
            )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticeListScreen(
    onStartPracticeClick: () -> Unit,
    isBackgroundSessionActive: Boolean,
    onResumeSession: () -> Unit,
    onCalendarClick: () -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: PracticeViewModel = viewModel()
) {
    val sessions by viewModel.sessions.collectAsState()
    val context = LocalContext.current
    
    // Check if service is running
    val isServiceRunning = remember { mutableStateOf(false) }
    
    // State for showing version info dialog
    var showVersionInfo by remember { mutableStateOf(false) }
    
    // Check if the service is running
    LaunchedEffect(Unit) {
        isServiceRunning.value = PracticeTrackingService.isServiceRunning
        Log.d("PracticeListScreen", "Service running check: ${isServiceRunning.value}")
    }
    
    // Always refresh sessions when this screen appears
    LaunchedEffect(Unit) {
        Log.d("PracticeListScreen", "Screen appeared, refreshing sessions")
        viewModel.refreshSessions()
    }
    
    // Display version info dialog when needed
    if (showVersionInfo) {
        VersionInfoDialog(
            onDismiss = { showVersionInfo = false }
        )
    }
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Practice Diary") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                navigationIcon = {
                    // Info icon in the top left corner
                    IconButton(onClick = { showVersionInfo = true }) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "App Information"
                        )
                    }
                },
                actions = {
                    // Settings icon
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Default.Settings, 
                            contentDescription = "Settings"
                        )
                    }
                    // Calendar icon
                    IconButton(onClick = onCalendarClick) {
                        Icon(
                            imageVector = Icons.Default.DateRange, 
                            contentDescription = "View Calendar"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = if (isServiceRunning.value) onResumeSession else onStartPracticeClick,
                containerColor = if (isServiceRunning.value) 
                    MaterialTheme.colorScheme.errorContainer 
                else 
                    MaterialTheme.colorScheme.primaryContainer
            ) {
                if (isServiceRunning.value) {
                    // Show flashing dot for ongoing session
                    Box(
                        modifier = Modifier
                            .size(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        FlashingDot(modifier = Modifier.size(24.dp))
                    }
                } else {
                    // Show add icon to start new session
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Start new practice session"
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Display notification banner if service is running in background
            if (isServiceRunning.value) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Practice session active",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Session recording in background",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Button(
                            onClick = onResumeSession,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text("Resume")
                        }
                    }
                }
            }
            
            if (sessions.isEmpty() && !isServiceRunning.value) {
                // Display placeholder when no sessions exist and no active session
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No practice sessions yet. Tap + to start one.",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                // Display session list when sessions exist
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    items(sessions) { session ->
                        PracticeSessionItem(
                            session = session,
                            onDelete = { viewModel.deleteSession(session.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PracticeSessionItem(
    session: PracticeSession, 
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = session.getFormattedDate(),
                    style = MaterialTheme.typography.titleMedium
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = session.getFormattedStartTime(),
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    IconButton(onClick = { showDeleteConfirmation = true }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete session",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Total: ${session.getFormattedTotalTime()}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Practice: ${session.getFormattedPracticeTime()}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                progress = { session.getPracticePercentage() / 100f }
            )
            
            Text(
                text = "${session.getPracticePercentage()}% of session spent practicing",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
    
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete Practice Session") },
            text = { Text("Are you sure you want to delete this practice session? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteConfirmation = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticeSessionScreen(
    viewModel: PracticeViewModel,
    onEndSession: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isServiceRunning by remember { mutableStateOf(false) }
    val uiState by viewModel.uiState.collectAsState()

    // --- Permission Handling ---
    val permissionsToRequest = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.POST_NOTIFICATIONS)
        } else {
            arrayOf(Manifest.permission.RECORD_AUDIO)
        }
    }
    var hasPermissions by remember { mutableStateOf(false) }

    // Permission Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            val allGranted = permissions.values.all { it }
            hasPermissions = allGranted
            if (allGranted) {
                Log.d("PracticeSessionScreen", "Permissions granted by user.")
                if (!isServiceRunning) {
                   startTrackingService(context)
                   isServiceRunning = true
                }
            } else {
                Log.w("PracticeSessionScreen", "Required permissions were denied.")
                onEndSession()
            }
        }
    )

    // Effect to check permissions and start service on initial composition or when permissions change
    LaunchedEffect(key1 = hasPermissions) {
        val allCurrentlyGranted = permissionsToRequest.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        hasPermissions = allCurrentlyGranted

        if (allCurrentlyGranted) {
            Log.d("PracticeSessionScreen", "Permissions granted.")
            if (!isServiceRunning) {
                startTrackingService(context)
                isServiceRunning = true
            }
        } else {
            if (!hasPermissions) {
                 Log.d("PracticeSessionScreen", "Requesting permissions...")
                 permissionLauncher.launch(permissionsToRequest)
            }
        }
    }

    // Effect to stop the service when the composable is disposed (leaves the screen)
    DisposableEffect(Unit) {
        onDispose {
            Log.d("PracticeSessionScreen", "Disposing PracticeSessionScreen.")
            // Don't stop the service when screen is disposed - this allows background operation
            Log.d("PracticeSessionScreen", "Keeping service running in background")
            
            // No saving on dispose - only save when End Session button is clicked
            Log.d("PracticeSessionScreen", "NOT saving session on dispose - save only happens on End Session button")
        }
    }
    // --- End Permission and Lifecycle Handling ---

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Practice Session") },
                navigationIcon = {
                    IconButton(onClick = {
                        // Keep service running when navigating back
                        Log.d("PracticeSessionScreen", "Navigating back but keeping service running")
                        onEndSession()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Display Session Duration
            Text(
                text = "Session Time",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = uiState.formattedTotalSessionTime,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            
            // Display Accumulated Practice Time
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 4.dp)
            ) {
                Text(
                    text = "Practice Time",
                    style = MaterialTheme.typography.labelLarge
                )
                
                // Red dot indicator that shows when practicing
                if (uiState.detectionStatus == "Practicing") {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(
                                color = MaterialTheme.colorScheme.error,
                                shape = androidx.compose.foundation.shape.CircleShape
                            )
                    )
                }
            }
            Text(
                text = uiState.formattedPracticeTime,
                style = MaterialTheme.typography.displayMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Remove the text status display and replace with spacer
            Spacer(modifier = Modifier.height(16.dp))

            if (isServiceRunning) {
                Text("Monitoring active...", style = MaterialTheme.typography.bodyMedium)
            } else if (!hasPermissions) {
                Text("Waiting for permissions...", style = MaterialTheme.typography.bodyMedium)
            }
            
            // Add Goal Setting Section
            Spacer(modifier = Modifier.height(24.dp))
            
            // Session Goal Setting
            val currentGoalMinutes = uiState.goalMinutes
            var goalMinutesText by remember { mutableStateOf(if (currentGoalMinutes > 0) currentGoalMinutes.toString() else "") }
            var showGoalInput by remember { mutableStateOf(false) }
            var isGoalReached = uiState.goalReached
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isGoalReached) 
                        MaterialTheme.colorScheme.tertiaryContainer 
                    else 
                        MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Practice Goal",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isGoalReached) 
                            MaterialTheme.colorScheme.onTertiaryContainer 
                        else 
                            MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    if (showGoalInput) {
                        // Goal input field
                        OutlinedTextField(
                            value = goalMinutesText,
                            onValueChange = { 
                                // Only allow numeric input
                                if (it.isEmpty() || it.all { char -> char.isDigit() }) {
                                    goalMinutesText = it
                                }
                            },
                            label = { Text("Minutes") },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number
                            ),
                            singleLine = true,
                            modifier = Modifier.width(150.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Cancel button
                            OutlinedButton(
                                onClick = { 
                                    showGoalInput = false
                                    goalMinutesText = if (currentGoalMinutes > 0) 
                                        currentGoalMinutes.toString() else ""
                                }
                            ) {
                                Text("Cancel")
                            }
                            
                            // Save button
                            Button(
                                onClick = { 
                                    val minutes = goalMinutesText.toIntOrNull() ?: 0
                                    viewModel.updateGoalMinutes(minutes)
                                    showGoalInput = false
                                }
                            ) {
                                Text("Save")
                            }
                        }
                    } else {
                        // Goal display
                        if (currentGoalMinutes > 0) {
                            Text(
                                text = if (isGoalReached) 
                                    "Goal Reached: ${currentGoalMinutes} minutes" 
                                else 
                                    "Goal: ${currentGoalMinutes} minutes",
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (isGoalReached) 
                                    MaterialTheme.colorScheme.onTertiaryContainer 
                                else 
                                    MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        } else {
                            Text(
                                text = "No goal set",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Button(onClick = { showGoalInput = true }) {
                            Text(if (currentGoalMinutes > 0) "Change Goal" else "Set Goal")
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // End Session Button - This stops the service and saves the session
            Button(
                onClick = {
                    // Stop the service. Saving logic is now handled within the service itself.
                    stopTrackingService(context)
                    isServiceRunning = false // Update UI state immediately
                    
                    // Navigate back
                    onEndSession()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier.width(200.dp)
            ) {
                Text("End Session")
            }
        }
    }
}

// Helper functions to start/stop service
private fun startTrackingService(context: android.content.Context) {
    val intent = Intent(context, PracticeTrackingService::class.java)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(intent)
    } else {
        context.startService(intent)
    }
}

private fun stopTrackingService(context: android.content.Context) {
    val intent = Intent(context, PracticeTrackingService::class.java)
    context.stopService(intent)
}

// Helper function moved outside the Composable
private fun getPackageInfo(context: Context): PackageInfo? {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0)
        }
    } catch (e: Exception) {
        // Log error or handle appropriately
        null
    }
}

@Composable
fun VersionInfoDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    // Now calls the correctly scoped getPackageInfo function
    val packageInfo = remember { getPackageInfo(context) } 

    val versionName = packageInfo?.versionName ?: "Unknown"
    val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        packageInfo?.longVersionCode?.toString() ?: "Unknown"
    } else {
        @Suppress("DEPRECATION")
        packageInfo?.versionCode?.toString() ?: "Unknown"
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Practice Companion",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column {
                Text("Version: $versionName")
                Text("Build: $versionCode")
                Spacer(modifier = Modifier.height(8.dp))
                Text("© 2024 Kailuo Wang")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // State for settings values
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }
    
    // State for individual settings
    var gracePeriodSeconds by remember { 
        mutableStateOf((settingsManager.getGracePeriodMs() / 1000).toString()) 
    }
    var autoEndMinutes by remember { 
        mutableStateOf((settingsManager.getAutoEndThresholdMs() / (60 * 1000)).toString()) 
    }
    
    // Validation state
    var gracePeriodError by remember { mutableStateOf(false) }
    var autoEndError by remember { mutableStateOf(false) }
    
    // Coroutine scope for saving
    val scope = rememberCoroutineScope()
    
    // Function to validate and save settings
    fun saveSettings() {
        // Validate grace period (1-60 seconds)
        val gracePeriodValue = gracePeriodSeconds.toIntOrNull() ?: 0
        gracePeriodError = gracePeriodValue < 1 || gracePeriodValue > 60
        
        // Validate auto-end threshold (1-120 minutes)
        val autoEndValue = autoEndMinutes.toIntOrNull() ?: 0
        autoEndError = autoEndValue < 1 || autoEndValue > 120
        
        // If no errors, save settings
        if (!gracePeriodError && !autoEndError) {
            scope.launch {
                settingsManager.setGracePeriodMs(gracePeriodValue * 1000L)
                settingsManager.setAutoEndThresholdMs(autoEndValue * 60 * 1000L)
            }
        }
    }
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Title
            Text(
                text = "Practice Detection Settings",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // Grace Period Setting
            Column {
                Text(
                    text = "Grace Period",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "How long to wait before considering a practice stopped (in seconds)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = gracePeriodSeconds,
                    onValueChange = { 
                        // Only allow digits
                        if (it.isEmpty() || it.all { c -> c.isDigit() }) {
                            gracePeriodSeconds = it
                            gracePeriodError = false
                        }
                    },
                    label = { Text("Seconds") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = gracePeriodError,
                    supportingText = if (gracePeriodError) {
                        { Text("Must be between 1-60 seconds") }
                    } else null,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            // Auto-End Threshold Setting
            Column {
                Text(
                    text = "Auto-End Threshold",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "How long of silence before automatically ending the session (in minutes)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = autoEndMinutes,
                    onValueChange = { 
                        // Only allow digits
                        if (it.isEmpty() || it.all { c -> c.isDigit() }) {
                            autoEndMinutes = it
                            autoEndError = false
                        }
                    },
                    label = { Text("Minutes") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = autoEndError,
                    supportingText = if (autoEndError) {
                        { Text("Must be between 1-120 minutes") }
                    } else null,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            // Save Button
            Button(
                onClick = { saveSettings() },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Save Settings")
            }
            
            // Info Card
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "About These Settings",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Grace Period: When the app detects that you've stopped playing, it will wait this many seconds before stopping the timer. This prevents the timer from stopping during short pauses.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Auto-End Threshold: If no playing is detected for this amount of time, the session will automatically end to save battery.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

// --- Previews ---

@Preview(showBackground = true, name = "List Screen - No Active Session")
@Composable
fun PracticeListScreenPreview() {
    PracticeCompTheme {
        PracticeListScreen(
            onStartPracticeClick = {}, 
            isBackgroundSessionActive = false, 
            onResumeSession = {}, 
            onCalendarClick = {},
            onSettingsClick = {}
        )
    }
}

@Preview(showBackground = true, name = "List Screen - With Active Session")
@Composable
fun PracticeListScreenWithActiveSessionPreview() {
    PracticeCompTheme {
        PracticeListScreen(
            onStartPracticeClick = {}, 
            isBackgroundSessionActive = true, 
            onResumeSession = {}, 
            onCalendarClick = {},
            onSettingsClick = {}
        )
    }
}

@Preview(showBackground = true, name = "Session Item")
@Composable
fun PracticeSessionItemPreview() {
    val sampleSession = PracticeSession(
        totalTimeMillis = 3600000, // 1 hour
        practiceTimeMillis = 2400000 // 40 minutes
    )
    
    PracticeCompTheme {
        PracticeSessionItem(
            session = sampleSession,
            onDelete = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PracticeSessionScreenPreview() {
    val context = LocalContext.current
    val previewViewModel = PracticeAppContainer.provideViewModel(context.applicationContext as Application)
    
    PracticeCompTheme {
        PracticeSessionScreen(
             viewModel = previewViewModel,
             onEndSession = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    PracticeCompTheme {
        SettingsScreen(
            onBackClick = {}
        )
    }
}