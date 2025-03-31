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
import androidx.compose.foundation.background
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
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
    NavHost(
        navController = navController,
        startDestination = AppDestinations.PRACTICE_LIST,
        modifier = Modifier.fillMaxSize()
    ) {
        composable(route = AppDestinations.PRACTICE_LIST) {
            PracticeListScreen(
                onStartPracticeClick = { navController.navigate(AppDestinations.PRACTICE_SESSION) }
            )
        }
        composable(route = AppDestinations.PRACTICE_SESSION) {
            PracticeSessionScreen(
                viewModel = practiceViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticeListScreen(
    onStartPracticeClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PracticeViewModel = viewModel()
) {
    val sessions by viewModel.sessions.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Practice Diary") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onStartPracticeClick) {
                Icon(Icons.Filled.Add, contentDescription = "Start new practice session")
            }
        }
    ) { innerPadding ->
        if (sessions.isEmpty()) {
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No practice sessions yet. Tap + to start one.")
            }
        } else {
            // Group sessions by date
            val sessionsByDate = sessions.groupBy { it.getFormattedDate() }
            
            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp) // Add padding at the bottom for better scrolling
            ) {
                sessionsByDate.forEach { (date, sessionsForDate) ->
                    // Add date header
                    item(key = "header_$date") {
                        Text(
                            text = date,
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    
                    // Add items for this date
                    items(
                        items = sessionsForDate,
                        key = { it.id } // Use unique ID for better performance and animation
                    ) { session ->
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
    onNavigateBack: () -> Unit,
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
                onNavigateBack()
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
            if (isServiceRunning) {
                Log.d("PracticeSessionScreen", "Stopping service on dispose.")
                stopTrackingService(context)
                isServiceRunning = false
                
                val totalTime = DetectionStateHolder.state.value.totalSessionTimeMillis
                val practiceTime = DetectionStateHolder.state.value.accumulatedTimeMillis
                
                Log.d("PracticeSessionScreen", "Saving session on dispose - Total time: $totalTime ms, Practice time: $practiceTime ms")
                
                // Save the session data when leaving
                viewModel.saveSession(
                    totalTimeMillis = totalTime,
                    practiceTimeMillis = practiceTime
                )
            }
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
                        onNavigateBack()
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

            // Status text section with fixed height
            Box(
                modifier = Modifier
                    .height(MaterialTheme.typography.headlineSmall.lineHeight.value.dp + 16.dp)
                    .padding(bottom = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                if (uiState.detectionStatus.isNotEmpty()) {
                    Text(
                        text = "Status: ${uiState.detectionStatus}",
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
            }

            if (isServiceRunning) {
                Text("Monitoring active...", style = MaterialTheme.typography.bodyMedium)
            } else if (!hasPermissions) {
                Text("Waiting for permissions...", style = MaterialTheme.typography.bodyMedium)
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // End Session Button
            Button(
                onClick = {
                    if (isServiceRunning) {
                        stopTrackingService(context)
                        isServiceRunning = false
                        
                        val totalTime = DetectionStateHolder.state.value.totalSessionTimeMillis
                        val practiceTime = DetectionStateHolder.state.value.accumulatedTimeMillis
                        
                        Log.d("PracticeSessionScreen", "Saving session from button - Total time: $totalTime ms, Practice time: $practiceTime ms")
                        
                        // Save session data when ending session with button
                        viewModel.saveSession(
                            totalTimeMillis = totalTime,
                            practiceTimeMillis = practiceTime
                        )
                        
                        // Navigate back after saving
                        onNavigateBack()
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
        PracticeListScreen(onStartPracticeClick = {})
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
             onNavigateBack = {}
        )
    }
}