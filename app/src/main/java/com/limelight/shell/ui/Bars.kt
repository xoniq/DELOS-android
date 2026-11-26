package com.limelight.shell.ui

import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.snap
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.Canvas
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.rememberUpdatedState
import com.limelight.R
import com.limelight.shell.system.BatteryStatus
import com.limelight.shell.system.WifiStatus
import kotlin.math.roundToInt

/**
 * The status and hint bars are scaled as a whole by the interface scale (the prototype's CSS zoom);
 * inside them text keeps its base size relative to the bar.
 */
@Composable
fun ZoomedBar(uiScale: Float, content: @Composable () -> Unit) {
    val stage = LocalStage.current
    CompositionLocalProvider(
        // The bar's stage is as many stage pixels across as the zoomed bar really has room for
        LocalStage provides stage.copy(
            dpPerPx = stage.dpPerPx * uiScale, textScale = 1f, safe = stage.safe.dividedBy(uiScale),
            stageWidth = stage.stageWidth / uiScale, stageHeight = stage.stageHeight / uiScale,
        ),
        content = content,
    )
}

/** A top navigation button of the status bar. */
@Immutable
data class NavButton(val icon: String, val label: String, val current: Boolean, val onClick: () -> Unit)

/**
 * Delos draws its own status bar (the system bars are hidden): brand on the left, the main navigation
 * in the middle and Wi-Fi, battery and clock on the right. [onBack]: the screen can go back; a round back button before the brand
 * does it while no controller is the input ([StatusBarBack]).
 */
@Composable
fun StatusBar(
    nav: List<NavButton>?,
    pcName: String?,
    pcOnline: Boolean,
    wifi: WifiStatus,
    battery: BatteryStatus?,
    clock: String,
    modifier: Modifier = Modifier,
    /** False where the screen shows something else in the corner (the start screen's profile). */
    brand: Boolean = true,
    onBack: (() -> Unit)? = null,
    cellularLevel: Int? = null,
) {
    val stage = LocalStage.current
    if (stage.compact) {
        CompactStatusBar(nav, pcName, pcOnline, wifi, battery, clock, modifier, brand, onBack, cellularLevel)
        return
    }
    val colors = LocalDelosColors.current
    // The display cutout pads the bar (zero on a screen without one, so nothing moves there)
    val safe = stage.safe
    Box(modifier.fillMaxWidth().padding(top = safe.top.px).height(104.px).padding(start = 72.px + safe.left.px, end = 72.px + safe.right.px)) {
        Row(Modifier.align(Alignment.CenterStart), verticalAlignment = Alignment.CenterVertically) {
            StatusBarBack(onBack)
            if (brand) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.px)) {
                    LogoMark()
                    BasicText("DELOS", style = TextStyle(color = colors.text, fontFamily = Unbounded, fontWeight = FontWeight.SemiBold, fontSize = 26.tpx, letterSpacing = .14.em))
                    if (pcName != null) PcPill(pcName, pcOnline)
                }
            }
        }
        if (nav != null) TopNav(nav, Modifier.align(Alignment.Center))
        StatusCluster(wifi, battery, clock, Modifier.align(Alignment.CenterEnd), cellularLevel = cellularLevel)
    }
}

/**
 * The round glass back button at the start of the status bar (`BackButton` of DELOS for Apple): shown while the screen can go back
 * ([onBack]) and no controller is the input (a controller has B), it comes and goes with [InputKindVisibility] and the brand after it
 * glides. A tap does what B does.
 */
@Composable
private fun StatusBarBack(onBack: (() -> Unit)?) {
    val colors = LocalDelosColors.current
    val back = remember { arrayOfNulls<() -> Unit>(1) }
    if (onBack != null) back[0] = onBack
    val label = stringResource(R.string.shell_hint_back)
    val c = StatusBarLayout
    InputKindVisibility(c.BACK_RULE, visible = onBack != null, after = c.BACK_GAP.px) {
        Box(
            Modifier.size(c.BACK_SIZE.px).pointerTap(999f) { back[0]?.invoke() }
                .clip(CircleShape).glass(colors.surface).border(1.px, colors.line, CircleShape)
                .semantics { contentDescription = label; role = Role.Button; onClick { back[0]?.invoke(); true } },
            contentAlignment = Alignment.Center,
        ) {
            Symbol(Symbols.ARROW_BACK, c.BACK_ARROW.tpx, colors.text)
        }
    }
}

/** Wi-Fi, the battery with its percentage and the clock, the same gap between them ([gap]; [batteryGap] between the battery and its [percent]). */
@Composable
private fun StatusCluster(
    wifi: WifiStatus, battery: BatteryStatus?, clock: String, modifier: Modifier = Modifier,
    gap: Float = StatusBarLayout.STATUS_GAP, batteryGap: Float = StatusBarLayout.BATTERY_GAP, percent: Boolean = true,
    cellularLevel: Int? = null,
) {
    val colors = LocalDelosColors.current
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(gap.px)) {
        // Wi-Fi (plus cellular signal bars when on cellular), battery and clock in the same color
        Symbol(wifiIcon(wifi), 30.tpx, colors.text)
        if (cellularLevel != null) {
            CellularSignalBars(cellularLevel)
        }
        if (battery != null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(batteryGap.px)) {
                BatteryIndicator(battery)
                // Equal-width digits: from 10 to 99 % the bar does not shift when the level changes
                if (percent) BasicText(
                    "${(battery.level * 100).roundToInt()}%",
                    style = TextStyle(color = colors.text, fontFamily = Figtree, fontWeight = FontWeight.SemiBold, fontSize = 22.tpx, fontFeatureSettings = "tnum"),
                )
            }
        }
        BasicText(clock, style = TextStyle(color = colors.text, fontFamily = Figtree, fontWeight = FontWeight.SemiBold, fontSize = 24.tpx, fontFeatureSettings = "tnum"))
    }
}

/**
 * The status bar of a compact stage (about 1050 to 1490 stage pixels across, fewer with a larger interface scale): the same three parts,
 * but the navigation's buttons are 56 px, the connected PC is only its status dot with the PC glyph (its name is in Home's hint bar and
 * on the Devices screen; a screen reader still gets it) and the wordmark goes under [Compact.NARROW_STAGE]. [fitStatusBar] places the
 * navigation: on the bar's middle while that leaves [StatusBarLayout.MIN_GAP] to both clusters, else in the middle of the free space
 * between them; when even that is too narrow the status cluster's gaps tighten and, last, the battery's percentage goes ([StatusBarStep]).
 */
@Composable
private fun CompactStatusBar(
    nav: List<NavButton>?, pcName: String?, pcOnline: Boolean, wifi: WifiStatus, battery: BatteryStatus?, clock: String, modifier: Modifier, brand: Boolean,
    onBack: (() -> Unit)?, cellularLevel: Int? = null,
) {
    val colors = LocalDelosColors.current
    val stage = LocalStage.current
    val safe = stage.safe
    val wordmark = stage.stageWidth >= Compact.NARROW_STAGE
    val edges = LocalStatusBarEdges.current
    val c = StatusBarLayout
    val sideLeft = (c.SIDE + safe.left).px
    val sideRight = (c.SIDE + safe.right).px
    val minGap = c.MIN_GAP.px
    val buttons = nav?.size ?: 0
    val navWidth = c.navWidth(buttons, c.NAV_BUTTON, c.NAV_COMPACT.gap).px
    SubcomposeLayout(modifier.fillMaxWidth().padding(top = safe.top.px).height(104.px)) { constraints ->
        val open = Constraints(maxHeight = constraints.maxHeight)
        val width = constraints.maxWidth
        // The back button and the brand: the whole left cluster, measured as it is (the navigation's place follows it as the button comes and goes)
        val left = subcompose("left") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusBarBack(onBack)
                if (brand) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.px)) {
                    LogoMark()
                    if (wordmark) BasicText("DELOS", style = TextStyle(color = colors.text, fontFamily = Unbounded, fontWeight = FontWeight.SemiBold, fontSize = 26.tpx, letterSpacing = .14.em))
                    if (pcName != null) PcDot(pcName, pcOnline)
                }
            }
        }.firstOrNull()?.measure(open)
        // Only the steps that are tried are composed: the full cluster first
        val rights = arrayOfNulls<Placeable>(StatusBarStep.entries.size)
        fun right(step: StatusBarStep): Placeable = rights[step.ordinal] ?: subcompose(step) {
            StatusCluster(
                wifi, battery, clock, gap = if (step.tight) c.STATUS_GAP_TIGHT else c.STATUS_GAP,
                batteryGap = if (step.tight) c.BATTERY_GAP_TIGHT else c.BATTERY_GAP, percent = step.percent,
                cellularLevel = cellularLevel,
            )
        }.first().measure(open).also { rights[step.ordinal] = it }
        val fit = fitStatusBar(width.toFloat(), sideLeft.toPx(), sideRight.toPx(), minGap.toPx()) { step ->
            StatusBarWidths(
                (left?.width ?: 0).toFloat(), if (buttons == 0) 0f else navWidth.toPx(), right(step).width.toFloat(),
            )
        }
        val status = right(fit.step)
        val bar = if (nav != null) subcompose("nav") {
            TopNav(nav, Modifier, buttonSize = c.NAV_COMPACT.size, gap = c.NAV_COMPACT.gap)
        }.first().measure(open) else null
        val statusX = width - sideRight.roundToPx() - status.width
        layout(width, constraints.maxHeight) {
            fun y(p: Placeable) = (constraints.maxHeight - p.height) / 2
            left?.placeRelative(sideLeft.roundToPx(), y(left))
            bar?.placeRelative(fit.navX.roundToInt(), y(bar))
            status.placeRelative(statusX, y(status))
            edges?.rightStartPx = statusX
        }
    }
}

/**
 * The connected PC on a compact stage: its status dot (green while the PC is online, grey otherwise) and the PC glyph in the pill of
 * [PcPill], without the name. The name is what a screen reader says.
 */
@Composable
private fun PcDot(name: String, online: Boolean) {
    val colors = LocalDelosColors.current
    val description = stringResource(if (online) R.string.shell_side_connected else R.string.shell_side_offline, name)
    Row(
        Modifier.semantics { contentDescription = description }
            .clip(CircleShape).glass(colors.surface).border(1.px, colors.line, CircleShape).padding(horizontal = 14.px, vertical = 8.px),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.px),
    ) {
        Box(Modifier.size(10.px).clip(CircleShape).background(if (online) ONLINE else colors.text3))
        Symbol(Symbols.DESKTOP_WINDOWS, 24.tpx, colors.text2)
    }
}

private fun wifiIcon(wifi: WifiStatus) = when (wifi.level) {
    null -> Symbols.WIFI_OFF
    0, 1 -> Symbols.NETWORK_WIFI_1_BAR
    2 -> Symbols.NETWORK_WIFI_2_BAR
    3 -> Symbols.NETWORK_WIFI_3_BAR
    else -> Symbols.WIFI
}

@Composable
private fun CellularSignalBars(level: Int) {
    val colors = LocalDelosColors.current
    val activeColor = if (level <= 2) Color(0xFFF59E0B) else colors.text
    val inactiveColor = colors.text.copy(alpha = 0.25f)
    Canvas(Modifier.size(28.px, 24.px)) {
        val bars = 4
        val gap = size.width * 0.12f
        val barWidth = (size.width - gap * (bars - 1)) / bars
        val corner = CornerRadius(barWidth / 2f, barWidth / 2f)
        for (i in 0 until bars) {
            val h = size.height * (0.30f + 0.70f * ((i + 1) / bars.toFloat()))
            val x = i * (barWidth + gap)
            val y = size.height - h
            drawRoundRect(
                color = if (i < level) activeColor else inactiveColor,
                topLeft = Offset(x, y),
                size = Size(barWidth, h),
                cornerRadius = corner,
            )
        }
    }
}

/** Battery at or below this level shows red while not charging (like iOS). */
private const val LOW_BATTERY = .2f

internal val LowRed = androidx.compose.ui.graphics.Color(0xFFFF453A)

/**
 * The battery in the style of iOS: a light outline with a small cap, filled green to the charge level
 * (red when low). While charging, a bolt sits inside and the green pulses.
 */
@Composable
private fun BatteryIndicator(battery: BatteryStatus) {
    val colors = LocalDelosColors.current
    val stage = LocalStage.current
    // While charging, one cycle of 1.8 s: the green fades up and down, and the moment it is at full strength
    // a ring pulses out from the outline, like the one around the focused cover. The phase is read while drawing only, so
    // the animation redraws the icon without recomposing it every frame.
    val phase: State<Float>? = if (battery.charging) {
        rememberInfiniteTransition(label = "charging").animateFloat(
            0f, 1f, infiniteRepeatable(tween(1800, easing = LinearEasing)), label = "chargingPhase",
        )
    } else null
    val fill = if (!battery.charging && battery.level <= LOW_BATTERY) LowRed else OnlineGreen
    Box(Modifier.size(48.px, 24.px), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val p = phase?.value ?: 0f
            val pulse = if (phase != null) .55f + .45f * EaseOut.transform(if (p < .5f) p * 2f else (1f - p) * 2f) else 1f
            val wave = EaseOut.transform((p + .5f) % 1f)
            val k = stage.dpPerPx * density
            val stroke = 2f * k
            val cap = 3f * k
            val body = Size(size.width - cap - 1.5f * k, size.height)
            if (battery.charging) {
                val reach = (2f + 10f * wave) * k
                drawRoundRect(
                    OnlineGreen.copy(alpha = .7f * (1f - wave)), Offset(stroke / 2f - reach, stroke / 2f - reach),
                    Size(body.width - stroke + reach * 2f, body.height - stroke + reach * 2f), CornerRadius(7f * k + reach), style = Stroke(1.5f * k),
                )
            }
            // The outline and the cap on the right
            drawRoundRect(colors.text.copy(alpha = .9f), Offset(stroke / 2f, stroke / 2f), Size(body.width - stroke, body.height - stroke), CornerRadius(7f * k), style = Stroke(stroke))
            drawRoundRect(colors.text.copy(alpha = .6f), Offset(body.width + 1.5f * k, size.height * .32f), Size(cap, size.height * .36f), CornerRadius(1.5f * k))
            // The charge, inset from the outline
            val inset = stroke + 2f * k
            val inner = Size(body.width - inset * 2f, body.height - inset * 2f)
            drawRoundRect(
                fill.copy(alpha = pulse), Offset(inset, inset), Size(inner.width * battery.level.coerceIn(.04f, 1f), inner.height), CornerRadius(4f * k),
            )
        }
        if (battery.charging) Symbol(Symbols.BOLT, 20.tpx, colors.text, Modifier.offset(x = (-2).px), filled = true)
    }
}

/** The Delos ring with its small moon (`.logo-mark`): drawn for 40 stage px; at another [size] the stroke and the moon scale with it, so the mark keeps the look of the launcher icon. */
@Composable
fun LogoMark(size: Float = 40f) {
    val colors = LocalDelosColors.current
    val stage = LocalStage.current
    Box(Modifier.size(size.px).drawBehind {
        val k = stage.dpPerPx * density * (size / 40f)
        val stroke = 3f * k
        drawCircle(colors.accent, radius = this.size.minDimension / 2f - stroke / 2f, style = Stroke(stroke))
        val moon = Offset(this.size.width - 1.5f * k, 7.5f * k)
        drawCircle(colors.accent2.copy(alpha = .35f), radius = 9f * k, center = moon)
        drawCircle(colors.accent2, radius = 5.5f * k, center = moon)
    })
}

/** The connected PC next to the brand; the dot is green while the PC is online. */
@Composable
private fun PcPill(name: String, online: Boolean) {
    val colors = LocalDelosColors.current
    Row(
        Modifier.clip(CircleShape).glass(colors.surface).border(1.px, colors.line, CircleShape).padding(horizontal = 18.px, vertical = 8.px),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.px),
    ) {
        Box(Modifier.size(10.px).clip(CircleShape).background(if (online) ONLINE else colors.text3))
        BasicText(name, style = TextStyle(color = colors.text2, fontFamily = Figtree, fontSize = 20.tpx))
    }
}

private val ONLINE = androidx.compose.ui.graphics.Color(0xFF3DDC84)

/** The top navigation: round buttons [buttonSize] wide and [gap] apart in a glass pill (64 and 16; a compact stage's are smaller, see [StatusBarLayout]). */
@Composable
internal fun TopNav(buttons: List<NavButton>, modifier: Modifier, buttonSize: Float = StatusBarLayout.NAV.size, gap: Float = StatusBarLayout.NAV.gap) {
    val colors = LocalDelosColors.current
    val glass = LocalBackdropHaze.current
    val requesters = remember(buttons.size) { List(buttons.size) { FocusRequester() } }
    TransientPillHost(
        modifier,
        background = {
            // Frosted glass in its own layer: the pill itself is not clipped, the focused button's glow and
            // tooltip reach outside it
            Box(
                Modifier
                    .matchParentSize()
                    .clip(CircleShape)
                    .then(if (glass != null) Modifier.backdropBlur(glass, 8f, colors.surface) else Modifier.background(colors.surface))
                    .border(1.px, colors.line, CircleShape),
            )
        },
    ) {
        Row(Modifier.padding(StatusBarLayout.NAV_PADDING.px), horizontalArrangement = Arrangement.spacedBy(gap.px)) {
            buttons.forEachIndexed { i, button ->
                NavButtonView(
                    button, requesters[i],
                    wrapLeft = if (i == 0 && requesters.size > 1) requesters.last() else null,
                    wrapRight = if (i == requesters.lastIndex && requesters.size > 1) requesters.first() else null,
                    size = buttonSize,
                )
            }
        }
    }
}

/** What a focused top menu button is scaled to; the transient pill takes its focused bounds. */
private val NAV_FOCUS_SCALE = StatusBarLayout.NAV_LOOK.scale

@Composable
private fun NavButtonView(button: NavButton, requester: FocusRequester, wrapLeft: FocusRequester?, wrapRight: FocusRequester?, size: Float) {
    val colors = LocalDelosColors.current
    val pill = LocalTransientPill.current
    val reduced = LocalReducedMotion.current
    var focused by remember { mutableStateOf(false) }
    val position = remember { arrayOfNulls<LayoutCoordinates>(1) }
    // The icon's colour follows the pill under it (dark while the accent pill is there) instead of switching at once
    val iconColor by animateColorAsState(
        when {
            focused -> colors.onAccent
            button.current -> colors.accent
            else -> colors.text2
        },
        if (reduced) snap() else tween(Motion.FADE, easing = Motion.Ease), label = "navIcon",
    )
    Box(contentAlignment = Alignment.TopCenter) {
        Box(
            Modifier
                // Left of the first button is the last one, right of the last one the first (cyclic, like the stream menu's tile bar)
                .focusProperties {
                    wrapLeft?.let { left = it }
                    wrapRight?.let { right = it }
                }
                .focusRequester(requester)
                // The layout bounds the pill glides between (as a focused button looks: scaled), before the focus scale
                .onGloballyPositioned { position[0] = it }
                .focusEffects(
                    onClick = button.onClick, corner = 999f, focusScale = NAV_FOCUS_SCALE, ring = null,
                    glow = FocusGlow(offsetY = 10f, blur = 30f, spread = -8f),
                    onFocusChange = {
                        focused = it
                        pill?.focusChanged(it, position[0], NAV_FOCUS_SCALE)
                    },
                )
                .size(size.px)
                .clip(CircleShape)
                // The focused button's accent fill; while the transient pill is the focus, the pill is what shows, the fill fades in over it at the hand-over
                .drawBehind { if (focused) drawRect(colors.accent, alpha = pill?.handover() ?: 1f) },
            contentAlignment = Alignment.Center,
        ) {
            Symbol(button.icon, 34.tpx, iconColor, filled = focused)
            // The page you are on: a small dot in the third color under its icon (the focused button is filled instead)
            if (button.current && !focused) Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 5.px).size(6.px).clip(CircleShape).background(colors.accent3))
        }
        // Tooltip under the focused button (`.nav-btn .tip`); it takes the button's size, so a long label
        // cannot widen the bar, and overflows it centered
        AnimatedVisibility(
            focused,
            modifier = Modifier.matchParentSize().wrapContentSize(Alignment.TopCenter, unbounded = true).offset(y = (size + 20f).px),
            enter = fadeIn(tween(Motion.FADE, easing = Motion.Ease)) + slideInVertically(tween(Motion.POP_IN, easing = EaseOut)) { -it / 4 },
            exit = fadeOut(tween(Motion.EXIT, easing = Motion.Ease)) + slideOutVertically(tween(Motion.EXIT, easing = Motion.In)) { -it / 4 },
        ) {
            BasicText(
                button.label,
                style = TextStyle(color = colors.text, fontFamily = Figtree, fontWeight = FontWeight.SemiBold, fontSize = 18.tpx),
                modifier = Modifier.clip(CircleShape).glass(colors.surface3).padding(horizontal = 14.px, vertical = 6.px),
            )
        }
    }
}

/**
 * A complete cap of the keyboard and mouse library in the shared font (`Symbols.MAC_*`): what a keyboard or a mouse does instead of a
 * pad's button (the keys of [com.limelight.shell.GlobalKeys.keyboardButton]). [symbols] are the caps drawn side by side (the arrows are
 * two keys).
 */
enum class InputCap(val symbols: List<String>) {
    RETURN(listOf(Symbols.MAC_RETURN)),
    SPACE(listOf(Symbols.MAC_SPACE)),
    ESCAPE(listOf(Symbols.MAC_ESCAPE)),
    TAB(listOf(Symbols.MAC_TAB)),
    LETTER_F(listOf(Symbols.MAC_LETTER_F)),
    LETTER_O(listOf(Symbols.MAC_LETTER_O)),
    LEFT_BRACKET(listOf(Symbols.MAC_LEFT_BRACKET)),
    RIGHT_BRACKET(listOf(Symbols.MAC_RIGHT_BRACKET)),
    ARROWS(listOf(Symbols.MAC_ARROW_LEFT, Symbols.MAC_ARROW_RIGHT)),
    ARROW_DOWN(listOf(Symbols.MAC_ARROW_DOWN)),
    CLICK(listOf(Symbols.MAC_MOUSE_CLICK_LEFT)),
    SECONDARY_CLICK(listOf(Symbols.MAC_MOUSE_CLICK_RIGHT)),
}

/**
 * What a hint's button names: a place on the connected controller ([Pad], drawn with its glyph family by [ControllerGlyphs]), the adjust
 * cue, which is no button, or what the other kinds of input do instead ([InputKind]): a tap of the finger ([TAP]), a click of the mouse
 * ([CLICK]), a key of the keyboard ([Cap]). The names A, B, X, Y, L1, R1 are positions (A is the south button, B the east one...), as on
 * an Xbox pad; a PlayStation pad draws the cross for [A] and a Nintendo pad its own letters.
 */
@Immutable
sealed interface Glyph {
    data class Pad(val role: GlyphRole) : Glyph

    data class Cap(val cap: InputCap) : Glyph

    /**
     * [scale] multiplies the 24 px base font size to even out icons that look too big or too small next to
     * the others. The scales are measured against the Material Symbols Rounded glyphs (FILL 1, wght 600,
     * opsz 24, 960 units/em) the sizes were tuned on by eye (b83165f2) and recalibrated for the Phosphor Fill
     * glyphs (1024 units/em), which have another ink size in the em box:
     * `new = old * (oldInk / 960) / (newInk / 1024)`, ink being the glyph's bounding box.
     */
    data class Icon(val symbol: String, val scale: Float = 1f) : Glyph

    companion object {
        val A = Pad(GlyphRole.SOUTH)
        val B = Pad(GlyphRole.EAST)
        val X = Pad(GlyphRole.WEST)
        val Y = Pad(GlyphRole.NORTH)
        val L1 = Pad(GlyphRole.LEFT_SHOULDER)
        val R1 = Pad(GlyphRole.RIGHT_SHOULDER)
        val MENU_LEFT = Pad(GlyphRole.MENU_LEFT)
        val MENU_RIGHT = Pad(GlyphRole.MENU_RIGHT)

        /** Down on the D-pad (the game overview) and left-right (browsing its screenshots): drawn in the outline cut, the pressed arm filled. */
        val DPAD_DOWN = Pad(GlyphRole.DPAD_DOWN)
        val DPAD_HORIZONTAL = Pad(GlyphRole.DPAD_HORIZONTAL)

        /** Material swap_horiz 0.827 x 0.661 em vs Phosphor arrows-left-right 0.6875 x 0.6875 em -> area-equivalent 1.075 (17.7 px). */
        val ADJUST = Icon(Symbols.SWAP_HORIZ, scale = 1.075f)

        /** The primary action for a finger: the hand tapping (the font has no touch caps, so it keeps the disc of the other cues). */
        val TAP = Icon(Symbols.TOUCH_APP)

        /** The primary action for a mouse or a trackpad: the mouse's left click. */
        val CLICK = Cap(InputCap.CLICK)

        /** The context menu for a mouse or a trackpad: the right click (a trackpad's two-finger click). */
        val SECONDARY_CLICK = Cap(InputCap.SECONDARY_CLICK)
    }
}

/**
 * [shortLabel] is the label a cramped (compact) bar uses before it drops the label altogether. [tappable]: a finger, a mouse or a
 * keyboard can press it in the bar (the library's Filter): while no controller is the input it is a glyphless pill that runs the bar's
 * `onTap`; a controller has the button itself, and the hint stays a hint (`Hint.tappable` of DELOS for Apple). [pair]: a second button
 * drawn right after [glyph] for the opposite direction of the same action (L1 and R1: Browse).
 */
@Immutable
data class Hint(
    val glyph: Glyph,
    val label: String,
    val shortLabel: String? = null,
    val tappable: Boolean = false,
    val icon: String? = null,
    val pair: Glyph? = null,
)

/** The bottom bar: an optional note on the left, the button hints on the right; [onTap] runs a [Hint.tappable] hint's pill. */
@Composable
fun HintBar(hints: List<Hint>, note: String?, modifier: Modifier = Modifier, noteLabel: String? = null, onTap: ((Hint) -> Unit)? = null) {
    val colors = LocalDelosColors.current
    val stage = LocalStage.current
    if (stage.compact) {
        CompactHintBar(hints, note, noteLabel, modifier, onTap)
        return
    }
    val safe = stage.safe
    Row(
        modifier.fillMaxWidth().padding(bottom = safe.bottom.px).height(92.px).padding(start = 72.px + safe.left.px, end = 72.px + safe.right.px),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(34.px),
    ) {
        if (note != null) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.px)) {
            if (noteLabel != null) BasicText(noteLabel, style = TextStyle(color = colors.accent3,
                fontFamily = Figtree, fontWeight = FontWeight.SemiBold, fontSize = 20.tpx, letterSpacing = .14.em),
                modifier = Modifier.alignByBaseline())
            BasicText(note, style = TextStyle(color = colors.text3, fontFamily = Figtree, fontSize = 21.tpx),
                modifier = Modifier.alignByBaseline())
        }
        Spacer(Modifier.weight(1f))
        hints.forEach { hint -> HintCell(hint, HintDetail.FULL, onTap) }
    }
}

/**
 * The hint bar of a compact stage: the same note and hints, but only as much of them as the width holds ([fitHintBar]). The hints are the
 * bar, so they are never cut and never overlap the note; the note (with its "Library" label, or alone) stays only while every hint keeps
 * a label. When the labels do not all fit, the least important hints lose theirs first (a shorter label before none at all), the
 * most important (A, then Back) keep theirs longest.
 */
@Composable
private fun CompactHintBar(hints: List<Hint>, note: String?, noteLabel: String?, modifier: Modifier, onTap: ((Hint) -> Unit)?) {
    val safe = LocalStage.current.safe
    val noteVariants = remember(note, noteLabel) {
        buildList {
            if (note != null && noteLabel != null) add(true)
            if (note != null) add(false)
        }
    }
    val noteGap = 34.px
    val hintGap = 28.px
    SubcomposeLayout(
        modifier.fillMaxWidth().padding(bottom = safe.bottom.px).height(92.px).padding(start = 72.px + safe.left.px, end = 72.px + safe.right.px),
    ) { constraints ->
        val open = Constraints()
        val notes = noteVariants.mapIndexed { i, withLabel ->
            subcompose("note$i") { HintNote(note!!, if (withLabel) noteLabel else null) }.map { it.measure(open) }
        }
        val cells = hints.mapIndexed { i, hint ->
            fun cell(detail: HintDetail) = subcompose("hint$i${detail.name}") { HintCell(hint, detail, onTap) }.map { it.measure(open) }
            val full = cell(HintDetail.FULL)
            val short = if (hint.shortLabel != null) cell(HintDetail.SHORT) else null
            val bare = cell(HintDetail.BARE)
            mapOf(HintDetail.FULL to full, HintDetail.SHORT to (short ?: full), HintDetail.BARE to bare)
        }
        fun width(placeables: List<Placeable>) = placeables.sumOf { it.width }
        val items = hints.mapIndexed { i, hint ->
            val full = width(cells[i].getValue(HintDetail.FULL))
            val short = if (hint.shortLabel != null) width(cells[i].getValue(HintDetail.SHORT)).takeIf { it < full } else null
            HintFitItem(hintRank(hint.glyph), width(cells[i].getValue(HintDetail.BARE)), short, full, hintGroup(hint.glyph))
        }
        val fit = fitHintBar(notes.map { width(it) }, noteGap.roundToPx(), items, hintGap.roundToPx(), constraints.maxWidth)
        layout(constraints.maxWidth, constraints.maxHeight) {
            fit.note?.let { notes[it].forEach { p -> p.placeRelative(0, (constraints.maxHeight - p.height) / 2) } }
            val gap = hintGap.roundToPx()
            val placed = fit.details.mapIndexed { i, detail -> cells[i].getValue(detail) }
            var x = constraints.maxWidth - placed.sumOf { width(it) } - gap * (placed.size - 1).coerceAtLeast(0)
            placed.forEach { cell ->
                cell.forEach { p -> p.placeRelative(x, (constraints.maxHeight - p.height) / 2) }
                x += width(cell) + gap
            }
        }
    }
}

/** The note of a hint bar: its small label, then the sentence, on one baseline. */
@Composable
private fun HintNote(note: String, noteLabel: String?) {
    val colors = LocalDelosColors.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.px)) {
        if (noteLabel != null) BasicText(noteLabel, style = TextStyle(color = colors.accent3,
            fontFamily = Figtree, fontWeight = FontWeight.SemiBold, fontSize = 20.tpx, letterSpacing = .14.em),
            modifier = Modifier.alignByBaseline())
        BasicText(note, style = TextStyle(color = colors.text3, fontFamily = Figtree, fontSize = 21.tpx),
            modifier = Modifier.alignByBaseline(), maxLines = 1)
    }
}

/**
 * One hint: its button and, by [detail], its full label, its short label or nothing. A [Hint.tappable] hint is a glyphless pill with its
 * label that runs [onTap] while no controller is the input, and turns into the hint when one is (the animation of [InputKindVisibility]).
 */
@Composable
private fun HintCell(hint: Hint, detail: HintDetail, onTap: ((Hint) -> Unit)?) {
    val colors = LocalDelosColors.current
    val reduced = LocalReducedMotion.current
    val label = if (detail == HintDetail.SHORT) hint.shortLabel ?: hint.label else hint.label
    val pill = hint.tappable && onTap != null && ControllerGlyphs.kind != InputKind.CONTROLLER
    androidx.compose.animation.Crossfade(
        pill,
        animationSpec = if (reduced) tween(Motion.FADE, easing = Motion.EaseOutCss) else tween(Motion.ROW_IN, easing = EaseOut),
        label = "hintPill",
    ) { asPill ->
        if (asPill) {
            val tap by rememberUpdatedState(onTap)
            // A button, and it looks like one: the glass pill with its symbol (the filter's funnel) and label
            Row(
                Modifier
                    .pointerTap(999f, key = hint) { tap?.invoke(hint) }
                    .clip(CircleShape)
                    .glass(colors.surface)
                    .border(1.px, colors.line, CircleShape)
                    .semantics(mergeDescendants = true) { role = Role.Button; onClick { tap?.invoke(hint); true } }
                    .height(52.px)
                    .padding(start = (if (hint.icon != null) 18 else 24).px, end = 24.px),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.px),
            ) {
                hint.icon?.let { Symbol(it, 26.tpx, colors.text) }
                BasicText(label, maxLines = 1,
                    style = TextStyle(color = colors.text, fontFamily = Figtree, fontWeight = FontWeight.SemiBold, fontSize = 21.tpx))
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.px)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.px)) {
                    GlyphButton(hint.glyph)
                    hint.pair?.let { GlyphButton(it) }
                }
                if (detail != HintDetail.BARE) BasicText(
                    label, style = TextStyle(color = colors.text2, fontFamily = Figtree, fontSize = 21.tpx), maxLines = 1,
                )
            }
        }
    }
}

/** Native input caps occupy .75 em; their visible circular button remains [size] stage pixels. */
internal const val BUTTON_INK_FRACTION = .75f

/** Complete pad, key and mouse caps come from the shared font; the other cues retain their existing disc. */
@Composable
fun GlyphButton(glyph: Glyph, size: Int = 38) {
    val colors = LocalDelosColors.current
    when (glyph) {
        // The pressed D-pad reads better in the outline cut, where the pressed arm is the filled one
        is Glyph.Pad -> FontCap(ControllerGlyphs.family.fontSymbol(glyph.role), size, colors.text, filled = glyph != Glyph.DPAD_DOWN && glyph != Glyph.DPAD_HORIZONTAL)
        is Glyph.Cap -> Row(verticalAlignment = Alignment.CenterVertically) { glyph.cap.symbols.forEach { FontCap(it, size, colors.text) } }
        is Glyph.Icon -> Box(
            Modifier.size(size.px).clip(CircleShape).background(colors.text),
            contentAlignment = Alignment.Center,
        ) {
            Symbol(glyph.symbol, (24f * glyph.scale * size / 38f).tpx, colors.bg, filled = true)
        }
    }
}

/** One complete cap of the shared font, [size] stage pixels high. */
@Composable
private fun FontCap(symbol: String, size: Int, color: Color, filled: Boolean = true) {
    // The glyph is text (em.tpx grows with the text scale): its box grows the same way, so the cap is never cut at a text scale above 100 %
    val em = size / BUTTON_INK_FRACTION
    val scale = LocalStage.current.textScale
    Box(Modifier.width((em * scale).px).height((size * scale).px), contentAlignment = Alignment.Center) {
        Symbol(symbol, em.tpx, color, modifier = Modifier.requiredSize((em * scale).px), filled = filled)
    }
}
