package com.kailuowang.practicecomp

import android.app.Application
import android.util.Log

/**
 * Application container that provides dependencies across the app
 * Used to allow the service to access the ViewModel
 */
object PracticeAppContainer {
    private var viewModel: PracticeViewModel? = null
    private var goalsViewModel: GoalsViewModel? = null
    
    // Provide the singleton instance of PracticeViewModel
    fun provideViewModel(application: Application): PracticeViewModel {
        if (viewModel == null) {
            Log.d("PracticeAppContainer", "Creating new PracticeViewModel instance")
            viewModel = PracticeViewModel(application)
        }
        return viewModel!!
    }
    
    // Provide the singleton instance of GoalsViewModel
    fun provideGoalsViewModel(application: Application): GoalsViewModel {
        if (goalsViewModel == null) {
            Log.d("PracticeAppContainer", "Creating new GoalsViewModel instance")
            goalsViewModel = GoalsViewModel(application)
        }
        return goalsViewModel!!
    }
    
    // For backward compatibility with existing code
    fun clearViewModel() {
        viewModel = null
        Log.d("PracticeAppContainer", "Cleared PracticeViewModel instance")
    }
    
    // Clear all ViewModel instances (e.g. for testing)
    fun clearViewModels() {
        viewModel = null
        goalsViewModel = null
        Log.d("PracticeAppContainer", "Cleared all ViewModel instances")
    }
} 