package com.limelight.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import android.telephony.CellInfo
import android.telephony.CellSignalStrength
import android.telephony.CellSignalStrengthLte
import android.telephony.SignalStrength
import android.telephony.SubscriptionManager
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay

/**
 * Detects whether the device is actively streaming over a cellular connection and reads the
 * effective signal level (0..4 bars) of the active data SIM without requiring runtime permissions.
 */
object CellularSignal {
    @JvmStatic
    fun level(context: Context): Int? {
        val connMgr = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return null
        val network = connMgr.activeNetwork ?: return null
        val caps = connMgr.getNetworkCapabilities(network) ?: return null
        val isCellular = caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
            (caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) &&
                !caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) &&
                connMgr.isActiveNetworkMetered)
        if (!isCellular) return null

        val telephony = activeDataTelephony(context) ?: return null
        val signalStrength = runCatching { telephony.signalStrength }.getOrNull() ?: return null
        return effectiveLevel(signalStrength)
    }

    @JvmStatic
    fun isWeak(context: Context): Boolean {
        val bars = level(context) ?: return false
        return isWeakLevel(bars)
    }

    @JvmStatic
    fun isWeakLevel(level: Int): Boolean = level <= CellSignalStrength.SIGNAL_STRENGTH_MODERATE

    internal fun effectiveLevel(signalStrength: SignalStrength): Int {
        val strengths = signalStrength.cellSignalStrengths
        if (strengths.isEmpty()) return signalStrength.level
        val levels = strengths.map { strength ->
            var lvl = strength.level
            // On Samsung/5G NSA devices, LTE RSRP <= -105 dBm displays as <= 2 bars even when AOSP reports level 3
            if (strength is CellSignalStrengthLte &&
                strength.rsrp != CellInfo.UNAVAILABLE &&
                strength.rsrp <= -105 &&
                lvl > CellSignalStrength.SIGNAL_STRENGTH_MODERATE
            ) {
                lvl = CellSignalStrength.SIGNAL_STRENGTH_MODERATE
            }
            lvl
        }
        return levels.minOrNull() ?: signalStrength.level
    }

    internal fun activeDataTelephony(context: Context): TelephonyManager? {
        val base = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager ?: return null
        val dataSubId = SubscriptionManager.getDefaultDataSubscriptionId()
        return if (dataSubId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            runCatching { base.createForSubscriptionId(dataSubId) }.getOrDefault(base)
        } else {
            base
        }
    }
}

@Composable
fun rememberCellularSignalLevel(): Int? {
    val context = LocalContext.current.applicationContext
    var level by remember { mutableStateOf(CellularSignal.level(context)) }
    DisposableEffect(context) {
        val connMgr = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val telephony = CellularSignal.activeDataTelephony(context)

        val netCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                level = CellularSignal.level(context)
            }

            override fun onLost(network: Network) {
                level = CellularSignal.level(context)
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                level = CellularSignal.level(context)
            }
        }
        val netRegistered = connMgr != null && runCatching {
            connMgr.registerDefaultNetworkCallback(netCallback)
            true
        }.getOrDefault(false)

        var telCallback: Any? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && telephony != null) {
            telCallback = registerSignalListener(context, telephony) {
                level = CellularSignal.level(context)
            }
        }

        onDispose {
            if (netRegistered) runCatching { connMgr.unregisterNetworkCallback(netCallback) }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && telephony != null && telCallback != null) {
                unregisterSignalListener(telephony, telCallback)
            }
        }
    }
    LaunchedEffect(context) {
        while (true) {
            delay(3_000L)
            level = CellularSignal.level(context)
        }
    }
    return level
}

@Composable
fun rememberWeakMobileSignal(): Boolean {
    val level = rememberCellularSignalLevel()
    return level != null && CellularSignal.isWeakLevel(level)
}

@RequiresApi(Build.VERSION_CODES.S)
private fun registerSignalListener(
    context: Context,
    telephony: TelephonyManager,
    onUpdate: () -> Unit,
): Any? {
    val cb = object : TelephonyCallback(), TelephonyCallback.SignalStrengthsListener {
        override fun onSignalStrengthsChanged(signalStrength: SignalStrength) {
            onUpdate()
        }
    }
    return if (runCatching {
            telephony.registerTelephonyCallback(context.mainExecutor, cb)
            true
        }.getOrDefault(false)
    ) cb else null
}

@RequiresApi(Build.VERSION_CODES.S)
private fun unregisterSignalListener(telephony: TelephonyManager, callback: Any) {
    runCatching {
        telephony.unregisterTelephonyCallback(callback as TelephonyCallback)
    }
}
