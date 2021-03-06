package io.keinix.peertopeerblesample.ble

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.util.Log
import androidx.annotation.RequiresPermission
import io.keinix.peertopeerblesample.util.struct.ExpirationSet

val BluetoothAdapter?.isBleOn get() = this != null && isEnabled

/**
 * Manages BLE connections and operations from this device to other devices with this
 * application installed. A single device can act as both a BLE client and server
 * at the same time.
 */
class BleManager(context: Context, dataExchangeManager: BleDataExchangeManager) {

    var isStarted = false
    private val tag = BleManager::class.java.canonicalName
    private val adapter get() = BluetoothAdapter.getDefaultAdapter()

    // BLE Callbacks are executed on a binder thread. This handlers gets the work
    // off that binder thread as soon as possible and back onto the thread the
    // dataExchangeManager passed to this class expects.
    private val handler = Handler()

    // Limit the rate at which bleData is parsed per user
    private val userIds = ExpirationSet<Int>(25000, 30)

    // These methods will be called from inside a BLE callback
    private val rateLimitedDataExchangeManager = object : BleDataExchangeManager {
        override fun onDataReceived(data: BleData) {
            if (userIds.add(data.userId)) dataExchangeManager.onDataReceived(data)
        }

        override fun getBleData(): BleData = dataExchangeManager.getBleData()
    }

    private val clientManager = ClientBleManager(context, rateLimitedDataExchangeManager, handler)
    private val serverManager = ServerBleManager(context, rateLimitedDataExchangeManager, handler)

    private val canBeClient: Boolean = adapter != null &&
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)

    // Bluetooth must currently be turned on for the check to succeed
    private val canBeServer: Boolean
        get() = adapter.bluetoothLeAdvertiser != null

    @RequiresPermission(anyOf = [ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION])
    fun start() {
        Log.d(tag, "BleManager started")
        if (canBeClient) clientManager.start()
        if (canBeServer) serverManager.start()
        isStarted = true
    }

    fun stop() {
        Log.d(tag, "BleManager stopped")
        if (canBeClient) clientManager.stop()
        if (canBeServer) serverManager.stop()
        userIds.clear()
        isStarted = false
    }

    /**
     * Methods used for swapping data between a remote BLE client or server
     */
    interface BleDataExchangeManager {

        /**
         * @param data received from a remote BLE device
         */
        fun onDataReceived(data: BleData)

        /**
         * @return data that should be sent to a remote BLE device
         */
        fun getBleData(): BleData
    }
}