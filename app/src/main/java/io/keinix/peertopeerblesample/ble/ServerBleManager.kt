package io.keinix.peertopeerblesample.ble

import android.bluetooth.*
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import com.google.gson.Gson
import io.keinix.peertopeerblesample.util.BleUuids
import java.nio.charset.Charset
import java.util.*

class ServerBleManager(private val context: Context,
                       private val dataExchangeManager: BleManager.BleDataExchangeManager) {

    private val tag = ServerBleManager::class.java.canonicalName
    private val gson = Gson()

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val adapter: BluetoothAdapter? get() = BluetoothAdapter.getDefaultAdapter()
    private val advertiser get() = adapter?.bluetoothLeAdvertiser

    var gattServer: BluetoothGattServer? = null

    private val readCharacteristic = BluetoothGattCharacteristic(
        UUID.fromString(BleUuids.READ_UUID),
        BluetoothGattCharacteristic.PROPERTY_READ,
        BluetoothGattCharacteristic.PERMISSION_READ
    )

    private val writeCharacteristic = BluetoothGattCharacteristic(
        UUID.fromString(BleUuids.WRITE_UUID),
        BluetoothGattCharacteristic.PROPERTY_WRITE,
        BluetoothGattCharacteristic.PERMISSION_WRITE
    )

    private val bleService = BluetoothGattService(
        UUID.fromString(BleUuids.SERVICE_UUID),
        BluetoothGattService.SERVICE_TYPE_PRIMARY
    ).apply {
        addCharacteristic(readCharacteristic)
        addCharacteristic(writeCharacteristic)
    }

    private val advertiseSettings = AdvertiseSettings.Builder()
        .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
        .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
        .setTimeout(0)
        .setConnectable(true)
        .build()

    private val advertiseData = AdvertiseData.Builder()
        .addServiceUuid(ParcelUuid.fromString(BleUuids.SERVICE_UUID))
        .build()

    private val advertiseCallback = object : AdvertiseCallback() { }

    private val serverCallback = object : BluetoothGattServerCallback() {

        override fun onCharacteristicReadRequest(device: BluetoothDevice?,
                                                 requestId: Int,
                                                 offset: Int,
                                                 characteristic: BluetoothGattCharacteristic?) {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic)
            Log.d(tag, "BLE server received a READ request")
            // send data to remote BLE client
            val bleData = dataExchangeManager.getBleData()
            val stringValue = gson.toJson(bleData)
            val value = stringValue?.toByteArray()
            characteristic?.value = value
            gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value)
        }

        override fun onCharacteristicWriteRequest(device: BluetoothDevice?,
                                                  requestId: Int,
                                                  characteristic: BluetoothGattCharacteristic?,
                                                  preparedWrite: Boolean,
                                                  responseNeeded: Boolean,
                                                  offset: Int,
                                                  value: ByteArray?) {
            super.onCharacteristicWriteRequest(
                device,
                requestId,
                characteristic,
                preparedWrite,
                responseNeeded,
                offset,
                value
            )
            Log.d(tag, "BLE server received a WRITE request")
            // receive data from remote ble client
            characteristic?.value = value
            val bleData = gson.fromJson(characteristic?.getStringValue(offset), BleData::class.java)
            if (bleData != null) dataExchangeManager.onDataReceived(bleData)
            if (responseNeeded) {
                gattServer?.sendResponse(
                    device,
                    requestId,
                    BluetoothGatt.GATT_SUCCESS,
                    offset,
                    null
                )
            }
        }
    }

    fun start() {
        Log.d(tag, "ServerBleManager started")
        openServer()
        startAdvertising()
    }

    fun stop() {
        Log.d(tag, "ServerBleManager stopped")
        stopAdvertising()
        closeServer()
    }

    private fun openServer() {
        if (adapter.isBleOn && gattServer == null) {
            gattServer = bluetoothManager.openGattServer(context, serverCallback)
            gattServer?.addService(bleService)
        }
    }

    private fun closeServer() {
        gattServer?.clearServices()
        gattServer?.close()
        gattServer = null
    }

    private fun startAdvertising() {
        if (adapter.isBleOn) {
            advertiser?.startAdvertising(advertiseSettings, advertiseData, advertiseCallback)
        }
    }

    private fun stopAdvertising() {
        if (adapter.isBleOn) {
            advertiser?.stopAdvertising(advertiseCallback)
        }
    }
}