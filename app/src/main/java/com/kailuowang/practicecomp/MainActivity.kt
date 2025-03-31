package com.kailuowang.practicecomp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kailuowang.practicecomp.ui.theme.PracticeCompTheme

// Define navigation routes
object AppDestinations {
    const val PRACTICE_LIST = "practiceList"
    const val INSTRUMENT_SELECTION = "instrumentSelection"
    const val PRACTICE_SESSION = "practiceSession"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PracticeCompTheme {
                PracticeApp() // Main app composable with navigation
            }
        }
    }
}

// Main app composable managing navigation
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
                onStartPracticeClick = { navController.navigate(AppDestinations.INSTRUMENT_SELECTION) }
            )
        }
        composable(route = AppDestinations.INSTRUMENT_SELECTION) {
            InstrumentSelectionScreen(
                viewModel = practiceViewModel,
                onNavigateBack = {
                    practiceViewModel.resetSelectionToDefault()
                    navController.popBackStack()
                 },
                onConfirm = {
                    practiceViewModel.confirmInstrumentSelection()
                    navController.navigate(AppDestinations.PRACTICE_SESSION) {
                        popUpTo(AppDestinations.INSTRUMENT_SELECTION) { inclusive = true }
                    }
                }
            )
        }
        composable(route = AppDestinations.PRACTICE_SESSION) {
            PracticeSessionScreen(
                viewModel = practiceViewModel,
                onNavigateBack = { navController.popBackStack(AppDestinations.PRACTICE_LIST, inclusive = false) }
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

// New screen for selecting the instrument
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstrumentSelectionScreen(
    viewModel: PracticeViewModel,
    onNavigateBack: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Select Instrument") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
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
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                 Text("Choose your instrument for this session:", style = MaterialTheme.typography.titleMedium)
                 Spacer(modifier = Modifier.height(16.dp))
                 uiState.availableInstruments.forEach { instrument ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .selectable(
                                selected = (instrument == uiState.selectedInstrument),
                                onClick = { viewModel.selectInstrument(instrument) },
                                role = Role.RadioButton
                            )
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (instrument == uiState.selectedInstrument),
                            onClick = null
                        )
                        Text(
                            text = instrument,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                }
            }

            Button(
                onClick = onConfirm,
                enabled = uiState.selectedInstrument != null,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Confirm")
            }
        }
    }
}

// Screen for the active practice session
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticeSessionScreen(
    viewModel: PracticeViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Practice Session") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
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
            Text("Instrument: ${uiState.defaultInstrument ?: "Not Selected"}")
            Spacer(modifier = Modifier.height(20.dp))
            Text("Timer and other session details will go here.")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PracticeListScreenPreview() {
    PracticeCompTheme {
        PracticeListScreen(onStartPracticeClick = {})
    }
}

@Preview(showBackground = true, name = "Instrument Selection")
@Composable
fun InstrumentSelectionScreenPreview() {
    PracticeCompTheme {
        val dummyViewModel = PracticeViewModel()
        InstrumentSelectionScreen(
            viewModel = dummyViewModel,
            onNavigateBack = {},
            onConfirm = {}
        )
    }
}

@Preview(showBackground = true, name = "Practice Session")
@Composable
fun PracticeSessionScreenPreview() {
    PracticeCompTheme {
        val dummyViewModel = PracticeViewModel()
        PracticeSessionScreen(
             viewModel = dummyViewModel,
             onNavigateBack = {}
        )
    }
}