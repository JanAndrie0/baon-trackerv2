package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ai.AiParseUiState
import com.example.ai.configureAppCheck
import com.example.data.local.ThemeMode
import com.example.data.local.TransactionEntity
import com.example.ui.components.AiDraftConfirmSheet
import com.example.ui.components.QuickAddBottomSheet
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.InsightsScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.BaonTrackerTheme
import com.example.ui.viewmodel.BaonViewModel
import kotlinx.coroutines.launch

const val ROUTE_HOME = "home"
const val ROUTE_HISTORY = "history"
const val ROUTE_INSIGHTS = "insights"
const val ROUTE_SETTINGS = "settings"

private data class BaonTab(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val BAON_TABS = listOf(
    BaonTab(ROUTE_HOME, R.string.tab_home, Icons.Filled.Home, Icons.Outlined.Home),
    BaonTab(ROUTE_HISTORY, R.string.tab_history, Icons.Filled.History, Icons.Outlined.History),
    BaonTab(ROUTE_INSIGHTS, R.string.tab_insights, Icons.Filled.Insights, Icons.Outlined.Insights),
    BaonTab(ROUTE_SETTINGS, R.string.tab_settings, Icons.Filled.Settings, Icons.Outlined.Settings)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure Firebase App Check with launch intent before any UI or ViewModels
        configureAppCheck(this, intent)

        // STEP 2: Enable edge-to-edge display
        enableEdgeToEdge()

        setContent {
            val viewModel: BaonViewModel = viewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val isDarkTheme = when (uiState.preferences.themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> systemDark
            }

            LaunchedEffect(isDarkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = if (isDarkTheme) {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.TRANSPARENT
                        )
                    },
                    navigationBarStyle = if (isDarkTheme) {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.TRANSPARENT
                        )
                    }
                )
            }

            BaonTrackerTheme(
                darkTheme = isDarkTheme,
                appPalette = uiState.preferences.appPalette
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BaonTrackerApp(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun BaonTrackerApp(viewModel: BaonViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val aiParseState by viewModel.aiParseState.collectAsStateWithLifecycle()
    val recapState by viewModel.recapState.collectAsStateWithLifecycle()

    if (!uiState.isLoaded) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (!uiState.preferences.hasCompletedOnboarding || uiState.activeCycle == null) {
        OnboardingScreen(
            onComplete = { name, freq, amount, nextDate, walletMap ->
                viewModel.completeOnboarding(name, freq, amount, nextDate, walletMap)
            }
        )
        return
    }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: ROUTE_HOME
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showQuickAdd by remember { mutableStateOf(false) }
    var editingTransaction by remember { mutableStateOf<TransactionEntity?>(null) }

    val navigateToTab: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (!useRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 4.dp
                    ) {
                        BAON_TABS.forEach { tab ->
                            val selected = currentRoute == tab.route
                            val label = stringResource(tab.labelRes)
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateToTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = label
                                    )
                                },
                                label = { Text(label) },
                                modifier = Modifier.testTag("nav_tab_${tab.route}")
                            )
                        }
                    }
                }
            },
            floatingActionButton = {
                if (currentRoute == ROUTE_HOME || currentRoute == ROUTE_HISTORY) {
                    ExtendedFloatingActionButton(
                        onClick = {
                            editingTransaction = null
                            showQuickAdd = true
                        },
                        icon = { Icon(imageVector = Icons.Filled.Add, contentDescription = null) },
                        text = { Text("Add") },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.testTag("fab_quick_add")
                    )
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail(modifier = Modifier.fillMaxHeight()) {
                        BAON_TABS.forEach { tab ->
                            val selected = currentRoute == tab.route
                            val label = stringResource(tab.labelRes)
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = label
                                    )
                                },
                                label = { Text(label) },
                                modifier = Modifier.testTag("rail_tab_${tab.route}")
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = ROUTE_HOME,
                    modifier = Modifier.weight(1f)
                ) {
                    composable(ROUTE_HOME) {
                        HomeScreen(
                            uiState = uiState,
                            aiParseState = aiParseState,
                            onEditTransaction = { tx ->
                                editingTransaction = tx
                                showQuickAdd = true
                            },
                            onParseAiText = { text -> viewModel.parseQuickEntryText(text) },
                            onPickReceiptUri = { uri -> viewModel.parseReceiptImageUri(uri) },
                            onProcessQueueItem = { item -> viewModel.processQueuedItem(item) },
                            onRemoveQueueItem = { item -> viewModel.removeQueuedItem(item) },
                            onDismissAiBanner = { viewModel.dismissAiState() },
                            onToggleTheme = { mode -> viewModel.updateThemeMode(mode) }
                        )
                    }

                    composable(ROUTE_HISTORY) {
                        HistoryScreen(
                            uiState = uiState,
                            groupProvider = { query, walletId, catId ->
                                viewModel.groupTransactionsForHistory(
                                    items = uiState.allTransactions,
                                    searchQuery = query,
                                    selectedWalletId = walletId,
                                    selectedCategoryId = catId
                                )
                            },
                            onEditTransaction = { tx ->
                                editingTransaction = tx
                                showQuickAdd = true
                            }
                        )
                    }

                    composable(ROUTE_INSIGHTS) {
                        InsightsScreen(
                            uiState = uiState,
                            recapState = recapState,
                            onRefreshRecap = { viewModel.generateWeeklyRecap() }
                        )
                    }

                    composable(ROUTE_SETTINGS) {
                        SettingsScreen(
                            uiState = uiState,
                            onUpdateStudentName = { viewModel.updateStudentName(it) },
                            onUpdateThemeMode = { viewModel.updateThemeMode(it) },
                            onUpdateAppPalette = { viewModel.updateAppPalette(it) },
                            onUpdateCycle = { amount, freq, nextDate, startFresh ->
                                viewModel.updateCycle(amount, freq, nextDate, startFresh)
                            },
                            onUpdateEnvelope = { catId, cap ->
                                viewModel.updateCategoryEnvelope(catId, cap)
                            },
                            onUpdateReminder = { enabled, hour, min ->
                                viewModel.updateReminder(enabled, hour, min)
                            },
                            onGenerateCsv = { viewModel.generateCsvContent() },
                            onClearAllData = { viewModel.clearAllLocalData() },
                            onShowMessage = { msg ->
                                scope.launch {
                                    snackbarHostState.showSnackbar(msg)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showQuickAdd) {
        QuickAddBottomSheet(
            wallets = uiState.wallets.map { it.wallet },
            categories = uiState.categories,
            editingTransaction = editingTransaction,
            onDismiss = {
                showQuickAdd = false
                editingTransaction = null
            },
            onSave = { editId, walletId, categoryId, kind, amountCentavos, note, occurredAtMillis ->
                viewModel.saveTransaction(
                    editingId = editId,
                    walletId = walletId,
                    categoryId = categoryId,
                    kind = kind,
                    amountCentavos = amountCentavos,
                    note = note,
                    occurredAtMillis = occurredAtMillis
                )
                showQuickAdd = false
                editingTransaction = null
            },
            onDelete = { txId ->
                viewModel.softDeleteTransaction(txId)
                scope.launch {
                    val result = snackbarHostState.showSnackbar(
                        message = "Entry deleted",
                        actionLabel = "Undo",
                        duration = SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.undoLastDelete()
                    } else {
                        viewModel.clearUndoMarker()
                    }
                }
            }
        )
    }

    val currentAi = aiParseState
    if (currentAi is AiParseUiState.DraftsReady) {
        AiDraftConfirmSheet(
            initialDrafts = currentAi.drafts,
            queueItemId = currentAi.queueItemId,
            categories = uiState.categories,
            wallets = uiState.wallets.map { it.wallet },
            onDismiss = { viewModel.dismissAiState() },
            onConfirmSave = { confirmedDrafts, qId ->
                viewModel.confirmAiDrafts(confirmedDrafts, qId)
                scope.launch {
                    snackbarHostState.showSnackbar("Saved ${confirmedDrafts.size} AI entry(s)!")
                }
            }
        )
    }
}
