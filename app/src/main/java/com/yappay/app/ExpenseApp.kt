package com.yappay.app

import android.app.Application
import android.content.Context
import com.yappay.app.data.AppDatabase
import com.yappay.app.data.CategoryRepository
import com.yappay.app.data.ExpenseRepository
import com.yappay.app.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Manual dependency injection. NO Hilt, NO Koin, NO Dagger.
 * Every ViewModel gets its dependencies from `(application as ExpenseApp).container`
 * inside its `Factory` (see VoiceEntryViewModel.Factory for the pattern).
 */
class AppContainer(context: Context) {
    private val database: AppDatabase = AppDatabase.create(context)
    val expenseRepository: ExpenseRepository = ExpenseRepository(database.expenseDao())
    val categoryRepository: CategoryRepository = CategoryRepository(database.categoryDao())
    val settingsRepository: SettingsRepository = SettingsRepository(context.applicationContext)
}

/** Registered in AndroidManifest.xml via android:name=".ExpenseApp". */
class ExpenseApp : Application() {
    /**
     * Outlives Activities and is created without touching Room/DataStore. Used by the widget
     * quick-capture flow so a save that has already been handed off is not cancelled when the
     * transparent capture Activity finishes.
     */
    val appScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * Lazy on purpose. The home-screen widget's quick-capture process must reach its first frame
     * before it pays for Room/DataStore construction; the capture and widget paths access this
     * from appScope, never from the first-frame main thread.
     */
    val container: AppContainer by lazy { AppContainer(this) }
}
