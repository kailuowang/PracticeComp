package com.kailuowang.practicecomp

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsManagementScreen(
    onBackClick: () -> Unit,
    viewModel: GoalsViewModel = viewModel()
) {
    val allGoals by viewModel.allGoals.collectAsState()
    var newGoalDescription by remember { mutableStateOf("") }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Technical Goals") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate back"
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
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Input field for adding new goals
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newGoalDescription,
                    onValueChange = { newGoalDescription = it },
                    label = { Text("New technical goal") },
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                )
                
                Button(
                    onClick = {
                        if (newGoalDescription.isNotBlank()) {
                            viewModel.createGoal(newGoalDescription)
                            newGoalDescription = ""
                        }
                    },
                    enabled = newGoalDescription.isNotBlank()
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add goal"
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (allGoals.isEmpty()) {
                // Display placeholder when no goals exist
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No technical goals yet. Create one above.",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                // Display list of goals
                Text(
                    text = "All Goals",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                LazyColumn(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(allGoals) { goal ->
                        GoalItem(
                            goal = goal,
                            onToggleAchievement = { isAchieved ->
                                viewModel.toggleGoalAchievement(goal.id, isAchieved)
                            },
                            onDelete = { viewModel.deleteGoal(goal.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GoalItem(
    goal: TechnicalGoal,
    onToggleAchievement: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox for achievement status
            Checkbox(
                checked = goal.isAchieved,
                onCheckedChange = { onToggleAchievement(it) }
            )
            
            // Goal description
            Text(
                text = goal.description,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp)
            )
            
            // Delete button
            IconButton(onClick = { showDeleteConfirmation = true }) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete goal",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
    
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete Goal") },
            text = { Text("Are you sure you want to delete this goal?") },
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