package com.kailuowang.practicecomp

import android.app.Application
import android.util.Log

/**
 * Application container that provides dependencies across the app
 * Used to allow the service to access the ViewModel
 */
object PracticeAppContainer {
    private var viewModel: PracticeViewModel? = null
    
    // Provide the singleton instance of PracticeViewModel
    fun provideViewModel(application: Application): PracticeViewModel {
        if (viewModel == null) {
            Log.d("PracticeAppContainer", "Creating new ViewModel instance")
            viewModel = PracticeViewModel(application)
        }
        return viewModel!!
    }
    
    // Optional: Clear the ViewModel instance if needed (e.g. for testing)
    fun clearViewModel() {
        viewModel = null
    }
} 