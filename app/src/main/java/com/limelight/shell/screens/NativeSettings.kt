package com.limelight.shell.screens

import android.content.Intent
import android.net.Uri
import android.content.SharedPreferences
import android.content.res.Resources
import java.util.Locale
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.limelight.BuildConfig
import com.limelight.R
import com.limelight.diag.DiagLog
import com.limelight.display.MainScreenChoice
import com.limelight.display.SecondDisplayOverride
import androidx.preference.PreferenceManager
import com.limelight.shell.DialogField
import com.limelight.shell.DialogSpec
import com.limelight.shell.Layer
import com.limelight.shell.LocalFocusTracker
import com.limelight.shell.LocalLibrary
import com.limelight.shell.LocalShell
import com.limelight.shell.data.Library
import com.limelight.shell.data.LocalArtwork
import com.limelight.shell.data.downloadAll
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.limelight.shell.Screen
import com.limelight.shell.StatsPosition
import com.limelight.shell.companion.CompanionDim
import com.limelight.shell.companion.rememberDualScreenPlan
import com.limelight.shell.data.Pc
import com.limelight.shell.settings.Appearance
import com.limelight.shell.settings.BackgroundKind
import com.limelight.shell.settings.ClockFormat
import com.limelight.shell.settings.Palette
import com.limelight.shell.settings.SettingImpact
import com.limelight.shell.settings.SettingSpec
import com.limelight.shell.settings.SettingsTab
import com.limelight.shell.settings.SettingsHelp
import com.limelight.shell.settings.Startup
import com.limelight.shell.settings.OscOpacity
import com.limelight.shell.settings.StreamMenuSettings
import com.limelight.shell.settings.StreamPresetStore
import com.limelight.shell.LocalBackupActions
import com.limelight.utils.ToastKind
import com.limelight.shell.ui.RowValue
import com.limelight.shell.ui.Symbols
import com.limelight.shell.ui.hsl
import com.limelight.shell.ui.systemAccentHue
import com.limelight.shell.ui.textResources
import com.limelight.update.Updates

internal data class SettingsItem(
    val key: String,
    val tab: SettingsTab,
    val section: String,
    val icon: String,
    val label: String,
    val description: String,
    val value: RowValue,
    val enabled: Boolean = true,
    val detail: String = description,
    val searchTerms: String = "",
    val impact: SettingImpact? = null,
)

/** The choice names and the alias words (English and localized, see [SearchAliases]) a setting can be found by, besides its own texts. */
internal fun settingSearchTerms(res: Resources, spec: SettingSpec): String =
    (spec.choices.map { "${it.label} ${SettingsHelp.choice(res, spec.key, it.value)}" } + listOfNotNull(SearchAliases.terms(res, spec.key).ifEmpty { null }))
        .joinToString(" ")

/**
 * The settings whose texts contain every word of [query], in any order and case. Kana, Hangul initials and pinyin initials
 * match too, so the letter picker of Japanese, Korean and Chinese reaches every title (see [SearchText]).
 */
internal fun searchSettings(res: Resources, items: List<SettingsItem>, query: String): List<SettingsItem> {
    val words = query.trim().lowercase(Locale.ROOT).split(Regex("\\s+"))
    val language = res.configuration.locales.get(0).language
    return items.filter { item ->
        val tab = item.tab.label(res)
        val text = "${item.label} ${item.description} ${item.detail} ${item.searchTerms} ${item.section} $tab"
        val matcher = SearchText.matcher(text, "${item.label} ${item.section} $tab", language)
        words.all { matcher.matches(it) }
    }
}

/** Shell preferences and device actions participate in the same search as stream settings. */
@Composable
internal fun nativeSettings(
    a: Appearance, update: ((Appearance) -> Appearance) -> Unit,
    startup: Startup, updateStartup: ((Startup) -> Startup) -> Unit, pcs: List<Pc>,
    /** The current profile's settings (the stream menu shortcut is kept there). */
    settings: SharedPreferences,
): List<SettingsItem> {
    val context = LocalContext.current
    val res = textResources()
    val shell = LocalShell.current
    fun providerWebsite(url: String) {
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
            .onFailure { shell.showToast(res.getString(R.string.settings_native_no_browser_toast), Symbols.ERROR) }
    }
    val systemHue = remember { systemAccentHue(context) }
    val list = mutableListOf<SettingsItem>()
    val interfaceSection = res.getString(R.string.settings_native_section_interface)
    val streamMenuSection = res.getString(R.string.settings_native_section_stream_menu)
    val startupSection = res.getString(R.string.settings_native_section_startup)
    val pairedSection = res.getString(R.string.settings_native_section_paired_pcs)
    val sgdbSection = res.getString(R.string.settings_native_section_steamgriddb)
    val delosSection = res.getString(R.string.settings_native_section_delos)
    val gameDataSection = res.getString(R.string.settings_native_section_game_data)
    /** [choiceHelp] explains the selected choice of a choice row (see [NativeSettingsHelp.choice]). */
    fun row(key: String, icon: String, title: String, value: RowValue, description: String, enabled: Boolean = true,
            tab: SettingsTab = SettingsTab.APPEARANCE, section: String = interfaceSection, choiceHelp: String = "") {
        val help = SettingsHelp.description(res, key, description)
        list += SettingsItem(key, tab, section, icon, title, help, value, enabled,
            NativeSettingsHelp.details(help, value, choiceHelp))
    }
    row("ui_dark", Symbols.DARK_MODE, res.getString(R.string.settings_native_theme_title),
        RowValue.Choice(res.getString(if (a.dark) R.string.settings_native_theme_dark else R.string.settings_native_theme_light), { update { it.copy(dark = !it.dark) } },
            options = listOf(res.getString(R.string.settings_native_theme_dark), res.getString(R.string.settings_native_theme_light)),
            selected = if (a.dark) 0 else 1, onPick = { i -> update { it.copy(dark = i == 0) } }),
        res.getString(R.string.settings_native_theme_description), choiceHelp = NativeSettingsHelp.choice(res, "ui_dark", if (a.dark) "dark" else "light"))
    row("ui_material_you", Symbols.SYSTEM_COLORS, res.getString(R.string.settings_native_material_you_title), RowValue.Toggle(a.materialYou) { on -> update { it.copy(materialYou = on) } },
        if (systemHue != null) res.getString(R.string.settings_native_material_you_description)
        else res.getString(R.string.settings_native_material_you_fallback_description, Palette.MINT.label(res)))
    row("ui_palette", Symbols.PALETTE, res.getString(R.string.settings_native_palette_title), RowValue.Choice(a.palette.label(res), { dir -> update { it.copy(palette = it.palette.step(dir)) } }, hsl(a.palette.accent, 90f, 60f),
        options = Palette.entries.map { it.label(res) }, selected = a.palette.ordinal, onPick = { i -> update { it.copy(palette = Palette.entries[i]) } }),
        if (a.materialYou) res.getString(R.string.settings_native_needs_material_you_off) else res.getString(R.string.settings_native_palette_description), !a.materialYou)
    row("ui_match_game_colors", Symbols.MATCH_COLORS, res.getString(R.string.settings_native_match_colors_title), RowValue.Toggle(a.matchGameColors) { on -> update { it.copy(matchGameColors = on) } },
        if (a.materialYou) res.getString(R.string.settings_native_needs_material_you_off) else res.getString(R.string.settings_native_match_colors_description), !a.materialYou)
    row("ui_clock", Symbols.SCHEDULE, res.getString(R.string.settings_native_clock_title), RowValue.Choice(a.clock.label(res), { dir -> update { it.copy(clock = it.clock.step(dir)) } },
        options = ClockFormat.entries.map { it.label(res) }, selected = a.clock.ordinal, onPick = { i -> update { it.copy(clock = ClockFormat.entries[i]) } }),
        res.getString(R.string.settings_native_clock_description), choiceHelp = NativeSettingsHelp.choice(res, "ui_clock", a.clock.name))
    row("ui_background", Symbols.ANIMATION, res.getString(R.string.settings_native_background_title), RowValue.Choice(a.background.label(res), { dir -> update { it.copy(background = it.background.step(dir)) } },
        options = BackgroundKind.entries.map { it.label(res) }, selected = a.background.ordinal, onPick = { i -> update { it.copy(background = BackgroundKind.entries[i]) } }),
        res.getString(R.string.settings_native_background_description), choiceHelp = NativeSettingsHelp.choice(res, "ui_background", a.background.name))
    row("ui_hero_background", Symbols.WALLPAPER, res.getString(R.string.settings_native_hero_background_title), RowValue.Toggle(a.heroBackground) { on -> update { it.copy(heroBackground = on) } },
        res.getString(R.string.settings_native_hero_background_description))
    row("ui_hero_blur", Symbols.BLUR_ON, res.getString(R.string.settings_native_hero_blur_title), RowValue.Slider(a.heroBlur, 0, 40, 5, " px") { v -> update { it.copy(heroBlur = v) } },
        res.getString(R.string.settings_native_hero_blur_description), a.heroBackground)
    row("ui_hero_opacity", Symbols.OPACITY, res.getString(R.string.settings_native_hero_opacity_title), RowValue.Slider(a.heroOpacity, 0, 100, 5, " %") { v -> update { it.copy(heroOpacity = v) } },
        res.getString(R.string.settings_native_hero_opacity_description), a.heroBackground)
    row("ui_scale", Symbols.FORMAT_SIZE, res.getString(R.string.settings_native_scale_title), RowValue.Slider(a.uiScale, 100, 130, 5, " %") { v -> update { it.copy(uiScale = v) } },
        res.getString(R.string.settings_native_scale_description))
    row("ui_reduced_motion", Symbols.REDUCED_MOTION, res.getString(R.string.settings_native_animations_title),
        RowValue.Choice(res.getString(if (a.reducedMotion) R.string.settings_native_animations_reduced else R.string.settings_native_animations_full), { update { it.copy(reducedMotion = !it.reducedMotion) } },
            options = listOf(res.getString(R.string.settings_native_animations_full), res.getString(R.string.settings_native_animations_reduced)),
            selected = if (a.reducedMotion) 1 else 0, onPick = { i -> update { it.copy(reducedMotion = i == 1) } }),
        res.getString(R.string.settings_native_animations_description), choiceHelp = NativeSettingsHelp.choice(res, "ui_reduced_motion", if (a.reducedMotion) "reduced" else "full"))
    row("ui_haptics", Symbols.HAPTICS, res.getString(R.string.settings_native_haptics_title), RowValue.Toggle(a.haptics) { on -> update { it.copy(haptics = on) } },
        res.getString(R.string.settings_native_haptics_description), tab = SettingsTab.CONTROLS)
    row("ui_sounds", Symbols.MUSIC_NOTE, res.getString(R.string.settings_native_sounds_title), RowValue.Toggle(a.sounds) { on -> update { it.copy(sounds = on) } },
        res.getString(R.string.settings_native_sounds_description), tab = SettingsTab.AUDIO)
    // None by default (a tap of Guide or Back opens the stream menu, issue #102); the list clears them or records new ones
    val menuButtons = StreamMenuSettings.buttons(settings)
    val recordMenuButtons = { shell.buttonCapture = { buttons -> StreamMenuSettings.setButtons(settings, buttons) } }
    row("ui_stream_menu_buttons", Symbols.STREAM_MENU_BUTTONS, res.getString(R.string.settings_native_menu_buttons_title),
        RowValue.Choice(if (menuButtons.isEmpty()) res.getString(R.string.row_not_set) else StreamMenuSettings.label(res, menuButtons), { recordMenuButtons() },
            options = listOf(res.getString(R.string.row_not_set), res.getString(R.string.settings_native_menu_buttons_record)),
            selected = if (menuButtons.isEmpty()) 0 else -1,
            onPick = { i -> if (i == 0) StreamMenuSettings.clearButtons(settings) else recordMenuButtons() }),
        res.getString(R.string.settings_native_menu_buttons_description),
        tab = SettingsTab.CONTROLS, section = streamMenuSection)
    row("ui_stream_menu_swipe", Symbols.SWIPE_UP, res.getString(R.string.settings_native_menu_swipe_title), RowValue.Toggle(StreamMenuSettings.swipe(settings)) { on ->
        settings.edit().putBoolean(StreamMenuSettings.KEY_SWIPE, on).apply()
    }, res.getString(R.string.settings_native_menu_swipe_description),
        tab = SettingsTab.CONTROLS, section = streamMenuSection)
    // The on-screen controls' opacity, the same profile value as the stream menu's More slider; only where there is a touchscreen to use them
    // (the other on-screen controls settings are phone-only, see PhoneOnlySettings)
    if (context.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_TOUCHSCREEN)) {
        row(OscOpacity.KEY, Symbols.OPACITY, res.getString(R.string.settings_native_osc_opacity_title),
            RowValue.Slider(OscOpacity.read(settings), OscOpacity.MIN, OscOpacity.MAX, OscOpacity.STEP, " %") { v -> OscOpacity.write(settings, v) },
            res.getString(R.string.summary_osc_opacity), tab = SettingsTab.CONTROLS, section = res.getString(R.string.category_on_screen_controls_settings))
        val screenshotGesture = com.limelight.utils.ScreenshotGesture.readGesture(settings)
        row(com.limelight.utils.ScreenshotGesture.KEY_GESTURE, Symbols.IMAGE, res.getString(R.string.settings_native_screenshot_gesture_title),
            RowValue.Choice(res.getString(screenshotGesture.labelRes), { dir ->
                com.limelight.utils.ScreenshotGesture.writeGesture(settings, screenshotGesture.step(dir))
            }, options = com.limelight.utils.ScreenshotGesture.entries.map { res.getString(it.labelRes) },
                selected = screenshotGesture.ordinal,
                onPick = { i -> com.limelight.utils.ScreenshotGesture.writeGesture(settings, com.limelight.utils.ScreenshotGesture.entries[i]) }),
            res.getString(R.string.settings_native_screenshot_gesture_description),
            tab = SettingsTab.CONTROLS, section = streamMenuSection)
    }
    row(com.limelight.utils.ScreenshotGesture.KEY_INCLUDE_OVERLAYS, Symbols.PHOTO_LIBRARY, res.getString(R.string.settings_native_screenshot_overlays_title),
        RowValue.Toggle(com.limelight.utils.ScreenshotGesture.readIncludeOverlays(settings)) { on ->
            com.limelight.utils.ScreenshotGesture.writeIncludeOverlays(settings, on)
        }, res.getString(R.string.settings_native_screenshot_overlays_description),
        tab = SettingsTab.CONTROLS, section = streamMenuSection)
    val statsPosition = StatsPosition.read(settings)
    row("ui_stats_position", Symbols.STATS_POSITION, res.getString(R.string.settings_native_stats_position_title),
        RowValue.Choice(res.getString(statsPosition.labelRes), { dir ->
            settings.edit().putString(StatsPosition.KEY, statsPosition.step(dir).name).apply()
        }, options = StatsPosition.entries.map { res.getString(it.labelRes) }, selected = statsPosition.ordinal, onPick = { i ->
            settings.edit().putString(StatsPosition.KEY, StatsPosition.entries[i].name).apply()
        }), res.getString(R.string.settings_native_stats_position_description), tab = SettingsTab.DISPLAY,
        section = res.getString(R.string.settings_native_section_statistics), choiceHelp = NativeSettingsHelp.choice(res, "ui_stats_position", statsPosition.name))
    // Device-wide, not per profile: which role a second screen of THIS device has (docs/DUAL_SCREEN.md)
    val devicePrefs = remember { PreferenceManager.getDefaultSharedPreferences(context.applicationContext) }
    var secondDisplay by remember { mutableStateOf(SecondDisplayOverride.read(devicePrefs)) }
    val dualScreen by rememberDualScreenPlan()
    row(SecondDisplayOverride.KEY, Symbols.EXTERNAL_DISPLAY, res.getString(R.string.settings_native_second_display_title),
        RowValue.Choice(res.getString(secondDisplay.labelRes), { dir ->
            secondDisplay = secondDisplay.step(dir)
            SecondDisplayOverride.write(devicePrefs, secondDisplay)
        }, options = SecondDisplayOverride.entries.map { res.getString(it.labelRes) }, selected = secondDisplay.ordinal, onPick = { i ->
            secondDisplay = SecondDisplayOverride.entries[i]
            SecondDisplayOverride.write(devicePrefs, secondDisplay)
        }), res.getString(R.string.settings_native_second_display_description), tab = SettingsTab.DISPLAY,
        section = res.getString(R.string.settings_native_section_displays), choiceHelp = NativeSettingsHelp.choice(res, SecondDisplayOverride.KEY, secondDisplay.name))
    // Only a device with a built-in second panel has a main screen to choose; the row appears and goes with that display
    if (dualScreen != null) {
        val mainScreen = MainScreenChoice.read(devicePrefs)
        row(MainScreenChoice.KEY, Symbols.MONITOR, res.getString(R.string.settings_native_main_screen_title),
            RowValue.Choice(res.getString(mainScreen.labelRes), { dir -> MainScreenChoice.write(devicePrefs, mainScreen.step(dir)) },
                options = MainScreenChoice.entries.map { res.getString(it.labelRes) }, selected = mainScreen.ordinal,
                onPick = { i -> MainScreenChoice.write(devicePrefs, MainScreenChoice.entries[i]) }),
            res.getString(R.string.settings_native_main_screen_description), tab = SettingsTab.DISPLAY,
            section = res.getString(R.string.settings_native_section_displays), choiceHelp = NativeSettingsHelp.choice(res, MainScreenChoice.KEY, mainScreen.name))
        // The idle dimming of the companion screen (OLED burn-in); the delay only matters while dimming is on
        var dim by remember { mutableStateOf(CompanionDim.read(devicePrefs)) }
        val section = res.getString(R.string.settings_native_section_displays)
        row(CompanionDim.KEY_ENABLED, Symbols.EXTERNAL_DISPLAY, res.getString(R.string.settings_native_companion_dim_title),
            RowValue.Choice(res.getString(if (dim.enabled) R.string.common_on else R.string.common_off), { _ ->
                dim = dim.copy(enabled = !dim.enabled)
                CompanionDim.writeEnabled(devicePrefs, dim.enabled)
            }, options = listOf(res.getString(R.string.common_on), res.getString(R.string.common_off)), selected = if (dim.enabled) 0 else 1, onPick = { i ->
                dim = dim.copy(enabled = i == 0)
                CompanionDim.writeEnabled(devicePrefs, dim.enabled)
            }), res.getString(R.string.settings_native_companion_dim_description), tab = SettingsTab.DISPLAY, section = section,
            choiceHelp = NativeSettingsHelp.choice(res, CompanionDim.KEY_ENABLED, if (dim.enabled) "on" else "off"))
        if (companionDimDelayShown(true, dim)) {
            row(CompanionDim.KEY_MINUTES, Symbols.TIMER, res.getString(R.string.settings_native_companion_dim_minutes_title),
                RowValue.Choice(res.getQuantityString(R.plurals.settings_native_companion_dim_minutes_value, dim.minutes, dim.minutes), { dir ->
                    dim = dim.copy(minutes = CompanionDim.step(dim.minutes, dir))
                    CompanionDim.writeMinutes(devicePrefs, dim.minutes)
                }, options = CompanionDim.MINUTES.map { res.getQuantityString(R.plurals.settings_native_companion_dim_minutes_value, it, it) },
                    selected = CompanionDim.MINUTES.indexOf(dim.minutes), onPick = { i ->
                        dim = dim.copy(minutes = CompanionDim.MINUTES[i])
                        CompanionDim.writeMinutes(devicePrefs, dim.minutes)
                    }), res.getString(R.string.settings_native_companion_dim_minutes_description), tab = SettingsTab.DISPLAY, section = section)
        }
    }
    val choices = listOf<Pc?>(null) + pcs.filter { it.paired }.sortedBy { it.name.lowercase() }
    val index = choices.indexOfFirst { it?.uuid == startup.defaultPc }.coerceAtLeast(0)
    val noDefaultPc = res.getString(R.string.settings_native_default_pc_none)
    row("ui_default_pc", Symbols.STAR, res.getString(R.string.settings_native_default_pc_title), RowValue.Choice(choices[index]?.name ?: noDefaultPc, { dir ->
        val next = choices[Math.floorMod(index + dir, choices.size)]
        updateStartup { it.copy(defaultPc = next?.uuid, skipStart = next != null && it.skipStart) }
    }, options = choices.map { it?.name ?: noDefaultPc }, selected = index, onPick = { i ->
        val next = choices[i]
        updateStartup { it.copy(defaultPc = next?.uuid, skipStart = next != null && it.skipStart) }
    }), res.getString(R.string.settings_native_default_pc_description), choices.size > 1, SettingsTab.DEVICES, startupSection,
        NativeSettingsHelp.choice(res, "ui_default_pc", if (choices[index] == null) "none" else "pc"))
    row("ui_skip_start", Symbols.FAST_FORWARD, res.getString(R.string.settings_native_skip_start_title), RowValue.Toggle(startup.skipStart) { on -> updateStartup { it.copy(skipStart = on) } },
        res.getString(R.string.settings_native_skip_start_description), startup.defaultPc != null, SettingsTab.DEVICES, startupSection)
    row("devices_manage", Symbols.DEVICES, res.getString(R.string.settings_native_manage_devices_title), RowValue.Action { shell.navigate(Screen.Devices) },
        res.getString(R.string.settings_native_manage_devices_description), tab = SettingsTab.DEVICES, section = pairedSection)
    row("devices_pair", Symbols.ADD_LINK, res.getString(R.string.settings_native_pair_title), RowValue.Action { shell.navigate(Screen.PairPc(Screen.Settings)) },
        res.getString(R.string.settings_native_pair_description), tab = SettingsTab.DEVICES, section = pairedSection)
    val artwork = LocalArtwork.current
    val tracker = LocalFocusTracker.current
    val library = LocalLibrary.current as? Library.Ready
    val backupActions = LocalBackupActions.current
    val scope = rememberCoroutineScope()
    // Stream presets: one-tap switching between Home, Balanced, TV, Travel, and custom saved presets
    val presetsSection = res.getString(R.string.settings_native_section_presets)
    var presetVersion by remember(settings) { mutableStateOf(0) }
    val presets = remember(settings, presetVersion) { StreamPresetStore.all(settings) }
    val activePreset = remember(settings, presetVersion, a, startup, pcs) { StreamPresetStore.matchingPreset(settings) }
    val customLabel = res.getString(R.string.settings_native_stream_preset_custom)
    val presetOptions = presets.map { it.name }
    val activePresetIndex = presets.indexOfFirst { it.id == activePreset?.id }
    row("stream_preset", Symbols.TUNE, res.getString(R.string.settings_native_stream_preset_title),
        RowValue.Choice(activePreset?.name ?: customLabel, { dir ->
            if (presets.isNotEmpty()) {
                val base = if (activePresetIndex >= 0) activePresetIndex else if (dir >= 0) -1 else 0
                val next = presets[Math.floorMod(base + dir, presets.size)]
                StreamPresetStore.apply(settings, next)
                presetVersion++
            }
        }, options = presetOptions, selected = activePresetIndex, onPick = { i ->
            presets.getOrNull(i)?.let {
                StreamPresetStore.apply(settings, it)
                presetVersion++
            }
        }), res.getString(R.string.settings_native_stream_preset_description), tab = SettingsTab.STREAM, section = presetsSection)
    row("stream_preset_save", Symbols.STAR, res.getString(R.string.settings_native_stream_preset_save_title), RowValue.Action {
        shell.openLayer(Layer.Dialog(DialogSpec(
            res.getString(R.string.settings_native_stream_preset_save_dialog_title),
            res.getString(R.string.settings_native_stream_preset_save_description),
            listOf(DialogField(Symbols.TUNE, "", res.getString(R.string.settings_native_stream_preset_save_dialog_title), maxLength = 32)),
            res.getString(R.string.firstrun_create_save),
        ) { values ->
            val entered = values.firstOrNull().orEmpty().trim()
            if (entered.isEmpty()) return@DialogSpec false
            val saved = StreamPresetStore.saveCustom(settings, entered)
            presetVersion++
            shell.showToast(res.getString(R.string.settings_native_stream_preset_saved, saved.name), Symbols.CHECK_CIRCLE, kind = ToastKind.SUCCESS)
            true
        }), tracker)
    }, res.getString(R.string.settings_native_stream_preset_save_description), tab = SettingsTab.STREAM, section = presetsSection)
    row("stream_preset_delete", Symbols.DELETE, res.getString(R.string.settings_native_stream_preset_delete_title), RowValue.Action {
        val target = activePreset ?: return@Action
        if (!target.builtIn) {
            StreamPresetStore.deleteCustom(settings, target.id)
            presetVersion++
            shell.showToast(res.getString(R.string.settings_native_stream_preset_deleted, target.name), Symbols.DELETE)
        }
    }, res.getString(R.string.settings_native_stream_preset_delete_description), activePreset != null && !activePreset.builtIn, SettingsTab.STREAM, presetsSection)
    row("covers_sgdb_key", Symbols.KEY, res.getString(R.string.settings_native_sgdb_key_title),
        RowValue.Text(res.getString(if (artwork.enabled) R.string.settings_native_sgdb_key_added else R.string.settings_native_sgdb_key_not_set)) {
        shell.openLayer(Layer.Dialog(DialogSpec(
            res.getString(R.string.settings_native_sgdb_key_title),
            res.getString(R.string.settings_native_sgdb_key_note),
            listOf(DialogField(Symbols.KEY, artwork.apiKey, res.getString(R.string.settings_native_sgdb_key_placeholder), password = true, maxLength = 64)),
            res.getString(R.string.settings_native_sgdb_key_save),
        ) { values ->
            artwork.apiKey = values.first()
            shell.showToast(res.getString(if (artwork.enabled) R.string.settings_native_sgdb_key_saved_toast else R.string.settings_native_sgdb_off_toast), Symbols.CHECK_CIRCLE)
            true
        }), tracker)
    }, res.getString(R.string.settings_native_sgdb_key_description),
        tab = SettingsTab.COVERS, section = sgdbSection)
    row("covers_sgdb_auto", Symbols.CLOUD_DOWNLOAD, res.getString(R.string.settings_native_sgdb_auto_title), RowValue.Toggle(artwork.automatic) { on -> artwork.automatic = on },
        res.getString(R.string.settings_native_sgdb_auto_description),
        artwork.enabled, SettingsTab.COVERS, sgdbSection)
    fun downloadAll(refresh: Boolean) {
        val ready = library ?: return shell.showToast(res.getString(R.string.settings_native_connect_first_toast), Symbols.ERROR)
        scope.launch {
            shell.showToast(res.getString(R.string.settings_native_downloading_toast, ready.pc.name), Symbols.DOWNLOAD)
            var failed = false
            val found = artwork.downloadAll(ready.pc, ready.apps, refresh) {
                failed = true
                shell.showToast(it.text(res), Symbols.ERROR)
            }
            if (!failed) shell.showToast(
                if (found == 0 && !refresh) res.getString(R.string.settings_native_artwork_complete_toast) else res.getQuantityString(R.plurals.settings_native_artwork_ready_toast, found, found),
                Symbols.CHECK_CIRCLE,
            )
        }
    }
    row("covers_sgdb_missing", Symbols.DOWNLOAD, res.getString(R.string.settings_native_missing_artwork_title), RowValue.Action { downloadAll(refresh = false) },
        library?.pc?.name?.let { res.getString(R.string.settings_native_missing_artwork_description, it) } ?: res.getString(R.string.settings_native_missing_artwork_description_current),
        artwork.enabled, SettingsTab.COVERS, sgdbSection)
    row("covers_sgdb_refresh", Symbols.REFRESH, res.getString(R.string.settings_native_refresh_artwork_title), RowValue.Action { downloadAll(refresh = true) },
        res.getString(R.string.settings_native_refresh_artwork_description), artwork.enabled, SettingsTab.COVERS, sgdbSection)
    row("covers_sgdb_choose", Symbols.IMAGE_CHOOSE, res.getString(R.string.settings_native_choose_artwork_title), RowValue.Info(res.getString(R.string.settings_native_choose_artwork_value)),
        res.getString(R.string.settings_native_choose_artwork_description),
        tab = SettingsTab.COVERS, section = sgdbSection)
    row("about_version", Symbols.VERSION, res.getString(R.string.settings_native_version_title), RowValue.Info(BuildConfig.VERSION_NAME), res.getString(R.string.settings_native_version_description), tab = SettingsTab.ABOUT, section = delosSection)
    row("about_based", Symbols.ACCOUNT_TREE, res.getString(R.string.settings_native_based_title), RowValue.Info("Artemis · Moonlight"), // l10n-ignore: product names
        res.getString(R.string.settings_native_based_description), tab = SettingsTab.ABOUT, section = delosSection)
    row("about_welcome", Symbols.REPLAY, res.getString(R.string.settings_native_welcome_title), RowValue.Action { shell.navigate(Screen.Boot(back = Screen.Settings)) },
        res.getString(R.string.settings_native_welcome_description), tab = SettingsTab.ABOUT, section = delosSection)
    val backupSection = res.getString(R.string.settings_native_section_backup)
    row("about_export_backup", Symbols.SHARE_LOG, res.getString(R.string.settings_native_export_backup_title),
        RowValue.Action { backupActions.export() },
        res.getString(R.string.settings_native_export_backup_description), tab = SettingsTab.ABOUT, section = backupSection)
    row("about_import_backup", Symbols.DOWNLOAD, res.getString(R.string.settings_native_import_backup_title),
        RowValue.Action { backupActions.import() },
        res.getString(R.string.settings_native_import_backup_description), tab = SettingsTab.ABOUT, section = backupSection)
    // In-app updates (docs/UPDATES.md): the switch is device-wide and on by default; off means no request at all
    val updateSection = res.getString(R.string.settings_native_section_updates)
    val updateStore = remember { Updates.store(context) }
    val updateController = remember { Updates.controller(context) }
    var updatesOn by remember { mutableStateOf(updateStore.enabled) }
    var updateStatus by remember { mutableStateOf<String?>(null) }
    row("about_update_check", Symbols.CLOUD_DOWNLOAD, res.getString(R.string.settings_native_update_check_title), RowValue.Toggle(updatesOn) { on ->
        updatesOn = on
        updateStore.enabled = on
        updateStatus = null
    }, res.getString(R.string.settings_native_update_check_description), tab = SettingsTab.ABOUT, section = updateSection)
    row("about_update_now", Symbols.REFRESH, res.getString(R.string.settings_native_update_now_title), RowValue.Text(updateStatus ?: res.getString(R.string.settings_native_update_now_value)) {
        updateStatus = res.getString(R.string.settings_native_update_checking)
        scope.launch { updateStatus = updateResultText(res, updateController.checkNow()) }
    }, res.getString(R.string.settings_native_update_now_description), updatesOn, SettingsTab.ABOUT, updateSection)
    if (BuildConfig.DEBUG) {
        // Debug builds never offer an update; with a test server (docs/UPDATES.md "Testing") the flow can be tried against it
        row("about_update_debug_server", Symbols.DEBUG, "Update test server", // l10n-ignore: debug build only
            RowValue.Text(updateStore.debugServer ?: "off") { // l10n-ignore: debug build only
                shell.openLayer(Layer.Dialog(DialogSpec(
                    "Update test server", // l10n-ignore: debug build only
                    "URL of a release JSON, http allowed. Empty turns it off.", // l10n-ignore: debug build only
                    listOf(DialogField(Symbols.DOWNLOAD, updateStore.debugServer.orEmpty(), "http://10.0.2.2:8080/latest", maxLength = 300)), // l10n-ignore: debug build only
                    "Save", // l10n-ignore: debug build only
                ) { values ->
                    updateStore.debugServer = values.first()
                    true
                }), tracker)
            }, "Debug builds only: where the update check asks instead of GitHub.", // l10n-ignore: debug build only
            tab = SettingsTab.ABOUT, section = updateSection)
    }
    var diagnosticLogs by remember { mutableStateOf(DiagLog.enabled) }
    row("about_diag_logs", Symbols.DIAGNOSTIC_LOGS, res.getString(R.string.settings_native_diag_logs_title), RowValue.Toggle(diagnosticLogs) { on ->
        diagnosticLogs = on
        DiagLog.setEnabled(context, on)
    }, res.getString(R.string.settings_native_diag_logs_description),
        tab = SettingsTab.ABOUT, section = res.getString(R.string.settings_native_section_diagnostics))
    row("about_igdb", Symbols.PROVIDER_IGDB, res.getString(R.string.settings_native_igdb_title),
        RowValue.Action { providerWebsite("https://www.igdb.com/") },
        res.getString(R.string.settings_native_igdb_description), tab = SettingsTab.ABOUT, section = gameDataSection)
    row("about_pcgw", Symbols.PROVIDER_PCGW, res.getString(R.string.settings_native_pcgw_title),
        RowValue.Action { providerWebsite("https://www.pcgamingwiki.com/wiki/PCGamingWiki:Copyrights") },
        res.getString(R.string.settings_native_pcgw_description), tab = SettingsTab.ABOUT, section = gameDataSection)
    row("about_steam", Symbols.STEAM, res.getString(R.string.settings_native_steam_title),
        RowValue.Action { providerWebsite("https://store.steampowered.com/") },
        res.getString(R.string.settings_native_steam_description), tab = SettingsTab.ABOUT, section = gameDataSection)
    row("about_hltb", Symbols.COMPLETION_TIME, res.getString(R.string.settings_native_hltb_title),
        RowValue.Action { providerWebsite("https://howlongtobeat.com/") },
        res.getString(R.string.settings_native_hltb_description), tab = SettingsTab.ABOUT, section = gameDataSection)
    return list
}

/** Whether the "Dim after" row shows: only with a companion display, and not while idle dimming is off. */
internal fun companionDimDelayShown(hasCompanion: Boolean, dim: CompanionDim.Policy): Boolean = hasCompanion && dim.enabled
