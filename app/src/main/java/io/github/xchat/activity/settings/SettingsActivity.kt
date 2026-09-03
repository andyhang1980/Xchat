package io.github.xchat.activity.settings

import android.os.Build
import android.os.Bundle
import android.view.RoundedCorner
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.PredictiveBackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.Keep
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.outlined.Account_circle
import com.composables.icons.materialsymbols.outlined.Add_circle
import com.composables.icons.materialsymbols.outlined.Article
import com.composables.icons.materialsymbols.outlined.Bug_report
import com.composables.icons.materialsymbols.outlined.Call
import com.composables.icons.materialsymbols.outlined.Camera
import com.composables.icons.materialsymbols.outlined.Chat
import com.composables.icons.materialsymbols.outlined.Checklist
import com.composables.icons.materialsymbols.outlined.Comedy_mask
import com.composables.icons.materialsymbols.outlined.Contact_page
import com.composables.icons.materialsymbols.outlined.Contacts
import com.composables.icons.materialsymbols.outlined.Home
import com.composables.icons.materialsymbols.outlined.Imagesearch_roller
import com.composables.icons.materialsymbols.outlined.Movie
import com.composables.icons.materialsymbols.outlined.Newspaper
import com.composables.icons.materialsymbols.outlined.Notifications
import com.composables.icons.materialsymbols.outlined.Package_2
import com.composables.icons.materialsymbols.outlined.Payments
import com.composables.icons.materialsymbols.outlined.Settings
import com.composables.icons.materialsymbols.outlined.Terminal
import com.composables.icons.materialsymbols.outlined.Tune
import com.composables.icons.materialsymbols.outlined.Wand_stars
import com.composables.icons.materialsymbols.outlinedfilled.Article
import com.composables.icons.materialsymbols.outlinedfilled.Home
import com.composables.icons.materialsymbols.outlinedfilled.Settings
import com.composables.icons.materialsymbols.outlinedfilled.Tune
import dev.ujhhgtg.comptime.nameOf
import io.github.xchat.features.core.BaseFeature
import io.github.xchat.features.core.ClickableFeature
import io.github.xchat.features.core.SwitchFeature
import io.github.xchat.preferences.WePrefs
import io.github.xchat.ui.content.FloatingBottomBar
import io.github.xchat.ui.content.FloatingBottomBarDefaults
import io.github.xchat.ui.content.FloatingBottomBarItem
import io.github.xchat.ui.content.liquid.vibrancy
import io.github.xchat.ui.utils.theme.ModuleTheme
import io.github.xchat.ui.utils.theme.ThemeSettings
import io.github.xchat.utils.WeLogger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.BasicComponentDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.blur
import top.yukonga.miuix.kmp.blur.drawBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.squircle.squircleClip
import top.yukonga.miuix.kmp.squircle.squircleSurface
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.sqrt
import androidx.compose.material3.Icon as M3Icon
import androidx.compose.material3.Text as M3Text

val LocalComponentActivity = staticCompositionLocalOf<ComponentActivity> { error("not provided") }

@Keep
class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            CompositionLocalProvider(
                LocalContext provides this,
                LocalActivity provides this,
                LocalComponentActivity provides this
            ) {
                // Theme-mode drives the whole Settings surface (miuix + the Material
                // FloatingBottomBar), so resolve dark once and pass it to both wrappers.
                val dark = ThemeSettings.themeMode.resolve()
                ModuleTheme(darkTheme = dark) {
                    SettingsRoot(onFinish = { finish() })
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
//  Feature categories (name -> icon), mirrored from the legacy MainSettingsScreen
// ---------------------------------------------------------------------------

val FEATURE_CATEGORIES = listOf(
    "聊天" to MaterialSymbols.Outlined.Chat,
    "联系人与群组" to MaterialSymbols.Outlined.Contacts,
    "红包与支付" to MaterialSymbols.Outlined.Payments,
    "朋友圈" to MaterialSymbols.Outlined.Camera,
    "系统与隐私" to MaterialSymbols.Outlined.Wand_stars,
    "音视频通话" to MaterialSymbols.Outlined.Call,
    "通知" to MaterialSymbols.Outlined.Notifications,
    "界面美化" to MaterialSymbols.Outlined.Imagesearch_roller,
    "公众号" to MaterialSymbols.Outlined.Newspaper,
    "小程序" to MaterialSymbols.Outlined.Package_2,
    "视频号" to MaterialSymbols.Outlined.Movie,
    "个人资料" to MaterialSymbols.Outlined.Account_circle,
    "调试" to MaterialSymbols.Outlined.Bug_report,
    "脚本 (JS)" to MaterialSymbols.Outlined.Terminal,
    "脚本 (Java)" to MaterialSymbols.Outlined.Terminal,
    "娱乐" to MaterialSymbols.Outlined.Comedy_mask,
    "批量操作" to MaterialSymbols.Outlined.Checklist,
    "首页右上角菜单" to MaterialSymbols.Outlined.Add_circle,
    "联系人详情页面" to MaterialSymbols.Outlined.Contact_page,
)

// ---------------------------------------------------------------------------
//  Root: three-tab pager + floating bottom bar, with category drill-down
// ---------------------------------------------------------------------------

@Composable
private fun SettingsRoot(onFinish: () -> Unit) {
    var openedCategory by rememberSaveable { mutableStateOf<String?>(null) }
    var showLicense by rememberSaveable { mutableStateOf(false) }

    val pagerState = rememberPagerState(pageCount = { 4 })
    val isDragged by pagerState.interactionSource.collectIsDraggedAsState()
    val scope = rememberCoroutineScope()
    val backdrop = rememberLayerBackdrop()

    val barBottomPadding = 12.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    // No overlay open: plain back finishes the activity.
    BackHandler(enabled = openedCategory == null && !showLicense) { onFinish() }

    val drillTarget: DrillTarget? = when {
        openedCategory != null -> DrillTarget.Category(openedCategory!!)
        showLicense -> DrillTarget.License
        else -> null
    }

    MiuixDrillDownScaffold(
        target = drillTarget,
        onClose = {
            openedCategory = null
            showLicense = false
        },
    ) {
        // Background scene: the three-tab pager + floating bar. Parallaxed + dimmed by the
        // scaffold while a drill-down is open.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MiuixTheme.colorScheme.background),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .layerBackdrop(backdrop),
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    key = { it },
                ) { page ->
                    when (page) {
                        0 -> HomePager(onOpenFeatures = { scope.launch { pagerState.animateScrollToPage(1) } })
                        1 -> FeaturesPager(onOpenCategory = { openedCategory = it })
                        2 -> LogsPager()
                        else -> SettingsPager(onOpenLicense = { showLicense = true })
                    }
                }
            }

            val haptic = LocalHapticFeedback.current

            FloatingBottomBar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    )
                    .padding(bottom = barBottomPadding),
                selectedIndex = { pagerState.targetPage },
                // Track the pager's fractional scroll 1:1 during a finger swipe; a tab *tap*
                // (programmatic animateScrollToPage, isDragged == false) springs the pill across
                // with the press/glass bulge instead of a flat translate.
                progress = { pagerState.currentPage + pagerState.currentPageOffsetFraction },
                isTracking = { isDragged },
                onSelected = { scope.launch { pagerState.animateScrollToPage(it) } },
                backdrop = backdrop,
                tabsCount = TAB_ITEMS.size,
                isBlurEnabled = true,
                colors = FloatingBottomBarDefaults.colors(
                    containerColor = MiuixTheme.colorScheme.surfaceContainer,
                    indicatorColor = MiuixTheme.colorScheme.primary,
                    contentColor = MiuixTheme.colorScheme.onSurfaceSecondary,
                    activeContentColor = MiuixTheme.colorScheme.primary,
                ),
            ) {
                // Key the fill crossfade to targetPage (same driver as the pill), not
                // settledPage — settledPage only updates when animateScrollToPage fully
                // finishes, so the icon would fill a beat after the pill has arrived.
                val target = pagerState.targetPage
                TAB_ITEMS.forEachIndexed { index, item ->
                    FloatingBottomBarItem(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                            scope.launch { pagerState.animateScrollToPage(index) }
                        },
                        modifier = Modifier.defaultMinSize(minWidth = 76.dp),
                    ) {
                        Crossfade(
                            targetState = index == target,
                            animationSpec = tween(200),
                            label = "navIcon",
                        ) { selected ->
                            M3Icon(
                                imageVector = if (selected) item.filled else item.outlined,
                                contentDescription = item.label,
                            )
                        }
                        M3Text(
                            text = item.label,
                            fontSize = 11.sp,
                            lineHeight = 14.sp,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Visible
                        )
                    }
                }
            }
        }
    }
}

/**
 * The miuix navigation3 spring easing, ported verbatim from the miuix fork's
 * `NavTransitionEasing(response = 0.8, damping = 0.95)`. It's the analytic solution of an
 * under-damped spring expressed as an [Easing], which is what gives the Miuix predictive-back
 * transition its exact feel (vs. a plain tween or a stock Compose spring).
 */
private val NavAnimationEasing: Easing = run {
    val response = 0.8
    val damping = 0.95
    val omega = 2.0 * PI / response
    val k = omega * omega
    val c = damping * 4.0 * PI / response
    val w = sqrt(4.0 * k - c * c) / 2.0
    val r = -c / 2.0
    val c2 = r / w
    Easing { fraction ->
        val t = fraction.toDouble()
        val decay = exp(r * t)
        (decay * (-cos(w * t) + c2 * sin(w * t)) + 1.0).toFloat()
    }
}

/** Device hardware corner radius (API 31+), else a sane squircle fallback. */
@Composable
private fun deviceCornerRadiusDp(): Dp {
    val view = LocalView.current
    val density = LocalDensity.current
    return remember(view) {
        val px = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            view.rootWindowInsets
                ?.getRoundedCorner(RoundedCorner.POSITION_BOTTOM_LEFT)
                ?.radius ?: 0
        } else {
            0
        }
        if (px > 0) with(density) { px.toDp() } else 32.dp
    }
}

/** Which drill-down screen is showing (mutually exclusive: category detail or license). */
private sealed interface DrillTarget {
    data class Category(val name: String) : DrillTarget
    data object License : DrillTarget
}

/**
 * Faithful reproduction of miuix's navigation3 "Miuix" predictive-back transition. Unlike a
 * plain overlay slide, the *background scene* (pager + bar, in [background]) reacts too:
 *
 *  - top scene (the drill-down [DrillTarget]) slides its full width to the right on dismiss,
 *    squircle-corner-clipped at the device radius — no scale, no fade;
 *  - background scene parallaxes in from `-width/4` and is dimmed up to 50% black, both driven
 *    by the same gesture fraction;
 *  - committed open/close and gesture cancel settle on [NavAnimationEasing] (the miuix spring).
 *
 * `fraction` runs 0 (background fully covered by the top scene) → 1 (top scene fully dismissed).
 */
@Composable
private fun MiuixDrillDownScaffold(
    target: DrillTarget?,
    onClose: () -> Unit,
    background: @Composable BoxScope.() -> Unit,
) {
    var last by remember { mutableStateOf(target) }
    if (target != null) last = target

    val visible = target != null
    // 0f = top scene fully covers the background, 1f = fully dismissed to the right.
    val fraction = remember { Animatable(if (visible) 0f else 1f) }

    LaunchedEffect(visible) {
        fraction.animateTo(if (visible) 0f else 1f, animationSpec = tween(500, easing = NavAnimationEasing))
    }

    // In-app predictive back: seek `fraction` 1:1 from the gesture, commit on finish, spring
    // back to covered on cancel.
    PredictiveBackHandler(enabled = visible) { events ->
        try {
            events.collect { event -> fraction.snapTo(event.progress) }
            onClose()
        } catch (e: CancellationException) {
            fraction.animateTo(0f, animationSpec = tween(500, easing = NavAnimationEasing))
            throw e
        }
    }

    val cornerRadius = deviceCornerRadiusDp()
    val dimColor = Color.Black

    Box(modifier = Modifier.fillMaxSize()) {
        val p = fraction.value

        // Background scene: parallax + dim. It stays put once fully covered (occluded anyway).
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { translationX = -size.width * 0.25f * (1f - p) },
        ) {
            background()
            if (p < 1f) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .graphicsLayer { alpha = 0.5f * (1f - p) }
                        .background(dimColor),
                )
            }
        }

        // Top scene: full-width slide + squircle corner clip. Rendered while open and during the
        // close animation (keeping the last value so content doesn't blank out mid-transition).
        if (visible || p < 1f) {
            val value = last
            if (value != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { translationX = size.width * p }
                        .squircleClip(cornerRadius)
                        .background(MiuixTheme.colorScheme.background)
                        // Swallow taps so the background isn't clickable through the overlay.
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {},
                        ),
                ) {
                    when (value) {
                        is DrillTarget.Category -> CategoryDetailScreen(categoryName = value.name, onBack = onClose)
                        DrillTarget.License -> LicenseScreen(onBack = onClose)
                    }
                }
            }
        }
    }
}

private data class NavItem(val label: String, val outlined: ImageVector, val filled: ImageVector)

private val TAB_ITEMS = listOf(
    NavItem("主页", MaterialSymbols.Outlined.Home, MaterialSymbols.OutlinedFilled.Home),
    NavItem("功能", MaterialSymbols.Outlined.Tune, MaterialSymbols.OutlinedFilled.Tune),
    NavItem("日志", MaterialSymbols.Outlined.Article, MaterialSymbols.OutlinedFilled.Article),
    NavItem("设置", MaterialSymbols.Outlined.Settings, MaterialSymbols.OutlinedFilled.Settings),
)

/** Bottom padding so scrollable content clears the floating bar. */
val CONTENT_BOTTOM_INSET = 88.dp

// ---------------------------------------------------------------------------
//  Shared scaffold: miuix Scaffold + collapsing TopAppBar + scrollable column
// ---------------------------------------------------------------------------

@Composable
fun MiuixListScaffold(
    title: String,
    navigationIcon: @Composable (() -> Unit)? = null,
    content: LazyListScope.() -> Unit,
) {
    val scrollBehavior = MiuixScrollBehavior()
    // The LazyColumn is registered as the blur source; the top bar samples that captured
    // layer through drawBackdrop and paints itself transparent, so scrolled content shows
    // through blurred behind the collapsed bar (InstallerX's useBlur pattern, on miuix-blur glass).
    val barBackdrop = rememberLayerBackdrop()
    val barTint = MiuixTheme.colorScheme.surface.copy(alpha = 0.67f)
    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.drawBackdrop(
                    backdrop = barBackdrop,
                    shape = { RectangleShape },
                    effects = {
                        vibrancy()
                        blur(24.dp.toPx(), 24.dp.toPx())
                    },
                    onDrawSurface = { drawRect(barTint) },
                ),
                // Transparent so the miuix bar's own opaque surface doesn't hide the blur.
                color = Color.Transparent,
                title = title,
                scrollBehavior = scrollBehavior,
                navigationIcon = { navigationIcon?.invoke() },
            )
        },
        popupHost = {},
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxHeight()
                .layerBackdrop(barBackdrop)
                .scrollEndHaptic()
                .overScrollVertical()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .padding(horizontal = 12.dp),
            contentPadding = innerPadding,
            overscrollEffect = null,
            content = content,
        )
    }
}

// ---------------------------------------------------------------------------
//  Grouped card: emit each row as its own LazyColumn item so the list stays
//  virtualized, while the per-corner squircle background makes the rows read as
//  one continuous card (round top on the first row, round bottom on the last).
// ---------------------------------------------------------------------------

/**
 * Squircle background matching [Card]'s look but applied per-row, so a long feature list can be
 * emitted as individual `item {}`s (virtualized) instead of one giant `item { Card { forEach } }`
 * that composes every row at once. [index]/[count] pick which corners are rounded.
 */
@Composable
fun Modifier.groupedCardItem(index: Int, count: Int): Modifier {
    val r = CardDefaults.CornerRadius
    val z = 0.dp
    val top = index == 0
    val bottom = index == count - 1
    return fillMaxWidth()
        .squircleSurface(
            color = MiuixTheme.colorScheme.surfaceContainer,
            topStart = if (top) r else z,
            topEnd = if (top) r else z,
            bottomEnd = if (bottom) r else z,
            bottomStart = if (bottom) r else z,
        )
}

// ---------------------------------------------------------------------------
//  Shared feature row (miuix) — used by category detail and search
// ---------------------------------------------------------------------------

@Composable
fun FeatureRow(
    item: BaseFeature,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val context = LocalComponentActivity.current
    val configKey = item.name

    DisposableEffect(configKey) {
        (item as SwitchFeature).setToggleCompletionCallback { onCheckedChange(item.isEnabled) }
        onDispose {}
    }

    fun toggle(requested: Boolean) {
        item as SwitchFeature
        if (item.onBeforeToggle(requested, context)) {
            WePrefs.putBool(configKey, requested)
            item.isEnabled = requested
            onCheckedChange(requested)
        }
    }

    when (item) {
        is ClickableFeature -> BasicComponent(
            onClick = {
                runCatching { item.onClick(context) }
                    .onFailure { WeLogger.e(nameOf(SettingsActivity::class), "onClick failed for ${item.displayName}", it) }
            },
            endActions = {
                if (!item.noSwitchWidget) {
                    Switch(checked = checked, onCheckedChange = { toggle(it) })
                }
            },
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.name,
                    fontSize = MiuixTheme.textStyles.headline1.fontSize,
                    fontWeight = FontWeight.Medium,
                    color = BasicComponentDefaults.titleColor().color,
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector = MaterialSymbols.Outlined.Settings,
                    contentDescription = "Configurable",
                    modifier = Modifier
                        .padding(end = if (!item.noSwitchWidget) 8.dp else 0.dp)
                        .size(20.dp),
                    tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
            Text(
                text = item.description,
                fontSize = MiuixTheme.textStyles.body2.fontSize,
                color = BasicComponentDefaults.summaryColor().color,
            )
        }

        is SwitchFeature -> SwitchPreference(
            title = item.name,
            summary = item.description,
            checked = checked,
            onCheckedChange = { toggle(it) },
        )
    }
}








