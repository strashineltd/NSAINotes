package com.nsai.notes.presentation.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import android.os.SystemClock
import android.widget.Toast
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.nsai.notes.performance.FluidityManager
import com.nsai.notes.performance.InputThrottler
import com.nsai.notes.performance.AnimationBudget
import com.nsai.notes.presentation.ai.AIChatScreen
import com.nsai.notes.presentation.ai.AIHomeScreen
import com.nsai.notes.presentation.ai.AIModelSettingsScreen
import com.nsai.notes.presentation.ai.MCPSkillManageScreen
import com.nsai.notes.presentation.files.FileListScreen
import com.nsai.notes.presentation.notes.NoteEditScreen
import com.nsai.notes.presentation.notes.NoteListScreen
import com.nsai.notes.presentation.settings.SettingsScreen
import com.nsai.notes.presentation.tags.TagManageScreen
import com.nsai.notes.presentation.theme.LocalAnimationConfig
import com.nsai.notes.presentation.theme.StandardEasing

private val tabRoutes = setOf(Screen.NoteList.route, Screen.Files.route, Screen.AIHome.route)

@Composable
fun NSAINavGraph(
    navController: NavHostController = rememberNavController(),
    fluidityManager: FluidityManager,
    inputThrottler: InputThrottler
) {
    val tokens = LocalAnimationConfig.current
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute in listOf(
        Screen.NoteList.route,
        Screen.Files.route,
        Screen.AIHome.route
    )

    // Double-back to exit — only on root tab screens
    var backPressTime by remember { mutableLongStateOf(0L) }
    val context = androidx.compose.ui.platform.LocalContext.current

    // Hero transition state
    var heroState by remember { mutableStateOf<HeroState>(HeroState.Idle) }
    var aiIconBounds by remember { mutableStateOf(HeroBounds()) }
    var previousTab by remember { mutableStateOf(Screen.NoteList.route) }
    val fluidityConfig by fluidityManager.config.collectAsState()
    val isHeroAnimating = heroState !is HeroState.Idle
    val heroBudgetOk = fluidityConfig.animationBudget != AnimationBudget.MINIMAL
    val effectiveShowBottomBar = showBottomBar || isHeroAnimating

    val backHandlerEnabled = showBottomBar && !isHeroAnimating
    BackHandler(enabled = backHandlerEnabled) {
        // On AI tab: back returns to previous tab
        if (currentRoute == Screen.AIHome.route) {
            navController.navigate(previousTab) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
            return@BackHandler
        }
        // On other root tabs: double-back to exit
        val now = SystemClock.elapsedRealtime()
        if (now - backPressTime < 2000) {
            (context as? android.app.Activity)?.finish()
        } else {
            backPressTime = now
            Toast.makeText(context, "再按一次退出应用", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(currentRoute) {
        currentRoute?.let { route ->
            val screen = when (route) {
                Screen.NoteList.route -> "notes"
                Screen.Files.route -> "files"
                Screen.AIHome.route -> "ai"
                else -> route
            }
            fluidityManager.onScreenChange(screen)
        }
    }

    Box(Modifier.fillMaxSize()) {
    val density = LocalDensity.current
    val config = LocalConfiguration.current
    val screenWidthPx = density.density * config.screenWidthDp
    val screenHeightPx = density.density * config.screenHeightDp

    Scaffold(
        bottomBar = {
            if (effectiveShowBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    BottomNavItem.entries.forEach { item ->
                        val icon: ImageVector = when (item) {
                            BottomNavItem.NOTES -> Icons.Default.EditNote
                            BottomNavItem.FILES -> Icons.Default.Folder
                            BottomNavItem.AI -> Icons.Default.AutoAwesome
                        }
                        val selected = currentRoute == item.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (currentRoute == item.route) return@NavigationBarItem
                                // 记录离开AI前的tab
                                if (currentRoute == Screen.AIHome.route) {
                                    previousTab = currentRoute ?: Screen.NoteList.route
                                }
                                // 统一导航逻辑：所有tab切换直接执行
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Box(
                                    modifier = if (item == BottomNavItem.AI) {
                                        Modifier.onGloballyPositioned { coords ->
                                            val pos = coords.positionInWindow()
                                            val size = coords.size
                                            val new = HeroBounds(pos.x, pos.y, size.width.toFloat(), size.height.toFloat())
                                            if (new != aiIconBounds) aiIconBounds = new
                                        }
                                    } else Modifier
                                ) {
                                    Icon(icon, contentDescription = item.label)
                                }
                            },
                            label = { Text(item.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.NoteList.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = {
                if (isHeroAnimating) fadeIn(tween(0)) else
                if (targetState.destination.route in tabRoutes && initialState.destination.route in tabRoutes) {
                    fadeIn(animationSpec = tween(tokens.fastDuration / 2, easing = StandardEasing))
                } else {
                    fadeIn(animationSpec = tween(tokens.normalDuration, easing = StandardEasing)) +
                        slideIntoContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.Start,
                            animationSpec = tween(tokens.normalDuration, easing = StandardEasing)
                        )
                }
            },
            exitTransition = {
                if (isHeroAnimating) fadeOut(tween(0)) else
                if (targetState.destination.route in tabRoutes && initialState.destination.route in tabRoutes) {
                    fadeOut(animationSpec = tween(tokens.fastDuration / 2, easing = StandardEasing))
                } else {
                    fadeOut(animationSpec = tween(tokens.fastDuration, easing = StandardEasing)) +
                        slideOutOfContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.Start,
                            animationSpec = tween(tokens.fastDuration, easing = StandardEasing)
                        )
                }
            },
            popEnterTransition = {
                if (isHeroAnimating) fadeIn(tween(0)) else
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = spring(
                        dampingRatio = tokens.springDamping,
                        stiffness = tokens.springStiffness
                    )
                ) + fadeIn(animationSpec = tween(tokens.normalDuration, easing = StandardEasing))
            },
            popExitTransition = {
                if (isHeroAnimating) fadeOut(tween(0)) else
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = tween(tokens.normalDuration, easing = StandardEasing)
                ) + fadeOut(animationSpec = tween(tokens.fastDuration, easing = StandardEasing))
            }
        ) {
            composable(Screen.NoteList.route) {
                NoteListScreen(
                    onNavigateToEdit = { noteId ->
                        navController.navigate(Screen.NoteEdit.createRoute(noteId))
                    },
                    onNavigateToTags = { navController.navigate(Screen.Tags.route) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
                )
            }

            composable(Screen.Files.route) {
                FileListScreen()
            }

            composable(
                route = Screen.AIModelSettings.route,
                enterTransition = { fadeIn(animationSpec = tween(tokens.fastDuration, easing = StandardEasing)) },
                exitTransition = { fadeOut(animationSpec = tween(tokens.fastDuration, easing = StandardEasing)) },
                popEnterTransition = { fadeIn(animationSpec = tween(tokens.fastDuration, easing = StandardEasing)) },
                popExitTransition = { fadeOut(animationSpec = tween(tokens.fastDuration, easing = StandardEasing)) }
            ) {
                AIModelSettingsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.AIHome.route,
                enterTransition = {
                    if (isHeroAnimating) fadeIn(tween(0))
                    else fadeIn(animationSpec = tween(tokens.normalDuration, easing = StandardEasing))
                },
                exitTransition = {
                    if (isHeroAnimating) fadeOut(tween(0))
                    else fadeOut(animationSpec = tween(tokens.normalDuration, easing = StandardEasing))
                },
                popEnterTransition = {
                    if (isHeroAnimating) fadeIn(tween(0))
                    else fadeIn(animationSpec = tween(tokens.normalDuration, easing = StandardEasing))
                },
                popExitTransition = {
                    if (isHeroAnimating) fadeOut(tween(0))
                    else fadeOut(animationSpec = tween(tokens.normalDuration, easing = StandardEasing))
                }
            ) {
                AIHomeScreen(
                    onNavigateToNoteChat = { noteId ->
                        navController.navigate(Screen.AIChat.createRoute(noteId))
                    },
                    onNavigateToModelSettings = {
                        navController.navigate(Screen.AIModelSettings.route)
                    },
                    onNavigateToMCPSkill = {
                        navController.navigate(Screen.MCPSkill.route)
                    },
                    onExitAI = {
                        navController.navigate(previousTab) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }

            composable(
                route = Screen.NoteEdit.route,
                arguments = listOf(navArgument("noteId") { type = NavType.LongType }),
                enterTransition = {
                    scaleIn(
                        animationSpec = spring(
                            dampingRatio = tokens.springDamping,
                            stiffness = tokens.springStiffness
                        ),
                        initialScale = 0.3f
                    ) + fadeIn(animationSpec = tween(tokens.fastDuration, easing = StandardEasing))
                },
                exitTransition = {
                    scaleOut(
                        animationSpec = tween(tokens.fastDuration, easing = StandardEasing),
                        targetScale = 0.3f
                    ) + fadeOut(animationSpec = tween(tokens.fastDuration, easing = StandardEasing))
                },
                popEnterTransition = {
                    fadeIn(animationSpec = tween(tokens.normalDuration, easing = StandardEasing))
                },
                popExitTransition = {
                    scaleOut(
                        animationSpec = spring(
                            dampingRatio = tokens.springDamping,
                            stiffness = tokens.springStiffness
                        ),
                        targetScale = 0.8f
                    ) + fadeOut(animationSpec = tween(tokens.fastDuration, easing = StandardEasing))
                }
            ) { entry ->
                val noteId = entry.arguments?.getLong("noteId") ?: -1L
                NoteEditScreen(
                    noteId = if (noteId == -1L) null else noteId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToAIChat = { id ->
                        navController.navigate(Screen.AIChat.createRoute(id))
                    }
                )
            }

            composable(
                route = Screen.AIChat.route,
                arguments = listOf(navArgument("noteId") { type = NavType.LongType }),
                enterTransition = {
                    scaleIn(
                        animationSpec = spring(
                            dampingRatio = tokens.springDamping,
                            stiffness = tokens.springStiffness
                        ),
                        initialScale = 0.3f
                    ) + fadeIn(animationSpec = tween(tokens.fastDuration, easing = StandardEasing))
                },
                exitTransition = {
                    scaleOut(
                        animationSpec = tween(tokens.fastDuration, easing = StandardEasing),
                        targetScale = 0.3f
                    ) + fadeOut(animationSpec = tween(tokens.fastDuration, easing = StandardEasing))
                },
                popEnterTransition = {
                    fadeIn(animationSpec = tween(tokens.normalDuration, easing = StandardEasing))
                },
                popExitTransition = {
                    scaleOut(
                        animationSpec = spring(
                            dampingRatio = tokens.springDamping,
                            stiffness = tokens.springStiffness
                        ),
                        targetScale = 0.8f
                    ) + fadeOut(animationSpec = tween(tokens.fastDuration, easing = StandardEasing))
                }
            ) { entry ->
                val noteId = entry.arguments?.getLong("noteId") ?: return@composable
                AIChatScreen(
                    noteId = noteId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.Tags.route,
                enterTransition = {
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Start,
                        animationSpec = tween(tokens.normalDuration, easing = StandardEasing)
                    ) + fadeIn(animationSpec = tween(tokens.normalDuration, easing = StandardEasing))
                },
                exitTransition = { fadeOut(animationSpec = tween(tokens.fastDuration, easing = StandardEasing)) },
                popEnterTransition = { fadeIn(animationSpec = tween(tokens.normalDuration, easing = StandardEasing)) },
                popExitTransition = {
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Start,
                        animationSpec = tween(tokens.normalDuration, easing = StandardEasing)
                    ) + fadeOut(animationSpec = tween(tokens.fastDuration, easing = StandardEasing))
                }
            ) {
                TagManageScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.Settings.route,
                enterTransition = {
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Start,
                        animationSpec = tween(tokens.normalDuration, easing = StandardEasing)
                    ) + fadeIn(animationSpec = tween(tokens.normalDuration, easing = StandardEasing))
                },
                exitTransition = { fadeOut(animationSpec = tween(tokens.fastDuration, easing = StandardEasing)) },
                popEnterTransition = { fadeIn(animationSpec = tween(tokens.normalDuration, easing = StandardEasing)) },
                popExitTransition = {
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Start,
                        animationSpec = tween(tokens.normalDuration, easing = StandardEasing)
                    ) + fadeOut(animationSpec = tween(tokens.fastDuration, easing = StandardEasing))
                }
            ) {
                SettingsScreen(
                    onNavigateBack = { navController.popBackStack() }
                    // Activation navigation temporarily removed
                )
            }

            composable(
                route = Screen.MCPSkill.route,
                enterTransition = {
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Start,
                        animationSpec = tween(tokens.normalDuration, easing = StandardEasing)
                    ) + fadeIn(animationSpec = tween(tokens.normalDuration, easing = StandardEasing))
                },
                exitTransition = { fadeOut(animationSpec = tween(tokens.fastDuration, easing = StandardEasing)) },
                popEnterTransition = { fadeIn(animationSpec = tween(tokens.normalDuration, easing = StandardEasing)) },
                popExitTransition = {
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Start,
                        animationSpec = tween(tokens.normalDuration, easing = StandardEasing)
                    ) + fadeOut(animationSpec = tween(tokens.fastDuration, easing = StandardEasing))
                }
            ) {
                MCPSkillManageScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }

    if (heroState !is HeroState.Idle) {
        HeroOverlay(
            state = heroState,
            screenWidthPx = screenWidthPx,
            screenHeightPx = screenHeightPx,
            animationSpeedMultiplier = fluidityConfig.animationSpeedMultiplier,
            onAnimationEnd = { heroState = HeroState.Idle }
        )
    }
    }
}
