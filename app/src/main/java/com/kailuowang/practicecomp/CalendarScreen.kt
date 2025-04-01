package com.kailuowang.practicecomp

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kailuowang.practicecomp.ui.theme.PracticeCompTheme
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticeCalendarScreen(
    onBackClick: () -> Unit,
    viewModel: PracticeViewModel = viewModel()
) {
    val currentDate = remember { LocalDate.now() }
    var selectedMonth by remember { mutableStateOf(YearMonth.from(currentDate)) }
    
    // Calculate overall practice statistics
    val monthlyPracticeTime = viewModel.getPracticeDurationForMonth(selectedMonth)
    val lifetimePracticeTime = viewModel.getLifetimePracticeDuration()
    
    val monthlyPracticeFormatted = viewModel.formatPracticeDuration(monthlyPracticeTime)
    val lifetimePracticeFormatted = viewModel.formatPracticeDuration(lifetimePracticeTime)
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Practice Calendar") },
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
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Month navigation
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    selectedMonth = selectedMonth.minusMonths(1)
                }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Previous Month"
                    )
                }
                
                Text(
                    text = selectedMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                    style = MaterialTheme.typography.titleLarge
                )
                
                IconButton(onClick = {
                    selectedMonth = selectedMonth.plusMonths(1)
                }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Next Month"
                    )
                }
            }
            
            // Calendar
            MonthCalendar(
                selectedMonth = selectedMonth,
                currentDate = currentDate,
                viewModel = viewModel
            )
            
            // Statistics section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Practice Statistics",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "This Month:",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = monthlyPracticeFormatted,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Lifetime Total:",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = lifetimePracticeFormatted,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MonthCalendar(
    selectedMonth: YearMonth,
    currentDate: LocalDate,
    viewModel: PracticeViewModel
) {
    // Day of week labels
    val daysOfWeek = DayOfWeek.values()
    val firstDayOfWeek = WeekFields.of(Locale.getDefault()).firstDayOfWeek
    
    // Reorder days to match the locale's first day of week
    val orderedDaysOfWeek = (0..6).map { i ->
        val index = (firstDayOfWeek.value - 1 + i) % 7
        daysOfWeek[index]
    }
    
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        // Calendar header with days of week
        Row(modifier = Modifier.fillMaxWidth()) {
            for (dayOfWeek in orderedDaysOfWeek) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Calendar grid
        val firstDayOfMonth = selectedMonth.atDay(1)
        val lastDayOfMonth = selectedMonth.atEndOfMonth()
        
        // Calculate the day of week index for the first day of month (0-based, adjusted for locale)
        val firstDayOfMonthIndex = (firstDayOfMonth.dayOfWeek.value - firstDayOfWeek.value + 7) % 7
        
        // Calculate the number of weeks in the month view
        val daysInMonth = lastDayOfMonth.dayOfMonth
        val totalCells = firstDayOfMonthIndex + daysInMonth
        val weeksInMonth = (totalCells + 6) / 7 // Round up to full weeks
        
        // Create calendar grid
        for (week in 0 until weeksInMonth) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (dayOfWeek in 0..6) { // 7 days in a week
                    val dayIndex = week * 7 + dayOfWeek
                    val dayNumber = dayIndex - firstDayOfMonthIndex + 1
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .padding(4.dp)
                    ) {
                        if (dayNumber in 1..daysInMonth) {
                            // This is a valid day of the month
                            val date = selectedMonth.atDay(dayNumber)
                            val isCurrentDay = date == currentDate
                            
                            // Calculate practice time for this day
                            val practiceDuration = viewModel.getPracticeDurationForDate(date.atStartOfDay())
                            val hasPractice = practiceDuration > 0
                            
                            // Day container
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(
                                        if (isCurrentDay) MaterialTheme.colorScheme.primaryContainer
                                        else Color.Transparent
                                    )
                                    .border(
                                        width = if (isCurrentDay) 0.dp else 1.dp,
                                        color = if (isCurrentDay) Color.Transparent else MaterialTheme.colorScheme.outlineVariant,
                                        shape = CircleShape
                                    )
                                    .let { mod ->
                                        if (isCurrentDay) {
                                            mod.testTag("currentDay")
                                        } else {
                                            mod
                                        }
                                    },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                // Day number
                                Text(
                                    text = date.dayOfMonth.toString(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isCurrentDay) 
                                        MaterialTheme.colorScheme.onPrimaryContainer 
                                    else 
                                        MaterialTheme.colorScheme.onSurface
                                )
                                
                                // Practice indicator
                                if (hasPractice) {
                                    val formattedDuration = viewModel.formatPracticeDuration(practiceDuration)
                                    Text(
                                        text = formattedDuration,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isCurrentDay) 
                                            MaterialTheme.colorScheme.onPrimaryContainer 
                                        else 
                                            MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PracticeCalendarScreenPreview() {
    PracticeCompTheme {
        PracticeCalendarScreen(onBackClick = {})
    }
} 