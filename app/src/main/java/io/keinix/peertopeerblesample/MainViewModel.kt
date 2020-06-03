package io.keinix.peertopeerblesample

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.app.Activity
import android.app.Application
import android.app.Service
import androidx.annotation.RequiresPermission
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.keinix.peertopeerblesample.ble.BleData
import io.keinix.peertopeerblesample.ble.BleManager

/**
 * In this simple example an [AndroidViewModel] is used to prevent ble scanning / advertising
 * from being effected by the [Activity] lifecycle. If your use case requires a single
 * ble scan or advertising period across multiple [Activity]s or a long running background
 * ble tasks, consider using a [Service].
 */
class MainViewModel(app: Application) : AndroidViewModel(app) {

    val isBleStarted get() = bleManager.isStarted

    private val _bleUpdateResultLiveData = MutableLiveData<BleUpdateResult>()
    val bleUpdateResultLiveData: LiveData<BleUpdateResult> = _bleUpdateResultLiveData

    private var totalBleMessageCount = 0
    var lastBleMessage = "--"


    val userId = (0 until 10000).random()

    private val bleDataExchangeManager = object : BleManager.BleDataExchangeManager {
        override fun onDataReceived(data: BleData) {
            totalBleMessageCount++
            _bleUpdateResultLiveData.value = BleUpdateResult(totalBleMessageCount, data.message)
        }

        override fun getBleData(): BleData {
            return BleData(userId, lastBleMessage)
        }
    }

    private val bleManager = BleManager(app, bleDataExchangeManager)

    override fun onCleared() {
        bleManager.stop()
        super.onCleared()
    }

    @RequiresPermission(anyOf = [ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION])
    fun startBle(): LiveData<BleUpdateResult> {
        if (!bleManager.isStarted) bleManager.start()
        return bleUpdateResultLiveData
    }

    fun stopBle() {
        if (bleManager.isStarted) bleManager.stop()
    }

    class BleUpdateResult(val totalMessageCount: Int, val newMessage: String)
}