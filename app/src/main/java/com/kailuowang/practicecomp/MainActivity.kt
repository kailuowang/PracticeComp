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
    modifier: Modifier = Modifier
) {
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
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Practice sessions will appear here.")
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
    var hasPermissions by remember { mutableStateOf(false) } // Initially assume no permissions

    // Check initial permission status
    LaunchedEffect(Unit) {
        hasPermissions = permissionsToRequest.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            hasPermissions = permissions.values.all { it } // Update based on grant results
            if (hasPermissions) {
                // Permissions granted, try starting service immediately if user intended
                 if (!isServiceRunning) { startTrackingService(context); isServiceRunning = true }
            } else {
                // TODO: Show rationale or message if permissions are denied
                Log.w("PracticeSessionScreen", "Required permissions not granted.")
            }
        }
    )
    // --- End Permission Handling ---

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Practice Session") },
                navigationIcon = {
                    IconButton(onClick = {
                        // Ensure service is stopped if user navigates back while running
                        if (isServiceRunning) {
                           stopTrackingService(context)
                           isServiceRunning = false
                        }
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
            // Display Detection Status
            Text(
                text = "Status: ${uiState.detectionStatus}",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Text("Timer and other session details will go here.")
            Spacer(modifier = Modifier.height(32.dp))

            // Start/Stop Tracking Buttons
            if (isServiceRunning) {
                Button(onClick = {
                    stopTrackingService(context)
                    isServiceRunning = false
                }) {
                    Text("Stop Tracking")
                }
            } else {
                Button(onClick = {
                    // Check permissions before starting
                    if (hasPermissions) {
                        startTrackingService(context)
                        isServiceRunning = true
                    } else {
                        permissionLauncher.launch(permissionsToRequest)
                    }
                }) {
                    Text("Start Tracking")
                }
            }
            // TODO: Display tracking status/detected time
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

@Preview(showBackground = true, name = "Practice Session")
@Composable
fun PracticeSessionScreenPreview() {
    PracticeCompTheme {
        PracticeSessionScreen(
             viewModel = viewModel(),
             onNavigateBack = {}
        )
    }
}