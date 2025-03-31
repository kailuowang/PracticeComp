package com.kailuowang.practicecomp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.kailuowang.practicecomp.ui.theme.PracticeCompTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PracticeCompTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    PracticeListScreen()
                }
            }
        }
    }
}

// Define the new screen composable
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticeListScreen(modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Practice Diary") }, // TODO: Replace with string resource
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { /* TODO: Handle start practice session click */ }) {
                Icon(Icons.Filled.Add, contentDescription = "Start new practice session") // TODO: Replace with string resource
            }
        }
    ) { innerPadding ->
        // Placeholder for the list of practice sessions
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Practice sessions will appear here.") // TODO: Replace with actual list
        }
    }
}

// Add a Preview for PracticeListScreen
@Preview(showBackground = true)
@Composable
fun PracticeListScreenPreview() {
    PracticeCompTheme {
        PracticeListScreen()
    }
}

// Remove or comment out the old BakingScreen if it's no longer needed
// @Composable
// fun BakingScreen(modifier: Modifier = Modifier) { ... }
// @Preview(...)
// fun BakingScreenPreview() { ... }