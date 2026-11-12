package com.gymplanner.app

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.automirrored.rounded.FormatListBulleted
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.gymplanner.app.core.ui.AppTheme
import com.gymplanner.app.feature.onboarding.OnboardingScreen
import com.gymplanner.app.feature.plan.DayEditorScreen
import com.gymplanner.app.feature.plan.ExerciseDemoScreen
import com.gymplanner.app.feature.plan.ExercisePickerScreen
import com.gymplanner.app.feature.plan.ImportPlanScreen
import com.gymplanner.app.feature.plan.PlanExerciseScreen
import com.gymplanner.app.feature.plan.PlanListScreen
import com.gymplanner.app.feature.plan.PlanScreen
import com.gymplanner.app.feature.progress.ExerciseHistoryScreen
import com.gymplanner.app.feature.progress.ProgressScreen
import com.gymplanner.app.feature.settings.SettingsScreen
import com.gymplanner.app.feature.today.SummaryScreen
import com.gymplanner.app.feature.today.TodayScreen
import com.gymplanner.app.feature.today.WorkoutScreen
import com.gymplanner.app.feature.track.CardioEntryScreen
import com.gymplanner.app.feature.track.TrackScreen
import com.gymplanner.app.feature.weighin.WeighInPermissionPrompt
import kotlinx.coroutines.flow.MutableStateFlow

object Routes {
    const val TODAY = "today"
    const val PLAN = "plan"
    const val TRACK = "track"
    const val PROGRESS = "progress"
    const val ONBOARDING = "onboarding"
    const val SETTINGS = "settings"
    const val PLANS = "plans"
    const val WORKOUT = "workout/{sessionId}"
    const val SUMMARY = "summary/{sessionId}"
    const val DAY = "day/{dayId}"
    const val PICKER = "picker/{dayId}"
    const val PLAN_EXERCISE = "planExercise/{id}"
    const val EXERCISE_HISTORY = "exercise/{exerciseId}"
    const val IMPORT = "import?uri={uri}"
    const val DEMO = "demo/{exerciseId}"
    const val CARDIO = "cardio?id={id}"
    fun cardio(id: Long? = null) = if (id == null) "cardio" else "cardio?id=$id"
    fun demo(id: Long) = "demo/$id"

    fun workout(id: Long) = "workout/$id"
    fun summary(id: Long) = "summary/$id"
    fun day(id: Long) = "day/$id"
    fun picker(dayId: Long) = "picker/$dayId"
    fun planExercise(id: Long) = "planExercise/$id"
    fun exerciseHistory(id: Long) = "exercise/$id"
    fun import(uri: Uri) = "import?uri=${Uri.encode(uri.toString())}"
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab(Routes.TODAY, "Today", Icons.Rounded.FitnessCenter),
    Tab(Routes.PLAN, "Plan", Icons.AutoMirrored.Rounded.FormatListBulleted),
    Tab(Routes.TRACK, "Track", Icons.Rounded.MonitorHeart),
    Tab(Routes.PROGRESS, "Progress", Icons.Rounded.BarChart),
)

@Composable
fun AppRoot(pendingImport: MutableStateFlow<Uri?>) {
    val app = LocalContext.current.applicationContext as GymApp
    val ready by app.ready.collectAsStateWithLifecycle()
    val profileId by app.container.prefs.currentProfileId.collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize().background(AppTheme.colors.background)) {
        when {
            !ready -> Unit // brief blank screen while the library is seeded on first run
            profileId == null -> OnboardingScreen(isFirstProfile = true, onDone = {}, onCancel = null)
            else -> MainNav(pendingImport)
        }
    }
}

@Composable
private fun MainNav(pendingImport: MutableStateFlow<Uri?>) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val showTabs = route in tabs.map { it.route }
    val profileId by appContainerState()

    // A plan file was opened from another app → show the import preview.
    val incoming by pendingImport.collectAsStateWithLifecycle()
    LaunchedEffect(incoming) {
        incoming?.let { nav.navigate(Routes.import(it)); pendingImport.value = null }
    }
    // Switching profile → back to Today, fresh.
    var lastProfileId by remember { mutableStateOf(profileId) }
    LaunchedEffect(profileId) {
        if (profileId != lastProfileId) {
            lastProfileId = profileId
            nav.goToTab(Routes.TODAY, reset = true)
        }
    }

    val currentProfile by (LocalContext.current.applicationContext as GymApp).container.profiles.currentProfile.collectAsStateWithLifecycle(null)
    WeighInPermissionPrompt(currentProfile?.weighInEnabled == true)

    val openProfileMenuTargets = ProfileMenuTargets(
        onAddProfile = { nav.navigate(Routes.ONBOARDING) },
        onSettings = { nav.navigate(Routes.SETTINGS) },
    )

    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) {
            NavHost(
                navController = nav,
                startDestination = Routes.TODAY,
                enterTransition = { slideInHorizontally(tween(300)) { it } + fadeIn(tween(300)) },
                exitTransition = { slideOutHorizontally(tween(300)) { -it / 4 } + fadeOut(tween(300)) },
                popEnterTransition = { slideInHorizontally(tween(300)) { -it / 4 } + fadeIn(tween(300)) },
                popExitTransition = { slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300)) },
            ) {
                tabComposable(Routes.TODAY) {
                    TodayScreen(
                        menu = openProfileMenuTargets,
                        onOpenWorkout = { nav.navigate(Routes.workout(it)) },
                        onGoToPlan = { nav.goToTab(Routes.PLAN) },
                        onLogCardio = { nav.navigate(Routes.cardio()) },
                        onOpenProgress = { nav.goToTab(Routes.PROGRESS) },
                    )
                }
                tabComposable(Routes.PLAN) {
                    PlanScreen(
                        menu = openProfileMenuTargets,
                        onOpenDay = { nav.navigate(Routes.day(it)) },
                        onOpenPlans = { nav.navigate(Routes.PLANS) },
                    )
                }
                tabComposable(Routes.TRACK) {
                    TrackScreen(menu = openProfileMenuTargets, onLogCardio = { nav.navigate(Routes.cardio(it)) })
                }
                composable(Routes.CARDIO, arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = -1L })) { entry ->
                    CardioEntryScreen(editId = entry.arguments?.getLong("id")?.takeIf { it > 0 }, onDone = { nav.popBackStack() })
                }
                tabComposable(Routes.PROGRESS) {
                    ProgressScreen(menu = openProfileMenuTargets, onOpenExercise = { nav.navigate(Routes.exerciseHistory(it)) })
                }
                composable(
                    Routes.ONBOARDING,
                    enterTransition = { slideInVertically(tween(350)) { it } },
                    exitTransition = { ExitTransition.None },
                    popExitTransition = { slideOutVertically(tween(300)) { it } },
                ) {
                    OnboardingScreen(
                        isFirstProfile = false,
                        onDone = { nav.goToTab(Routes.TODAY, reset = true) },
                        onCancel = { nav.popBackStack() },
                    )
                }
                composable(Routes.SETTINGS) {
                    SettingsScreen(onBack = { nav.popBackStack() }, onAddProfile = { nav.navigate(Routes.ONBOARDING) })
                }
                composable(Routes.PLANS) {
                    PlanListScreen(onBack = { nav.popBackStack() }, onImport = { nav.navigate(Routes.import(it)) })
                }
                composable(
                    Routes.WORKOUT,
                    arguments = listOf(navArgument("sessionId") { type = NavType.LongType }),
                    enterTransition = { slideInVertically(tween(350)) { it } },
                    popExitTransition = { slideOutVertically(tween(300)) { it } },
                ) { entry ->
                    WorkoutScreen(
                        sessionId = entry.arguments!!.getLong("sessionId"),
                        // Guarded: the session can finish/discard while this screen is animating out.
                        onClose = { if (nav.currentDestination?.route == Routes.WORKOUT) nav.popBackStack() },
                        onFinished = { id ->
                            nav.navigate(Routes.summary(id)) { popUpTo(Routes.TODAY) }
                        },
                        onHowTo = { nav.navigate(Routes.demo(it)) },
                    )
                }
                composable(
                    Routes.SUMMARY,
                    arguments = listOf(navArgument("sessionId") { type = NavType.LongType }),
                    enterTransition = { fadeIn(tween(300)) },
                ) { entry ->
                    SummaryScreen(sessionId = entry.arguments!!.getLong("sessionId"), onDone = { nav.popBackStack() })
                }
                composable(Routes.DAY, arguments = listOf(navArgument("dayId") { type = NavType.LongType })) { entry ->
                    val dayId = entry.arguments!!.getLong("dayId")
                    DayEditorScreen(
                        dayId = dayId,
                        onBack = { nav.popBackStack() },
                        onAddExercise = { nav.navigate(Routes.picker(dayId)) },
                        onEditExercise = { nav.navigate(Routes.planExercise(it)) },
                    )
                }
                composable(
                    Routes.PICKER,
                    arguments = listOf(navArgument("dayId") { type = NavType.LongType }),
                    enterTransition = { slideInVertically(tween(350)) { it } },
                    popExitTransition = { slideOutVertically(tween(300)) { it } },
                ) { entry ->
                    ExercisePickerScreen(
                        dayId = entry.arguments!!.getLong("dayId"),
                        onClose = { nav.popBackStack() },
                        onAdded = { peId ->
                            nav.popBackStack()
                            nav.navigate(Routes.planExercise(peId))
                        },
                    )
                }
                composable(Routes.PLAN_EXERCISE, arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                    PlanExerciseScreen(
                        id = entry.arguments!!.getLong("id"),
                        onBack = { nav.popBackStack() },
                        onHowTo = { nav.navigate(Routes.demo(it)) },
                    )
                }
                composable(Routes.DEMO, arguments = listOf(navArgument("exerciseId") { type = NavType.LongType })) { entry ->
                    ExerciseDemoScreen(exerciseId = entry.arguments!!.getLong("exerciseId"), onBack = { nav.popBackStack() })
                }
                composable(Routes.EXERCISE_HISTORY, arguments = listOf(navArgument("exerciseId") { type = NavType.LongType })) { entry ->
                    ExerciseHistoryScreen(exerciseId = entry.arguments!!.getLong("exerciseId"), onBack = { nav.popBackStack() })
                }
                composable(
                    Routes.IMPORT,
                    arguments = listOf(navArgument("uri") { type = NavType.StringType; nullable = true }),
                    enterTransition = { slideInVertically(tween(350)) { it } },
                    popExitTransition = { slideOutVertically(tween(300)) { it } },
                ) { entry ->
                    val uri = entry.arguments?.getString("uri")?.let { it.toUri() }
                    ImportPlanScreen(
                        uri = uri,
                        onClose = { nav.popBackStack() },
                        onImported = { nav.goToTab(Routes.PLAN, reset = true) },
                    )
                }
            }
            // Solid strip behind the status bar so scrolled content never runs under the clock.
            Spacer(
                Modifier.fillMaxWidth().windowInsetsTopHeight(WindowInsets.statusBars)
                    .background(AppTheme.colors.background.copy(alpha = 0.96f))
            )
        }
        AnimatedVisibility(visible = showTabs, enter = fadeIn(), exit = fadeOut()) {
            TabBar(current = route, onSelect = { nav.goToTab(it) })
        }
    }
}

/** Where the profile avatar menu can take you. Passed to every tab. */
data class ProfileMenuTargets(val onAddProfile: () -> Unit, val onSettings: () -> Unit)

@Composable
private fun appContainerState() =
    (LocalContext.current.applicationContext as GymApp).container.prefs.currentProfileId.collectAsStateWithLifecycle()

private fun androidx.navigation.NavGraphBuilder.tabComposable(route: String, content: @Composable () -> Unit) {
    composable(
        route,
        enterTransition = { fadeIn(tween(200)) },
        exitTransition = {
            if (targetState.destination.route in tabs.map { it.route }) fadeOut(tween(200))
            else slideOutHorizontally(tween(300)) { -it / 4 } + fadeOut(tween(300))
        },
        popEnterTransition = {
            if (initialState.destination.route in tabs.map { it.route }) fadeIn(tween(200))
            else slideInHorizontally(tween(300)) { -it / 4 } + fadeIn(tween(300))
        },
        popExitTransition = { fadeOut(tween(200)) },
    ) { content() }
}

private fun NavHostController.goToTab(route: String, reset: Boolean = false) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = !reset }
        launchSingleTop = true
        restoreState = !reset
    }
}

/** iOS-style tab bar: hairline on top, icon above a small label, accent for the selected tab. */
@Composable
private fun TabBar(current: String?, onSelect: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().background(AppTheme.colors.bottomBar)) {
        HorizontalDivider(thickness = 1.dp, color = AppTheme.colors.separator)
        Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 8.dp).height(64.dp)) {
            tabs.forEach { tab ->
                val selected = tab.route == current
                val color = if (selected) AppTheme.colors.accent else AppTheme.colors.secondaryLabel
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            role = Role.Tab,
                        ) { onSelect(tab.route) }
                        .padding(top = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(tab.icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.height(4.dp))
                    Text(tab.label, style = AppTheme.type.caption.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold), color = color)
                }
            }
        }
    }
}
