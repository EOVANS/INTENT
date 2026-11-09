package com.intent.reader.ui

import android.os.Build
import com.intent.reader.R
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NamedNavArgument
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.intent.reader.IntentApplication
import com.intent.reader.MainActivity
import com.intent.reader.SharedViewTransition
import com.intent.reader.data.prefs.SettingsState
import com.intent.reader.ui.components.GlassTabBar
import com.intent.reader.ui.components.IntentTab
import com.intent.reader.ui.components.rememberGlassAppearance
import com.intent.reader.ui.components.rememberTabScrollBehavior
import com.intent.reader.ui.home.HomeScreen
import com.intent.reader.ui.home.HomeViewModel
import com.intent.reader.ui.onboarding.OnboardingScreen
import com.intent.reader.ui.reader.ReaderScreen
import com.intent.reader.ui.reader.ReaderViewModel
import com.intent.reader.ui.search.AllArticlesScreen
import com.intent.reader.ui.search.CategorySourcesScreen
import com.intent.reader.ui.search.SearchScreen
import com.intent.reader.ui.search.SearchViewModel
import com.intent.reader.ui.search.SourceDetailScreen
import com.intent.reader.ui.search.SourcesScreen
import com.intent.reader.ui.settings.DeveloperScreen
import com.intent.reader.ui.settings.AboutScreen
import com.intent.reader.ui.settings.AiSettingsScreen
import com.intent.reader.ui.settings.AccessibilityScreen
import com.intent.reader.ui.settings.AdvancedMaterialScreen
import com.intent.reader.ui.settings.NotificationScreen
import com.intent.reader.ui.settings.SettingsScreen
import com.intent.reader.ui.settings.IconSettingsScreen
import com.intent.reader.ui.settings.FontSettingsScreen
import com.intent.reader.ui.settings.WidgetStyleSettingsScreen
import com.intent.reader.ui.settings.LanguageSettingsScreen
import com.intent.reader.ui.settings.StorageSettingsScreen
import com.intent.reader.ui.settings.UiSettingsScreen
import com.intent.reader.ui.settings.ThemeSettingsScreen
import com.intent.reader.ui.settings.UserSettingsScreen
import com.intent.reader.ui.settings.PrivacyScreen
import com.intent.reader.ui.settings.WebPageScreen
import com.intent.reader.ui.theme.IntentTheme
import com.intent.reader.ui.theme.IntentDimens
import kotlinx.coroutines.launch

object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val SEARCH = "search"
    const val SETTINGS = "settings"
    const val READER = "reader/{articleId}"
    const val NOTES = "notes"
    const val ARTICLE_NOTES = "article_notes/{articleId}"
    fun notes(articleId: String) = "article_notes/${android.net.Uri.encode(articleId)}"
    fun reader(id: String) = "reader/${android.net.Uri.encode(id)}"
    const val SOURCES = "sources"
    const val ALL_ARTICLES = "all_articles"
    const val SOURCE_DETAIL = "source/{sourceId}"
    fun source(id: String) = "source/$id"
    const val SEARCH_CATEGORY = "search_category/{key}"
    fun category(key: String) = "search_category/$key"
    const val SETTINGS_UI = "settings_ui"
    const val SETTINGS_ICON = "settings_icon"
    const val SETTINGS_FONT = "settings_font"
    const val SETTINGS_WIDGET_STYLE = "settings_widget_style"
    const val SETTINGS_LANGUAGE = "settings_language"
    const val SETTINGS_STORAGE = "settings_storage"
    const val SETTINGS_THEME = "settings_theme"
    const val SETTINGS_A11Y = "settings_a11y"
    const val SETTINGS_NOTIFY = "settings_notify"
    const val SETTINGS_USER = "settings_user"
    const val SETTINGS_PRIVACY = "settings_privacy"
    const val SETTINGS_ADVANCED = "settings_advanced"
    const val SETTINGS_DEVELOPER = "settings_developer"
    const val SETTINGS_ABOUT = "settings_about"
    const val SETTINGS_AI = "settings_ai"
    const val SETTINGS_WEB = "settings_web/{title}/{url}"
    fun webPage(title: String, url: String) =
        "settings_web/${android.net.Uri.encode(title)}/${android.net.Uri.encode(url)}"
}

/** Fourth-order superellipse: curvature and its first derivative vanish at each straight edge. */
private class PredictivePageShape(private val radiusDp: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val r = (radiusDp * density.density).coerceIn(0f, minOf(size.width, size.height) / 2f)
        if (r == 0f) return Outline.Rectangle(androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height))
        val path = Path().apply {
            moveTo(r, 0f)
            lineTo(size.width - r, 0f)
            fun corner(cx: Float, cy: Float, start: Double) {
                // Sample the superellipse densely to keep the rendered outline smooth,
                // including on high-density displays while the radius is animating.
                for (i in 1..96) {
                    val angle = start + Math.PI / 2 * i / 96
                    val x = kotlin.math.cos(angle)
                    val y = kotlin.math.sin(angle)
                    lineTo(cx + r * kotlin.math.sign(x).toFloat() * kotlin.math.sqrt(kotlin.math.abs(x)).toFloat(),
                        cy + r * kotlin.math.sign(y).toFloat() * kotlin.math.sqrt(kotlin.math.abs(y)).toFloat())
                }
            }
            corner(size.width - r, r, -Math.PI / 2)
            lineTo(size.width, size.height - r)
            corner(size.width - r, size.height - r, 0.0)
            lineTo(r, size.height)
            corner(r, size.height - r, Math.PI / 2)
            lineTo(0f, r)
            corner(r, r, Math.PI)
            close()
        }
        return Outline.Generic(path)
    }
}

private const val ArticleTransitionDuration = 520
private const val PageTransitionDuration = 420
private val PageTransitionEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
private val ArticleTransitionEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

/**
 * Position of a destination in the tab bar, or -1 for anything that is not a top-level
 * tab (reader, a settings sub-page, …).
 */
private fun tabOrder(route: String?): Int = when (route) {
    Routes.HOME -> 0
    Routes.NOTES -> 1
    Routes.SEARCH -> 2
    else -> -1
}

/**
 * True when both ends are top-level tabs and we are moving towards a *lower* index
 * (搜索 → 笔记). `navigate()` has no sense of tab order, so without this every tap —
 * including a step backwards along the bar — played the same right-to-left slide.
 */
private fun AnimatedContentTransitionScope<NavBackStackEntry>.slidesBackwards(): Boolean {
    val from = tabOrder(initialState.destination.route)
    val to = tabOrder(targetState.destination.route)
    return to >= 0 && from > to
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun IntentNavHost(
    activity: MainActivity,
    standaloneArticleId: String? = null,
    standaloneNotes: Boolean = false,
    startOnOnboarding: Boolean,
    widthSizeClass: WindowWidthSizeClass,
    startArticleId: String? = null,
    onStartArticleConsumed: () -> Unit = {},
) {
    val context = LocalContext.current
    val app = context.applicationContext as IntentApplication
    val container = app.container
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()
    var sharedArticleId by remember { mutableStateOf<String?>(null) }
    var sharedNoteId by remember { mutableStateOf<String?>(null) }
    var kitLaunchPending by remember { mutableStateOf(false) }
    val kitSupported = remember { SharedViewTransition.isSupported() }

    // Widget deep-link: a tapped article id arrives after the NavHost is already
    // composed. Consume it once, verify the article still exists (retention may
    // have cleaned it up -> degrade gracefully to the home feed), then navigate.
    androidx.compose.runtime.LaunchedEffect(startArticleId) {
        val id = startArticleId ?: return@LaunchedEffect
        if (startOnOnboarding) { onStartArticleConsumed(); return@LaunchedEffect }
        val exists = try { container.database.articleDao().getById(id) != null }
        catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
        catch (_: Exception) { false }
        if (exists) navController.navigate(Routes.reader(id))
        onStartArticleConsumed()
    }
    val tabBehavior = rememberTabScrollBehavior()
    val settings by container.settingsRepository.settings
        .collectAsState(initial = SettingsState())
    // Bumped when the user re-taps the Home tab while already on Home; the feed
    // observes it and scrolls back to the greeting header.
    var homeScrollToTopTick by rememberSaveable { mutableIntStateOf(0) }

    val backStackEntry by navController.currentBackStackEntryAsState()
    // Use the *destination hierarchy* so any parent graph match also counts; this
    // is robust during transitions where the entry can briefly hold an
    // intermediate route. Reset the tab bar's transient hidden state whenever we
    // land back on a top-level destination (popBack / navigate home).
    val currentRoute = backStackEntry?.destination?.route
    val articleTransitionCorner by animateDpAsState(
        targetValue = if (currentRoute == Routes.READER || currentRoute == Routes.ARTICLE_NOTES) 0.dp else IntentDimens.radiusCard.dp,
        animationSpec = tween(ArticleTransitionDuration, easing = ArticleTransitionEasing),
        label = "article transition corner",
    )
    val currentTab = when (currentRoute) {
        Routes.HOME -> IntentTab.HOME
        Routes.NOTES -> IntentTab.NOTES
        Routes.SEARCH -> IntentTab.SEARCH
        else -> null
    }
    // Returning to a top-level tab must always make the bar visible again, even
    // if the previous scroll direction had collapsed it. The bar is pinned
    // visible on Home / Search / Settings (doc: pinned on main page).
    androidx.compose.runtime.LaunchedEffect(currentTab) {
        if (currentTab != null) tabBehavior.show()
    }
    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    // Tab bar is pinned on main pages; only the keyboard can hide it.
    val tabHidden = imeVisible
    val density = LocalDensity.current
    val bottomBarPad = with(density) {
        WindowInsets.navigationBars.getBottom(this).toDp() + 92.dp
    }

    val dark = IntentTheme.colors.isDark
    val transitionBackground = IntentTheme.colors.bg.toArgb()
    val liquidGlass = Build.VERSION.SDK_INT >= 33 && settings.glassEffectEnabled &&
        !tabHidden && currentTab != null
    // Keep the NavHost's recording layer stable while navigating. Attaching it
    // only when a tab becomes current recreates the layer in the middle of a pop.
    val backdrop = if (Build.VERSION.SDK_INT >= 33 && settings.glassEffectEnabled) rememberLayerBackdrop() else null
    val glassOverLight = rememberGlassAppearance(liquidGlass && settings.glassAdaptiveTint)
    androidx.compose.runtime.CompositionLocalProvider(
        com.intent.reader.ui.components.LocalHeaderMaterial provides com.intent.reader.ui.components.HeaderMaterial(
            liquidGlass = Build.VERSION.SDK_INT >= 33 && settings.glassEffectEnabled,
            clearMaterial = settings.glassTransparency > 0.72f,
            adaptiveTint = settings.glassAdaptiveTint,
            overLight = !dark,
            reduceMotion = settings.reduceAnimations,
            scrollEdgeBlur = settings.glassScrollEdgeBlur,
            transparency = settings.glassTransparency,
        ),
    ) {
    Box(
        Modifier
            .fillMaxSize()
            .nestedScroll(tabBehavior.connection)
    ) {
        SharedTransitionLayout {
        fun NavGraphBuilder.framedComposable(
            route: String,
            arguments: List<NamedNavArgument> = emptyList(),
            content: @Composable AnimatedVisibilityScope.(NavBackStackEntry) -> Unit,
        ) {
            composable(route, arguments = arguments) { entry ->
                val exiting by transition.animateFloat(
                    transitionSpec = { tween(PageTransitionDuration, easing = PageTransitionEasing) },
                    label = "page edge",
                ) { state ->
                    if (state == EnterExitState.PostExit) 1f else 0f
                }
                val sharedPage = !settings.reduceAnimations && when (entry.destination.route) {
                    Routes.READER -> sharedArticleId != null && entry.arguments?.getString("articleId") == sharedArticleId
                    Routes.ARTICLE_NOTES -> sharedNoteId != null && entry.arguments?.getString("articleId") == sharedNoteId
                    else -> false
                }
                val peerTabTransition = currentTab != null && tabOrder(entry.destination.route) >= 0
                // On a forward navigation the outgoing entry becomes the previous entry.
                // During a predictive pop it remains the current entry until the pop commits.
                val forwardExit = navController.previousBackStackEntry?.id == entry.id
                var gestureTransition by remember(entry) { mutableStateOf(false) }
                // NavHost prepares the previous entry before committing a predictive
                // pop. A normal navigation changes the current entry immediately.
                val predictiveUnderlay = forwardExit &&
                    transition.currentState != EnterExitState.Visible &&
                    transition.targetState == EnterExitState.Visible
                androidx.compose.runtime.SideEffect {
                    if (predictiveUnderlay) gestureTransition = true
                    else if (transition.currentState == transition.targetState) gestureTransition = false
                }
                val scrim by transition.animateFloat(
                    transitionSpec = { tween(PageTransitionDuration, easing = PageTransitionEasing) },
                    label = "page scrim",
                ) { state ->
                    when (state) {
                        EnterExitState.PreEnter -> 0.28f
                        EnterExitState.Visible -> 0f
                        EnterExitState.PostExit -> 0f
                    }
                }
                // Reach a substantial corner early in the gesture, then keep
                // rounding smoothly as the page travels off screen.
                val radius = if (sharedPage || forwardExit || peerTabTransition || settings.reduceAnimations) 0f
                    else 72f * kotlin.math.sqrt(exiting.coerceIn(0f, 1f))
                val shape = remember(radius) { PredictivePageShape(radius) }
                Box(Modifier.fillMaxSize().clip(shape).background(IntentTheme.colors.bg).drawWithContent {
                    drawContent()
                    if ((predictiveUnderlay || gestureTransition) && !sharedPage && !peerTabTransition && !settings.reduceAnimations) {
                        drawRect(Color.Black.copy(alpha = scrim.coerceIn(0f, 1f)))
                    }
                }.then(
                    if (radius > 0f) Modifier.border(0.5.dp, (if (dark) Color.White else Color.Black).copy(alpha = 0.18f * exiting), shape)
                    else Modifier
                )) { content(entry) }
            }
        }
        NavHost(
            navController = navController,
            startDestination = when {
                standaloneArticleId != null && standaloneNotes -> Routes.notes(standaloneArticleId)
                standaloneArticleId != null -> Routes.reader(standaloneArticleId)
                startOnOnboarding -> Routes.ONBOARDING
                else -> Routes.HOME
            },
            modifier = Modifier
                .fillMaxSize()
                // Isolate the NavHost's animated destination z-order in both
                // material modes, so it cannot cover the sibling tab bar.
                .then(if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier.graphicsLayer())
                .background(IntentTheme.colors.bg),
            // Opaque pages with independent scrims; full travel avoids disappearing mid-screen.
            // Paired with enableOnBackInvokedCallback=true for predictive back.
            // Top-level tabs are peers: crossfade instead of implying a page stack.
            enterTransition = {
                if (!settings.reduceAnimations && ((initialState.destination.route == Routes.HOME && targetState.destination.route == Routes.READER && sharedArticleId != null) || (initialState.destination.route == Routes.NOTES && targetState.destination.route == Routes.ARTICLE_NOTES && sharedNoteId != null))) {
                    fadeIn(tween(ArticleTransitionDuration, easing = ArticleTransitionEasing))
                } else if (tabOrder(initialState.destination.route) >= 0 && tabOrder(targetState.destination.route) >= 0) {
                    if (settings.reduceAnimations) androidx.compose.animation.EnterTransition.None else fadeIn(tween(180))
                } else {
                slideInHorizontally(
                    initialOffsetX = { if (slidesBackwards()) -it else it },
                    animationSpec = tween(PageTransitionDuration, easing = PageTransitionEasing),
                )
                }
            },
            exitTransition = {
                if (!settings.reduceAnimations && ((initialState.destination.route == Routes.HOME && targetState.destination.route == Routes.READER && sharedArticleId != null) || (initialState.destination.route == Routes.NOTES && targetState.destination.route == Routes.ARTICLE_NOTES && sharedNoteId != null))) {
                    fadeOut(tween(ArticleTransitionDuration, easing = ArticleTransitionEasing))
                } else if (tabOrder(initialState.destination.route) >= 0 && tabOrder(targetState.destination.route) >= 0) {
                    if (settings.reduceAnimations) androidx.compose.animation.ExitTransition.None else fadeOut(tween(180))
                } else {
                slideOutHorizontally(
                    targetOffsetX = { if (slidesBackwards()) it / 4 else -it / 4 },
                    animationSpec = tween(PageTransitionDuration, easing = PageTransitionEasing),
                )
                }
            },
            popEnterTransition = {
                if (tabOrder(initialState.destination.route) >= 0 && tabOrder(targetState.destination.route) >= 0) {
                    if (settings.reduceAnimations) androidx.compose.animation.EnterTransition.None else fadeIn(tween(180))
                } else if (!settings.reduceAnimations && ((initialState.destination.route == Routes.READER && targetState.destination.route == Routes.HOME && sharedArticleId != null) || (initialState.destination.route == Routes.ARTICLE_NOTES && targetState.destination.route == Routes.NOTES && sharedNoteId != null))) fadeIn(tween(ArticleTransitionDuration, easing = ArticleTransitionEasing)) else
                // Keep the revealed page opaque throughout the gesture. Fading it in
                // exposes the host background and can look like a flash on commit.
                slideInHorizontally(initialOffsetX = { -it / 4 }, animationSpec = tween(PageTransitionDuration, easing = PageTransitionEasing))
            },
            popExitTransition = {
                if (tabOrder(initialState.destination.route) >= 0 && tabOrder(targetState.destination.route) >= 0) {
                    if (settings.reduceAnimations) androidx.compose.animation.ExitTransition.None else fadeOut(tween(180))
                } else if (!settings.reduceAnimations && ((initialState.destination.route == Routes.READER && targetState.destination.route == Routes.HOME && sharedArticleId != null) || (initialState.destination.route == Routes.ARTICLE_NOTES && targetState.destination.route == Routes.NOTES && sharedNoteId != null))) fadeOut(tween(ArticleTransitionDuration, easing = ArticleTransitionEasing)) else
                slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(PageTransitionDuration, easing = PageTransitionEasing))
            },
        ) {
            framedComposable(Routes.ONBOARDING) {
                OnboardingScreen(onFinish = { name ->
                    scope.launch {
                        container.settingsRepository.setUserName(name)
                        container.settingsRepository.setOnboardingVersion(1)
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.ONBOARDING) { inclusive = true }
                        }
                    }
                })
            }

            framedComposable(Routes.HOME) {
                val vm: HomeViewModel = viewModel(initializer = { HomeViewModel(app) })
                val state by vm.state.collectAsState()
                val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                        when (event) {
                            androidx.lifecycle.Lifecycle.Event.ON_RESUME -> vm.setHomeForeground(true)
                            androidx.lifecycle.Lifecycle.Event.ON_PAUSE -> vm.setHomeForeground(false)
                            else -> Unit
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    vm.setHomeForeground(lifecycleOwner.lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED))
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                        vm.setHomeForeground(false)
                    }
                }
                Box(
                    Modifier
                        .fillMaxSize()
                ) {
                    HomeScreen(
                        state = state,
                        widthClass = widthSizeClass,
                        onRefresh = { vm.refresh(context) },
                        onOpenArticle = { sharedArticleId = null; navController.navigate(Routes.reader(it)) },
                        onOpenCard = { id ->
                            if (!kitLaunchPending) {
                                if (kitSupported && !settings.reduceAnimations) {
                                    kitLaunchPending = true
                                    SharedViewTransition.open(
                                        activity, id,
                                        with(density) { IntentDimens.radiusCard.dp.toPx() },
                                        with(density) { 80.dp.roundToPx() },
                                        with(density) { bottomBarPad.roundToPx() },
                                        backgroundColor = transitionBackground,
                                    ) { opened ->
                                        kitLaunchPending = false
                                        if (!opened && !activity.isFinishing && !activity.isDestroyed) {
                                            sharedArticleId = id
                                            navController.navigate(Routes.reader(id))
                                        }
                                    }
                                } else {
                                    sharedArticleId = id
                                    navController.navigate(Routes.reader(id))
                                }
                            }
                        },
                        cardTransitionModifier = { id ->
                            if (kitSupported) {
                                DisposableEffect(id) { onDispose { SharedViewTransition.removeTarget(id) } }
                            }
                            val tracking = if (kitSupported) Modifier.onGloballyPositioned { coordinates ->
                                val position = coordinates.localToWindow(Offset.Zero)
                                val window = android.graphics.Rect(
                                    kotlin.math.round(position.x).toInt(), kotlin.math.round(position.y).toInt(),
                                    kotlin.math.round(position.x).toInt() + coordinates.size.width,
                                    kotlin.math.round(position.y).toInt() + coordinates.size.height,
                                )
                                val decor = activity.window.decorView
                                val onScreen = IntArray(2)
                                val inWindow = IntArray(2)
                                decor.getLocationOnScreen(onScreen)
                                decor.getLocationInWindow(inWindow)
                                val screen = android.graphics.Rect(window).apply {
                                    offset(onScreen[0] - inWindow[0], onScreen[1] - inWindow[1])
                                }
                                SharedViewTransition.updateTarget(id, window, screen, decor.width, decor.height)
                            } else Modifier
                            if (!settings.reduceAnimations) tracking.sharedBounds(
                                sharedContentState = rememberSharedContentState(key = "article-$id"),
                                animatedVisibilityScope = this@framedComposable,
                                boundsTransform = { _, _ -> tween(ArticleTransitionDuration, easing = ArticleTransitionEasing) },
                            ).clip(RoundedCornerShape(articleTransitionCorner)) else tracking
                        },
                        onSetRead = { id, read -> vm.setRead(id, read) },
                        onOpenSource = { navController.navigate(Routes.source(it)) },
                        onRefreshAiQuote = { vm.refreshAiQuote(regenerate = true) },
                        onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                        contentPadding = PaddingValues(bottom = bottomBarPad),
                        scrollToTopTick = homeScrollToTopTick,
                    )
                }
            }

            framedComposable(Routes.NOTES) {
                com.intent.reader.ui.reader.NotesScreen(
                    articleId = null,
                    popupBackdrop = backdrop,
                    userName = settings.userName,
                    contentPadding = PaddingValues(bottom = bottomBarPad),
                    onOpenArticleNotes = { id ->
                        if (!kitLaunchPending) {
                            if (kitSupported && !settings.reduceAnimations) {
                                kitLaunchPending = true
                                SharedViewTransition.open(
                                    activity, id,
                                    with(density) { IntentDimens.radiusCard.dp.toPx() },
                                    with(density) { 80.dp.roundToPx() },
                                    with(density) { bottomBarPad.roundToPx() },
                                    backgroundColor = transitionBackground,
                                    isNote = true,
                                ) { opened ->
                                    kitLaunchPending = false
                                    if (!opened && !activity.isFinishing && !activity.isDestroyed) {
                                        sharedNoteId = id
                                        navController.navigate(Routes.notes(id))
                                    }
                                }
                            } else {
                                sharedNoteId = id
                                navController.navigate(Routes.notes(id))
                            }
                        }
                    },
                    cardTransitionModifier = { id ->
                        val targetKey = "note:$id"
                        if (kitSupported) {
                            DisposableEffect(targetKey) { onDispose { SharedViewTransition.removeTarget(targetKey) } }
                        }
                        val tracking = if (kitSupported) Modifier.onGloballyPositioned { coordinates ->
                            val position = coordinates.localToWindow(Offset.Zero)
                            val window = android.graphics.Rect(
                                kotlin.math.round(position.x).toInt(), kotlin.math.round(position.y).toInt(),
                                kotlin.math.round(position.x).toInt() + coordinates.size.width,
                                kotlin.math.round(position.y).toInt() + coordinates.size.height,
                            )
                            val decor = activity.window.decorView
                            val onScreen = IntArray(2)
                            val inWindow = IntArray(2)
                            decor.getLocationOnScreen(onScreen)
                            decor.getLocationInWindow(inWindow)
                            val screen = android.graphics.Rect(window).apply {
                                offset(onScreen[0] - inWindow[0], onScreen[1] - inWindow[1])
                            }
                            SharedViewTransition.updateTarget(targetKey, window, screen, decor.width, decor.height)
                        } else Modifier
                        if (!settings.reduceAnimations) tracking.sharedBounds(
                            sharedContentState = rememberSharedContentState(key = "note-$id"),
                            animatedVisibilityScope = this@framedComposable,
                            boundsTransform = { _, _ -> tween(ArticleTransitionDuration, easing = ArticleTransitionEasing) },
                        ).clip(RoundedCornerShape(articleTransitionCorner)) else tracking
                    },
                )
            }

            framedComposable(Routes.ARTICLE_NOTES, arguments = listOf(navArgument("articleId") { type = NavType.StringType })) { entry ->
                val articleId = entry.arguments?.getString("articleId")
                com.intent.reader.ui.reader.NotesScreen(
                    articleId = articleId,
                    popupBackdrop = backdrop,
                    modifier = if (!settings.reduceAnimations && sharedNoteId == articleId && articleId != null) Modifier.sharedBounds(
                        sharedContentState = rememberSharedContentState(key = "note-$articleId"),
                        animatedVisibilityScope = this@framedComposable,
                        boundsTransform = { _, _ -> tween(ArticleTransitionDuration, easing = ArticleTransitionEasing) },
                    ).clip(RoundedCornerShape(articleTransitionCorner)) else Modifier,
                    onBack = { if (standaloneNotes && standaloneArticleId != null) activity.finish() else navController.popBackStack() },
                )
            }

            framedComposable(Routes.SEARCH) {
                val vm: SearchViewModel = viewModel(initializer = { SearchViewModel(app) })
                val state by vm.state.collectAsState()
                SearchScreen(
                    state = state,
                    onBack = { navController.popBackStack() },
                    onQueryChange = vm::onQueryChange,
                    onAddSource = vm::addSource,
                    onOpenCategory = { navController.navigate(Routes.category(it)) },
                    onOpenAllArticles = { navController.navigate(Routes.ALL_ARTICLES) },
                    onOpenSources = { navController.navigate(Routes.SOURCES) },
                    onOpenSource = { navController.navigate(Routes.source(it)) },
                    onOpenArticle = { navController.navigate(Routes.reader(it)) },
                    contentPadding = PaddingValues(bottom = bottomBarPad),
                )
            }

            framedComposable(Routes.SEARCH_CATEGORY, arguments = listOf(
                navArgument("key") { type = NavType.StringType },
            )) { backStackEntry ->
                val vm: SearchViewModel = viewModel(initializer = { SearchViewModel(app) })
                val state by vm.state.collectAsState()
                CategorySourcesScreen(
                    categoryKey = backStackEntry.arguments?.getString("key").orEmpty(),
                    sources = state.sources,
                    onBack = { navController.popBackStack() },
                    onToggle = vm::toggleRecommended,
                    onOpenSource = { navController.navigate(Routes.source(it)) },
                )
            }

            framedComposable(Routes.SETTINGS) {
                // No viewport padding here: the content scrolls under the
                // translucent tab bar and SettingsScaffold reserves the bar's
                // footprint at the end of its scroll content. Padding the whole
                // viewport on top of that left a dead band at the bottom.
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                    onOpenUi = { navController.navigate(Routes.SETTINGS_UI) },
                    onOpenA11y = { navController.navigate(Routes.SETTINGS_A11Y) },
                    onOpenNotify = { navController.navigate(Routes.SETTINGS_NOTIFY) },
                    onOpenUser = { navController.navigate(Routes.SETTINGS_USER) },
                    onOpenPrivacy = { navController.navigate(Routes.SETTINGS_PRIVACY) },
                    onOpenAdvanced = { navController.navigate(Routes.SETTINGS_ADVANCED) },
                    onOpenAbout = { navController.navigate(Routes.SETTINGS_ABOUT) },
                    onOpenAi = { navController.navigate(Routes.SETTINGS_AI) },
                    onOpenStorage = { navController.navigate(Routes.SETTINGS_STORAGE) },
                )
            }

            framedComposable(Routes.SETTINGS_STORAGE) {
                StorageSettingsScreen(onBack = { navController.popBackStack() })
            }

            framedComposable(Routes.SETTINGS_AI) {
                AiSettingsScreen(onBack = { navController.popBackStack() }, popupBackdrop = backdrop)
            }

            framedComposable(Routes.SETTINGS_DEVELOPER) {
                DeveloperScreen(onBack = { navController.popBackStack() })
            }

            framedComposable(Routes.SETTINGS_ABOUT) {
                AboutScreen(
                    onOpenDeveloper = { navController.navigate(Routes.SETTINGS_DEVELOPER) },
                    onBack = { navController.popBackStack() },
                    onOpenPage = { title, url ->
                        navController.navigate(Routes.webPage(title, url))
                    },
                )
            }

            framedComposable(
                Routes.SETTINGS_WEB,
                arguments = listOf(
                    navArgument("title") { type = NavType.StringType },
                    navArgument("url") { type = NavType.StringType },
                ),
            ) { entry ->
                WebPageScreen(
                    title = entry.arguments?.getString("title") ?: "",
                    url = entry.arguments?.getString("url") ?: "",
                    onBack = { navController.popBackStack() },
                )
            }

            framedComposable(
                Routes.READER,
                arguments = listOf(navArgument("articleId") { type = NavType.StringType }),
            ) { entry ->
                val articleId = entry.arguments?.getString("articleId") ?: ""
                val vm: ReaderViewModel = viewModel(
                    key = "reader_$articleId",
                    initializer = { ReaderViewModel(app).also { it.load(articleId) } },
                )
                val state by vm.state.collectAsState()
                // Reading time accrues only while this destination is actually
                // resumed. ON_PAUSE fires when the user navigates away or the
                // app goes to the background, so neither counts as reading.
                val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner, articleId, state.article?.id) {
                    val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                        when (event) {
                            androidx.lifecycle.Lifecycle.Event.ON_RESUME ->
                                vm.setReadingForeground(state.article != null)
                            androidx.lifecycle.Lifecycle.Event.ON_PAUSE,
                            androidx.lifecycle.Lifecycle.Event.ON_STOP ->
                                vm.setReadingForeground(false)
                            else -> Unit
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    if (lifecycleOwner.lifecycle.currentState
                            .isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)
                    ) {
                        vm.setReadingForeground(state.article != null)
                    }
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                        vm.setReadingForeground(false)
                    }
                }
                ReaderScreen(
                    state = state,
                    modifier = if (!settings.reduceAnimations && sharedArticleId == articleId) Modifier.sharedBounds(
                        sharedContentState = rememberSharedContentState(key = "article-$articleId"),
                        animatedVisibilityScope = this@framedComposable,
                        boundsTransform = { _, _ -> tween(ArticleTransitionDuration, easing = ArticleTransitionEasing) },
                    ).clip(RoundedCornerShape(articleTransitionCorner)) else Modifier,
                    onBack = { if (standaloneArticleId != null) activity.finish() else navController.popBackStack() },
                    onOpenInBrowser = { vm.openInBrowser(context, it) },
                    onShare = { url ->
                        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(android.content.Intent.EXTRA_TEXT, url)
                        }
                        context.startActivity(android.content.Intent.createChooser(intent, context.getString(R.string.reader_share)))
                    },
                    onAiSummary = {
                        if (state.aiEnabled) vm.summarize()
                        else navController.navigate(Routes.SETTINGS_AI)
                    },
                    onRegenerateSummary = { vm.summarize(regenerate = true) },
                    onDismissSummary = vm::dismissSummary,
                    onOpenNotes = { navController.navigate(Routes.notes(articleId)) },
                    onSaveNote = vm::saveNote,
                    onDeleteNote = vm::deleteNote,
                )
            }

            framedComposable(Routes.SOURCES) {
                val sources by container.sourceRepository.observeSources()
                    .collectAsState(initial = emptyList())
                val unread by produceState<Map<String, Int>>(emptyMap(), sources) {
                    value = container.database.articleDao().unreadBySource()
                        .associate { it.sourceId to it.cnt }
                }
                SourcesScreen(
                    sources = sources,
                    unreadBySource = unread,
                    onBack = { navController.popBackStack() },
                    onOpenSource = { navController.navigate(Routes.source(it)) },
                    onRemove = { id ->
                        scope.launch { container.sourceRepository.remove(id) }
                    },
                )
            }

            framedComposable(Routes.ALL_ARTICLES) {
                val articles by container.database.articleDao().observeRecent(2000)
                    .collectAsState(initial = emptyList())
                val sources by container.sourceRepository.observeSources()
                    .collectAsState(initial = emptyList())
                AllArticlesScreen(
                    articles = articles,
                    sources = sources.associateBy { it.id },
                    onBack = { navController.popBackStack() },
                    onOpenArticle = { navController.navigate(Routes.reader(it)) },
                    onSetRead = { id, read ->
                        scope.launch { container.database.articleDao().setRead(id, read) }
                    },
                )
            }

            framedComposable(
                Routes.SOURCE_DETAIL,
                arguments = listOf(navArgument("sourceId") { type = NavType.StringType }),
            ) { entry ->
                val sourceId = entry.arguments?.getString("sourceId") ?: ""
                val source by produceState<com.intent.reader.data.db.Source?>(null, sourceId) {
                    value = container.sourceRepository.getById(sourceId)
                }
                val articles by container.database.articleDao().observeBySource(sourceId)
                    .collectAsState(initial = emptyList())
                SourceDetailScreen(
                    source = source,
                    articles = articles,
                    onBack = { navController.popBackStack() },
                    onOpenArticle = { navController.navigate(Routes.reader(it)) },
                )
            }

            framedComposable(Routes.SETTINGS_UI) {
                UiSettingsScreen(
                    onBack = { navController.popBackStack() },
                    onOpenIcon = { navController.navigate(Routes.SETTINGS_ICON) },
                    onOpenFont = { navController.navigate(Routes.SETTINGS_FONT) },
                    onOpenWidget = { navController.navigate(Routes.SETTINGS_WIDGET_STYLE) },
                    onOpenLanguage = { navController.navigate(Routes.SETTINGS_LANGUAGE) },
                    onOpenTheme = { navController.navigate(Routes.SETTINGS_THEME) },
                )
            }

            framedComposable(Routes.SETTINGS_ICON) {
                IconSettingsScreen(onBack = { navController.popBackStack() })
            }
            framedComposable(Routes.SETTINGS_FONT) {
                FontSettingsScreen(onBack = { navController.popBackStack() })
            }
            framedComposable(Routes.SETTINGS_WIDGET_STYLE) {
                WidgetStyleSettingsScreen(onBack = { navController.popBackStack() })
            }
            framedComposable(Routes.SETTINGS_LANGUAGE) {
                LanguageSettingsScreen(onBack = { navController.popBackStack() }, onRecreate = { (context as? android.app.Activity)?.recreate() })
            }

            framedComposable(Routes.SETTINGS_THEME) {
                ThemeSettingsScreen(onBack = { navController.popBackStack() })
            }

            framedComposable(Routes.SETTINGS_A11Y) {
                AccessibilityScreen(onBack = { navController.popBackStack() })
            }

            framedComposable(Routes.SETTINGS_NOTIFY) {
                NotificationScreen(onBack = { navController.popBackStack() })
            }

            framedComposable(Routes.SETTINGS_USER) {
                UserSettingsScreen(onBack = { navController.popBackStack() })
            }

            framedComposable(Routes.SETTINGS_PRIVACY) {
                PrivacyScreen(onBack = { navController.popBackStack() })
            }

            framedComposable(Routes.SETTINGS_ADVANCED) {
                AdvancedMaterialScreen(onBack = { navController.popBackStack() })
            }
        }

        } // SharedTransitionLayout

        if (currentTab != null) {
            Box(Modifier.align(Alignment.BottomCenter)) {
            GlassTabBar(
                selected = currentTab,
                onSelect = { tab ->
                    val route = when (tab) {
                        IntentTab.HOME -> Routes.HOME
                        IntentTab.NOTES -> Routes.NOTES
                        IntentTab.SEARCH -> Routes.SEARCH
                    }
                    if (currentRoute != route) {
                        navController.navigate(route) {
                            popUpTo(Routes.HOME) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    } else if (tab == IntentTab.HOME) {
                        // Re-tapping the active Home tab scrolls the feed to top.
                        homeScrollToTopTick++
                    }
                },
                modifier = Modifier,
                hidden = tabHidden,
                liquidGlass = liquidGlass,
                backdrop = backdrop,
                clearMaterial = settings.glassTransparency > 0.72f,
                adaptiveTint = settings.glassAdaptiveTint,
                overLight = glassOverLight,
                transparency = settings.glassTransparency,
                reduceMotion = settings.reduceAnimations,
            )
            }
        }
    }
}
}
