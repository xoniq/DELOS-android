package com.limelight.utils

import android.content.Context
import android.telephony.CellSignalStrength
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class CellularSignalTest {
    @Test
    fun weakLevelMatchesNonePoorAndModerate() {
        assertTrue(CellularSignal.isWeakLevel(CellSignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN))
        assertTrue(CellularSignal.isWeakLevel(CellSignalStrength.SIGNAL_STRENGTH_POOR))
        assertTrue(CellularSignal.isWeakLevel(CellSignalStrength.SIGNAL_STRENGTH_MODERATE))
        assertFalse(CellularSignal.isWeakLevel(CellSignalStrength.SIGNAL_STRENGTH_GOOD))
        assertFalse(CellularSignal.isWeakLevel(CellSignalStrength.SIGNAL_STRENGTH_GREAT))
    }

    @Test
    fun isWeakReturnsFalseWithoutCellularTransport() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertFalse(CellularSignal.isWeak(context))
    }
}
