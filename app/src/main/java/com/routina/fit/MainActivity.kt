package com.routina.fit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.routina.fit.ui.AddItemScreen
import com.routina.fit.ui.DefaultsScreen
import com.routina.fit.ui.ExerciseEditScreen
import com.routina.fit.ui.FitViewModel
import com.routina.fit.ui.HistoryScreen
import com.routina.fit.ui.LibraryScreen
import com.routina.fit.ui.NEW_ROUTINE
import com.routina.fit.ui.RoutineEditScreen
import com.routina.fit.ui.RoutinesScreen
import com.routina.fit.ui.RunScreen
import com.routina.fit.ui.SessionScreen
import com.routina.fit.ui.SettingsScreen
import com.routina.fit.ui.SummaryScreen
import com.routina.fit.ui.TodayScreen
import com.routina.fit.ui.theme.FitTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            FitTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    FitRoot()
                }
            }
        }
    }
}

/**
 * 底部分頁只放「地方」。執行、摘要、編輯、加一項這些是任務，一律全螢幕蓋上去、不顯示分頁列。
 *
 * 計數沒有自己的分頁——它就是首頁。原本那個「計數」分頁是先選動作再開始數，
 * 等於每天都要重新設定一次同一件事；現在今天要做什麼本來就列在首頁上，點一下就是一下。
 */
private enum class Tab(val route: String, val label: Int, val icon: ImageVector) {
    TODAY(Routes.TODAY, R.string.tab_today, Icons.Default.Today),
    ROUTINES(Routes.ROUTINES, R.string.tab_routines, Icons.AutoMirrored.Filled.ListAlt),
    LIBRARY(Routes.LIBRARY, R.string.tab_library, Icons.Default.FitnessCenter),
    HISTORY(Routes.HISTORY, R.string.tab_history, Icons.Default.History)
}

private object Routes {
    const val TODAY = "today"
    const val ROUTINES = "routines"
    const val LIBRARY = "library"
    const val HISTORY = "history"

    const val RUN = "run"
    const val SUMMARY = "summary"
    const val SETTINGS = "settings"
    const val DEFAULTS = "defaults"
    const val EXERCISE_NEW = "exercise/new"

    // 加一項可以帶一個動作進來（從動作庫的「加到今天」），也可以空手進來自己打
    const val ITEM_NEW = "item/new?exerciseId={exerciseId}"

    const val EXERCISE_EDIT = "exercise/{exerciseId}"
    const val ROUTINE_EDIT = "routine/{routineId}"
    const val SESSION = "session/{sessionId}"

    const val ARG_EXERCISE_ID = "exerciseId"
    const val ARG_ROUTINE_ID = "routineId"
    const val ARG_SESSION_ID = "sessionId"

    fun newItem(exerciseId: String = "") =
        if (exerciseId.isEmpty()) "item/new" else "item/new?exerciseId=$exerciseId"

    fun exercise(id: String) = "exercise/$id"
    fun routine(id: String) = "routine/$id"
    fun session(id: String) = "session/$id"
}

@Composable
private fun FitRoot() {
    val navController = rememberNavController()
    val viewModel: FitViewModel = viewModel()

    // 分頁全是頂層路由，直接比 route 就夠，不需要走 hierarchy
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val onTab = Tab.entries.any { it.route == currentRoute }

    Scaffold(
        bottomBar = {
            // 全螢幕的畫面不顯示分頁列——它們是任務，不是地方
            if (onTab) {
                NavigationBar {
                    Tab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = tab.route == currentRoute,
                            onClick = { navController.switchTab(tab.route) },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(stringResource(tab.label)) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.TODAY,
            // 分頁列的高度由 Scaffold 給，全螢幕畫面自己用 Scaffold 處理插入邊
            modifier = if (onTab) Modifier.padding(padding) else Modifier
        ) {

            composable(Routes.TODAY) {
                TodayScreen(
                    viewModel = viewModel,
                    onAddItem = { navController.navigate(Routes.newItem()) },
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) }
                )
            }

            composable(Routes.ROUTINES) {
                RoutinesScreen(
                    viewModel = viewModel,
                    onEditRoutine = { routineId -> navController.navigate(Routes.routine(routineId)) },
                    onStartRoutine = { routineId ->
                        if (viewModel.startRoutine(routineId)) navController.navigate(Routes.RUN)
                    }
                )
            }

            composable(Routes.LIBRARY) {
                LibraryScreen(
                    viewModel = viewModel,
                    onEditExercise = { id -> navController.navigate(Routes.exercise(id)) },
                    onNewExercise = { navController.navigate(Routes.EXERCISE_NEW) },
                    onAddToToday = { id -> navController.navigate(Routes.newItem(id)) }
                )
            }

            composable(Routes.HISTORY) {
                HistoryScreen(
                    viewModel = viewModel,
                    onOpenSession = { id -> navController.navigate(Routes.session(id)) }
                )
            }

            // 執行中與摘要都靠 ViewModel 裡那一份 RunState，路由本身不帶參數。
            // 練完（phase 變成 DONE）由 RunScreen 換到摘要，並把自己從返回堆疊移掉。
            composable(Routes.RUN) {
                RunScreen(
                    viewModel = viewModel,
                    onDone = {
                        navController.navigate(Routes.SUMMARY) {
                            popUpTo(Routes.RUN) { inclusive = true }
                        }
                    },
                    onQuit = { navController.popBackStack() }
                )
            }

            composable(Routes.SUMMARY) {
                SummaryScreen(
                    viewModel = viewModel,
                    onClose = { navController.popBackStack() }
                )
            }

            composable(
                route = Routes.ITEM_NEW,
                arguments = listOf(
                    navArgument(Routes.ARG_EXERCISE_ID) {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { entry ->
                AddItemScreen(
                    viewModel = viewModel,
                    exerciseId = entry.arguments?.getString(Routes.ARG_EXERCISE_ID),
                    onDone = { navController.popBackStack() }
                )
            }

            composable(Routes.EXERCISE_NEW) {
                ExerciseEditScreen(
                    viewModel = viewModel,
                    exerciseId = null,
                    onDone = { navController.popBackStack() }
                )
            }

            composable(
                route = Routes.EXERCISE_EDIT,
                arguments = listOf(navArgument(Routes.ARG_EXERCISE_ID) { type = NavType.StringType })
            ) { entry ->
                ExerciseEditScreen(
                    viewModel = viewModel,
                    exerciseId = entry.arguments?.getString(Routes.ARG_EXERCISE_ID),
                    onDone = { navController.popBackStack() }
                )
            }

            composable(
                route = Routes.ROUTINE_EDIT,
                arguments = listOf(navArgument(Routes.ARG_ROUTINE_ID) { type = NavType.StringType })
            ) { entry ->
                RoutineEditScreen(
                    viewModel = viewModel,
                    routineId = entry.arguments?.getString(Routes.ARG_ROUTINE_ID) ?: NEW_ROUTINE,
                    onStart = { routineId ->
                        if (viewModel.startRoutine(routineId)) {
                            navController.navigate(Routes.RUN) {
                                popUpTo(Routes.ROUTINES) { inclusive = false }
                            }
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Routes.SESSION,
                arguments = listOf(navArgument(Routes.ARG_SESSION_ID) { type = NavType.StringType })
            ) { entry ->
                SessionScreen(
                    viewModel = viewModel,
                    sessionId = entry.arguments?.getString(Routes.ARG_SESSION_ID).orEmpty(),
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Routes.SETTINGS) {
                SettingsScreen(
                    viewModel = viewModel,
                    onOpenDefaults = { navController.navigate(Routes.DEFAULTS) },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Routes.DEFAULTS) {
                DefaultsScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

/**
 * 切分頁的標準作法：回到起始分頁再上去，並存/還原各分頁自己的捲動位置，
 * 連點同一顆也不會疊出一堆同樣的畫面。
 */
private fun NavHostController.switchTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
