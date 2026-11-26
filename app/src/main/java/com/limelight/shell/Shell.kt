package com.limelight.shell

import androidx.compose.animation.core.snap
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.limelight.shell.ui.Motion
import com.limelight.shell.ui.sideMenuWidthPx
import android.app.ActivityOptions
import android.content.res.Resources
import androidx.annotation.StringRes
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import com.limelight.R
import com.limelight.display.DualScreen
import com.limelight.shell.ui.textResources
import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.staticCompositionLocalOf
import com.limelight.shell.data.BackupManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.graphicsLayer
import com.limelight.shell.settings.BackgroundKind
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.limelight.shell.data.App
import com.limelight.shell.data.GameLauncher
import com.limelight.shell.data.Library
import com.limelight.shell.data.Pc
import com.limelight.shell.data.AVATARS
import com.limelight.shell.data.PcRepository
import com.limelight.shell.data.PcSetup
import com.limelight.shell.data.ProfileStore
import com.limelight.shell.data.LocalGameMetadata
import com.limelight.shell.screens.HomeScreen
import com.limelight.shell.screens.openMetadataMenu
import com.limelight.shell.screens.BootScreen
import com.limelight.shell.screens.CreateProfileScreen
import com.limelight.shell.screens.DevicesScreen
import com.limelight.shell.screens.PairPcScreen
import com.limelight.shell.screens.PinScreen
import com.limelight.shell.screens.ProfileScreen
import com.limelight.shell.screens.StartScreen
import com.limelight.shell.screens.SettingsScreen
import com.limelight.shell.screens.GameSettingsScreen
import com.limelight.shell.screens.appArt
import com.limelight.shell.screens.step
import com.limelight.shell.settings.Appearance
import com.limelight.shell.settings.StartupStore
import com.limelight.shell.system.rememberBatteryStatus
import com.limelight.shell.system.rememberClock
import com.limelight.shell.system.rememberWifiStatus
import com.limelight.shell.ui.Backdrop
import com.limelight.shell.ui.DelosColors
import com.limelight.shell.ui.ContextMenuLayer
import com.limelight.shell.ui.Cue
import com.limelight.shell.ui.EaseOut
import com.limelight.shell.ui.Feedback
import com.limelight.shell.ui.Glyph
import com.limelight.shell.ui.Hint
import com.limelight.shell.ui.InputCap
import com.limelight.shell.ui.InputKind
import com.limelight.shell.ui.HintBar
import com.limelight.shell.ui.ControllerGlyphs
import com.limelight.shell.ui.adaptedTo
import com.limelight.shell.ui.HeroBitmaps
import com.limelight.shell.ui.HeroMotion
import com.limelight.shell.ui.HANDOFF_PROGRESS
import com.limelight.shell.ui.LaunchCard
import com.limelight.shell.ui.LaunchHandoff
import com.limelight.shell.ui.LaunchOverlay
import com.limelight.shell.ui.LocalBackdropHaze
import com.limelight.shell.ui.LocalBackdropMirror
import com.limelight.shell.ui.backdropMirrorSource
import com.limelight.shell.ui.rememberBackdropMirror
import com.limelight.shell.ui.LocalDelosColors
import com.limelight.shell.ui.LocalFeedback
import com.limelight.shell.ui.NavButton
import com.limelight.shell.ui.LocalRowPicker
import com.limelight.shell.ui.RowPickerLayer
import com.limelight.shell.ui.RowPopup
import com.limelight.shell.ui.RowValue
import com.limelight.shell.ui.Scrim
import com.limelight.shell.ui.screenTransition
import com.limelight.shell.ui.SettingRow
import com.limelight.shell.ui.FocusedListNavigation
import com.limelight.shell.ui.focusedListRow
import com.limelight.shell.ui.rememberFocusedListNavigation
import com.limelight.shell.screens.SideMenuHeader
import com.limelight.shell.screens.SidePc
import com.limelight.shell.screens.SidePcState
import com.limelight.shell.screens.rememberProfilePlaytime
import com.limelight.shell.ui.SideMenuLayer
import com.limelight.shell.ui.SideMenuScroll
import com.limelight.shell.ui.px
import com.limelight.shell.ui.StatusBar
import com.limelight.shell.ui.Symbols
import com.limelight.shell.ui.ToastHost
import com.limelight.utils.ToastKind
import com.limelight.shell.ui.ZoomedBar
import com.limelight.shell.ui.AvatarBadge
import com.limelight.shell.ui.ButtonCaptureLayer
import com.limelight.shell.ui.DialogLayer
import com.limelight.shell.ui.PortalOverlay
import com.limelight.shell.ui.StreamResultLayer
import com.limelight.shell.ui.ProblemOfferCard
import com.limelight.shell.ui.UpdateOfferCard
import com.limelight.update.UpdateState
import com.limelight.update.Updates
import com.limelight.diag.ProblemMonitor
import com.limelight.diag.ProblemReporter
import com.limelight.diag.ReportMode
import com.limelight.diag.ReportOutcomeText
import com.limelight.diag.ReportUploader
import com.limelight.shell.ui.LocalImeAvoidance
import com.limelight.shell.ui.rememberImeAvoidance
import com.limelight.shell.ui.secondaryClickOutside
import com.limelight.shell.data.LocalArtwork
import com.limelight.shell.data.downloadAll
import com.limelight.shell.data.BundledArtwork
import com.limelight.shell.data.ArtKind
import com.limelight.shell.data.rememberArtwork
import com.limelight.shell.ui.ArtworkProgressBar
import com.limelight.shell.screens.ArtworkScreen
import com.limelight.shell.screens.DebugInfoScreen
import com.limelight.shell.screens.OscPreviewScreen
import com.limelight.shell.screens.FilterDraft
import com.limelight.shell.screens.FilterModalLayer
import com.limelight.shell.screens.filterHintLabel
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

data class BackupActions(val export: () -> Unit, val import: () -> Unit)

val LocalBackupActions = staticCompositionLocalOf { BackupActions({}, {}) }

/**
 * The new UI (Phase 6, step 6.3): background, screens with transitions, status bar, hint bar, the
 * side menu (left menu button), context menus (right menu button / Start / X) and toasts.
 */
@Composable
fun Shell(
    appearance: Appearance,
    update: ((Appearance) -> Appearance) -> Unit,
    keys: GlobalKeys,
    repo: PcRepository,
    launcher: GameLauncher,
    profileStore: ProfileStore,
    startupStore: StartupStore,
    setup: PcSetup,
    links: MutableStateFlow<ShellLink?>,
    results: MutableStateFlow<StreamResult?>,
    shell: ShellState,
    /** Debug screen previews only: the profiles the screens show instead of the store's (the store is never given any, see `ScreenPreviews`). */
    previewProfiles: com.limelight.shell.data.Profiles? = null,
) {
    val res = textResources()
    val colors = LocalDelosColors.current
    val feedback = LocalFeedback.current
    val tracker = remember { FocusTracker() }
    val haze = rememberHazeState()
    // Only the animated background, for glass that is always on (the focused row, the top bar)
    val backdropHaze = rememberHazeState()
    // The same background, recorded for a page that continues it over the dashboard (the game overview)
    val backdropMirror = rememberBackdropMirror()
    var blurSource by remember { mutableStateOf(false) }
    // The glass cards over the screen (the problem offer, the update offer, the button capture) blur what lies under them through the same source as the layers
    var updateGlass by remember { mutableStateOf(false) }
    val screenGlass = shell.layer != null || shell.portal != null || shell.picker != null || shell.buttonCapture != null || (shell.offerShown && shell.problem != null) || updateGlass
    LaunchedEffect(screenGlass) {
        if (screenGlass) {
            blurSource = true
        } else {
            delay(600)
            blurSource = false
        }
    }
    val scope = rememberCoroutineScope()
    // The connection line the launch screen tells under the title (null: the greeting)
    val launchLine = remember { mutableStateOf<String?>(null) }
    // Set when the launch overlay was cleared as the stream window took over; the next resume does the return work
    val returningFromStream = remember { mutableStateOf(false) }
    // Stays set until the next launch: the overlay it cleared leaves without a fade (a fade would run after the return)
    val launchExitInstant = remember { mutableStateOf(false) }
    val activity = LocalActivity.current
    // What the system shows while this window comes back to the front is NOT drawn by us: it is a snapshot starting window. Recorded on
    // the Odin (the SurfaceFlinger layer list showed "SnapshotStartingWindow" for this task during the flash): it held the last frame
    // from before the stream, the launch tunnel. The window no longer lets the system snapshot it (ShellActivity), and the system then
    // shows a plain starting window in the task's background color: the palette's own, so it blends into the exit scene.
    val taskBackground = colors.bg.toArgb()
    LaunchedEffect(taskBackground) {
        // Android 13+ (the same versions that can switch the snapshot off, see ShellActivity)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            activity?.setTaskDescription(android.app.ActivityManager.TaskDescription.Builder().setBackgroundColor(taskBackground).build())
        }
    }
    val actions = remember(launcher, res) { ShellGameActions(res, shell, launcher, feedback, scope) }
    val reducedMotion by rememberUpdatedState(appearance.reducedMotion)
    val activityContext = androidx.compose.ui.platform.LocalContext.current
    val appContext = androidx.compose.ui.platform.LocalContext.current.applicationContext
    val linkTests = remember { com.limelight.shell.link.LinkTestStore(appContext) }
    val linkTester = remember { com.limelight.shell.link.LinkTester(appContext, repo) }
    val devices = remember { ShellDeviceActions(activityContext, shell, tracker, repo, setup, startupStore, feedback, scope, linkTester, linkTests) { reducedMotion } }
    val storedProfiles by profileStore.profiles.collectAsStateWithLifecycle()
    val profiles = previewProfiles ?: storedProfiles
    val startup by startupStore.startup.collectAsStateWithLifecycle()
    val pcs by repo.pcs.collectAsStateWithLifecycle()

    // "Skip the start screen": straight into the default device
    LaunchedEffect(Unit) {
        val start = startupStore.startup.value
        val default = start.defaultPc
        if (shell.screen == Screen.Start && start.skipStart && default != null) {
            repo.awaitPc(default, 4_000) { it.paired }?.let { devices.connect(it) }
        }
    }

    // Links and shortcuts, once there is a profile (the first launch keeps them until then). Links can
    // come from any web page or app, so pairing and starting a game ask first (a security prompt)
    val link by links.collectAsStateWithLifecycle()
    LaunchedEffect(link, profiles.all.isEmpty()) {
        val l = link ?: return@LaunchedEffect
        if (profiles.all.isEmpty()) return@LaunchedEffect
        links.value = null
        when (l) {
            is ShellLink.Pair -> {
                feedback.play(Cue.OPEN)
                val who = l.name?.let { "$it (${l.address})" } ?: l.address
                shell.openLayer(
                    Layer.Dialog(
                        DialogSpec(res.getString(R.string.shell_link_pair_title, who), res.getString(R.string.shell_link_pair_message), emptyList(), res.getString(R.string.shell_link_pair_action)) {
                            devices.addAndPair(l.address, l.otp, Screen.Start)
                            true
                        }
                    ),
                    tracker,
                )
            }
            is ShellLink.Launch -> {
                feedback.play(Cue.OPEN)
                val game = l.appName?.takeIf { it.isNotEmpty() }
                val host = l.hostName?.takeIf { it.isNotEmpty() }
                val title = if (game != null) res.getString(R.string.shell_link_start_title, game) else res.getString(R.string.shell_link_start_title_unnamed)
                val message = when {
                    game != null && host != null -> res.getString(R.string.shell_link_start_message, game, host)
                    game != null -> res.getString(R.string.shell_link_start_message_game, game)
                    host != null -> res.getString(R.string.shell_link_start_message_host, host)
                    else -> res.getString(R.string.shell_link_start_message_unnamed)
                }
                shell.openLayer(
                    Layer.Dialog(
                        DialogSpec(title, message, emptyList(), res.getString(R.string.shell_link_start_action)) {
                            activity?.startActivity(l.intent)
                            true
                        }
                    ),
                    tracker,
                )
            }
            is ShellLink.OpenPc -> repo.awaitPc(l.uuid, 4_000) { it.paired }?.let { devices.connect(it) }
        }
    }

    // A stream that ended on its own (it never started, the connection broke, the PC closed it): Home of
    // that PC first, then a toast or the result overlay. Shown once; the activity only held it until here.
    val streamResult by results.collectAsStateWithLifecycle()
    LaunchedEffect(streamResult) {
        val r = streamResult ?: return@LaunchedEffect
        // Cleared only once it has been taken over below (the change restarts this effect)
        if (r.pcUuid.isNotEmpty()) repo.selectPc(r.pcUuid)
        if (profiles.all.isNotEmpty() && shell.screen !is Screen.Boot && shell.screen !is Screen.CreateProfile) shell.navigate(Screen.Home)
        if (r.kind == StreamResultKind.HOST_CLOSED) {
            results.value = null
            shell.showToast(r.title, Symbols.INFO)
            return@LaunchedEffect
        }
        // The launch scene that was waiting for the stream goes away when the shell resumes; the overlay follows it
        withTimeoutOrNull(3_000) { snapshotFlow { shell.launch == null }.first { it } }
        feedback.play(Cue.OPEN)
        results.value = null
        shell.result = r
        // A failed stream is also a problem the user may report: the card follows once the result has been answered
        ProblemMonitor.streamFailed(appContext, r.title, r.details)?.let { if (shell.problem == null) shell.problem = it }
    }

    // A crash, ANR or unclean exit of the last run: one card offers the report, once per problem (docs/DIAGNOSTICS.md)
    LaunchedEffect(Unit) {
        val found = withContext(Dispatchers.IO) { ProblemMonitor.detectAtStart(appContext) }
        if (found != null && shell.problem == null) shell.problem = found
    }
    fun dismissProblem() {
        val p = shell.problem ?: return
        shell.problem = null
        ProblemMonitor.resolve(p)
        scope.launch {
            delay(400)
            feedback.silenceNextFocus()
            runCatching { shell.restoreScreenFocus?.invoke() }
        }
    }
    // Report: upload the cleaned log to the report service and open the prefilled GitHub issue with its link; when the upload is
    // off, fails or the user chose Downloads, save the log there and let the user attach it (docs/DIAGNOSTICS.md)
    fun reportProblem(mode: ReportMode) {
        val p = shell.problem ?: return
        shell.problem = null
        scope.launch {
            if (mode == ReportMode.UPLOAD && ReportUploader.shared.enabled) shell.showToast(ReportOutcomeText.uploading(res), Symbols.REPORT_ISSUE)
            val result = ProblemReporter.report(appContext, p, mode)
            ProblemMonitor.resolve(p)
            shell.showToast(ReportOutcomeText.text(res, result), if (ReportOutcomeText.succeeded(result)) Symbols.CHECK_CIRCLE else Symbols.WARNING, durationMs = 6_500)
            delay(900)
            if (!ProblemReporter.openIssue(appContext, result.issueUrl)) {
                feedback.play(Cue.ERROR)
                shell.showToast(res.getString(R.string.shell_report_no_browser), Symbols.ERROR)
            }
        }
    }
    // An update (docs/UPDATES.md): the check on app start is silent unless a newer version can be installed; the card follows the
    // problem card and shows on the same screens and never over a layer, the portal, the launch loader or the result
    val updates = remember { Updates.controller(appContext) }
    val updateState by updates.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { updates.checkInBackground() }
    val updateRequested by updates.requested.collectAsStateWithLifecycle()
    // A background offer waits for the start or home screen; an offer from Check now shows over Settings at once
    val updateScreen = shell.screen == Screen.Home || shell.screen == Screen.Start || (updateRequested && (shell.screen == Screen.Settings || shell.screen == Screen.SettingsSearch))
    val updateShown = updateState !is UpdateState.Idle && shell.problem == null && updateScreen &&
        shell.layer == null && shell.portal == null && shell.launch == null && shell.result == null
    LaunchedEffect(updateShown) { updateGlass = updateShown }
    // The verified file goes to the system installer only while the card shows, so Android's prompt never lands on a stream or a loader
    LaunchedEffect(updateShown, updateState) { if (updateShown && updateState is UpdateState.Verified) updates.installVerified() }
    // Back from the system page where the user allowed installing: the update goes on
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { updates.onResume() }
    fun updateBack() {
        feedback.play(Cue.BACK)
        if (updateState is UpdateState.Downloading) updates.cancel() else updates.later()
        scope.launch {
            delay(400)
            feedback.silenceNextFocus()
            runCatching { shell.restoreScreenFocus?.invoke() }
        }
    }
    fun dismissResult() {
        shell.result = null
        scope.launch {
            delay(400)
            // A failed stream is followed by the report card, which took the focus when it appeared: the library must not take it back,
            // or the card cannot be reached with a controller (found on the 1.0.0 release candidate)
            if (shell.offerShown) return@launch
            feedback.silenceNextFocus()
            runCatching { shell.restoreScreenFocus?.invoke() }
        }
    }
    // Try again: the same game on the same PC, started the way Home starts it (the per-game settings are read again)
    fun retryStream() {
        val r = shell.result ?: return
        shell.result = null
        scope.launch {
            val target = if (r.pcUuid.isEmpty()) null else repo.awaitPc(r.pcUuid, 4_000) { it.paired && it.online }
            if (target == null) {
                feedback.play(Cue.ERROR)
                shell.showToast(res.getString(R.string.shell_retry_unreachable, r.pcName), Symbols.WIFI_OFF)
                return@launch
            }
            repo.selectPc(target.uuid)
            val ready = withTimeoutOrNull(5_000) { repo.library.first { it is Library.Ready && it.pc.uuid == target.uuid } } as? Library.Ready
            val app = ready?.apps?.let { apps -> apps.firstOrNull { it.id == r.appId } ?: apps.firstOrNull { it.name == r.appName } }
            if (app == null) {
                feedback.play(Cue.ERROR)
                shell.showToast(
                    if (r.appName.isEmpty()) res.getString(R.string.shell_retry_game_missing_unnamed, target.name) else res.getString(R.string.shell_retry_game_missing, r.appName, target.name),
                    Symbols.ERROR,
                )
                return@launch
            }
            val cover = activity?.let { withContext(Dispatchers.IO) { StreamLoader.loadCover(it, target.uuid, app.id) } }
            actions.start(target, app, cover)
        }
    }

    // The stream window inherits the last drawn tunnel progress, scene origin and game identity.
    LaunchedEffect(shell.launch?.id) {
        val launch = shell.launch ?: return@LaunchedEffect
        launchExitInstant.value = false
        LaunchHandoff.background = launch.background
        LaunchHandoff.cancelRequested = false
        LaunchHandoff.heroTurn = if (appearance.reducedMotion || activity?.let(HeroMotion::animatorsOff) == true) Float.NaN else HeroMotion.turnAt(System.nanoTime())
        LaunchHandoff.progress = 0f
        LaunchHandoff.travel = 0f
        LaunchHandoff.speedProgress = 0f
        // The first witty connection line is told here already; the stream window goes on with the next ones
        StreamLoader.resetLines()
        launchLine.value = null
        delay(if (appearance.reducedMotion) 150 else 350)
        launchLine.value = StreamLoader.firstLine(res, launch.pc.name)
        delay(if (appearance.reducedMotion) 150 else 350)
        shell.diving = true
        // Only a game that really opens counts as last played (not one cancelled in the loader)
        repo.setLastPlayed(launch.pc.uuid, launch.app.id)
        // The stream (DelosStreamActivity, launchMode singleTask) opens in its own task, where the system ignores custom
        // activity animations and slides the new window in. A scale-up is honoured for new tasks; from the
        // whole screen it only fades the stream window in over this one (a clip reveal also shifts it up a
        // little), and that starts with the same scene.
        val options = activity?.window?.decorView?.let { root ->
            DualScreen.placeOnMain(root.context, ActivityOptions.makeScaleUpAnimation(root, 0, 0, root.width, root.height)).toBundle()
        }
        activity?.startActivity(launch.intent, options)
    }
    // Back from the stream: the overlay goes away, the focus stays on the game, the PC's state is refreshed.
    // Left through the stream menu, the portal's burst that began over the stream ends here.
    // The stream window fully covers this one: the launch overlay (and its tunnel) goes away now, not at the next resume,
    // where it would flash for a few frames when the stream is left. The resume work is remembered for that moment.
    // The system keeps a snapshot of this window from the moment it stops and shows it while the window comes back to the
    // front (recorded on the Odin: the tunnel flashed for 0.3 s on leaving a stream, and the log proved the overlay itself was
    // already gone: it was the snapshot). The snapshot is taken at stop, about 0.45 s after the pause, so the overlay is swapped
    // for the opaque background of the exit scene early enough for a frame of it to be drawn before that.
    fun handOverToStream() {
        if (shell.launch != null && shell.diving) {
            shell.exitCover = true
            shell.exitLights = false
            shell.launch = null
            shell.diving = false
            returningFromStream.value = true
            launchExitInstant.value = true
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) {
        if (shell.launch != null && shell.diving) scope.launch {
            delay(HAND_OVER_DELAY_MS)
            handOverToStream()
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { handOverToStream() }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        val arrival = StreamExit.take()
        // Not coming back through the exit portal (a cancelled or failed launch): the cover of the hand-over goes away
        if (arrival == null && shell.exitCover) {
            shell.exitCover = false
            shell.exitLights = false
        }
        arrival?.let { exit ->
            scope.launch {
                // Quit game: the portal waits, with its text and pulses, until the PC has really quit the game (at most 25 s),
                // and only then bursts into the dashboard. Disconnect bursts at once.
                val waitForQuit = StreamExit.quitting
                // The chosen animated background fades in over the dashboard first (the dashboard's own background still
                // settles after the return), and only then does the portal burst into it
                shell.exitCover = true
                // The flat background first (what the system window and the end of the stream window's scene showed), then the lights
                shell.exitLights = true
                shell.portalBurst = false
                shell.portal = Portal(-1, if (waitForQuit) Symbols.POWER_SETTINGS_NEW else Symbols.HOME, exit.label, exit.sub, cancellable = false, arriving = true)
                delay(if (waitForQuit) 400 else 600)
                if (waitForQuit) {
                    var waited = 0
                    while (StreamExit.quitting && waited < 25_000) {
                        delay(100)
                        waited += 100
                    }
                }
                // The loader just ends: the background and the portal fade away together into the dashboard, no second scene
                feedback.play(Cue.SELECT)
                shell.exitCover = false
                shell.exitLights = false
                shell.portal = null
            }
        }
        if (returningFromStream.value || (shell.launch != null && shell.diving)) {
            returningFromStream.value = false
            shell.launch = null
            shell.diving = false
            repo.refresh()
            tracker.requester?.let {
                feedback.silenceNextFocus()
                runCatching { it.requestFocus() }
            }
        }
    }
    val wifi by rememberWifiStatus()
    val battery by rememberBatteryStatus()
    val clock by rememberClock(appearance.clock)
    val library by repo.library.collectAsStateWithLifecycle()
    val metadata = LocalGameMetadata.current
    // The library filter's choices while it is open: applied only on confirm, dropped when it closes without
    val filterDraft = remember(metadata) { FilterDraft({ metadata.filters }, metadata::updateFilters) }
    val openMetadata: (Pc, App) -> Unit = remember(scope, shell, tracker, metadata) {
        { selectedPc, app -> openMetadataMenu(res, scope, shell, tracker, metadata, selectedPc, app) }
    }
    LaunchedEffect(metadata, profiles.current?.id, (library as? Library.Ready)?.let { it.pc.uuid to it.apps.map { app -> app.id to app.name } }) {
        val ready = library as? Library.Ready ?: return@LaunchedEffect
        metadata.ensureLibrary(ready.pc, ready.apps)
    }
    // SteamGridDB: games never looked up get their best-rated artwork in the background
    val artwork = LocalArtwork.current
    LaunchedEffect((library as? Library.Ready)?.let { it.pc.uuid to it.apps.map { a -> a.id } }, artwork.enabled, artwork.automatic) {
        val ready = library as? Library.Ready ?: return@LaunchedEffect
        if (!artwork.enabled || !artwork.automatic) return@LaunchedEffect
        artwork.downloadAll(ready.pc, ready.apps, refresh = false) { shell.showToast(it.text(res), Symbols.ERROR) }
    }
    val showHidden by repo.showHidden.collectAsStateWithLifecycle()
    val uiScale = appearance.uiScale / 100f
    val pc = when (val l = library) {
        is Library.Ready -> l.pc
        is Library.Offline -> l.pc
        else -> null
    }
    // The companion screen of a dual-screen device shows the same PC as the status bar
    val companionPc = pc?.name.takeIf { shell.screen.main }
    LaunchedEffect(companionPc) { shell.pcName = companionPc }

    // Touch: a drag in from the left edge opens the side menu and the panel follows the finger; a drag to the left closes it
    // (SideMenuSwipe). Reduced motion: the drag only toggles the menu once it is long enough.
    val sideDrag = remember { SideMenuDrag() }
    val sideWidth by rememberUpdatedState(sideMenuWidthPx())
    val density = LocalDensity.current
    val sideFling by rememberUpdatedState(with(density) { SideMenuSwipe.FLING_DP_PER_S.dp.toPx() })

    fun toggleSide() {
        if (shell.layer != null && shell.layer != Layer.Side) return
        // A touch drag owns the menu until it has settled
        if (sideDrag.engaged) return
        if (shell.layer == Layer.Side) {
            feedback.play(Cue.BACK)
            shell.closeLayer()
        } else {
            feedback.play(Cue.OPEN)
            shell.openLayer(Layer.Side, tracker)
        }
    }

    fun sideSwipeOpens() = shell.screen.sideMenu && shell.overviewGame == null && shell.layer == null && shell.picker == null && shell.launch == null &&
        shell.portal == null && shell.result == null && shell.buttonCapture == null

    fun sideSwipeCloses() = shell.layer == Layer.Side && shell.picker == null

    fun startSideDrag(opening: Boolean) {
        if (reducedMotion) return
        sideDrag.settle?.cancel()
        sideDrag.start = when {
            opening -> 0f
            sideDrag.engaged -> sideDrag.progress.coerceAtMost(1f)
            else -> 1f
        }
        sideDrag.progress = sideDrag.start
        sideDrag.engaged = true
        if (opening) shell.openLayer(Layer.Side, tracker)
    }

    fun moveSideDrag(travel: Float) {
        if (!reducedMotion && sideDrag.engaged) sideDrag.progress = SideMenuSwipe.progress(sideDrag.start, travel, sideWidth)
    }

    fun endSideDrag(opening: Boolean, travel: Float, velocity: Float) {
        if (reducedMotion) {
            if (SideMenuSwipe.reducedToggles(opening, travel, sideWidth)) toggleSide()
            return
        }
        if (!sideDrag.engaged) return
        val open = SideMenuSwipe.settlesOpen(opening, sideDrag.progress, velocity, sideFling)
        if (open && opening) feedback.play(Cue.OPEN)
        if (!open && !opening) feedback.play(Cue.BACK)
        sideDrag.settle = scope.launch {
            animate(sideDrag.progress, if (open) 1f else 0f, velocity / sideWidth, spring(Motion.GEO_DAMPING, Motion.GEO_STIFFNESS)) { value, _ ->
                sideDrag.progress = value
            }
            if (!open) {
                if (shell.layer == Layer.Side) shell.closeLayer()
                // The panel and the scrim leave from where the drag left them (no exit transition) before they take their transitions back
                delay(Motion.EXIT.toLong())
            }
            sideDrag.engaged = false
        }
    }

    fun confirmFilters() {
        if (shell.layer != Layer.Filters) return
        feedback.play(Cue.BACK)
        filterDraft.confirm()
        shell.closeLayer()
    }

    fun toggleFilters() {
        when (shell.layer) {
            // Y in the open filter applies its choices, as its "Show N apps" pill does
            Layer.Filters -> confirmFilters()
            null -> {
                feedback.play(Cue.OPEN)
                filterDraft.begin()
                shell.openLayer(Layer.Filters, tracker)
            }
            else -> Unit
        }
    }

    fun toggleContext() {
        when (shell.layer) {
            is Layer.Context -> {
                feedback.play(Cue.BACK)
                shell.closeLayer()
            }
            null -> {
                // A settings row's Options opens its popup, the one a tap opens (it plays its own cue)
                tracker.options?.let { it(); return }
                val menu = tracker.contextMenu ?: return
                feedback.play(Cue.OPEN)
                shell.openLayer(Layer.Context(menu, tracker.bounds()), tracker)
            }
            else -> Unit
        }
    }

    DisposableEffect(keys) {
        keys.canUndo = { shell.toast?.action != null && shell.portal == null && shell.launch == null && shell.result == null }
        keys.onUndo = { shell.runToastAction() }
        // Y: settings search, or the artwork page's filter sheet
        keys.canSearch = {
            shell.layer == null && shell.picker == null && shell.portal == null && shell.launch == null &&
                (shell.screen == Screen.Settings || (shell.screen is Screen.Artwork && shell.onArtworkFilter != null))
        }
        keys.onSearch = { if (shell.screen is Screen.Artwork) shell.onArtworkFilter?.invoke() else shell.onSettingsSearch?.invoke() }
        // Y opens the filters over a library with games (and closes them again); nothing else may react to it
        keys.canFilter = {
            shell.screen == Screen.Home && shell.overviewGame == null && shell.picker == null && shell.portal == null && shell.launch == null && shell.result == null && (
                shell.layer == Layer.Filters || (shell.layer == null && (library as? Library.Ready)?.apps?.isNotEmpty() == true)
            )
        }
        keys.onFilter = { toggleFilters() }
        keys.listener = listener@{ code ->
            if (shell.launch != null || shell.portal != null || shell.result != null || shell.picker != null) return@listener
            when (code) {
                // Read the screen now: this listener is set once, so a value captured at the first composition (the first launch) would stay false
                KeyEvent.KEYCODE_BUTTON_SELECT -> if (shell.screen.sideMenu && shell.overviewGame == null) toggleSide()
                KeyEvent.KEYCODE_BUTTON_START -> toggleContext()
                KeyEvent.KEYCODE_BUTTON_X -> if (shell.layer == Layer.Filters) {
                    if (filterDraft.value.active) {
                        feedback.play(Cue.SELECT)
                        filterDraft.reset()
                    }
                } else if (shell.screen is Screen.Artwork && shell.layer == null && shell.onArtworkSearch != null) shell.onArtworkSearch?.invoke()
                else if (shell.screen is Screen.GameSettings && shell.layer == null && shell.onGameReset != null) shell.onGameReset?.invoke()
                else toggleContext()
                KeyEvent.KEYCODE_BUTTON_L1 -> if (shell.layer == null) shell.onShoulder?.invoke(-1)
                KeyEvent.KEYCODE_BUTTON_R1 -> if (shell.layer == null) shell.onShoulder?.invoke(1)
            }
        }
        onDispose {
            keys.listener = null
            keys.onUndo = null
            keys.canUndo = { false }
            keys.onSearch = null
            keys.canSearch = { false }
            keys.onFilter = null
            keys.canFilter = { false }
        }
    }

    BackHandler(enabled = shell.layer != null || shell.picker != null || shell.screen.back != null) {
        feedback.play(Cue.BACK)
        shell.back()
    }
    // B in the portal only cancels waiting for a PC to wake up; nothing underneath moves meanwhile
    BackHandler(enabled = shell.portal != null) { devices.cancelPortal() }
    // B during the loader cancels the start (registered last, so it wins while a game is starting); the companion's Cancel runs the same function
    fun cancelLaunch() {
        if (shell.launch == null) return
        feedback.play(Cue.BACK)
        // The stream window was already asked for: it ends its own attempt (now, or at the end of its onCreate) and the overlay goes away at once
        if (shell.diving) StreamLoader.cancelFromShell()
        shell.launch = null
        shell.diving = false
        tracker.requester?.let {
            feedback.silenceNextFocus()
            runCatching { it.requestFocus() }
        }
    }
    BackHandler(enabled = shell.launch != null) { cancelLaunch() }
    // B on the result overlay is Back to library (registered last, so it wins while the overlay is shown)
    BackHandler(enabled = shell.result != null) {
        feedback.play(Cue.BACK)
        dismissResult()
    }
    // B on the report card is Dismiss, wherever the focus is while the card shows
    BackHandler(enabled = shell.offerShown) {
        feedback.play(Cue.BACK)
        dismissProblem()
    }

    // B on the update card is Later (Cancel while downloading), registered after the report card's so the report wins while both could show
    BackHandler(enabled = updateShown && updateState !is UpdateState.Verified && updateState !is UpdateState.Installing) { updateBack() }

    // The filter modal belongs to a ready library: a PC that goes offline takes it down with it
    LaunchedEffect(shell.layer, library is Library.Ready) {
        if (shell.layer == Layer.Filters && library !is Library.Ready) shell.closeLayer()
    }

    // When a layer has closed, the focus returns to where it was (silently, like the prototype)
    LaunchedEffect(shell.layer) {
        if (shell.layer != null) return@LaunchedEffect
        val previous = shell.takeReturnFocus() ?: return@LaunchedEffect
        feedback.silenceNextFocus()
        val restored = runCatching { previous.requestFocus() }.getOrDefault(false)
        if (!restored) runCatching { shell.restoreScreenFocus?.invoke() }
        // The held element has the focus again by now (it let go of its hold then); otherwise the hold must not outlive the menu
        delay(150)
        tracker.release()
    }

    // When a row's popup has closed, the focus returns to the row (silently)
    LaunchedEffect(shell.picker) {
        if (shell.picker != null) return@LaunchedEffect
        val previous = shell.takePickerReturn() ?: return@LaunchedEffect
        feedback.silenceNextFocus()
        val restored = runCatching { previous.requestFocus() }.getOrDefault(false)
        if (!restored) runCatching { shell.restoreScreenFocus?.invoke() }
        delay(150)
        tracker.release()
    }

    val profile = profiles.current
    val configuration = LocalConfiguration.current
    val nav = remember(shell, shell.screen, configuration) {
        listOf(
            NavButton(Symbols.SETTINGS, res.getString(R.string.shell_nav_settings), shell.screen == Screen.Settings) { shell.navigate(Screen.Settings) },
            NavButton(Symbols.ACCOUNT_CIRCLE, res.getString(R.string.shell_nav_profile), shell.screen == Screen.Profile) { shell.navigate(Screen.Profile) },
            NavButton(Symbols.DEVICES, res.getString(R.string.shell_nav_devices), shell.screen == Screen.Devices) { shell.navigate(Screen.Devices) },
        )
    }

    // The keyboard never covers the text field being edited: the dialog or the page content that holds it moves up (docs/GUI.md, "Text input")
    val imeAvoidance = rememberImeAvoidance()
    // Where the status bar's Wi-Fi, battery and clock start: the start screen's profile chip keeps clear of them on a compact stage
    val statusEdges = remember { com.limelight.shell.ui.StatusBarEdges() }
    // The overview viewer's player window covers the hint bar below: it draws this one over the video
    val windowHintBar: @Composable () -> Unit = remember(shell, uiScale) { { WindowHintBar(shell, uiScale) } }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch(Dispatchers.IO) {
            val result = runCatching {
                val json = BackupManager.exportJson(activityContext, profileStore)
                activityContext.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(json.toByteArray(Charsets.UTF_8))
                } ?: error("Could not open output stream")
            }
            withContext(Dispatchers.Main) {
                result.onSuccess {
                    shell.showToast(res.getString(R.string.settings_native_backup_exported), Symbols.CHECK_CIRCLE, kind = ToastKind.SUCCESS)
                }.onFailure { e ->
                    shell.showToast(res.getString(R.string.settings_native_backup_failed, e.message ?: ""), Symbols.ERROR, kind = ToastKind.ERROR)
                }
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch(Dispatchers.IO) {
            val result = runCatching {
                val raw = activityContext.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                    ?: error("Could not read backup file")
                BackupManager.importJson(activityContext, raw, profileStore)
            }
            withContext(Dispatchers.Main) {
                result.onSuccess { info ->
                    repo.reloadRestoredComputers()
                    repo.refresh()
                    shell.showToast(
                        res.getString(R.string.settings_native_backup_imported, info.profileCount, info.pcCount),
                        Symbols.CHECK_CIRCLE,
                        kind = ToastKind.SUCCESS,
                    )
                    if (shell.screen is Screen.Boot) shell.navigate(Screen.Start)
                }.onFailure { e ->
                    shell.showToast(res.getString(R.string.settings_native_backup_failed, e.message ?: ""), Symbols.ERROR, kind = ToastKind.ERROR)
                }
            }
        }
    }
    val backupActions = remember {
        BackupActions(
            export = {
                val stamp = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                exportLauncher.launch("delos-backup-$stamp.delos.json")
            },
            import = {
                importLauncher.launch(arrayOf("application/json", "*/*"))
            },
        )
    }
    CompositionLocalProvider(
        LocalShell provides shell, LocalFocusTracker provides tracker, LocalGameActions provides actions, LocalDeviceActions provides devices,
        LocalBackupActions provides backupActions,
        LocalBackdropHaze provides backdropHaze, LocalBackdropMirror provides backdropMirror, LocalLibrary provides library, LocalImeAvoidance provides imeAvoidance,
        com.limelight.shell.screens.LocalLinkTests provides linkTests, com.limelight.shell.ui.LocalStatusBarEdges provides statusEdges,
        // Touch: a focused choice or slider row's tap opens its popup at the row (issue #92)
        LocalRowPicker provides { popup -> shell.openPicker(popup, tracker) },
        LocalWindowHintBar provides windowHintBar,
        // Touch: a long press on a cover (or any element with a context menu) opens it, as Start does
        LocalContextMenuOpener provides { menu, bounds ->
            if (longPressOpensContext(menu, true, shell.layer != null, shell.launch != null || shell.portal != null || shell.result != null || shell.picker != null)) {
                feedback.play(Cue.OPEN)
                shell.openLayer(Layer.Context(menu, bounds ?: tracker.bounds()), tracker)
            }
        },
    ) {
        // Gesture navigation reserves the left edge for Back: a band of it in the middle (Android grants at most 200 dp per edge) is
        // kept for the side menu's swipe while it can start; with 3-button navigation the whole edge works
        val sideEdge = with(density) { SideMenuSwipe.EDGE_DP.dp.toPx() }
        val sideExclusionMax = with(density) { SideMenuSwipe.MAX_EXCLUSION_DP.dp.toPx() }
        Box(
            Modifier
                .fillMaxSize()
                .then(if (sideSwipeOpens()) Modifier.systemGestureExclusion { coordinates ->
                    val band = SideMenuSwipe.exclusion(coordinates.size.height.toFloat(), sideEdge, sideExclusionMax)
                    Rect(band.left, band.top, band.right, band.bottom)
                } else Modifier)
                .sideMenuSwipe(
                    canOpen = ::sideSwipeOpens, isOpen = ::sideSwipeCloses,
                    onStart = ::startSideDrag, onMove = { _, travel -> moveSideDrag(travel) }, onEnd = ::endSideDrag,
                )
                // A right click beside the items of an open layer (a menu, the side menu, a popup) closes it, as B does; one on an item
                // is the item's (focusEffects: its Options)
                .secondaryClickOutside {
                    if (shell.layer != null || shell.picker != null) {
                        feedback.play(Cue.BACK)
                        shell.back()
                    }
                },
        ) {
            // The blur source re-records the whole screen every frame, so it is only active while an overlay
            // is shown (and while it fades out)
            // On in the very frame an overlay appears (its blur would otherwise start empty and clipped)
            Box(Modifier.fillMaxSize().then(if (blurSource || screenGlass || shell.result != null) Modifier.hazeSource(haze) else Modifier)) {
                Box(Modifier.fillMaxSize().hazeSource(backdropHaze).backdropMirrorSource(backdropMirror)) {
                    ShellBackdrop(shell, appearance, colors, heroShown = shell.screen == Screen.Home, animate = shell.launch == null && shell.portal == null)
                }
                AnimatedContent(
                    shell.screen,
                    // `.screen` (docs/GUI.md, "Motion"): the shared transition, only fades with reduced motion
                    transitionSpec = { screenTransition(appearance.reducedMotion) },
                    label = "screen",
                ) { screen ->
                    when (screen) {
                        Screen.Home -> HomeScreen(library, appearance, repo, profileStore, openMetadata)
                        Screen.Settings -> SettingsScreen(appearance, update, startup, startupStore::update, pcs, profileStore)
                        Screen.SettingsSearch -> SettingsScreen(appearance, update, startup, startupStore::update, pcs, profileStore, search = true)
                        Screen.Profile -> ProfileScreen(profiles, profileStore, pcs.firstOrNull { it.uuid == startup.defaultPc }?.name)
                        Screen.Devices -> DevicesScreen(pcs, pc?.uuid, startup.defaultPc)
                        Screen.Start -> StartScreen(profiles, profileStore, pcs, startup)
                        is Screen.Boot -> BootScreen(
                            onBegin = { shell.navigate(Screen.CreateProfile(editing = false, back = screen)) },
                            // PCs paired in the classic UI already: a default profile, then straight into home
                            onSkip = pcs.filter { it.paired }.let { paired ->
                                if (paired.isEmpty()) null else {
                                    {
                                        profileStore.create(res.getString(R.string.shell_default_profile_name), AVATARS.indexOfFirst { it.name == "Player" }.coerceAtLeast(0)) // l10n-ignore
                                        devices.connect(paired.firstOrNull { it.online } ?: paired.first())
                                    }
                                }
                            },
                            onImport = { backupActions.import() },
                        )
                        is Screen.CreateProfile -> CreateProfileScreen(screen.editing, profile.takeIf { screen.editing }) { name, avatar ->
                            if (screen.editing) {
                                profile?.let { p -> profileStore.update(p.id) { it.copy(name = name, avatar = avatar) } }
                                shell.showToast(res.getString(R.string.shell_profile_updated), Symbols.CHECK_CIRCLE)
                                shell.navigate(screen.back ?: Screen.Profile)
                            } else {
                                profileStore.create(name, avatar)
                                shell.showToast(res.getString(R.string.shell_welcome, name), Symbols.WAVING_HAND, kind = ToastKind.SUCCESS)
                                // The first launch goes on to pairing, unless PCs are paired already
                                shell.navigate(
                                    when {
                                        screen.back !is Screen.Boot -> screen.back ?: Screen.Start
                                        pcs.none { it.paired } -> Screen.PairPc(back = Screen.Start)
                                        else -> Screen.Start
                                    }
                                )
                            }
                        }
                        is Screen.PairPc -> PairPcScreen(screen, pcs, startup.defaultPc)
                        is Screen.Artwork -> ArtworkScreen(screen)
                        is Screen.GameSettings -> GameSettingsScreen(screen, profileStore, appearance)
                        Screen.DebugInfo -> DebugInfoScreen()
                        Screen.OscPreview -> OscPreviewScreen()
                        is Screen.Pin -> PinScreen(
                            screen, repo, setup,
                            onFailed = { message ->
                                feedback.play(Cue.ERROR)
                                shell.showToast(message, Symbols.ERROR)
                                shell.navigate(screen.back)
                            },
                            onCancel = { shell.navigate(screen.back) },
                        )
                    }
                }
                // The on-screen controls preview is full screen like a stream
                if (shell.screen != Screen.OscPreview) ZoomedBar(uiScale) {
                    // The back button (no controller in use) does what B does: the same back dispatch, so a screen's own handler wins as for B
                    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
                    val goBack = remember(backDispatcher) { { backDispatcher?.onBackPressed(); Unit } }
                    // The game overview's screenshot viewer covers the whole screen, the status bar too
                    val barAlpha by androidx.compose.animation.core.animateFloatAsState(
                        if (shell.overviewViewer != null) 0f else 1f, tween(Motion.FADE, easing = Motion.Ease), label = "statusBarUnderViewer",
                    )
                    Box(Modifier.graphicsLayer { alpha = barAlpha }) {
                        StatusBar(
                            nav.takeIf { shell.screen.main }, pcName = pc?.name.takeIf { shell.screen.main }, pcOnline = pc?.online == true,
                            wifi = wifi, battery = battery, clock = clock, brand = shell.screen != Screen.Start,
                            // The game overview goes back with the same button (its own back handler closes it), the dashboard has none
                            onBack = goBack.takeIf { shell.screen.back != null || shell.overviewGame != null },
                            cellularLevel = com.limelight.utils.rememberCellularSignalLevel(),
                        )
                    }
                }
            }

            // Overlays sit on their own glass: controls in them must not blur the background through it
            CompositionLocalProvider(LocalBackdropHaze provides null) {
                // While a touch drag moves the side menu, the scrim follows its progress (also as it leaves after a drag closed it)
                val sideProgress: (() -> Float)? = if (sideDrag.engaged && (shell.layer == Layer.Side || shell.layer == null)) ({ sideDrag.progress }) else null
                Scrim(shell.layer != null, light = shell.layer is Layer.Context, haze, progress = sideProgress) {
                    feedback.play(Cue.BACK)
                    shell.closeLayer()
                }
                val context = shell.layer as? Layer.Context
                ContextMenuLayer(context?.menu, context?.anchor, shell.menuPath, { shell.menuPath = it }, haze) { shell.closeLayer() }
                SideMenuLayer(
                    shell.layer == Layer.Side,
                    haze,
                    drag = if (sideDrag.engaged) ({ sideDrag.progress }) else null,
                    header = {
                        SideMenuHeader(
                            avatar = { profile?.let { AvatarBadge(it.look, 72f, 40f) } },
                            name = profile?.name ?: "Delos", // l10n-ignore
                            pc = pc?.let { SidePc(it.name, SidePcState.of(it, connecting = shell.launch?.pc?.uuid == it.uuid)) },
                            playtime = rememberProfilePlaytime(profileStore),
                        )
                    },
                ) { first ->
                    val hiddenCount = (library as? Library.Ready)?.hiddenCount ?: 0
                    val hiddenRow = hiddenCount > 0 || showHidden
                    val rowKeys = remember(hiddenRow) {
                        listOf("theme", "material-you", "background", "sounds", "haptics", "settings") +
                            if (hiddenRow) listOf("show-hidden") else emptyList()
                    }
                    val navigation = rememberFocusedListNavigation(rowKeys, allowUpExit = false)
                    SideMenuScroll(navigation) {
                        SideMenuRows(appearance, update, navigation, Modifier.focusRequester(first)) { shell.navigate(Screen.Settings) }
                        if (hiddenRow) {
                            SettingRow(
                                Symbols.VISIBILITY, res.getString(R.string.shell_side_show_hidden), RowValue.Toggle(showHidden) { repo.setShowHidden(it) },
                                Modifier.focusedListRow(navigation, "show-hidden"),
                                description = pc?.let { res.getString(R.string.shell_side_hidden_on, hiddenCount, it.name) } ?: res.getString(R.string.shell_side_hidden_on_this_pc, hiddenCount), compact = true,
                            )
                        }
                    }
                }

                DialogLayer((shell.layer as? Layer.Dialog)?.dialog, haze) {
                    feedback.play(Cue.BACK)
                    shell.closeLayer()
                }

                FilterModalLayer(shell.layer == Layer.Filters, library as? Library.Ready, filterDraft, haze, appearance.reducedMotion) { confirmFilters() }

                // The popup of a settings row (touch, issue #92): over every layer, the side menu's rows open it too
                RowPickerLayer(shell.picker, haze) { shell.closePicker() }

                // Fully there from the first frame: the stream window's portal already ended on this very background
                val cover by animateFloatAsState(if (shell.exitCover) 1f else 0f, if (shell.exitCover) snap() else tween(350, easing = EaseOut), label = "exitCover")
                val lights by animateFloatAsState(if (shell.exitLights) 1f else 0f, tween(if (shell.exitLights) 400 else 350, easing = EaseOut), label = "exitLights")
                if (cover > 0f) {
                    // Opaque flat background at once (it hides the dashboard and is the color the system window showed), the lights fade in over it
                    Box(Modifier.fillMaxSize().graphicsLayer { alpha = cover }.background(colors.bg)) {
                        Box(Modifier.fillMaxSize().graphicsLayer { alpha = lights }) {
                            ShellBackdrop(shell, appearance, colors, heroShown = appearance.background == BackgroundKind.STILL, animate = false)
                        }
                    }
                }
                // The portal of a stream exit sits on the opaque cover: it must not blur the dashboard behind (that was the third scene)
                PortalOverlay(shell.portal, shell.portalBurst, if (shell.portal?.arriving == true) null else haze, appearance.reducedMotion, onCancel = devices::cancelPortal)

                StreamResultLayer(shell.result, haze, appearance.reducedMotion, onRetry = ::retryStream, onBack = ::dismissResult)

                ButtonCaptureLayer(shell.buttonCapture != null, keys, haze) { buttons ->
                    val save = shell.buttonCapture
                    shell.buttonCapture = null
                    if (buttons != null) save?.invoke(buttons)
                }
            }

            val artworkProgress = artwork.progress.value
            ArtworkProgressBar(artworkProgress ?: metadata.progress.value,
                label = res.getString(if (artworkProgress != null) R.string.shell_progress_artwork else R.string.shell_progress_metadata))
            ShellHints(shell, library, pcs, tracker, keys, uiScale)
            val launch = shell.launch
            val launchLogo = launch?.let { rememberArtwork(artwork, it.pc, it.app, ArtKind.LOGO).value }
            // Read off the main thread: profile and per-game settings; the card gets them when they are ready
            val launchParameters by produceState("", launch?.id) {
                value = launch?.let { item -> activity?.let { withContext(Dispatchers.IO) { StreamLoader.parametersFor(it, item.pc.uuid, item.app.id) } } }.orEmpty()
            }
            LaunchOverlay(
                launch?.let { LaunchCard(it.id, it.app.name, launchLine.value ?: StreamLoader.greeting(res, it.pc.name),
                    appArt(it.pc, it.app, it.image), it.background, launchLogo, launchParameters) },
                appearance.reducedMotion,
                instantExit = launchExitInstant.value,
                onCancel = ::cancelLaunch,
            )
            ProblemOfferCard(if (shell.offerShown) shell.problem else null, haze, appearance.reducedMotion, onReport = { reportProblem(ReportMode.UPLOAD) }, onSaveToDownloads = { reportProblem(ReportMode.DOWNLOADS) }, onDismiss = ::dismissProblem)
            UpdateOfferCard(
                if (updateShown) updateState else null, haze, appearance.reducedMotion,
                onUpdate = { updates.accept() }, onAllow = updates::openPermissionSettings, onCancel = ::updateBack, onLater = ::updateBack,
            )
            // The second panel of a dual-screen device shows the launch, the portal, the result and the problem too, with the callbacks of these layers
            com.limelight.shell.companion.PublishCompanionOverlays(
                launchParameters, ::cancelLaunch, devices::cancelPortal, ::retryStream, ::dismissResult, { reportProblem(ReportMode.UPLOAD) }, ::dismissProblem,
            )
            ToastHost(shell.toast, onHidden = { shell.hideToast(it) }, onAction = { shell.runToastAction() }, actionPress = shell.toastPress)
        }
    }
}

/** Quick settings of the side menu. */
@Composable
private fun SideMenuRows(
    appearance: Appearance,
    update: ((Appearance) -> Appearance) -> Unit,
    navigation: FocusedListNavigation<String>,
    firstModifier: Modifier,
    openSettings: () -> Unit,
) {
    val res = textResources()
    val themeDark = stringResource(R.string.shell_side_theme_dark)
    val themeLight = stringResource(R.string.shell_side_theme_light)
    SettingRow(Symbols.DARK_MODE, stringResource(R.string.shell_side_theme), RowValue.Choice(if (appearance.dark) themeDark else themeLight, { update { it.copy(dark = !it.dark) } },
        options = listOf(themeDark, themeLight), selected = if (appearance.dark) 0 else 1, onPick = { i -> update { it.copy(dark = i == 0) } }),
        firstModifier.focusedListRow(navigation, "theme"), compact = true)
    SettingRow(Symbols.SYSTEM_COLORS, stringResource(R.string.shell_side_material_you), RowValue.Toggle(appearance.materialYou) { on -> update { it.copy(materialYou = on) } }, Modifier.focusedListRow(navigation, "material-you"), compact = true)
    SettingRow(Symbols.ANIMATION, stringResource(R.string.shell_side_background), RowValue.Choice(appearance.background.label(res), { dir -> update { it.copy(background = it.background.step(dir)) } },
        options = BackgroundKind.entries.map { it.label(res) }, selected = appearance.background.ordinal, onPick = { i -> update { it.copy(background = BackgroundKind.entries[i]) } }),
        Modifier.focusedListRow(navigation, "background"), compact = true)
    SettingRow(Symbols.MUSIC_NOTE, stringResource(R.string.shell_side_sounds), RowValue.Toggle(appearance.sounds) { on -> update { it.copy(sounds = on) } }, Modifier.focusedListRow(navigation, "sounds"), compact = true)
    SettingRow(Symbols.HAPTICS, stringResource(R.string.shell_side_haptics), RowValue.Toggle(appearance.haptics) { on -> update { it.copy(haptics = on) } }, Modifier.focusedListRow(navigation, "haptics"), compact = true)
    SettingRow(Symbols.SETTINGS, stringResource(R.string.shell_side_all_settings), RowValue.Action(openSettings), Modifier.focusedListRow(navigation, "settings"), compact = true)
}

/** The game actions of the shell: start with the launch animation, quit with toasts, no dialogs. */
private class ShellGameActions(
    private val res: Resources,
    private val shell: ShellState,
    private val launcher: GameLauncher,
    private val feedback: Feedback,
    private val scope: CoroutineScope,
) : GameActions {
    private var launches = 0

    override fun start(pc: Pc, app: App, image: ImageBitmap?) {
        if (shell.launch != null) return
        val start = launcher.prepare(pc, app)
        if (start == null) {
            feedback.play(Cue.ERROR)
            shell.showToast(res.getString(R.string.shell_game_unreachable, pc.name), Symbols.WIFI_OFF)
            return
        }
        if (start.virtualDisplayUnavailable) {
            shell.showToast(res.getString(R.string.shell_game_virtual_display_missing, pc.name), Symbols.MONITOR, kind = ToastKind.WARNING)
        }
        shell.diving = false
        shell.launch = LaunchRequest(++launches, pc, app, start.intent, image, HeroBitmaps.latest("${pc.uuid}/${app.id}"))
    }

    override fun quit(pc: Pc, app: App) {
        scope.launch {
            shell.showToast(res.getString(R.string.shell_game_quitting, app.name), Symbols.POWER_SETTINGS_NEW)
            report(launcher.quit(pc, app), pc, app)
        }
    }

    override fun quitAndStart(pc: Pc, running: App, app: App, image: ImageBitmap?) {
        scope.launch {
            shell.showToast(res.getString(R.string.shell_game_quitting, running.name), Symbols.POWER_SETTINGS_NEW)
            val result = launcher.quit(pc, running)
            if (result.success) start(pc, app, image) else report(result, pc, running)
        }
    }

    private fun report(result: GameLauncher.QuitResult, pc: Pc, app: App) {
        if (!result.success) feedback.play(Cue.ERROR)
        shell.showToast(result.text(res, pc.name, app.name), if (result.success) Symbols.CHECK_CIRCLE else Symbols.ERROR)
    }
}

/**
 * The animated background with the focused game's hero. It reads [ShellState.hero] itself, so a new hero (every focus move on the home
 * screen) recomposes just this, not the whole shell.
 */
@Composable
private fun ShellBackdrop(shell: ShellState, appearance: Appearance, colors: DelosColors, heroShown: Boolean, animate: Boolean) {
    Backdrop(appearance, colors, hero = if (heroShown) shell.hero else null, animate = animate)
}

/**
 * The hint bar and the library note. It reads the shell's state itself, so what only changes the hints (the game overview opening
 * or closing, its viewer) recomposes just this, not the whole shell.
 */
@Composable
private fun ShellHints(shell: ShellState, library: Library, pcs: List<Pc>, tracker: FocusTracker, keys: GlobalKeys, uiScale: Float) {
    val res = textResources()
    val metadata = LocalGameMetadata.current
    val note = (library as? Library.Ready)?.takeIf { shell.screen == Screen.Home && shell.layer == null && shell.overviewGame == null }?.let { ready ->
        val shown = if (metadata.filters.active) ready.apps.count { metadata.filters.matches(it, metadata.entry(ready.pc, it).data) } else ready.apps.size
        if (shown == ready.apps.size) res.getQuantityString(R.plurals.shell_home_apps_on_pc, shown, shown, ready.pc.name)
        else res.getQuantityString(R.plurals.shell_home_apps_filtered, ready.apps.size, shown, ready.apps.size, ready.pc.name)
    }
    val filterHint = (library as? Library.Ready)?.takeIf { it.apps.isNotEmpty() }?.let { filterHintLabel(res, metadata.filters) }
    // Written for a controller, shown for the input in use (a finger, a mouse, a keyboard: ui/HintKinds.kt)
    val hintEntries = hints(res, shell, tracker.adjustable, tracker.hasContextMenu, filterHint).adaptedTo(ControllerGlyphs.kind)
    com.limelight.shell.companion.PublishCompanionContext(hintEntries, library, pcs)
    // A tap on the Filter pill (no controller in use) does what Y does: opens the library's filters
    HintBarLayer(shell, hintEntries, note, uiScale) { if (keys.canFilter()) keys.onFilter?.invoke() }
}

/**
 * The hint bar for a window over the shell's own, which covers the shell's bar (the game overview viewer's player window): the
 * viewer's hints, read from the shell's state. Null outside the shell.
 */
internal val LocalWindowHintBar = staticCompositionLocalOf<(@Composable () -> Unit)?> { null }

@Composable
private fun WindowHintBar(shell: ShellState, uiScale: Float) {
    val entries = hints(textResources(), shell, adjustable = false, options = false, filterHint = null).adaptedTo(ControllerGlyphs.kind)
    HintBarLayer(shell, entries, note = null, uiScale, onTap = null)
}

/** The hint bar at the bottom, with the library [note]; it fades out with the game overview viewer's overlay. */
@Composable
private fun HintBarLayer(shell: ShellState, entries: List<Hint>, note: String?, uiScale: Float, onTap: ((Hint) -> Unit)?) {
    val res = textResources()
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        val viewerHintsAlpha by androidx.compose.animation.core.animateFloatAsState(
            if (shell.overviewViewer != null && !shell.overviewViewerControlsVisible) 0f else 1f,
            tween(Motion.FADE, easing = Motion.Ease), label = "viewerHints",
        )
        Box(Modifier.graphicsLayer { alpha = viewerHintsAlpha }) {
            ZoomedBar(uiScale) {
                HintBar(entries, note = note, noteLabel = if (note != null) res.getString(R.string.shell_hint_library_label) else null, onTap = onTap)
            }
        }
    }
}

/** Button hints for the current screen and layer (the prototype's updateHints). */
private fun hints(res: Resources, shell: ShellState, adjustable: Boolean, options: Boolean, filterHint: String?): List<Hint> {
    // [short]: an existing, shorter hint word a cramped (compact) bar tries before it drops the label
    fun hint(glyph: Glyph, @StringRes label: Int, @StringRes short: Int? = null) = Hint(glyph, res.getString(label), short?.let { res.getString(it) })
    val optionsHint = hint(Glyph.MENU_RIGHT, R.string.shell_hint_options).takeIf { options }
    val adjust = if (adjustable) listOf(hint(Glyph.ADJUST, R.string.shell_hint_adjust)) else emptyList()
    shell.portal?.let { return if (it.cancellable) listOf(hint(Glyph.B, R.string.shell_hint_cancel)) else emptyList() }
    if (shell.result != null) return listOf(hint(Glyph.A, R.string.shell_hint_select), hint(Glyph.B, R.string.shell_hint_back_to_library, R.string.shell_hint_back))
    shell.picker?.let { request ->
        return when (request.popup) {
            is RowPopup.Options -> listOf(hint(Glyph.A, R.string.shell_hint_select), hint(Glyph.B, R.string.shell_hint_close))
            is RowPopup.Value -> listOf(hint(Glyph.ADJUST, R.string.shell_hint_adjust), hint(Glyph.A, R.string.shell_hint_done), hint(Glyph.B, R.string.shell_hint_close))
        }
    }
    return when (shell.layer) {
        is Layer.Context -> listOf(hint(Glyph.A, R.string.shell_hint_select), hint(Glyph.B, if (shell.menuPath.isEmpty()) R.string.shell_hint_close else R.string.shell_hint_back))
        Layer.Side -> adjust + listOf(hint(Glyph.A, R.string.shell_hint_change), hint(Glyph.B, R.string.shell_hint_close))
        // Space toggles a chip and Return applies the choices (FilterChip), so the keyboard's hints name those keys
        Layer.Filters -> if (ControllerGlyphs.kind == InputKind.KEYBOARD) listOf(
            Hint(Glyph.Cap(InputCap.SPACE), res.getString(R.string.shell_hint_select)), Hint(Glyph.Cap(InputCap.RETURN), res.getString(R.string.shell_hint_apply)),
            hint(Glyph.B, R.string.shell_hint_close),
        ) else listOf(hint(Glyph.A, R.string.shell_hint_select), hint(Glyph.X, R.string.shell_hint_reset), hint(Glyph.Y, R.string.shell_hint_apply), hint(Glyph.B, R.string.shell_hint_close))
        is Layer.Dialog -> listOf(hint(Glyph.A, R.string.shell_hint_select), hint(Glyph.B, R.string.shell_hint_cancel))
        null -> when (val screen = shell.screen) {
            Screen.Home -> {
                val viewer = shell.overviewViewer
                when {
                    // The game overview (docs/gui/GAME_INFO.md, "Game overview") and its viewer's dock: L1 and R1 browse, on a video
                    // Left/Right seek on the timeline (or change the volume on its slider), A plays or pauses there and on play (else
                    // selects the focused button), Y mutes; on a screenshot Left/Right only walk previous, next and close
                    viewer != null -> {
                        val browse = Hint(Glyph.L1, res.getString(R.string.overview_hint_browse), pair = Glyph.R1)
                        val back = hint(Glyph.B, R.string.shell_hint_back)
                        if (!viewer.video) listOf(hint(Glyph.A, R.string.shell_hint_select), browse, back)
                        else listOfNotNull(
                            when (viewer.target) {
                                ViewerHintTarget.SEEK -> hint(Glyph.DPAD_HORIZONTAL, R.string.overview_hint_seek)
                                ViewerHintTarget.VOLUME -> hint(Glyph.DPAD_HORIZONTAL, R.string.overview_video_volume)
                                else -> null
                            },
                            when (viewer.target) {
                                ViewerHintTarget.SEEK, ViewerHintTarget.PLAY -> hint(Glyph.A, if (viewer.playing) R.string.overview_hint_pause else R.string.overview_hint_play)
                                else -> hint(Glyph.A, R.string.shell_hint_select)
                            },
                            hint(Glyph.Y, if (viewer.muted) R.string.overview_hint_unmute else R.string.overview_hint_mute),
                            browse,
                            back,
                        )
                    }
                    shell.overviewGame != null -> listOf(hint(Glyph.A, R.string.shell_hint_select), hint(Glyph.B, R.string.shell_hint_back))
                    else -> listOfNotNull(
                        // "Game info" is not repeated here: the hint under the focused cover (GameInfoHint) already names it
                        hint(Glyph.A, R.string.shell_hint_start), filterHint?.let { Hint(Glyph.Y, it, tappable = true, icon = Symbols.FILTER) },
                        hint(Glyph.MENU_LEFT, R.string.shell_hint_menu), optionsHint,
                    )
                }
            }
            Screen.Settings -> adjust + listOfNotNull(hint(Glyph.L1, R.string.shell_hint_previous_tab), hint(Glyph.R1, R.string.shell_hint_next_tab), hint(Glyph.Y, R.string.shell_hint_search), hint(Glyph.A, R.string.shell_hint_change), optionsHint, hint(Glyph.B, R.string.shell_hint_back))
            Screen.SettingsSearch -> listOf(hint(Glyph.A, R.string.shell_hint_select), hint(Glyph.B, R.string.shell_hint_back))
            Screen.Start -> listOfNotNull(hint(Glyph.A, R.string.shell_hint_connect), hint(Glyph.MENU_LEFT, R.string.shell_hint_menu), optionsHint)
            is Screen.Boot -> listOfNotNull(hint(Glyph.A, R.string.shell_hint_select), hint(Glyph.B, R.string.shell_hint_back).takeIf { screen.back != null })
            is Screen.CreateProfile -> listOf(hint(Glyph.A, R.string.shell_hint_select), hint(Glyph.B, R.string.shell_hint_back))
            is Screen.PairPc -> listOfNotNull(hint(Glyph.A, R.string.shell_hint_select), hint(Glyph.B, R.string.shell_hint_back), optionsHint)
            is Screen.Pin -> listOf(hint(Glyph.B, R.string.shell_hint_cancel))
            Screen.DebugInfo -> adjust + listOf(hint(Glyph.A, R.string.shell_hint_select), hint(Glyph.B, R.string.shell_hint_back))
            // Like a stream: nothing over the controls
            Screen.OscPreview -> emptyList()
            is Screen.Artwork -> if (BundledArtwork.kind(screen.app) != null) listOf(hint(Glyph.A, R.string.shell_hint_back), hint(Glyph.B, R.string.shell_hint_back))
                else if (shell.artworkSheetOpen) listOf(hint(Glyph.A, R.string.shell_hint_choose), hint(Glyph.X, R.string.shell_hint_reset_filters, R.string.shell_hint_reset), hint(Glyph.Y, R.string.shell_hint_done), hint(Glyph.B, R.string.shell_hint_close))
                else listOf(hint(Glyph.L1, R.string.shell_hint_previous_tab), hint(Glyph.R1, R.string.shell_hint_next_tab), hint(Glyph.A, R.string.shell_hint_use_image, R.string.shell_hint_choose), hint(Glyph.X, R.string.shell_hint_search_name, R.string.shell_hint_search), hint(Glyph.Y, R.string.shell_hint_filter), hint(Glyph.B, R.string.shell_hint_back))
            is Screen.GameSettings -> adjust + listOfNotNull(hint(Glyph.L1, R.string.shell_hint_previous_tab), hint(Glyph.R1, R.string.shell_hint_next_tab), hint(Glyph.A, R.string.shell_hint_change),
                hint(Glyph.X, R.string.shell_hint_reset_to_profile, R.string.shell_hint_reset).takeIf { shell.onGameReset != null }, optionsHint, hint(Glyph.B, R.string.shell_hint_back))
            Screen.Profile, Screen.Devices -> listOf(hint(Glyph.A, R.string.shell_hint_select), hint(Glyph.B, R.string.shell_hint_back), hint(Glyph.MENU_LEFT, R.string.shell_hint_menu))
        }
    }
}

/** How long after the stream window took over the launch overlay is swapped for the exit background (before the system snapshots the stopped window). */
private const val HAND_OVER_DELAY_MS = 200L
