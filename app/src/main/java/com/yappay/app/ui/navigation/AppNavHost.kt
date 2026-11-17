package com.yappay.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.yappay.app.ui.addedit.AddEditExpenseScreen
import com.yappay.app.ui.categories.CategoriesScreen
import com.yappay.app.ui.dashboard.DashboardScreen
import com.yappay.app.ui.home.HomeScreen
import com.yappay.app.ui.settings.SettingsScreen
import com.yappay.app.ui.summary.SummaryScreen

/**
 * String routes on purpose (no type-safe navigation, no kotlinx.serialization plugin).
 * There is NO bottom navigation bar: Home has top-bar icons for Summary and Settings,
 * every other screen has a back arrow that calls onBack/onDone.
 */
object Routes {
    const val HOME = "home"
    const val DASHBOARD = "dashboard"
    const val SUMMARY = "summary"
    const val SETTINGS = "settings"
    const val CATEGORIES = "categories"
    const val ADD = "add"
    const val EXPENSE_ID_ARG = "expenseId"
    const val EDIT = "edit/{$EXPENSE_ID_ARG}"
    fun edit(expenseId: Long) = "edit/$expenseId"
}

@Composable
fun AppNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onOpenSummary = { navController.navigate(Routes.SUMMARY) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onAddManually = { navController.navigate(Routes.ADD) },
                onEditExpense = { id -> navController.navigate(Routes.edit(id)) },
                onOpenDashboard = { navController.navigate(Routes.DASHBOARD) },
            )
        }
        composable(Routes.DASHBOARD) {
            DashboardScreen(
                onBack = { navController.popBackStack() },
                onAddManually = { navController.navigate(Routes.ADD) },
                onOpenSummary = { navController.navigate(Routes.SUMMARY) },
            )
        }
        composable(Routes.SUMMARY) {
            SummaryScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onOpenCategories = { navController.navigate(Routes.CATEGORIES) },
            )
        }
        composable(Routes.CATEGORIES) {
            CategoriesScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.ADD) {
            AddEditExpenseScreen(onDone = { navController.popBackStack() })
        }
        composable(
            route = Routes.EDIT,
            arguments = listOf(navArgument(Routes.EXPENSE_ID_ARG) { type = NavType.LongType }),
        ) {
            // AddEditExpenseViewModel reads the id itself from SavedStateHandle["expenseId"].
            AddEditExpenseScreen(onDone = { navController.popBackStack() })
        }
    }
}
