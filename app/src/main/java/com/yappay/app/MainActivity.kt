package com.yappay.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.yappay.app.ui.navigation.AppNavHost
import com.yappay.app.ui.theme.YapPayTheme
import com.yappay.app.widget.ExpenseWidgetProvider

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            YapPayTheme {
                AppNavHost()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // Pick up edits made in the app so the widget is not stale until its 30-minute update.
        if (!isChangingConfigurations) ExpenseWidgetProvider.refresh(this)
    }
}
