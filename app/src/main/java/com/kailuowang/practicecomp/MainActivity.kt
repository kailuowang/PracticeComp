package com.kailuowang.practicecomp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.kailuowang.practicecomp.ui.theme.PracticeCompTheme
import androidx.lifecycle.viewmodel.compose.viewModel

// Define navigation routes
object AppDestinations {
    const val PRACTICE_LIST = "practiceList"
    const val PRACTICE_SESSION = "practiceSession"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PracticeCompTheme {
                PracticeApp()
            }
        }
    }
}

@Composable
fun PracticeApp(
    navController: NavHostController = rememberNavController(),
    practiceViewModel: PracticeViewModel = viewModel()
) {
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
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticeListScreen(
    onStartPracticeClick: () -> Unit,
    isBackgroundSessionActive: Boolean,
    onResumeSession: () -> Unit,
    viewModel: PracticeViewModel = viewModel()
) {
    val sessions by viewModel.sessions.collectAsState()
    val context = LocalContext.current
    
    // Check if service is running
    val isServiceRunning = remember { mutableStateOf(false) }
    
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
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Practice Diary (v:28efe2a)") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onStartPracticeClick,
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Start new practice session"
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Display banner for active background session
            if (isServiceRunning.value) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
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
                        Text(
                            text = "You have an active practice session running in the background",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium
                        )
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
                        PracticeSessionItem(session = session)
                    }
                }
            }
        }
    }
}

@Composable
fun PracticeSessionItem(session: PracticeSession, modifier: Modifier = Modifier) {
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
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = session.getFormattedDate(),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = session.getFormattedStartTime(),
                    style = MaterialTheme.typography.titleMedium
                )
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
                title = { Text("Practice Session (v:28efe2a)") },
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
            Text(
                text = "Practice Time",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = uiState.formattedPracticeTime,
                style = MaterialTheme.typography.displayMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Display Detection Status only if it's not empty
            if (uiState.detectionStatus.isNotEmpty()) {
                Text(
                    text = "Status: ${uiState.detectionStatus}",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            } else {
                // Add padding to maintain layout consistency
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (isServiceRunning) {
                Text("Monitoring active...", style = MaterialTheme.typography.bodyMedium)
            } else if (!hasPermissions) {
                Text("Waiting for permissions...", style = MaterialTheme.typography.bodyMedium)
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // End Session Button - This stops the service and saves the session
            Button(
                onClick = {
                    if (isServiceRunning) {
                        stopTrackingService(context)
                        isServiceRunning = false
                        
                        // Save the session data ONLY when End Session button is clicked
                        val totalTime = DetectionStateHolder.state.value.totalSessionTimeMillis
                        val practiceTime = DetectionStateHolder.state.value.accumulatedTimeMillis
                        
                        // Only save if we have a meaningful practice session (more than 5 seconds)
                        if (totalTime > 5000) {
                            Log.d("PracticeSessionScreen", "Saving session from End button - Total: $totalTime, Practice: $practiceTime")
                            viewModel.saveSession(totalTimeMillis = totalTime, practiceTimeMillis = practiceTime)
                        } else {
                            Log.d("PracticeSessionScreen", "Session too short to save (<= 5 sec): $totalTime")
                        }
                        
                        // Navigate back after saving
                        onEndSession()
                    }
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

// --- Previews ---

@Preview(showBackground = true)
@Composable
fun PracticeListScreenPreview() {
    PracticeCompTheme {
        PracticeListScreen(onStartPracticeClick = {}, isBackgroundSessionActive = false, onResumeSession = {})
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
        PracticeSessionItem(session = sampleSession)
    }
}

@Preview(showBackground = true, name = "Practice Session")
@Composable
fun PracticeSessionScreenPreview() {
    val previewViewModel: PracticeViewModel = viewModel()
    PracticeCompTheme {
        PracticeSessionScreen(
             viewModel = previewViewModel,
             onEndSession = {}
        )
    }
}