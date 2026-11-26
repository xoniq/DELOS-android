package com.limelight.shell.screens

import com.limelight.shell.ui.Compact
import com.limelight.shell.ui.FocusSpacing
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Home on a compact stage (`HomeCompact.layout`): the gaps the user asked for hold on every compact stage, the covers keep their ratio and a second game shows. */
class HomeCompactLayoutTest {
    private val c = HomeCompact

    /** The compact stages of the handhelds (docs/HANDHELD_DEVICES.md): Thor bottom, RG DDS top and bottom, Retroid Pocket Mini, a 1:1 panel, Pocket Micro. */
    private val stages = listOf(1099f to 957f, 1488f to 893f, 1191f to 893f, 1108f to 831f, 1051f to 1051f, 1082f to 722f)

    private fun name(stage: Pair<Float, Float>) = "${stage.first.toInt()}x${stage.second.toInt()}"

    /** The interface scales: 1, and the default 1.15 (the bars and the texts are that much larger). */
    private val scales = listOf(1f, 1.15f)

    @Test
    fun theBlocksFollowEachOtherFromTheStatusBarToTheHintBar() {
        for (stage in stages) for (scale in scales) {
            val l = c.layout(stage.first, stage.second, scale)
            val n = "${name(stage)} at $scale"
            assertEquals("$n status bar", Compact.STATUS_BAR * scale, l.statusBottom, .01f)
            assertEquals("$n hint bar", stage.second - Compact.HINT_BAR * scale, l.hintTop, .01f)
            assertTrue("$n order", l.statusBottom <= l.titleTop && l.titleBottom <= l.infoTop && l.infoBottom <= l.coverTop && l.coverBottom <= l.hintTop)
            // The four gaps, the three blocks and the two ring reaches fill the room between the bars exactly
            val used = l.statusGap + l.titleBox + l.titleGap + l.infoHeight + l.chipsGap + 2f * l.ringReach + l.coverHeight + l.hintGap
            assertEquals("$n fills the room", l.hintTop - l.statusBottom, used, .05f)
        }
    }

    @Test
    fun theGapsTheUserAskedFor() {
        for (stage in stages) for (scale in scales) {
            val l = c.layout(stage.first, stage.second, scale)
            val n = "${name(stage)} at $scale"
            assertTrue("$n status bar to title ${l.statusGap}", l.statusGap >= 16f - .05f)
            // The title is centred: the same gap above and under it, and at least 16 to the info row
            assertTrue("$n title to the info row ${l.titleGap}", l.titleGap >= 16f - .05f)
            assertEquals("$n centred", l.statusGap, l.titleGap, .01f)
            assertTrue("$n chips to the focused cover's ring ${l.chipsGap}", l.chipsGap >= 24f - .05f)
            assertTrue("$n ring to the hint bar ${l.hintGap}", l.hintGap >= 28f - .05f)
            // The left-over room is not piled above the title: the gap above the covers and the one under them get four times as much
            assertTrue("$n more above the covers than above the title", l.chipsGap - 24f >= (l.statusGap - 16f) * 4f - .05f)
            assertTrue("$n more under the covers than above the title", l.hintGap - 28f >= (l.statusGap - 16f) * 4f - .05f)
            // The title box holds the title's line and at most the logo's 120
            assertTrue("$n title box ${l.titleBox}", l.titleBox >= 64f * scale - .01f && l.titleBox <= 160f + .01f)
        }
    }

    @Test
    fun theInfoRowIsAsTallAsTheBlockBesideTheBadgesOnEveryHandheldStage() {
        for (stage in stages) for (scale in scales) {
            val l = c.layout(stage.first, stage.second, scale)
            val n = "${name(stage)} at $scale"
            // The block (44 high at scale 1, growing with the texts) sits beside the badges: no row of its own is reserved
            assertTrue("$n beside", !l.blockAbove)
            assertEquals("$n info row", 44f * scale, l.infoHeight, .01f)
        }
    }

    @Test
    fun theBlockHasGenerousAirBeforeTheFirstBadge() {
        // Generous air between the block and the first pill; there is no divider between them
        assertTrue(c.BLOCK_GAP_X in 28f..32f)
    }

    @Test
    fun onAVeryNarrowStageTheBlockStandsAboveTheBadgesAndStillFits() {
        // The block's widest form, 30 and 160 of badges: under that (and the 144 of margins) it goes above
        assertEquals(460f * 1.15f + 30f + 160f, c.besideWidth(1.15f), .01f)
        assertTrue(c.blockAbove(c.besideWidth(1f) - 1f, 1f))
        assertTrue(!c.blockAbove(c.besideWidth(1f), 1f))
        for (scale in scales) {
            val narrow = c.layout(2f * Compact.SIDE + c.besideWidth(scale) - 1f, 1080f, scale)
            val wide = c.layout(2f * Compact.SIDE + c.besideWidth(scale), 1080f, scale)
            assertTrue("above at $scale", narrow.blockAbove && !wide.blockAbove)
            assertEquals(c.infoHeight(scale, true), narrow.infoHeight, .01f)
            assertEquals(44f * scale + c.BLOCK_ABOVE_GAP + c.chipRowHeight(scale), narrow.infoHeight, .01f)
            assertTrue("gaps at $scale: ${narrow.statusGap} ${narrow.chipsGap} ${narrow.hintGap}", narrow.statusGap >= 16f - .05f && narrow.chipsGap >= 24f - .05f && narrow.hintGap >= 28f - .05f)
        }
        // At the default interface scale that is a stage 719 + 144 = 863 wide
        assertEquals(863f, 2f * Compact.SIDE + c.besideWidth(1.15f), .01f)
    }

    @Test
    fun theRingIsTheFocusedCoversGrowthAndItsRing() {
        // A 450 px cover at 1.055 grows 12.375 px up and down; the 10 px ring grows with it
        assertEquals(12.375f + 10.55f, c.ringReach(450f), .001f)
        for (stage in stages) {
            val l = c.layout(stage.first, stage.second)
            assertEquals(c.ringReach(l.coverHeight), l.ringReach, .001f)
        }
    }

    @Test
    fun theCoverIsNeverNeedlesslyHugeAndNeverUnderTheFloor() {
        for (stage in stages) for (scale in scales) {
            val l = c.layout(stage.first, stage.second, scale)
            val n = "${name(stage)} at $scale"
            assertEquals("$n the cover keeps 2:3", 450f * l.coverScale, l.coverHeight, .01f)
            assertTrue("$n at most 0.40 of the height: ${l.coverHeight}", l.coverHeight <= stage.second * .40f + .01f)
            assertTrue("$n not larger than the prototype's", l.coverScale <= 1f)
            // No minimum but the floor: the comfortable gaps come first and the cover takes what is left (the 722-high stage: the floor). Only where
            // the floor and the least gaps do not fit together (the same stage at 1.15, with the streaming-time block's room) the cover gives way, down to the hard floor
            val gapsFit = l.statusGap >= 16f - .05f && l.hintGap >= 28f - .05f && l.chipsGap >= 24f - .05f
            assertTrue("$n at least the floor: ${l.coverScale}", l.coverScale >= c.FLOOR - .0002f || (gapsFit && l.coverScale >= c.HARD_FLOOR))
        }
    }

    @Test
    fun aSecondGameIsAlwaysClearlyThere() {
        for (stage in stages) for (scale in scales) {
            val l = c.layout(stage.first, stage.second, scale)
            val s = l.coverScale
            val tile = c.TILE * s
            // The focused wide cover (its left edge on the side margin, grown), a whole cover and at least a third of the next one are in
            // sight: the cover size is reckoned with the prototype's gaps and a PEEK_EDGE, which the wider focus-rule gaps take back
            val shown = stage.first - (Compact.SIDE + c.WIDE * c.FOCUS_SCALE * s + c.coverGap(s) + tile + c.coverGap(s))
            assertTrue("${name(stage)} at $scale shows $shown of a $tile cover", shown >= tile / 3f - .5f)
            // The focused first cover's ring (10 px beyond its edge) keeps the 48 px margin
            assertTrue("${name(stage)} at $scale ring margin", c.rowStart(s) - c.WIDE * s * (c.FOCUS_SCALE - 1f) - c.ringReach(0f) >= 48f)
        }
    }

    @Test
    fun aFocusedCoverKeepsItsRingOffItsNeighbour() {
        // focusEffects' look: 1.055 and a 10 px ring that grows with the cover; the wide first cover grows to the left only, so a 300 px
        // cover's reach is the one toward a neighbour
        assertEquals(FocusSpacing.TILE, c.COVER.look)
        assertEquals(c.TILE, c.COVER.size, 0f)
        assertEquals(45.6f, c.COVER.minGap, .001f)
        assertTrue(c.COVER.gap >= c.COVER.minGap)
        for (s in listOf(.3f, .45f, .5f, .6f, .7f, .8f, 1f)) {
            assertTrue("scale $s gap ${c.coverGap(s)}", c.coverGap(s) >= c.COVER.look.minGap(c.TILE * s) - .001f)
            assertTrue("scale $s never under the prototype's gap scaled", c.coverGap(s) >= c.COVER.gap * s)
        }
        // The prototype's size: 46; at 0.6 the ring, which does not shrink with the cover, sets it (46 scaled would be 27.6)
        assertEquals(46f, c.coverGap(1f), .001f)
        assertEquals(39f, c.coverGap(.6f), .01f)
    }

    @Test
    fun theNumbersOfTheSixStages() {
        // Cover scale at interface scale 1: the width rule where the stage is tall for its width, the comfortable gaps on the lower ones,
        // the least gaps on the lowest
        val expected = mapOf("1099x957" to .6554f, "1488x893" to .7665f, "1191x893" to .7155f, "1108x831" to .6359f, "1051x1051" to .624f, "1082x722" to .45f)
        for (stage in stages) assertEquals(name(stage), expected.getValue(name(stage)), c.layout(stage.first, stage.second).coverScale, .0005f)
        // The lowest stage's cover is at the floor and the least gaps (and the 64 px title box) are kept
        val low = c.layout(1082f, 722f)
        assertTrue(low.titleBox >= 64f - .1f && low.statusGap >= 16f - .1f && low.chipsGap >= 24f - .1f && low.hintGap >= 28f - .1f)
        // At the default interface scale the covers of the wide and the low stages give way to the comfortable gaps: the sizes they had before the streaming-time block
        assertEquals(.69f, c.layout(1488f, 893f, 1.15f).coverScale, .0005f)
        assertEquals(.5595f, c.layout(1108f, 831f, 1.15f).coverScale, .0005f)
        assertTrue(c.layout(1488f, 893f, 1.15f).chipsGap >= 33f && c.layout(1488f, 893f, 1.15f).hintGap >= 37f)
        // The others give the logo's box its 160 px (a logo up to 140 high), also at the default interface scale
        for (stage in stages.filter { it.second > 800f }) for (scale in scales) assertEquals(name(stage), 160f, c.layout(stage.first, stage.second, scale).titleBox, .01f)
    }

    @Test
    fun aLargerInterfaceScaleGrowsTheBarsAndTheChipsAndStillKeepsTheOrder() {
        for (stage in stages) for (scale in listOf(1.1f, 1.2f, 1.3f)) {
            val l = c.layout(stage.first, stage.second, scale)
            val n = "${name(stage)} at $scale"
            assertEquals("$n status bar", Compact.STATUS_BAR * scale, l.statusBottom, .01f)
            assertTrue("$n order", l.statusBottom <= l.titleTop + .05f && l.titleBottom <= l.infoTop && l.infoBottom <= l.coverTop - l.ringReach + .05f)
            assertTrue("$n the ring stays off the hint bar", l.coverBottom + l.ringReach <= l.hintTop + .05f)
            assertTrue("$n floor", l.coverScale >= c.FLOOR - .0002f || l.coverScale >= c.HARD_FLOOR)
        }
    }

    @Test
    fun theCutoutMovesTheBars() {
        val l = c.layout(1099f, 957f, safeTop = 30f, safeBottom = 20f)
        assertEquals(134f, l.statusBottom, 0f)
        assertEquals(957f - 20f - 92f, l.hintTop, 0f)
        assertTrue(l.statusGap >= 16f && l.hintGap >= 28f)
    }

    @Test
    fun shortWidePhoneStagesUseCompactLayoutAndKeepTwoToThreeCoverRatio() {
        val widePlan = com.limelight.shell.ui.StagePlanner.plan(900f, 405f)
        val wideStage = com.limelight.shell.ui.StageMetrics(
            widePlan.dpPerPx, 1f, widePlan.stageWidth, widePlan.stageHeight, widePlan.shape,
        )
        assertEquals(com.limelight.shell.ui.StageShape.WIDE, widePlan.shape)
        assertTrue(c.usesCompactLayout(wideStage))

        val layout = c.layout(wideStage.stageWidth, wideStage.stageHeight)
        val tileWidth = c.TILE * layout.coverScale
        assertEquals(2f / 3f, tileWidth / layout.coverHeight, 0.001f)
        assertTrue(layout.coverBottom + layout.ringReach <= layout.hintTop - c.GAP_HINT + 0.01f)

        val standardStage = com.limelight.shell.ui.StageMetrics(0.43f, 1f, 1920f, 1080f, com.limelight.shell.ui.StageShape.STANDARD)
        assertTrue(!c.usesCompactLayout(standardStage))
    }
}
