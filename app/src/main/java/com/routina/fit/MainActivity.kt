package com.routina.fit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.routina.fit.ui.CounterScreen
import com.routina.fit.ui.CounterSetupScreen
import com.routina.fit.ui.ExerciseEditScreen
import com.routina.fit.ui.FitViewModel
import com.routina.fit.ui.HistoryScreen
import com.routina.fit.ui.LibraryScreen
import com.routina.fit.ui.RoutineEditScreen
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
                    FitNavHost()
                }
            }
        }
    }
}

private object Routes {
    const val TODAY = "today"
    const val RUN = "run"
    const val SUMMARY = "summary"
    const val COUNTER_SETUP = "counter/new"
    const val COUNTER = "counter"
    const val LIBRARY = "library"
    const val EXERCISE_NEW = "exercise/new"
    const val HISTORY = "history"
    const val SETTINGS = "settings"

    const val EXERCISE_EDIT = "exercise/{exerciseId}"
    const val ROUTINE_EDIT = "routine/{routineId}"
    const val SESSION = "session/{sessionId}"

    const val ARG_EXERCISE_ID = "exerciseId"
    const val ARG_ROUTINE_ID = "routineId"
    const val ARG_SESSION_ID = "sessionId"

    fun exercise(id: String) = "exercise/$id"
    fun routine(id: String) = "routine/$id"
    fun session(id: String) = "session/$id"
}

@Composable
private fun FitNavHost() {
    val navController = rememberNavController()
    val viewModel: FitViewModel = viewModel()

    NavHost(navController = navController, startDestination = Routes.TODAY) {

        composable(Routes.TODAY) {
            TodayScreen(
                viewModel = viewModel,
                onStartRoutine = { routineId ->
                    if (viewModel.startRoutine(routineId)) navController.navigate(Routes.RUN)
                },
                onEditRoutine = { routineId -> navController.navigate(Routes.routine(routineId)) },
                onCounter = { navController.navigate(Routes.COUNTER_SETUP) },
                onOpenLibrary = { navController.navigate(Routes.LIBRARY) },
                onOpenHistory = { navController.navigate(Routes.HISTORY) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) }
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
                onQuit = { navController.popBackStack(Routes.TODAY, inclusive = false) }
            )
        }

        composable(Routes.SUMMARY) {
            SummaryScreen(
                viewModel = viewModel,
                onClose = { navController.popBackStack(Routes.TODAY, inclusive = false) }
            )
        }

        composable(Routes.COUNTER_SETUP) {
            CounterSetupScreen(
                viewModel = viewModel,
                onStart = {
                    navController.navigate(Routes.COUNTER) {
                        popUpTo(Routes.COUNTER_SETUP) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.COUNTER) {
            CounterScreen(
                viewModel = viewModel,
                onClose = { navController.popBackStack(Routes.TODAY, inclusive = false) }
            )
        }

        composable(Routes.LIBRARY) {
            LibraryScreen(
                viewModel = viewModel,
                onEditExercise = { id -> navController.navigate(Routes.exercise(id)) },
                onNewExercise = { navController.navigate(Routes.EXERCISE_NEW) },
                onBack = { navController.popBackStack() }
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
                routineId = entry.arguments?.getString(Routes.ARG_ROUTINE_ID).orEmpty(),
                onStart = { routineId ->
                    if (viewModel.startRoutine(routineId)) {
                        navController.navigate(Routes.RUN) {
                            popUpTo(Routes.TODAY) { inclusive = false }
                        }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.HISTORY) {
            HistoryScreen(
                viewModel = viewModel,
                onOpenSession = { id -> navController.navigate(Routes.session(id)) },
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
                onBack = { navController.popBackStack() }
            )
        }
    }
}
