package io.keinix.peertopeerblesample.ble

import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Handler
import android.os.ParcelUuid
import android.util.Log
import com.google.gson.Gson
import io.keinix.peertopeerblesample.util.BleUuids
import io.keinix.peertopeerblesample.util.struct.ExpirationSet
import io.keinix.peertopeerblesample.util.struct.OperationQueue
import java.util.*

class ClientBleManager(private val context: Context,
                       dataExchangeManager: BleManager.BleDataExchangeManager,
                       private val callbackHandler: Handler) {

    private val gson = Gson()

    // Limit the connections attempts for a device to once every 30 seconds
    private val deviceAddress = ExpirationSet<String>(30000, 30)

    // Used to execute ble operation in sequence across one or
    // multiple device. All BLE callbacks should call OperationQueue#operationComplete
    private val operationQueue = OperationQueue()

    // references of the connected servers are kept so they can by manually
    // disconnected if this manager is stopped
    private val connectedGattServers = mutableListOf<BluetoothGatt>()

    private val adapter: BluetoothAdapter? get() = BluetoothAdapter.getDefaultAdapter()
    private val scanner get() = adapter?.bluetoothLeScanner

    private val scanFilters = ScanFilter.Builder()
        .setServiceUuid(ParcelUuid.fromString(BleUuids.SERVICE_UUID))
        .build()
        .let { listOf(it) }

    private val scanSettings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
        .setNumOfMatches(ScanSettings.MATCH_NUM_ONE_ADVERTISEMENT)
        .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
        .build()

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            val device = result?.device ?: return
            if (deviceAddress.add(device.address)) {
                operationQueue.execute {
                    device.connectGatt(
                        context,
                        false,
                        gattCallback,
                        BluetoothDevice.TRANSPORT_LE
                    )
                }
            }
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            operationQueue.operationComplete()
            when {
                status != BluetoothGatt.GATT_SUCCESS -> {
                    if (gatt?.device != null)  {
                        deviceAddress.remove(gatt.device.address) // Used for rate limiting
                        when(newState) {
                            BluetoothProfile.STATE_DISCONNECTED -> gatt.close()
                            else -> gatt.disconnect()
                        }
                    }
                }
                newState == BluetoothProfile.STATE_CONNECTED -> {
                    if (gatt != null) {
                        connectedGattServers.add(gatt)
                        operationQueue.execute { gatt.requestMtu(512) }
                    }
                }
                newState == BluetoothProfile.STATE_DISCONNECTED -> {
                    if (gatt != null) {
                        connectedGattServers.remove(gatt)
                        gatt.close()
                    }
                }
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
            super.onMtuChanged(gatt, mtu, status)
            operationQueue.operationComplete()
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (gatt != null) operationQueue.execute { gatt.discoverServices() }
            } else {
                // You could retry here
                gatt?.disconnect()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)
            operationQueue.operationComplete()
            if (status == BluetoothGatt.GATT_SUCCESS) {
                // request data from remote BLE server
                if (gatt != null) operationQueue.execute { gatt.readData() }
            } else {
                // You could retry here
                gatt?.disconnect()
            }
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt?,
                                          characteristic: BluetoothGattCharacteristic?,
                                          status: Int) {
            super.onCharacteristicRead(gatt, characteristic, status)
            operationQueue.operationComplete()
            if (status == BluetoothGatt.GATT_SUCCESS) {
                // data received from remote BLE server
                val stringValue = characteristic?.getStringValue(0)
                callbackHandler.post {
                    stringValue
                        ?.let { string -> gson.fromJson(string, BleData::class.java) }
                        ?.let { bleData -> dataExchangeManager.onDataReceived(bleData) }
                }
                if (gatt != null)  {
                    // send data to remote BLE server
                    operationQueue.execute { gatt.writeData(dataExchangeManager.getBleData()) }
                }
            } else {
                // You could retry here
                gatt?.disconnect()
            }
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt?,
                                           characteristic: BluetoothGattCharacteristic?,
                                           status: Int) {
            super.onCharacteristicWrite(gatt, characteristic, status)
            operationQueue.operationComplete()
            // data sent to remote BLE server. One-off data exchange is complete
            gatt?.disconnect()
        }
    }

    fun start() {
        if (adapter.isBleOn) {
            scanner?.startScan(scanFilters, scanSettings, scanCallback)
        }
    }

    fun stop() {
        if (adapter.isBleOn) {
            scanner?.stopScan(scanCallback)
            connectedGattServers.forEach { it.disconnect() }
        }
        operationQueue.clear()
        deviceAddress.clear()
    }

    // Data will be received in BluetoothGattCallback#onCharacteristicRead
    private fun BluetoothGatt.readData() = readCharacteristic(readCharacteristic)

    private fun BluetoothGatt.writeData(data: BleData) {
        val value = gson.toJson(data)
        writeCharacteristic.let { characteristic ->
            characteristic.setValue(value)
            writeCharacteristic(characteristic)
        }
    }

    private val BluetoothGatt.writeCharacteristic
        get() = getService(UUID.fromString(BleUuids.SERVICE_UUID))
            .getCharacteristic(UUID.fromString(BleUuids.WRITE_UUID))

    private val BluetoothGatt.readCharacteristic
        get() = getService(UUID.fromString(BleUuids.SERVICE_UUID))
            .getCharacteristic(UUID.fromString(BleUuids.READ_UUID))
}