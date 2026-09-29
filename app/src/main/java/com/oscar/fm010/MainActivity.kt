package com.oscar.fm010

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.lifecycle.viewmodel.compose.viewModel
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
                onAddExpense = { title, amount, dueDate, cutoffDate, isCreditCard, category ->
                    viewModel.addExpense(title, amount, dueDate, cutoffDate, isCreditCard, category)
                },
                onDeleteExpense = { id ->
                    viewModel.deleteExpense(id)
                }
            )
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
}
