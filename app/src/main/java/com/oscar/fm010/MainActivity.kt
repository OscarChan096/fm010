package com.oscar.fm010

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.oscar.fm010.notification.NotificationHelper
import com.oscar.fm010.ui.about.AboutScreen
import com.oscar.fm010.ui.expenses.ExpensesScreen
import com.oscar.fm010.ui.home.HomeScreen
import com.oscar.fm010.ui.money.MoneyScreen
import com.oscar.fm010.ui.theme.FM010Theme
import com.oscar.fm010.ui.viewmodel.FinanceUiState
import com.oscar.fm010.ui.viewmodel.FinanceViewModel
import com.oscar.fm010.ui.viewmodel.RepositoryProvider

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        RepositoryProvider.initialize(applicationContext)
        NotificationHelper.createNotificationChannel(applicationContext)
        enableEdgeToEdge()
        setContent {
            val viewModel: FinanceViewModel = viewModel()
            val uiState by viewModel.uiState.collectAsState()

            FM010Theme(darkTheme = uiState.isDarkMode) {
                FM010App(viewModel = viewModel, uiState = uiState)
            }
        }
    }
}

@PreviewScreenSizes
@Composable
fun FM010App(
    viewModel: FinanceViewModel = viewModel(),
    uiState: FinanceUiState = viewModel<FinanceViewModel>().uiState.collectAsState().value
) {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }
    val context = LocalContext.current

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { }

        LaunchedEffect(Unit) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    LaunchedEffect(uiState.expenses) {
        if (uiState.expenses.isNotEmpty()) {
            NotificationHelper.scheduleRemindersForExpenses(context.applicationContext, uiState.expenses)
        }
    }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach { destination ->
                item(
                    icon = {
                        Icon(
                            painterResource(destination.icon),
                            contentDescription = destination.label
                        )
                    },
                    label = { Text(destination.label) },
                    selected = destination == currentDestination,
                    onClick = { currentDestination = destination }
                )
            }
        }
    ) {
        when (currentDestination) {
            AppDestinations.HOME -> HomeScreen(
                uiState = uiState,
                onNavigateToMoney = { currentDestination = AppDestinations.MONEY },
                onNavigateToExpenses = { currentDestination = AppDestinations.EXPENSES },
                onToggleDarkMode = { viewModel.toggleDarkMode(it) }
            )
            AppDestinations.MONEY -> MoneyScreen(
                currentTotalMoney = uiState.wallet.totalMoney,
                onUpdateMoney = { viewModel.updateWallet(it) }
            )
            AppDestinations.EXPENSES -> ExpensesScreen(
                expenses = uiState.expenses,
                onAddExpense = { title, amount, dueDate, cutoffDate, isCreditCard, category, reminderDays ->
                    viewModel.addExpense(context, title, amount, dueDate, cutoffDate, isCreditCard, category, reminderDays)
                },
                onUpdateExpense = { expense ->
                    viewModel.updateExpense(context, expense)
                },
                onDeleteExpense = { expense ->
                    viewModel.deleteExpense(context, expense.id, expense.reminderDays)
                }
            )
            AppDestinations.ABOUT -> AboutScreen()
        }
    }
}

enum class AppDestinations(
    val label: String,
    val icon: Int,
) {
    HOME("Dashboard", R.drawable.ic_home),
    MONEY("Mi Dinero", R.drawable.ic_account_box),
    EXPENSES("Gastos/Pagos", R.drawable.ic_favorite),
    ABOUT("Acerca de", R.drawable.ic_info),
}
