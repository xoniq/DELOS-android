package com.limelight.shell.screens

import com.limelight.shell.ui.Compact
import com.limelight.shell.ui.FocusGap
import com.limelight.shell.ui.FocusSpacing
import com.limelight.shell.ui.StageMetrics
import com.limelight.shell.ui.StagePlanner
import com.limelight.shell.ui.compact

/**
 * Home on a compact stage, laid out top-down (issue #9), stage pixels from the stage's top: the status bar, the title (or logo) box,
 * the info row (the badges, scrolling, with the streaming-time block at its right end; the block stands above the badges on a very narrow
 * stage), the covers and the hint bar, with the gaps between them. The title box is centred between the status bar and the info row:
 * [statusGap] and [titleGap] are equal. [HomeCompact.layout] computes it; pure, covered by `HomeCompactLayoutTest` on every compact stage.
 */
internal class HomeCompactLayout(
    /** The covers' scale against the prototype's 300 x 450 (the wide first one 988 x 450). */
    val coverScale: Float,
    val titleTop: Float,
    val titleBox: Float,
    val infoTop: Float,
    val infoHeight: Float,
    /** The streaming-time block does not fit beside the badges: it stands above them, inside [infoHeight]. */
    val blockAbove: Boolean,
    val coverTop: Float,
    val coverHeight: Float,
    /** How far the focused cover's ring reaches above and below the cover. */
    val ringReach: Float,
    val statusBottom: Float,
    val hintTop: Float,
) {
    val titleBottom get() = titleTop + titleBox
    val infoBottom get() = infoTop + infoHeight
    val coverBottom get() = coverTop + coverHeight

    /** The four gaps: status bar to title, title to the info row (equal to the first), info row to the focused cover's ring, that ring to the hint bar. */
    val statusGap get() = titleTop - statusBottom
    val titleGap get() = infoTop - titleBottom
    val chipsGap get() = coverTop - ringReach - infoBottom
    val hintGap get() = hintTop - (coverBottom + ringReach)
}

internal object HomeCompact {
    /**
     * The prototype's cover and the wide first one, which every scale keeps the ratio of: as wide as three covers and the two 44 px
     * gaps they had before the focus rule widened the row's gap ([COVER]).
     */
    const val TILE = 300f
    const val COVER_HEIGHT = 450f
    const val WIDE = 3 * TILE + 2 * 44f

    /**
     * The covers wear [FocusSpacing.TILE] (focusEffects' default: 1.055 and a 10 px ring that grows with them) and stand [COVER] apart
     * at the prototype's size. The focused wide first cover grows to the left only, so toward its neighbour it reaches no further than
     * a 300 px one.
     */
    val COVER = FocusGap(FocusSpacing.TILE, TILE, 46f)
    val FOCUS_SCALE = COVER.look.scale

    /** The least gaps: status bar to title, title to the info row (the same, the title is centred), info row to the focused cover's ring, ring to hint bar. */
    const val GAP_STATUS = 16f
    const val GAP_TITLE = 16f
    const val GAP_CHIPS = 24f
    const val GAP_HINT = 28f

    /**
     * The info row: the streaming-time block (docs/GUI.md) at its start, and the badges, which is an eyebrow line of 14 over a value line
     * of 30 at interface scale 1 and so sets the row's height. The badges (about 37 high) are centred on the block as a whole.
     */
    const val INFO_ROW_HEIGHT = 44f

    /** The air between the block and the first badge (no divider between them), and the least visible room of the badges beside the block. */
    const val BLOCK_GAP_X = 30f
    const val MIN_BADGES_WIDTH = 160f

    /** The widest the full block gets at interface scale 1 (a long Czech value with a relative week); with the two above it decides [blockAbove]. */
    const val BLOCK_WIDEST = 460f

    /** On a very narrow stage the block stands above the badges, this far from them. */
    const val BLOCK_ABOVE_GAP = 12f

    /** The info row at interface scale [scale] (its texts grow with it). */
    fun blockHeight(scale: Float): Float = INFO_ROW_HEIGHT * scale

    /** The room the badges need next to the block's widest form and its gap: under it the block goes above them (last resort; the stage is under 144 + this wide). */
    fun besideWidth(scale: Float): Float = BLOCK_WIDEST * scale + BLOCK_GAP_X + MIN_BADGES_WIDTH

    /** Whether the info area [areaWidth] wide is too narrow for the block beside the badges. */
    fun blockAbove(areaWidth: Float, scale: Float): Boolean = areaWidth < besideWidth(scale)

    /** The height of the info row: the block's, or with the block above the badges the block, [BLOCK_ABOVE_GAP] and a row of badges. */
    fun infoHeight(scale: Float, above: Boolean): Float = blockHeight(scale) + if (above) BLOCK_ABOVE_GAP + chipRowHeight(scale) else 0f

    /** The title box: tall enough for the 60 px title at the least; the comfortable gaps count it [TITLE_MAX] tall, leftover room grows it up to [TITLE_BOX_MAX] (a logo up to 140 high). */
    const val TITLE_MIN = 64f
    const val TITLE_MAX = 120f
    const val TITLE_BOX_MAX = 160f

    /** An info chip is 12 px of padding around a 23 px line of text. */
    const val CHIP_PADDING = 12f
    const val CHIP_LINE = 23f

    /** The gaps above and under the covers that a stage with room to spare gives them before the covers grow further. */
    const val COMFORT_CHIPS = 40f
    const val COMFORT_HINT = 44f

    /**
     * A cover is at most this share of the stage's height. It takes what the comfortable gaps leave and is never under [FLOOR] (where that does
     * not fit the gaps give way to their least).
     */
    const val MAX_HEIGHT_SHARE = .40f
    const val FLOOR = .45f

    /**
     * Where the covers stop shrinking for good. Below [FLOOR] they go only as far as the least gaps need: with the streaming-time block's room
     * reserved, the lowest stage at the default interface scale has not room for the floor and the least gaps (the 722-high Pocket Micro at 1.15).
     */
    const val HARD_FLOOR = .30f

    /** How the room that is left over is shared: a little above and under the title (each), most of it between the info row and the covers and under the covers. */
    const val SHARE_STATUS = .5f
    const val SHARE_CHIPS = 2f
    const val SHARE_HINT = 2f

    /** How far the ring of a focused cover [height] high reaches above and below it. */
    fun ringReach(height: Float): Float = COVER.look.reach(height)

    /**
     * The gap between two covers at [scale]: [COVER]'s scaled with them, but never less than the focus rule's for covers that size
     * (their ring does not shrink with them: 39 px at 0.6, where 46 scaled would be 28).
     */
    fun coverGap(scale: Float): Float = maxOf(COVER.gap * scale, COVER.look.minGap(TILE * scale))

    /** The room two rows of badges took before the info row (the badges used to wrap), 6 px apart: what the comfortable cover size still keeps. */
    fun twoRowsHeight(scale: Float): Float = 2f * chipRowHeight(scale) + 6f

    /** The room one row of chips takes at interface scale [scale]. */
    fun chipRowHeight(scale: Float): Float = CHIP_PADDING + CHIP_LINE * scale

    /** The row's right fade on a compact stage: opaque up to this far from the edge, clear at the least distance (the peeking cover stays readable). */
    const val FADE_SOLID = 72f
    const val FADE_CLEAR = 8f

    /** The peeking cover ends this far from the stage's right edge (inside the fade). */
    const val PEEK_EDGE = 24f

    /**
     * Where the row starts: the focused wide first cover grows to the left only, so its unfocused left edge lies that far right of
     * [Compact.SIDE] and the focused one (with the ring 10 px further out) stands exactly on it.
     */
    fun rowStart(scale: Float): Float = Compact.SIDE + WIDE * scale * (FOCUS_SCALE - 1f)

    /**
     * The widest cover scale at which, next to the focused wide first cover (its left edge [Compact.SIDE] from the stage's edge, grown by
     * [FOCUS_SCALE]), one whole cover and a third of the next one end [PEEK_EDGE] before the stage's right edge, counted with the
     * prototype's gaps ([PEEK_GAP] scaled). The covers keep that size: the focus rule's wider [coverGap] takes its extra from the peek,
     * so the next cover reaches up to about 26 px nearer the edge, still about a third of it in sight.
     */
    fun widthScale(stageWidth: Float): Float = (stageWidth - Compact.SIDE - PEEK_EDGE) / (WIDE * FOCUS_SCALE + 2f * PEEK_GAP + TILE * 4f / 3f)

    /** Whether the home screen needs the top-down responsive layout (compact stages and short wide phone stages under 1080 px). */
    fun usesCompactLayout(stage: StageMetrics): Boolean =
        stage.compact || stage.stageHeight < StagePlanner.STAGE_HEIGHT - 1f

    /** The gap [widthScale] counts with: the prototype's, scaled with the covers. */
    private const val PEEK_GAP = 44f

    /**
     * The layout on a stage [stageWidth] x [stageHeight] at interface scale [scale] (the bars and the texts grow with it), [safeTop] and
     * [safeBottom] being the display cutout. The cover height is the smallest of what the height leaves after the comfortable gaps (after
     * the least gaps below [FLOOR]), 0.40 of the stage's height and [widthScale]; what is left over then widens the
     * title box (up to [TITLE_BOX_MAX]) and is shared between the gaps by [SHARE_STATUS] (above and under the title, each) : [SHARE_CHIPS] : [SHARE_HINT].
     */
    fun layout(stageWidth: Float, stageHeight: Float, scale: Float = 1f, safeTop: Float = 0f, safeBottom: Float = 0f): HomeCompactLayout {
        val statusBottom = safeTop + Compact.STATUS_BAR * scale
        val hintTop = stageHeight - safeBottom - Compact.HINT_BAR * scale
        val room = hintTop - statusBottom
        val above = blockAbove(stageWidth - 2f * Compact.SIDE, scale)
        val info = infoHeight(scale, above)
        val titleMin = TITLE_MIN * scale
        val titleMax = maxOf(TITLE_MAX, titleMin)
        // Everything but the cover and the part of its ring that grows with it
        val fixed = GAP_STATUS + titleMin + GAP_TITLE + info + GAP_CHIPS + GAP_HINT + 2f * ringReach(0f)
        val byHeight = (room - fixed) / FOCUS_SCALE / COVER_HEIGHT
        // The same with the logo's comfortable box and comfortable gaps, still counting the two rows the badges took before the info row (the covers keep their
        // size; the room the single row saves goes to the logo's box and the gaps): what the cover gets while that is not under the minimum
        val comfortable = GAP_STATUS + titleMax + GAP_TITLE + maxOf(info, twoRowsHeight(scale)) + COMFORT_CHIPS + COMFORT_HINT + 2f * ringReach(0f)
        val byComfort = (room - comfortable) / FOCUS_SCALE / COVER_HEIGHT
        val byShare = stageHeight * MAX_HEIGHT_SHARE / COVER_HEIGHT
        val coverScale = minOf(byShare, widthScale(stageWidth), byComfort).coerceAtLeast(minOf(FLOOR, byHeight).coerceAtLeast(HARD_FLOOR))
        val cover = COVER_HEIGHT * coverScale
        val reach = ringReach(cover)
        var left = room - (GAP_STATUS + titleMin + GAP_TITLE + info + GAP_CHIPS + cover + 2f * reach + GAP_HINT)
        var statusGap = GAP_STATUS
        var titleGap = GAP_TITLE
        var chipsGap = GAP_CHIPS
        var hintGap = GAP_HINT
        var title = titleMin
        if (left > 0f) {
            val grow = minOf(left, maxOf(TITLE_BOX_MAX, titleMin) - titleMin)
            title += grow
            left -= grow
            // The title stays centred: the gap above it and the one under it grow by the same
            val shares = 2f * SHARE_STATUS + SHARE_CHIPS + SHARE_HINT
            statusGap += left * SHARE_STATUS / shares
            titleGap += left * SHARE_STATUS / shares
            chipsGap += left * SHARE_CHIPS / shares
            hintGap += left * SHARE_HINT / shares
        } else if (left < 0f) {
            // Not even the floor fits (a tiny stage at a large interface scale): the gaps give way, the one above the hint bar first,
            // then the two round the title together, then the one above the covers
            var over = -left
            fun give(gap: Float): Float {
                val cut = minOf(over, gap)
                over -= cut
                return gap - cut
            }
            hintGap = give(hintGap)
            val pair = give(statusGap + titleGap) / 2f
            statusGap = pair
            titleGap = pair
            chipsGap = give(chipsGap)
        }
        val titleTop = statusBottom + statusGap
        val infoTop = titleTop + title + titleGap
        val coverTop = infoTop + info + chipsGap + reach
        return HomeCompactLayout(coverScale, titleTop, title, infoTop, info, above, coverTop, cover, reach, statusBottom, hintTop)
    }
}
