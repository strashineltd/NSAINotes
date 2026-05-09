package com.nsai.notes.presentation.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
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
        Screen.AIHome.route,
        Screen.Files.route
    )

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

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
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
                        val iconScale by animateFloatAsState(
                            targetValue = if (selected) 1.15f else 1f,
                            animationSpec = spring(
                                dampingRatio = tokens.springDamping,
                                stiffness = tokens.springStiffness
                            ),
                            label = "navIconScale"
                        )
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (currentRoute != item.route && inputThrottler.shouldAllowNavigation(item.route)) {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(icon, contentDescription = item.label,
                                    modifier = Modifier.scale(iconScale))
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
                if (targetState.destination.route in tabRoutes && initialState.destination.route in tabRoutes) {
                    fadeIn(animationSpec = tween(tokens.fastDuration / 2))
                } else {
                    fadeIn(animationSpec = tween(tokens.normalDuration)) +
                        slideIntoContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.Start,
                            animationSpec = tween(tokens.normalDuration)
                        )
                }
            },
            exitTransition = {
                if (targetState.destination.route in tabRoutes && initialState.destination.route in tabRoutes) {
                    fadeOut(animationSpec = tween(tokens.fastDuration / 2))
                } else {
                    fadeOut(animationSpec = tween(tokens.fastDuration)) +
                        slideOutOfContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.Start,
                            animationSpec = tween(tokens.fastDuration)
                        )
                }
            },
            popEnterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = spring(
                        dampingRatio = tokens.springDamping,
                        stiffness = tokens.springStiffness
                    )
                ) + fadeIn(animationSpec = tween(tokens.normalDuration))
            },
            popExitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = tween(tokens.normalDuration)
                ) + fadeOut(animationSpec = tween(tokens.fastDuration))
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
                enterTransition = {
                    slideInVertically(
                        animationSpec = spring(
                            dampingRatio = tokens.springDamping,
                            stiffness = tokens.springStiffness
                        ),
                        initialOffsetY = { it / 6 }
                    ) + fadeIn(animationSpec = tween(tokens.normalDuration))
                },
                exitTransition = {
                    slideOutVertically(
                        animationSpec = tween(tokens.normalDuration),
                        targetOffsetY = { it / 8 }
                    ) + fadeOut(animationSpec = tween(tokens.fastDuration))
                },
                popEnterTransition = {
                    fadeIn(animationSpec = tween(tokens.normalDuration))
                },
                popExitTransition = {
                    slideOutVertically(
                        animationSpec = tween(tokens.normalDuration),
                        targetOffsetY = { it / 6 }
                    ) + fadeOut(animationSpec = tween(tokens.fastDuration))
                }
            ) {
                AIModelSettingsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.AIHome.route) {
                AIHomeScreen(
                    onNavigateToNoteChat = { noteId ->
                        navController.navigate(Screen.AIChat.createRoute(noteId))
                    },
                    onNavigateToModelSettings = {
                        navController.navigate(Screen.AIModelSettings.route)
                    },
                    onNavigateToMCPSkill = {
                        navController.navigate(Screen.MCPSkill.route)
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
                    ) + fadeIn(animationSpec = tween(tokens.fastDuration))
                },
                exitTransition = {
                    scaleOut(
                        animationSpec = tween(tokens.fastDuration),
                        targetScale = 0.3f
                    ) + fadeOut(animationSpec = tween(tokens.fastDuration))
                },
                popEnterTransition = {
                    fadeIn(animationSpec = tween(tokens.normalDuration))
                },
                popExitTransition = {
                    scaleOut(
                        animationSpec = spring(
                            dampingRatio = tokens.springDamping,
                            stiffness = tokens.springStiffness
                        ),
                        targetScale = 0.8f
                    ) + fadeOut(animationSpec = tween(tokens.fastDuration))
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
                arguments = listOf(navArgument("noteId") { type = NavType.LongType })
            ) { entry ->
                val noteId = entry.arguments?.getLong("noteId") ?: return@composable
                AIChatScreen(
                    noteId = noteId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Tags.route) {
                TagManageScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.MCPSkill.route) {
                MCPSkillManageScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
