package com.intent.reader.ui

import android.os.Build
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
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
import com.intent.reader.data.prefs.SettingsState
import com.intent.reader.ui.components.GlassLensParams
import com.intent.reader.ui.components.GlassMaterial
import com.intent.reader.ui.components.GlassShape
import com.intent.reader.ui.components.GlassTabBar
import com.intent.reader.ui.components.IntentTab
import com.intent.reader.ui.components.ScrollEdge
import com.intent.reader.ui.components.liquidGlassLens
import com.intent.reader.ui.components.rememberGlassAppearance
import com.intent.reader.ui.components.rememberTabScrollBehavior
import com.intent.reader.ui.components.scrollEdgeBlur
import com.intent.reader.ui.home.HomeScreen
import com.intent.reader.ui.home.HomeViewModel
import com.intent.reader.ui.onboarding.OnboardingScreen
import com.intent.reader.ui.reader.ReaderScreen
import com.intent.reader.ui.reader.ReaderViewModel
import com.intent.reader.ui.search.AllArticlesScreen
import com.intent.reader.ui.search.SearchScreen
import com.intent.reader.ui.search.SearchViewModel
import com.intent.reader.ui.search.SourceDetailScreen
import com.intent.reader.ui.search.SourcesScreen
import com.intent.reader.ui.settings.AboutScreen
import com.intent.reader.ui.settings.AccessibilityScreen
import com.intent.reader.ui.settings.AdvancedMaterialScreen
import com.intent.reader.ui.settings.NotificationScreen
import com.intent.reader.ui.settings.SettingsScreen
import com.intent.reader.ui.settings.UiSettingsScreen
import com.intent.reader.ui.settings.UserSettingsScreen
import com.intent.reader.ui.settings.WebPageScreen
import com.intent.reader.ui.theme.IntentTheme
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val SEARCH = "search"
    const val SETTINGS = "settings"
    const val READER = "reader/{articleId}"
    fun reader(id: String) = "reader/$id"
    const val SOURCES = "sources"
    const val ALL_ARTICLES = "all_articles"
    const val SOURCE_DETAIL = "source/{sourceId}"
    fun source(id: String) = "source/$id"
    const val SETTINGS_UI = "settings_ui"
    const val SETTINGS_A11Y = "settings_a11y"
    const val SETTINGS_NOTIFY = "settings_notify"
    const val SETTINGS_USER = "settings_user"
    const val SETTINGS_ADVANCED = "settings_advanced"
    const val SETTINGS_ABOUT = "settings_about"
    const val SETTINGS_WEB = "settings_web/{title}/{url}"
    fun webPage(title: String, url: String) =
        "settings_web/${android.net.Uri.encode(title)}/${android.net.Uri.encode(url)}"
}

/**
 * Position of a destination in the tab bar, or -1 for anything that is not a top-level
 * tab (reader, a settings sub-page, …).
 */
private fun tabOrder(route: String?): Int = when (route) {
    Routes.HOME -> 0
    Routes.SEARCH -> 1
    Routes.SETTINGS -> 2
    else -> -1
}

/**
 * True when both ends are top-level tabs and we are moving towards a *lower* index
 * (设置 → 搜索). `navigate()` has no sense of tab order, so without this every tap —
 * including a step backwards along the bar — played the same right-to-left slide.
 */
private fun AnimatedContentTransitionScope<NavBackStackEntry>.slidesBackwards(): Boolean {
    val from = tabOrder(initialState.destination.route)
    val to = tabOrder(targetState.destination.route)
    return to >= 0 && from > to
}

@Composable
fun IntentNavHost(
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

    // Widget deep-link: a tapped article id arrives after the NavHost is already
    // composed. Consume it once, verify the article still exists (retention may
    // have cleaned it up -> degrade gracefully to the home feed), then navigate.
    androidx.compose.runtime.LaunchedEffect(startArticleId) {
        val id = startArticleId ?: return@LaunchedEffect
        onStartArticleConsumed()
        if (startOnOnboarding) return@LaunchedEffect
        val exists = runCatching {
            container.database.articleDao().getById(id) != null
        }.getOrDefault(false)
        if (exists) navController.navigate(Routes.reader(id))
    }
    val tabBehavior = rememberTabScrollBehavior()
    val settings by container.settingsRepository.settings
        .collectAsState(initial = SettingsState())

    val backStackEntry by navController.currentBackStackEntryAsState()
    // Use the *destination hierarchy* so any parent graph match also counts; this
    // is robust during transitions where the entry can briefly hold an
    // intermediate route. Reset the tab bar's transient hidden state whenever we
    // land back on a top-level destination (popBack / navigate home).
    val currentRoute = backStackEntry?.destination?.route
    val currentTab = when (currentRoute) {
        Routes.HOME -> IntentTab.HOME
        Routes.SEARCH -> IntentTab.SEARCH
        Routes.SETTINGS -> IntentTab.SETTINGS
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

    // 通透玻璃效果: the AGSL lens pipeline needs Android 13 (API 33) and runs on the
    // content layer behind the pill, so the pill's on-screen bounds feed the shader.
    val dark = IntentTheme.colors.isDark
    // The lens shape is driven by the *reported* pill bounds, so it must also be
    // switched off on destinations without the bar — otherwise the last reported
    // bounds keep painting a stale glass capsule where the bar used to be.
    val liquidGlass = Build.VERSION.SDK_INT >= 33 && settings.glassEffectEnabled &&
        !tabHidden && currentTab != null
    var pillBounds by remember { mutableStateOf(Rect.Zero) }
    var pressPoint by remember { mutableStateOf(Offset.Zero) }
    var pressed by remember { mutableStateOf(false) }

    val glassClear = settings.glassMaterial == "clear"
    // 亮度自适应 must stay active in *both* materials: it is what keeps a light page's
    // glass transparent instead of grey, and a dark page's glass lifted out of black.
    val glassAdaptiveTint = settings.glassAdaptiveTint
    // 亮度自适应: the backdrop's overall brightness keeps the tint (and the light/dark
    // reading of the surface) in sync with whatever scrolls underneath the pill.
    val glassOverLight = rememberGlassAppearance(liquidGlass)
    val pressAnim by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = tween(durationMillis = 180),
        label = "glassPress",
    )
    // Quantised so the shader is rebuilt a handful of times per gesture instead of
    // once per animation frame; the optical difference between steps is invisible.
    val pressStep = (pressAnim * 4f).roundToInt() / 4f

    val lensParams = if (liquidGlass && pillBounds.width > 0f) {
        val halfH = pillBounds.height * 0.5f
        GlassLensParams(
            blurRadius = pillBounds.height * 0.12f,
            primary = GlassShape(
                centerX = pillBounds.center.x,
                centerY = pillBounds.center.y,
                halfWidth = pillBounds.width * 0.5f,
                halfHeight = halfH,
                cornerRadius = halfH,
            ),
            // 液态融合: a second glass blob is smooth-min'd (smin) into the pill. It is
            // only fed in *while pressed*, as a small disc under the finger — a
            // permanently fused selected-tab capsule leaves an internal ridge across the
            // bar (the smin crease is visible even when the shape is small), which reads
            // as a muddy smudge rather than glass.
            secondary = if (pressStep > 0.01f) {
                val r = pillBounds.height * 0.34f
                GlassShape(
                    centerX = pressPoint.x,
                    centerY = pressPoint.y,
                    halfWidth = r,
                    halfHeight = r,
                    cornerRadius = r,
                )
            } else {
                null
            },
            // Blend width kept well below the capsule's gap to the pill outline so the
            // bulge stays a local blob instead of deforming the pill silhouette.
            smoothing = with(density) { 16.dp.toPx() },
            material = if (glassClear) GlassMaterial.CLEAR else GlassMaterial.REGULAR,
            adaptiveTint = glassAdaptiveTint,
            // Thickness/refraction trimmed from 0.45/0.42 → 0.30/0.34 of the pill height:
            // a thinner bevel keeps the compressed background legible instead of turning
            // the whole rim into a smear band, which is what made the glass look “灰”.
            bevel = pillBounds.height * 0.30f,
            refract = pillBounds.height * 0.34f,
            dispersion = 0.12f,
            spec = if (dark) 0.30f else 0.45f,
            saturation = if (glassClear) 100f else 115f,
            press = pressStep,
            touchX = pressPoint.x,
            touchY = pressPoint.y,
            touchAmp = if (pressed) 1f else 0f,
            isOverLight = glassOverLight,
        )
    } else {
        null
    }

    Box(
        Modifier
            .fillMaxSize()
            .nestedScroll(tabBehavior.connection)
    ) {
        // The lens samples whatever this layer paints. Screens are padded to clear the
        // tab bar, so the strip behind the pill is *unpainted* — the layer there is fully
        // transparent, and the shader (which reads premultiplied RGB) turns that hole
        // into an opaque dark pill. An opaque base inside the layer restores the real
        // backdrop.
        NavHost(
            navController = navController,
            startDestination = if (startOnOnboarding) Routes.ONBOARDING else Routes.HOME,
            modifier = Modifier
                .fillMaxSize()
                // Order matters: `background` must be *inner* to the lens, otherwise it
                // is drawn outside the captured layer and the shader still sees a hole.
                // It draws before the children, so the UI still appears over it.
                .liquidGlassLens(lensParams)
                .background(if (dark) Color.Black else Color.White),
            // Native-style transitions: slide horizontally + crossfade.
            // Paired with enableOnBackInvokedCallback=true for predictive back.
            // Direction follows the tab bar order: 设置 → 搜索 slides the new page in
            // from the left, 搜索 → 设置 (and every non-tab push) from the right.
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { if (slidesBackwards()) -it / 4 else it / 4 },
                    animationSpec = tween(280),
                ) + fadeIn(animationSpec = tween(280))
            },
            exitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { if (slidesBackwards()) it / 4 else -it / 4 },
                    animationSpec = tween(280),
                ) + fadeOut(animationSpec = tween(280))
            },
            popEnterTransition = {
                slideInHorizontally(initialOffsetX = { -it / 4 }, animationSpec = tween(280)) +
                    fadeIn(animationSpec = tween(280))
            },
            popExitTransition = {
                slideOutHorizontally(targetOffsetX = { it / 4 }, animationSpec = tween(280)) +
                    fadeOut(animationSpec = tween(280))
            },
        ) {
            composable(Routes.ONBOARDING) {
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

            composable(Routes.HOME) {
                val vm: HomeViewModel = viewModel(initializer = { HomeViewModel(app) })
                val state by vm.state.collectAsState()
                // 🌫️ 渐进模糊: the feed's top edge blurs progressively into the status
                // bar instead of being cut off by a hard gradient scrim.
                Box(
                    Modifier
                        .fillMaxSize()
                        .then(
                            if (liquidGlass && settings.glassScrollEdgeBlur) {
                                Modifier.scrollEdgeBlur(
                                    edge = ScrollEdge.TOP,
                                    bandHeight = 72.dp,
                                    maxBlurRadius = 26.dp,
                                )
                            } else {
                                Modifier
                            }
                        )
                ) {
                    HomeScreen(
                        state = state,
                        widthClass = widthSizeClass,
                        onRefresh = { vm.refresh(context) },
                        onOpenArticle = { navController.navigate(Routes.reader(it)) },
                        onSetRead = { id, read -> vm.setRead(id, read) },
                        onOpenSource = { navController.navigate(Routes.source(it)) },
                        contentPadding = PaddingValues(bottom = bottomBarPad),
                    )
                }
            }

            composable(Routes.SEARCH) {
                val vm: SearchViewModel = viewModel(initializer = { SearchViewModel(app) })
                val state by vm.state.collectAsState()
                Box(Modifier.padding(bottom = bottomBarPad)) {
                    SearchScreen(
                        state = state,
                        onQueryChange = vm::onQueryChange,
                        onAddSource = vm::addSource,
                        onAddRecommended = vm::toggleRecommended,
                        onOpenAllArticles = { navController.navigate(Routes.ALL_ARTICLES) },
                        onOpenSources = { navController.navigate(Routes.SOURCES) },
                        onOpenSource = { navController.navigate(Routes.source(it)) },
                        onOpenArticle = { navController.navigate(Routes.reader(it)) },
                    )
                }
            }

            composable(Routes.SETTINGS) {
                // No viewport padding here: the content scrolls under the
                // translucent tab bar and SettingsScaffold reserves the bar's
                // footprint at the end of its scroll content. Padding the whole
                // viewport on top of that left a dead band at the bottom.
                SettingsScreen(
                    onOpenUi = { navController.navigate(Routes.SETTINGS_UI) },
                    onOpenA11y = { navController.navigate(Routes.SETTINGS_A11Y) },
                    onOpenNotify = { navController.navigate(Routes.SETTINGS_NOTIFY) },
                    onOpenUser = { navController.navigate(Routes.SETTINGS_USER) },
                    onOpenAdvanced = { navController.navigate(Routes.SETTINGS_ADVANCED) },
                    onOpenAbout = { navController.navigate(Routes.SETTINGS_ABOUT) },
                )
            }

            composable(Routes.SETTINGS_ABOUT) {
                AboutScreen(
                    onBack = { navController.popBackStack() },
                    onOpenPage = { title, url ->
                        navController.navigate(Routes.webPage(title, url))
                    },
                )
            }

            composable(
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

            composable(
                Routes.READER,
                arguments = listOf(navArgument("articleId") { type = NavType.StringType }),
            ) { entry ->
                val articleId = entry.arguments?.getString("articleId") ?: ""
                val vm: ReaderViewModel = viewModel(
                    key = "reader_$articleId",
                    initializer = {
                        ReaderViewModel(app).also { it.load(articleId); it.startTimer() }
                    },
                )
                val state by vm.state.collectAsState()
                DisposableEffect(articleId) {
                    onDispose { vm.stopTimer() }
                }
                ReaderScreen(
                    state = state,
                    onBack = { navController.popBackStack() },
                    onOpenInBrowser = { vm.openInBrowser(context, it) },
                )
            }

            composable(Routes.SOURCES) {
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

            composable(Routes.ALL_ARTICLES) {
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

            composable(
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

            composable(Routes.SETTINGS_UI) {
                UiSettingsScreen(
                    onBack = { navController.popBackStack() },
                    onRecreate = { (context as? android.app.Activity)?.recreate() },
                )
            }

            composable(Routes.SETTINGS_A11Y) {
                AccessibilityScreen(onBack = { navController.popBackStack() })
            }

            composable(Routes.SETTINGS_NOTIFY) {
                NotificationScreen(onBack = { navController.popBackStack() })
            }

            composable(Routes.SETTINGS_USER) {
                UserSettingsScreen(onBack = { navController.popBackStack() })
            }

            composable(Routes.SETTINGS_ADVANCED) {
                AdvancedMaterialScreen(onBack = { navController.popBackStack() })
            }
        }

        if (currentTab != null) {
            GlassTabBar(
                selected = currentTab,
                onSelect = { tab ->
                    val route = when (tab) {
                        IntentTab.HOME -> Routes.HOME
                        IntentTab.SEARCH -> Routes.SEARCH
                        IntentTab.SETTINGS -> Routes.SETTINGS
                    }
                    if (currentRoute != route) {
                        navController.navigate(route) {
                            popUpTo(Routes.HOME) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                modifier = Modifier.align(Alignment.BottomCenter),
                hidden = tabHidden,
                liquidGlass = liquidGlass,
                onPillBounds = { pillBounds = it },
                onPress = { position, isDown ->
                    if (isDown) pressPoint = position
                    pressed = isDown
                },
            )
        }
    }
}
